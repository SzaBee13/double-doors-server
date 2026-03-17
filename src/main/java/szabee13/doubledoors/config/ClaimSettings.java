package szabee13.doubledoors.config;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import szabee13.doubledoors.DoubleDoors;

/**
 * Manages per-GriefPrevention-claim settings, persisted to {@code claims.yml}.
 *
 * <p>Currently tracks which claims have opted out of villager linked-door behavior.</p>
 */
public final class ClaimSettings {

  private final DoubleDoors plugin;
  private final File dataFile;
  private final Set<Long> villagersBlockedClaims = new HashSet<>();

  /**
   * Loads claim settings from {@code claims.yml}.
   *
   * @param plugin the plugin instance
   */
  public ClaimSettings(DoubleDoors plugin) {
    this.plugin = plugin;
    this.dataFile = new File(plugin.getDataFolder(), "claims.yml");
    load();
  }

  /**
   * Reloads all claim settings from disk, clearing in-memory state.
   */
  public void load() {
    YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
    villagersBlockedClaims.clear();
    List<?> blocked = data.getList("villagersBlocked");
    if (blocked != null) {
      for (Object entry : blocked) {
        if (entry instanceof Number n) {
          villagersBlockedClaims.add(n.longValue());
        }
      }
    }
  }

  /**
   * Saves all claim settings synchronously to {@code claims.yml}.
   */
  public void save() {
    YamlConfiguration data = new YamlConfiguration();
    data.set("villagersBlocked", List.copyOf(villagersBlockedClaims));
    try {
      data.save(dataFile);
    } catch (IOException e) {
      plugin.getLogger().warning("Could not save claims.yml: " + e.getMessage());
    }
  }

  /**
   * Saves asynchronously; safe to call from the main thread after every mutation.
   */
  public void saveAsync() {
    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
  }

  /**
   * Returns whether villager linked-door openings are blocked for the given claim.
   *
   * @param claimId the GriefPrevention claim ID
   * @return true if villagers are blocked in this claim
   */
  public boolean isVillagersBlocked(long claimId) {
    return villagersBlockedClaims.contains(claimId);
  }

  /**
   * Toggles villager linked-door access for the given claim.
   *
   * @param claimId the GriefPrevention claim ID
   * @return true if villagers are now blocked, false if now allowed
   */
  public boolean toggleVillagersBlocked(long claimId) {
    if (villagersBlockedClaims.remove(claimId)) {
      saveAsync();
      return false;
    }
    villagersBlockedClaims.add(claimId);
    saveAsync();
    return true;
  }
}
