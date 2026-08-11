import Link from '@docusaurus/Link';
import Layout from '@theme/Layout';
import GithubIcon from '@site/src/components/GithubIcon';
import {useEffect, useState} from 'react';
import type {ReactNode} from 'react';
import ReactMarkdown from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import remarkGfm from 'remark-gfm';
import type {Components} from 'react-markdown';

const REPO = 'SzaBee13/double-doors-server';
const RELEASES_API = `https://api.github.com/repos/${REPO}/releases?per_page=15`;
const MODRINTH_DOWNLOAD = 'https://modrinth.com/plugin/double-doors-server#download';

type GitHubRelease = {
  tag_name: string;
  name: string;
  body: string;
  html_url: string;
  published_at: string;
  prerelease: boolean;
  draft: boolean;
};

type LoadState = {status: 'loading'} | {status: 'error'; message: string} | {status: 'ready'; releases: GitHubRelease[]};

const MARKDOWN_COMPONENTS: Components = {
  h1: ({children}) => <h2 className="dd-release-note__title">{children}</h2>,
  a: ({children, href}) => (
    <a href={href} target="_blank" rel="noreferrer">
      {children}
    </a>
  ),
};

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

export default function Releases(): ReactNode {
  const [state, setState] = useState<LoadState>({status: 'loading'});
  const [expanded, setExpanded] = useState<ReadonlySet<string>>(new Set());

  useEffect(() => {
    let cancelled = false;

    async function load(): Promise<void> {
      try {
        const response = await fetch(RELEASES_API);
        if (!response.ok) {
          throw new Error(`GitHub API responded with ${response.status}`);
        }
        const data = (await response.json()) as GitHubRelease[];
        const releases = data.filter((release) => !release.draft);
        if (!cancelled) {
          setState({status: 'ready', releases});
          setExpanded(new Set(releases.length > 0 ? [releases[0].tag_name] : []));
        }
      } catch (error) {
        if (!cancelled) {
          setState({
            status: 'error',
            message: error instanceof Error ? error.message : 'Failed to load releases',
          });
        }
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  function toggleRelease(tag: string): void {
    setExpanded((current) => {
      const next = new Set(current);
      if (next.has(tag)) {
        next.delete(tag);
      } else {
        next.add(tag);
      }
      return next;
    });
  }

  return (
    <Layout
      title="Releases"
      description="Release notes for every DoubleDoors version, powered by the GitHub API."
    >
      <main className="dd-main dd-main--releases">
        <section className="dd-release-hero">
          <div className="dd-container">
            <p className="dd-section__eyebrow">Changelog</p>
            <h1 className="dd-release-hero__title">Releases</h1>
            <p className="dd-release-hero__description">
              Release notes for every DoubleDoors version, fetched live from GitHub.
            </p>
            <Link className="dd-btn dd-btn--primary" to="/download">
              Download on Modrinth
            </Link>
          </div>
        </section>

        <section className="dd-section dd-section--releases">
          <div className="dd-container">
            {state.status === 'loading' && (
              <div className="dd-release-state" role="status">
                <span className="dd-spinner" aria-hidden="true" />
                <p>Loading release notes from GitHub…</p>
              </div>
            )}

            {state.status === 'error' && (
              <div className="dd-release-state dd-release-state--error" role="alert">
                <p className="dd-release-state__title">Could not load releases</p>
                <p className="dd-release-state__message">{state.message}</p>
                <p className="dd-release-state__hint">
                  You can still read the latest notes directly on{' '}
                  <a href={`https://github.com/${REPO}/releases`} target="_blank" rel="noreferrer">
                    GitHub
                  </a>
                  .
                </p>
              </div>
            )}

            {state.status === 'ready' && (
              <div className="dd-releases">
                {state.releases.map((release, index) => {
                  const isExpanded = expanded.has(release.tag_name);
                  return (
                    <article
                      className={`dd-release${index === 0 ? ' dd-release--latest' : ''}${release.prerelease ? ' dd-release--prerelease' : ''}`}
                      key={release.tag_name}
                    >
                      <header className="dd-release__header">
                        <div className="dd-release__heading">
                          <div className="dd-release__title-row">
                            <h2 className="dd-release__version">{release.name || release.tag_name}</h2>
                            {index === 0 && <span className="dd-release__badge dd-release__badge--latest">Latest</span>}
                            {release.prerelease && (
                              <span className="dd-release__badge dd-release__badge--prerelease">Pre-release</span>
                            )}
                          </div>
                          <p className="dd-release__date">{formatDate(release.published_at)}</p>
                        </div>
                        <div className="dd-release__links">
                          <a
                            className="dd-btn dd-btn--small dd-btn--primary"
                            href={MODRINTH_DOWNLOAD}
                            target="_blank"
                            rel="noreferrer"
                          >
                            Download
                          </a>
                          <a
                            className="dd-btn dd-btn--small dd-btn--ghost"
                            href={release.html_url}
                            target="_blank"
                            rel="noreferrer"
                          >
                            <GithubIcon size={14} />
                            GitHub
                          </a>
                          <button
                            type="button"
                            className="dd-btn dd-btn--small dd-btn--ghost dd-release__toggle"
                            aria-expanded={isExpanded}
                            onClick={() => toggleRelease(release.tag_name)}
                          >
                            {isExpanded ? 'Hide release notes' : 'Show release notes'}
                          </button>
                        </div>
                      </header>
                      {isExpanded && (
                        <div className="dd-release-note">
                          <ReactMarkdown
                            remarkPlugins={[remarkGfm]}
                            rehypePlugins={[rehypeRaw]}
                            components={MARKDOWN_COMPONENTS}
                          >
                            {release.body}
                          </ReactMarkdown>
                        </div>
                      )}
                    </article>
                  );
                })}
              </div>
            )}
          </div>
        </section>
      </main>
    </Layout>
  );
}
