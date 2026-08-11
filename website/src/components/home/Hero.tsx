import Link from '@docusaurus/Link';
import Button from '@site/src/components/Button';
import type {ReactNode} from 'react';

/** Landing-page hero with the logo, tagline, and primary actions. */
export default function Hero(): ReactNode {
  return (
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
          <Button to="/download">Download from Modrinth</Button>
          <Button variant="secondary" to="/releases">
            View releases
          </Button>
          <Button variant="ghost" to="/docs">
            Documentation
          </Button>
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
  );
}
