import {REPO} from '@site/src/components/releases/useReleases';
import type {ReactNode} from 'react';

type ReleaseStateProps = {
  state: {status: 'loading'} | {status: 'error'; message: string};
};

/** Loading or error placeholder for the release list. */
export default function ReleaseState({state}: ReleaseStateProps): ReactNode {
  if (state.status === 'loading') {
    return (
      <div className="dd-release-state" role="status">
        <span className="dd-spinner" aria-hidden="true" />
        <p>Loading release notes from GitHub…</p>
      </div>
    );
  }

  return (
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
  );
}
