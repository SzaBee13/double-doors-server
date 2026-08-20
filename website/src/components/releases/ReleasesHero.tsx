import Button from '@site/src/components/custom/Button';
import type {ReactNode} from 'react';

/** Releases page hero with the changelog title. */
export default function ReleasesHero(): ReactNode {
  return (
    <section className="dd-release-hero">
      <div className="dd-container">
        <p className="dd-section__eyebrow">Changelog</p>
        <h1 className="dd-release-hero__title">Releases</h1>
        <p className="dd-release-hero__description">
          Release notes for every DoubleDoors version, fetched live from GitHub.
        </p>
        <Button to="/download">Download on Modrinth</Button>
      </div>
    </section>
  );
}
