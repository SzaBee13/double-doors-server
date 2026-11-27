package me.szabee.doubledoors.bukkit.api;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Bukkit binding of the loader-neutral DoubleDoors API.
 *
 * <p>The API contract lives in {@link me.szabee.doubledoors.api.DoubleDoorsAPI};
 * this facade fixes its type parameters to Bukkit's platform types so Bukkit
 * plugins do not need to spell out the generic binding.</p>
 */
public interface DoubleDoorsAPI
  extends me.szabee.doubledoors.api.DoubleDoorsAPI<Block, Player, Material> {}
