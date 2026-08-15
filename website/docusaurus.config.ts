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
            '<svg class="dd-github-icon" viewBox="0 0 16 16" width="16" height="16" fill="currentColor" aria-hidden="true">' +
            '<path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"/>' +
            '</svg>' +
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
            {label: 'Installation', to: '/docs/1.4/bukkit-spigot-paper/Installation'},
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
