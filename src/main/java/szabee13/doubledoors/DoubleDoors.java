package szabee13.doubledoors;

import szabee13.doubledoors.config.ClaimSettings;
import szabee13.doubledoors.config.PlayerPreferences;
import szabee13.doubledoors.config.PluginConfig;
import szabee13.doubledoors.listeners.DoorInteractListener;
import szabee13.doubledoors.listeners.RedstoneListener;
import szabee13.doubledoors.util.DoorUtil;
import szabee13.doubledoors.util.ProtectionCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for DoubleDoors.
 */
public final class DoubleDoors extends JavaPlugin implements CommandExecutor, TabCompleter {
  private PluginConfig pluginConfig;
  private PlayerPreferences playerPreferences;
  private ClaimSettings claimSettings;

  /**
   * Gets the plugin configuration wrapper.
   *
   * @return the active plugin config
   */
  public PluginConfig getPluginConfig() {
    return pluginConfig;
  }

  /**
   * Gets the per-player preferences manager.
   *
   * @return the player preferences instance
   */
  public PlayerPreferences getPlayerPreferences() {
    return playerPreferences;
  }

  /**
   * Gets the per-claim settings manager.
   *
   * @return the claim settings instance
   */
  public ClaimSettings getClaimSettings() {
    return claimSettings;
  }

  /**
   * Checks whether the player can interact with a linked door block according to
   * active protection plugins.
   *
   * @param player the interacting player
   * @param linkedBlock the linked block to toggle
   * @return true if interaction should be allowed
   */
  public boolean canOpenLinkedDoor(Player player, Block linkedBlock) {
    return ProtectionCompat.canOpenLinkedDoor(this, player, linkedBlock);
  }

  /**
   * Checks whether double-door logic is globally enabled for a given player.
   *
   * @param player the player to check
   * @return true if behavior is enabled for the player
   */
  public boolean isEnabledForPlayer(Player player) {
    return playerPreferences.isEnabled(player.getUniqueId());
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    pluginConfig = new PluginConfig(this);
    playerPreferences = new PlayerPreferences(this);
    claimSettings = new ClaimSettings(this);

    getServer().getPluginManager().registerEvents(new DoorInteractListener(this), this);
    getServer().getPluginManager().registerEvents(new RedstoneListener(this), this);

    if (getCommand("doubledoors") != null) {
      getCommand("doubledoors").setExecutor(this);
      getCommand("doubledoors").setTabCompleter(this);
    }

    PluginManager pluginManager = getServer().getPluginManager();
    if (pluginManager.isPluginEnabled("LuckPerms")) {
      getLogger().info("LuckPerms detected: permissions handled via doubledoors.* nodes.");
    }
    if (pluginManager.isPluginEnabled("GriefPrevention")) {
      getLogger().info("GriefPrevention detected: linked door opens will respect claim build checks.");
    }
    if (pluginManager.isPluginEnabled("Geyser-Spigot") || pluginManager.isPluginEnabled("floodgate")) {
      getLogger().info("Geyser/Floodgate detected: duplicate interaction debounce is active.");
    }

    getLogger().info("DoubleDoors enabled.");
  }

  @Override
  public void onDisable() {
    if (playerPreferences != null) {
      playerPreferences.save();
    }
    getLogger().info("DoubleDoors disabled.");
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!command.getName().equalsIgnoreCase("doubledoors")) {
      return false;
    }

    if (args.length == 0) {
      sender.sendMessage("Usage: /" + label + " <reload|toggle [doors|gates|trapdoors]|server-toggle|grief villagers>");
      return true;
    }

    if (args[0].equalsIgnoreCase("reload")) {
      if (!sender.hasPermission("doubledoors.reload")) {
        sender.sendMessage("You do not have permission to use this command.");
        return true;
      }

      reloadConfig();
      pluginConfig.reload();
      playerPreferences.load();
      claimSettings.load();
      sender.sendMessage("DoubleDoors config and player preferences reloaded.");
      return true;
    }

    if (args[0].equalsIgnoreCase("toggle")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage("Only players can use /doubledoors toggle.");
        return true;
      }

      if (!sender.hasPermission("doubledoors.toggle")) {
        sender.sendMessage("You do not have permission to use this command.");
        return true;
      }

      // /doubledoors toggle [doors|gates|trapdoors]
      if (args.length >= 2) {
        UUID uuid = player.getUniqueId();
        switch (args[1].toLowerCase()) {
          case "doors" -> {
            boolean next = playerPreferences.toggleDoors(uuid);
            sender.sendMessage(next ? "Door linking enabled for you." : "Door linking disabled for you.");
          }
          case "gates" -> {
            boolean next = playerPreferences.toggleFenceGates(uuid);
            sender.sendMessage(next ? "Fence-gate linking enabled for you." : "Fence-gate linking disabled for you.");
          }
          case "trapdoors" -> {
            boolean next = playerPreferences.toggleTrapdoors(uuid);
            sender.sendMessage(next ? "Trapdoor linking enabled for you." : "Trapdoor linking disabled for you.");
          }
          default -> sender.sendMessage("Usage: /doubledoors toggle [doors|gates|trapdoors]");
        }
        return true;
      }

      boolean enabled = playerPreferences.toggleAll(player.getUniqueId());
      sender.sendMessage(enabled ? "DoubleDoors enabled for you." : "DoubleDoors disabled for you.");
      return true;
    }

    if (args[0].equalsIgnoreCase("server-toggle")) {
      if (!sender.hasPermission("doubledoors.server-toggle")) {
        sender.sendMessage("You do not have permission to use this command.");
        return true;
      }

      boolean nextState = !pluginConfig.isServerWideEnabled();
      pluginConfig.setServerWideEnabled(nextState);
      sender.sendMessage(nextState
          ? "DoubleDoors server-wide behavior enabled."
          : "DoubleDoors server-wide behavior disabled.");
      return true;
    }

    if (args[0].equalsIgnoreCase("grief")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage("Only players can use /" + label + " grief.");
        return true;
      }

      if (!sender.hasPermission("doubledoors.grief")) {
        sender.sendMessage("You do not have permission to use this command.");
        return true;
      }

      if (args.length < 2 || !args[1].equalsIgnoreCase("villagers")) {
        sender.sendMessage("Usage: /" + label + " grief villagers");
        return true;
      }

      Block standingBlock = player.getLocation().getBlock();
      long claimId = ProtectionCompat.getClaimIdAt(this, standingBlock);
      if (claimId < 0) {
        sender.sendMessage("You are not standing in a GriefPrevention claim, or GriefPrevention is not enabled.");
        return true;
      }

      if (!ProtectionCompat.isClaimManagerAt(this, player, standingBlock)) {
        sender.sendMessage("You do not have permission to manage this claim.");
        return true;
      }

      boolean blocked = claimSettings.toggleVillagersBlocked(claimId);
      sender.sendMessage(blocked
          ? "Villager linked-door access blocked for this claim."
          : "Villager linked-door access allowed for this claim.");
      return true;
    }

    sender.sendMessage("Usage: /" + label + " <reload|toggle [doors|gates|trapdoors]|server-toggle|grief villagers>");
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    List<String> completions = new ArrayList<>();
    if (!command.getName().equalsIgnoreCase("doubledoors")) {
      return completions;
    }

    if (args.length == 1) {
      for (String sub : List.of("reload", "toggle", "server-toggle", "grief")) {
        if (sub.startsWith(args[0].toLowerCase())) {
          completions.add(sub);
        }
      }
    } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
      for (String sub : List.of("doors", "gates", "trapdoors")) {
        if (sub.startsWith(args[1].toLowerCase())) {
          completions.add(sub);
        }
      }
    } else if (args.length == 2 && args[0].equalsIgnoreCase("grief")) {
      if ("villagers".startsWith(args[1].toLowerCase())) {
        completions.add("villagers");
      }
    }
    return completions;
  }
}
