import Layout from '@theme/Layout';
import ReleaseList from '@site/src/components/releases/ReleaseList';
import ReleasesHero from '@site/src/components/releases/ReleasesHero';
import ReleaseState from '@site/src/components/releases/ReleaseState';
import {useReleases} from '@site/src/components/releases/useReleases';
import type {ReactNode} from 'react';

export default function Releases(): ReactNode {
  const state = useReleases();

  return (
    <Layout
      title="Releases"
      description="Release notes for every DoubleDoors version, powered by the GitHub API."
    >
      <main className="dd-main dd-main--releases">
        <ReleasesHero />

        <section className="dd-section dd-section--releases">
          <div className="dd-container">
            {state.status === 'ready' ? (
              <ReleaseList releases={state.releases} />
            ) : (
              <ReleaseState state={state} />
            )}
          </div>
        </section>
      </main>
    </Layout>
  );
}
