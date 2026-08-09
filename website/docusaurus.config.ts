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
          type: 'docsVersionDropdown',
          position: 'right',
        },
        {
          href: 'https://github.com/SzaBee13/double-doors-server',
          label: 'GitHub',
          position: 'right',
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
            {label: 'Installation', to: '/docs/1.4/Installation'},
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
      copyright: `Copyright © ${new Date().getFullYear()} DoubleDoors contributors.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
