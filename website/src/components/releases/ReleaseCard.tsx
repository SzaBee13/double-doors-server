import GithubIcon from '@site/src/components/GithubIcon';
import ReleaseNotes from '@site/src/components/releases/ReleaseNotes';
import {formatDate, MODRINTH_DOWNLOAD} from '@site/src/components/releases/useReleases';
import type {GitHubRelease} from '@site/src/components/releases/useReleases';
import type {ReactNode} from 'react';

type ReleaseCardProps = {
  release: GitHubRelease;
  isLatest: boolean;
  isExpanded: boolean;
  onToggle: () => void;
};

/** Single release card with download, GitHub, and expandable release notes. */
export default function ReleaseCard({release, isLatest, isExpanded, onToggle}: ReleaseCardProps): ReactNode {
  const className = `dd-release${isLatest ? ' dd-release--latest' : ''}${
    release.prerelease ? ' dd-release--prerelease' : ''
  }`;

  return (
    <article className={className}>
      <header className="dd-release__header">
        <div className="dd-release__heading">
          <div className="dd-release__title-row">
            <h2 className="dd-release__version">{release.name || release.tag_name}</h2>
            {isLatest && <span className="dd-release__badge dd-release__badge--latest">Latest</span>}
            {release.prerelease && (
              <span className="dd-release__badge dd-release__badge--prerelease">Pre-release</span>
            )}
          </div>
          <p className="dd-release__date">{formatDate(release.published_at)}</p>
        </div>
        <div className="dd-release__links">
          <a className="dd-btn dd-btn--small dd-btn--primary" href={MODRINTH_DOWNLOAD} target="_blank" rel="noreferrer">
            Download
          </a>
          <a className="dd-btn dd-btn--small dd-btn--ghost" href={release.html_url} target="_blank" rel="noreferrer">
            <GithubIcon size={14} />
            GitHub
          </a>
          <button
            type="button"
            className="dd-btn dd-btn--small dd-btn--ghost dd-release__toggle"
            aria-expanded={isExpanded}
            onClick={onToggle}
          >
            {isExpanded ? 'Hide release notes' : 'Show release notes'}
          </button>
        </div>
      </header>
      {isExpanded && <ReleaseNotes body={release.body} />}
    </article>
  );
}
