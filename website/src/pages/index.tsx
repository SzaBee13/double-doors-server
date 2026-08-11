import Link from '@docusaurus/Link';
import Layout from '@theme/Layout';
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

const PLATFORMS = ['Bukkit', 'Spigot', 'Paper', 'Purpur', 'Folia', 'Velocity'];

const COMMANDS: {command: string; description: string}[] = [
  {command: '/doubledoors toggle', description: 'Toggle double-door behavior for yourself'},
  {command: '/doubledoors reload', description: 'Reload the plugin configuration'},
  {command: '/doubledoors locale', description: 'View or set your language'},
  {command: '/doubledoors knock-volume', description: 'Set your personal knock sound volume'},
];

export default function Home(): ReactNode {
  return (
    <Layout
      title="DoubleDoors — Synchronized doors for Minecraft servers"
      description="Open mirrored double doors together with low-latency syncing for your Minecraft server. Supports Bukkit, Spigot, Paper, Purpur, Folia, and Velocity."
    >
      <main className="dd-main">
        <section className="dd-hero">
          <div className="dd-hero__glow" aria-hidden="true" />
          <div className="dd-hero__inner">
            <img className="dd-logo" src="/img/logo.png" alt="DoubleDoors logo" width={128} height={128} />
            <p className="dd-eyebrow">Minecraft server plugin</p>
            <h1 className="dd-hero__title">
              DoubleDoors
              <span className="dd-hero__title-accent">.</span>
            </h1>
            <p className="dd-hero__tagline">
              Open mirrored double doors together — on the same tick, every time.
            </p>
            <p className="dd-hero__description">
              A lightweight Bukkit/Spigot plugin that links side-by-side doors, fence gates, and
              trapdoors into perfectly synchronized pairs, with low-latency syncing and optional
              compatibility handling for common server stacks.
            </p>
            <div className="dd-hero__actions">
              <Link className="dd-btn dd-btn--primary" to="/download">
                Download from Modrinth
              </Link>
              <Link className="dd-btn dd-btn--secondary" to="/releases">
                View releases
              </Link>
              <Link className="dd-btn dd-btn--ghost" to="/docs">
                Documentation
              </Link>
            </div>
            <div className="dd-hero__meta">
              <span>Java 25+</span>
              <span aria-hidden="true">•</span>
              <span>Minecraft 1.21.x · 26.1.x · 26.2.x</span>
              <span aria-hidden="true">•</span>
              <Link to="/translate">Help translate</Link>
            </div>
          </div>
        </section>

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

        <section className="dd-section dd-section--commands">
          <div className="dd-container">
            <p className="dd-section__eyebrow">Commands</p>
            <h2 className="dd-section__title">Simple commands, sane defaults</h2>
            <div className="dd-commands">
              {COMMANDS.map(({command, description}) => (
                <div className="dd-command" key={command}>
                  <code className="dd-command__name">{command}</code>
                  <span className="dd-command__description">{description}</span>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="dd-section dd-section--platforms">
          <div className="dd-container">
            <p className="dd-section__eyebrow">Platforms</p>
            <h2 className="dd-section__title">Runs where your server runs</h2>
            <ul className="dd-platforms">
              {PLATFORMS.map((platform) => (
                <li className="dd-platform" key={platform}>
                  {platform}
                </li>
              ))}
            </ul>
          </div>
        </section>

        <section className="dd-cta">
          <div className="dd-container dd-cta__inner">
            <h2 className="dd-cta__title">Ready for doors that just work?</h2>
            <p className="dd-cta__description">
              Grab the latest release, drop it in your plugins folder, and open a door.
            </p>
            <div className="dd-cta__actions">
              <Link className="dd-btn dd-btn--primary dd-btn--large" to="/download">
                Download now
              </Link>
              <Link className="dd-btn dd-btn--secondary dd-btn--large" to="/releases">
                Release notes
              </Link>
            </div>
          </div>
        </section>
      </main>
    </Layout>
  );
}
