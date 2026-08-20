import {useLocation} from '@docusaurus/router';
import {useEffect} from 'react';
import type {ReactNode} from 'react';

const MODRINTH_BASE = 'https://modrinth.com/plugin/double-doors-server';

export default function Download(): ReactNode {
  const location = useLocation();
  const params = new URLSearchParams(location.search);

  const target = new URL(MODRINTH_BASE);
  const version = params.get('version');
  const loader = params.get('loader');
  if (version) {
    target.searchParams.set('version', version);
  }
  if (loader) {
    target.searchParams.set('loader', loader);
  }
  target.hash = 'download';
  const targetUrl = target.toString();

  useEffect(() => {
    window.location.replace(targetUrl);
  }, [targetUrl]);

  return (
    <p>
      Redirecting to <a href={targetUrl}>Modrinth</a>… If you are not redirected automatically,
      click the link above.
    </p>
  );
}
