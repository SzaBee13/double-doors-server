import {execFileSync} from 'node:child_process';
import {mkdirSync, readFileSync, rmSync, writeFileSync} from 'node:fs';
import {join, resolve} from 'node:path';

const website = resolve(import.meta.dirname, '..');
const repository = resolve(website, '..');
const wiki = join(repository, 'wiki');
const manifest = JSON.parse(readFileSync(join(website, 'versions-migration.json'), 'utf8'));
function readWikiFile(commit, file) {
  return execFileSync('git', ['-C', wiki, 'show', `${commit}:${file}`], {encoding: 'utf8'});
}

function getPages(commit) {
  return execFileSync('git', ['-C', wiki, 'ls-tree', '-r', '--name-only', commit], {encoding: 'utf8'})
    .trim()
    .split('\n')
    .filter((file) => file.endsWith('.md') && !file.startsWith('_'));
}

function convertLinks(markdown) {
  return markdown
    .replace(/\[\[([^]|]+)\|([^]]+)\]\]/g, '[$1](./$2)')
    .replace(/\[\[([^]]+)\]\]/g, '[$1](./$1)')
    .replace(/\]\((?!https?:\/\/|#|\/|\.\/)([^)]+)\)/g, (match, target) => `](./${target.replace(/\.md$/, '')})`)
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;');
}

function convertPage(commit, file) {
  const id = file.slice(0, -'.md'.length);
  const source = readWikiFile(commit, file).replace(/^#\s+[^\n]+\n+/, '');
  const title = id.replace(/-/g, ' ');
  const slug = id === 'Home' ? '/' : `/${id}`;
  return `---\ntitle: ${title}\nid: ${id}\nslug: ${slug}\n---\n\n${convertLinks(source).trim()}\n`;
}

function writeVersion(version, commit) {
  const output = join(website, 'versioned_docs', `version-${version}`);
  rmSync(output, {recursive: true, force: true});
  mkdirSync(output, {recursive: true});
  const pages = getPages(commit);
  for (const file of pages) {
    writeFileSync(join(output, file), convertPage(commit, file));
  }
  return pages.map((file) => file.slice(0, -'.md'.length));
}

const current = writeVersion('1.4', manifest['1.4']);
const legacy = writeVersion('1.3', manifest['1.3']);
rmSync(join(website, 'docs'), {recursive: true, force: true});
mkdirSync(join(website, 'docs'), {recursive: true});
for (const id of current) {
  writeFileSync(join(website, 'docs', `${id}.md`), readFileSync(join(website, 'versioned_docs', 'version-1.4', `${id}.md`)));
}
writeFileSync(join(website, 'versions.json'), `${JSON.stringify(['1.4', '1.3'], null, 2)}\n`);

mkdirSync(join(website, 'versioned_sidebars'), {recursive: true});
writeFileSync(join(website, 'versioned_sidebars', 'version-1.4-sidebars.json'), `${JSON.stringify({docsSidebar: current}, null, 2)}\n`);
writeFileSync(join(website, 'versioned_sidebars', 'version-1.3-sidebars.json'), `${JSON.stringify({docsSidebar: legacy}, null, 2)}\n`);
