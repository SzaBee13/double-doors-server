import type {ReactNode} from 'react';

const PLATFORMS = ['Bukkit', 'Spigot', 'Paper', 'Purpur', 'Folia', 'Velocity'];

/** Supported platform chips. */
export default function PlatformsSection(): ReactNode {
  return (
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
  );
}
