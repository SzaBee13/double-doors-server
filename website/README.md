# DoubleDoors Documentation

The documentation site is built with Docusaurus and deploys from `website/`.

## Development

```bash
pnpm install
pnpm start
```

Build and type-check the site with:

```bash
pnpm build
pnpm typecheck
```

The current documentation source is `docs/`. Create a future minor snapshot with
`pnpm docusaurus docs:version <version>`.

## Deployment

Configure the Vercel project root as `website/` and attach
`doubledoors.szabee.me` as its custom domain. Vercel should use the standard
commands from `package.json` with Node 24 or newer.

## Historical migration

The historical snapshots were converted from the wiki once using
`versions-migration.json` and `scripts/migrate-wiki.mjs`. The source wiki is no
longer required for normal documentation work.
