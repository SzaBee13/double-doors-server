import {Redirect} from '@docusaurus/router';
import Head from '@docusaurus/Head';
import type {ReactNode} from 'react';

export default function DocsIndex(): ReactNode {
  return (
    <>
      <Head>
        <meta httpEquiv="refresh" content="0;url=/docs/1.4" />
      </Head>
      <Redirect to="/docs/1.4" />
      <p>Redirecting to the <a href="/docs/1.4">latest DoubleDoors documentation</a>...</p>
    </>
  );
}
