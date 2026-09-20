package com.sk89q.worldguard.bukkit;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardPlugin {
  private static final WorldGuardPlugin INSTANCE = new WorldGuardPlugin();

  public static WorldGuardPlugin inst() {
    return INSTANCE;
  }

  public Boolean canBuild(Player player, Location loc) {
    return false;
  }
}
