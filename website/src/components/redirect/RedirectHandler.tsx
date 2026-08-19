import {useLocation} from '@docusaurus/router';
import Head from '@docusaurus/Head';
import {useEffect, useState} from 'react';
import type {ReactNode} from 'react';

interface RedirectMap {
  [key: string]: {
    name: string;
    latest: string;
    versions: {
      [version: string]: string;
    };
  };
}

export default function RedirectHandler(): ReactNode {
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const pageKey = params.get('page');
  const version = params.get('v') || 'latest';

  const [error, setError] = useState<string | null>(null);
  const [targetUrl, setTargetUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!pageKey) {
      setError('Missing required parameter: page');
      return;
    }

    fetch('/redirects/map.json')
      .then((res) => {
        if (!res.ok) throw new Error('Failed to load redirect map');
        return res.json() as Promise<RedirectMap>;
      })
      .then((map) => {
        const entry = map[pageKey];
        if (!entry) {
          setError(`Unknown page: ${pageKey}`);
          return;
        }

        const targetVersion = version === 'latest' ? entry.latest : version;
        const path = entry.versions[targetVersion];
        if (!path) {
          setError(`Version ${targetVersion} not available for ${pageKey}`);
          return;
        }

        const url = `/docs/${targetVersion}${path}`;
        setTargetUrl(url);
        window.location.replace(url);
      })
      .catch(() => {
        setError('Failed to load redirect map');
      });
  }, [pageKey, version]);

  return (
    <>
      <Head>
        {targetUrl && <meta httpEquiv="refresh" content={`0;url=${targetUrl}`} />}
      </Head>
      <p>
        {error ? (
          <span style={{color: '#d32f2f'}}>{error}</span>
        ) : targetUrl ? (
          <>
            Redirecting to <a href={targetUrl}>documentation</a>… If you are not redirected
            automatically, click the link above.
          </>
        ) : (
          'Loading redirect information…'
        )}
      </p>
    </>
  );
}
