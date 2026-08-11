import Link from '@docusaurus/Link';
import type {ReactNode} from 'react';

type ButtonProps = {
  children: ReactNode;
  variant?: 'primary' | 'secondary' | 'ghost';
  size?: 'small' | 'large';
  /** Internal route (renders a Docusaurus <Link>). */
  to?: string;
  /** External URL (renders a plain anchor, opened in a new tab). */
  href?: string;
  onClick?: () => void;
  ariaExpanded?: boolean;
};

/**
 * Pill button used across the landing and releases pages. Renders an internal
 * link, an external link, or a native button depending on which props are set.
 */
export default function Button({
  children,
  variant = 'primary',
  size,
  to,
  href,
  onClick,
  ariaExpanded,
}: ButtonProps): ReactNode {
  const className = `dd-btn dd-btn--${variant}${size ? ` dd-btn--${size}` : ''}`;

  if (href !== undefined) {
    return (
      <a className={className} href={href} target="_blank" rel="noreferrer">
        {children}
      </a>
    );
  }

  if (to !== undefined) {
    return (
      <Link className={className} to={to}>
        {children}
      </Link>
    );
  }

  return (
    <button type="button" className={className} onClick={onClick} aria-expanded={ariaExpanded}>
      {children}
    </button>
  );
}
