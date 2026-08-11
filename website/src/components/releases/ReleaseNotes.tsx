import ReactMarkdown from 'react-markdown';
import rehypeRaw from 'rehype-raw';
import remarkGfm from 'remark-gfm';
import type {Components} from 'react-markdown';
import type {ReactNode} from 'react';

const MARKDOWN_COMPONENTS: Components = {
  h1: ({children}) => <h2 className="dd-release-note__title">{children}</h2>,
  a: ({children, href}) => (
    <a href={href} target="_blank" rel="noreferrer">
      {children}
    </a>
  ),
};

/** Renders a release body as styled markdown. */
export default function ReleaseNotes({body}: {body: string}): ReactNode {
  return (
    <div className="dd-release-note">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeRaw]}
        components={MARKDOWN_COMPONENTS}
      >
        {body}
      </ReactMarkdown>
    </div>
  );
}
