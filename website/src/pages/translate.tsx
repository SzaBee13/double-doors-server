import Head from '@docusaurus/Head';
import {useEffect} from 'react';
import type {ReactNode} from 'react';

const CROWDIN_URL = 'https://crowdin.com/project/double-doors-server';

export default function Translate(): ReactNode {
  useEffect(() => {
    window.location.replace(CROWDIN_URL);
  }, []);

  return (
    <>
      <Head>
        <meta httpEquiv="refresh" content={`0;url=${CROWDIN_URL}`} />
      </Head>
      <p>
        Redirecting to <a href={CROWDIN_URL}>Crowdin</a>… If you are not redirected automatically,
        click the link above.
      </p>
    </>
  );
}
