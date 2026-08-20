import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
  title: 'DoubleDoors',
  tagline: 'Synchronized doors and door-like blocks for Minecraft servers',
  favicon: 'img/favicon.ico',
  url: 'https://doubledoors.szabee.me',
  baseUrl: '/',
  organizationName: 'SzaBee13',
  projectName: 'double-doors-server',
  onBrokenLinks: 'throw',
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'throw',
    },
  },
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },
  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          routeBasePath: 'docs',
          lastVersion: '1.4',
          onlyIncludeVersions: ['1.4', '1.3'],
          includeCurrentVersion: false,
          versions: {
            '1.4': {label: '1.4', path: '1.4'},
            '1.3': {label: '1.3', path: '1.3'},
          },
          editUrl: 'https://github.com/SzaBee13/double-doors-server/edit/dev/website/',
        },
        blog: false,
        sitemap: {},
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],
  themeConfig: {
    image: 'img/logo.png',
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'DoubleDoors',
      logo: {
        alt: 'DoubleDoors logo',
        src: 'img/logo.png',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: 'Documentation',
        },
        {
          to: '/releases',
          position: 'left',
          label: 'Releases',
        },
        {
          to: '/download',
          position: 'right',
          label: 'Download',
        },
        {
          type: 'docsVersionDropdown',
          position: 'right',
        },
        {
          type: 'html',
          position: 'right',
          value:
            '<a class="navbar__link navbar__link--github" href="https://github.com/SzaBee13/double-doors-server" target="_blank" rel="noreferrer" aria-label="DoubleDoors on GitHub">' +
            '<img class="dd-github-icon dd-github-icon--light" src="/img/github/GitHub_Invertocat_Black.svg" alt="" width="16" height="16" aria-hidden="true" />' +
            '<img class="dd-github-icon dd-github-icon--dark" src="/img/github/GitHub_Invertocat_White.svg" alt="" width="16" height="16" aria-hidden="true" />' +
            '<span>GitHub</span>' +
            '</a>',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Documentation',
          items: [
            {label: 'Latest docs', to: '/docs/1.4'},
            {label: 'Installation', to: '/docs/1.4/bukkit/getting-started/installation'},
          ],
        },
        {
          title: 'Project',
          items: [
            {label: 'Releases', to: '/releases'},
            {label: 'Download', to: '/download'},
            {label: 'Translate', to: '/translate'},
          ],
        },
        {
          title: 'Community',
          items: [
            {label: 'Issues', href: 'https://github.com/SzaBee13/double-doors-server/issues'},
            {label: 'Discussions', href: 'https://github.com/SzaBee13/double-doors-server/discussions'},
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Szabolcs Győrffy & contributors.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
