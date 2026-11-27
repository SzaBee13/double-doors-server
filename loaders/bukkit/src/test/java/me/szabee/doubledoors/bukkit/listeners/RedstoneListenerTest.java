package me.szabee.doubledoors.bukkit.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import me.szabee.doubledoors.bukkit.DoubleDoors;
import me.szabee.doubledoors.bukkit.config.PluginConfig;
import me.szabee.doubledoors.bukkit.util.ProtectionCompat;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class RedstoneListenerTest {

  private static PluginConfig config(boolean enabled, boolean villagers) {
    PluginConfig config = mock(PluginConfig.class);
    when(config.isServerWideEnabled()).thenReturn(enabled);
    when(config.isEnableVillagerLinkedDoors()).thenReturn(villagers);
    return config;
  }

  private static DoubleDoors plugin(PluginConfig config) {
    DoubleDoors plugin = mock(DoubleDoors.class);
    when(plugin.getPluginConfig()).thenReturn(config);
    return plugin;
  }

  @Test
  void redstoneIgnoresStablePowerAndDisabledOrDisallowedSources() {
    Block block = mock(Block.class);
    DoubleDoors plugin = plugin(config(false, false));
    RedstoneListener listener = new RedstoneListener(plugin);
    listener.onBlockRedstone(new BlockRedstoneEvent(block, 0, 0));
    listener.onBlockRedstone(new BlockRedstoneEvent(block, 1, 1));

    PluginConfig enabled = config(true, false);
    when(plugin.getPluginConfig()).thenReturn(enabled);
    when(plugin.isLocationAllowed(block)).thenReturn(false);
    listener.onBlockRedstone(new BlockRedstoneEvent(block, 0, 1));
  }

  @Test
  void nonVillagersAreIgnored() {
    DoubleDoors plugin = plugin(config(true, true));
    RedstoneListener listener = new RedstoneListener(plugin);
    EntityInteractEvent interact = mock(EntityInteractEvent.class);
    when(interact.getEntity()).thenReturn(mock(Entity.class));
    listener.onVillagerInteract(interact);

  }

  @Test
  void supportedVillagerEventSchedulesWithExtraDelay() {
    Block block = mock(Block.class);
    PluginConfig config = config(true, true);
    when(config.getAnimationSyncExtraDelayTicks()).thenReturn(3);
    DoubleDoors plugin = plugin(config);
    Server server = mock(Server.class);
    BukkitScheduler scheduler = mock(BukkitScheduler.class);
    when(plugin.getServer()).thenReturn(server);
    when(server.getScheduler()).thenReturn(scheduler);
    Villager villager = mock(Villager.class);
    EntityInteractEvent event = mock(EntityInteractEvent.class);
    when(event.getEntity()).thenReturn(villager);
    when(event.getBlock()).thenReturn(block);

    try (
      MockedStatic<DoorInteractListener> door = mockStatic(DoorInteractListener.class);
      MockedStatic<ProtectionCompat> protection = mockStatic(ProtectionCompat.class)
    ) {
      door.when(() -> DoorInteractListener.isEnabledType(block, config, plugin)).thenReturn(true);
      protection.when(() -> ProtectionCompat.getClaimIdAt(plugin, block)).thenReturn(-1L);
      listener(plugin).onVillagerInteract(event);
    }

    verify(scheduler).runTaskLater(eq(plugin), any(Runnable.class), eq(5L));
  }

  private static RedstoneListener listener(DoubleDoors plugin) {
    return new RedstoneListener(plugin);
  }
}
