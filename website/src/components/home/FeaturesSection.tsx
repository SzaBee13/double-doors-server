import type {ReactNode} from 'react';
import DoorsIcon from '@site/static/img/features/doors.svg';
import ClockIcon from '@site/static/img/features/clock.svg';
import FenceIcon from '@site/static/img/features/fence.svg';
import PlayerIcon from '@site/static/img/features/player.svg';
import ShieldIcon from '@site/static/img/features/shield.svg';
import PlatformsIcon from '@site/static/img/features/platforms.svg';

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
    icon: <DoorsIcon />,
  },
  {
    title: 'Same-tick sync',
    description:
      'Low-latency linking keeps partner doors perfectly in sync — no visible delay between the two halves.',
    icon: <ClockIcon />,
  },
  {
    title: 'Fence gates & trapdoors',
    description:
      'Optional recursive opening extends linking to fence gates, trapdoors, and other door-like openables.',
    icon: <FenceIcon />,
  },
  {
    title: 'Per-player control',
    description:
      'Players toggle behavior for themselves with /doubledoors toggle, or operators can flip it server-wide.',
    icon: <PlayerIcon />,
  },
  {
    title: 'Protection friendly',
    description:
      'Soft integrates with GriefPrevention and works through LuckPerms permission nodes for safe linked doors.',
    icon: <ShieldIcon />,
  },
  {
    title: 'Multi-platform',
    description:
      'Runs on Bukkit, Spigot, Paper, Purpur, and Folia — plus an optional Velocity proxy plugin for multi-server setups.',
    icon: <PlatformsIcon />,
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
