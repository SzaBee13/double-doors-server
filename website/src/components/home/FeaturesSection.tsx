import type {ReactNode} from 'react';

type Feature = {
  title: string;
  description: string;
  icon: ReactNode;
};

const FEATURES: Feature[] = [
  {
    title: 'Mirrored double doors',
    description:
      'Open a door and its mirrored partner swings with it on the same tick — matching type, facing, and hinge.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
        <rect x="3" y="3" width="8" height="18" rx="1" />
        <rect x="13" y="3" width="8" height="18" rx="1" />
        <path d="M7 8l2 2 2-2M17 8l2 2 2-2" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    ),
  },
  {
    title: 'Same-tick sync',
    description:
      'Low-latency linking keeps partner doors perfectly in sync — no visible delay between the two halves.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 2" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    ),
  },
  {
    title: 'Fence gates & trapdoors',
    description:
      'Optional recursive opening extends linking to fence gates, trapdoors, and other door-like openables.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
        <path d="M4 21V10M9 21V10M14 21V10M19 21V10" strokeLinecap="round" />
        <path d="M2 14l20-4M2 18l20-4" strokeLinecap="round" />
      </svg>
    ),
  },
  {
    title: 'Per-player control',
    description:
      'Players toggle behavior for themselves with /doubledoors toggle, or operators can flip it server-wide.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 3a9 9 0 0 1 0 18z" fill="currentColor" stroke="none" />
      </svg>
    ),
  },
  {
    title: 'Protection friendly',
    description:
      'Soft integrates with GriefPrevention and works through LuckPerms permission nodes for safe linked doors.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
        <path d="M12 3l7 3v5c0 4.5-3 8.5-7 10-4-1.5-7-5.5-7-10V6z" strokeLinejoin="round" />
        <path d="M9.5 12l2 2 3.5-4" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    ),
  },
  {
    title: 'Multi-platform',
    description:
      'Runs on Bukkit, Spigot, Paper, Purpur, and Folia — plus an optional Velocity proxy plugin for multi-server setups.',
    icon: (
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
        <rect x="2" y="7" width="9" height="13" rx="1.5" />
        <rect x="13" y="4" width="9" height="16" rx="1.5" />
      </svg>
    ),
  },
];

/** Feature card grid. */
export default function FeaturesSection(): ReactNode {
  return (
    <section className="dd-section dd-section--features">
      <div className="dd-container">
        <p className="dd-section__eyebrow">Features</p>
        <h2 className="dd-section__title">Everything a double door should do</h2>
        <p className="dd-section__subtitle">
          Built for servers that care about doors that behave — low-latency, configurable, and
          protection-aware.
        </p>
        <div className="dd-features">
          {FEATURES.map((feature) => (
            <article className="dd-feature" key={feature.title}>
              <div className="dd-feature__icon">{feature.icon}</div>
              <h3 className="dd-feature__title">{feature.title}</h3>
              <p className="dd-feature__description">{feature.description}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
