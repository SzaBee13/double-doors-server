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

The site is hosted on Vercel. Connect the GitHub repository to a Vercel project
with the project root set to `website/`, then attach `doubledoors.szabee.me` as
its custom domain.

`vercel.json` pins the Docusaurus framework preset, the `pnpm build` build
command, and the `build/` output directory, and adds immutable caching for the
content-hashed assets. Vercel respects `package.json` (`packageManager` picks
pnpm, `engines.node >= 24` picks the Node runtime), so no per-project Vercel
dashboard settings are required beyond the project root.

## Historical migration

The historical snapshots were converted from the wiki once using
`versions-migration.json` and `scripts/migrate-wiki.mjs`. The source wiki is no
longer required for normal documentation work.
