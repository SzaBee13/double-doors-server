import {Redirect} from '@docusaurus/router';
import Head from '@docusaurus/Head';
import type {ReactNode} from 'react';

export default function Home(): ReactNode {
  return (
    <>
      <Head>
        <meta httpEquiv="refresh" content="0;url=/docs" />
      </Head>
      <Redirect to="/docs" />
      <p>Redirecting to the <a href="/docs">DoubleDoors documentation</a>...</p>
    </>
  );
}
