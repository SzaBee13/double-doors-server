import Layout from '@theme/Layout';
import CtaSection from '@site/src/components/home/CtaSection';
import CommandsSection from '@site/src/components/home/CommandsSection';
import FeaturesSection from '@site/src/components/home/FeaturesSection';
import Hero from '@site/src/components/home/Hero';
import PlatformsSection from '@site/src/components/home/PlatformsSection';
import type {ReactNode} from 'react';

export default function Home(): ReactNode {
  return (
    <Layout
      title="DoubleDoors — Synchronized doors for Minecraft servers"
      description="Open mirrored double doors together with low-latency syncing for your Minecraft server. Supports Bukkit, Spigot, Paper, Purpur, Folia, and Velocity."
    >
      <main className="dd-main">
        <Hero />
        <FeaturesSection />
        <CommandsSection />
        <PlatformsSection />
        <CtaSection />
      </main>
    </Layout>
  );
}
