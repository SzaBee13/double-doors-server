import {useEffect, useState} from 'react';

export const REPO = 'SzaBee13/double-doors-server';
export const RELEASES_API = `https://api.github.com/repos/${REPO}/releases?per_page=15`;
export const MODRINTH_DOWNLOAD = 'https://modrinth.com/plugin/double-doors-server#download';

export type GitHubRelease = {
  tag_name: string;
  name: string;
  body: string;
  html_url: string;
  published_at: string;
  prerelease: boolean;
  draft: boolean;
};

export type LoadState =
  | {status: 'loading'}
  | {status: 'error'; message: string}
  | {status: 'ready'; releases: GitHubRelease[]};

/** Formats an ISO timestamp as a long, localized date string. */
export function formatDate(value: string): string {
  return new Date(value).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

/** Fetches the latest non-draft releases from the GitHub API. */
export function useReleases(): LoadState {
  const [state, setState] = useState<LoadState>({status: 'loading'});

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

  return state;
}
