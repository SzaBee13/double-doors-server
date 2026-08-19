import ReleaseCard from '@site/src/components/releases/ReleaseCard';
import type {GitHubRelease} from '@site/src/components/releases/useReleases';
import {useState} from 'react';
import type {ReactNode} from 'react';

/** Expandable list of release cards. */
export default function ReleaseList({releases}: {releases: GitHubRelease[]}): ReactNode {
  const [expanded, setExpanded] = useState<ReadonlySet<string>>(
    () => new Set(releases.length > 0 ? [releases[0].tag_name] : []),
  );

  function toggleRelease(tag: string): void {
    setExpanded((current) => {
      const next = new Set(current);
      if (next.has(tag)) {
        next.delete(tag);
      } else {
        next.add(tag);
      }
      return next;
    });
  }

  return (
    <div className="dd-releases">
      {releases.map((release, index) => (
        <ReleaseCard
          key={release.tag_name}
          release={release}
          isLatest={index === 0}
          isExpanded={expanded.has(release.tag_name)}
          onToggle={() => toggleRelease(release.tag_name)}
        />
      ))}
    </div>
  );
}
