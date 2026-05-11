package me.szabee.doubledoors.util;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;

/**
 * Utility methods for finding connected door-like blocks.
 */
public final class DoorUtil {
  private static final ConcurrentMap<MirrorCacheKey, MirrorCacheEntry> MIRROR_CACHE = new ConcurrentHashMap<>();
  private static final int MIRROR_CACHE_MAX_SIZE = 4_096;
  private static final long MIRROR_CACHE_TRIM_INTERVAL_MILLIS = 5_000L;
  private static volatile long mirrorCacheTtlMillis = 1_200L;
  private static volatile long lastMirrorCacheTrimMillis;

  private DoorUtil() {
  }

  /**
   * Sets the mirror lookup cache TTL.
   *
   * @param ttlMillis cache TTL in milliseconds
   */
  public static void setMirrorCacheTtlMillis(long ttlMillis) {
  if (ttlMillis < 1L) {
    mirrorCacheTtlMillis = 1L;
    return;
  }
  mirrorCacheTtlMillis = ttlMillis;
  }

  /**
   * Finds the adjacent mirrored partner door for a standard double-door setup.
   *
   * <p>A valid partner has the same material and facing, opposite hinge, and is side-by-side.
   * This matches the usual double-door shape where both door handles face each other.</p>
   *
   * @param origin the clicked or triggered door block (upper or lower half)
   * @return the lower-half partner door block, or null when no mirrored partner exists
   */
  public static Block findMirroredDoubleDoorPartner(Block origin) {
  MirrorSearchResult analyzed = analyzeMirroredDoubleDoorPartner(origin);
  return analyzed.partner();
  }

  /**
   * Analyzes mirrored double-door partner lookup and includes a failure reason.
   *
   * @param origin the clicked or triggered door block (upper or lower half)
   * @return a detailed lookup result
   */
  public static MirrorSearchResult analyzeMirroredDoubleDoorPartner(Block origin) {
  Block originBase = toLowerDoorBlock(origin);
  if (originBase == null) {
    return MirrorSearchResult.failure("origin_is_not_door");
  }

  MirrorCacheKey cacheKey = new MirrorCacheKey(
    originBase.getWorld().getUID(),
    originBase.getX(),
    originBase.getY(),
    originBase.getZ());
  MirrorCacheEntry cacheEntry = MIRROR_CACHE.get(cacheKey);
  long now = System.currentTimeMillis();
  if (cacheEntry != null && isMirrorCacheEntryFresh(now, cacheEntry)) {
    if (!cacheEntry.found()) {
    return MirrorSearchResult.failure(cacheEntry.reason());
    }
    Block cachedPartner = originBase.getWorld().getBlockAt(cacheEntry.partnerX(), cacheEntry.partnerY(), cacheEntry.partnerZ());
    Block validated = matchingMirroredDoor(originBase, (Door) originBase.getBlockData(), cachedPartner);
    if (validated != null) {
    return MirrorSearchResult.success(validated);
    }
  } else if (cacheEntry != null) {
    MIRROR_CACHE.remove(cacheKey, cacheEntry);
  }

  Door originDoor = (Door) originBase.getBlockData();
  BlockFace facing = originDoor.getFacing();

  Block leftMatch = matchingMirroredDoor(originBase, originDoor, originBase.getRelative(leftOf(facing)));
  if (leftMatch != null) {
    putMirrorCacheEntry(cacheKey, MirrorCacheEntry.found(now, leftMatch), now);
    return MirrorSearchResult.success(leftMatch);
  }

  Block rightCandidate = originBase.getRelative(rightOf(facing));
  Block rightMatch = matchingMirroredDoor(originBase, originDoor, rightCandidate);
  if (rightMatch != null) {
    putMirrorCacheEntry(cacheKey, MirrorCacheEntry.found(now, rightMatch), now);
    return MirrorSearchResult.success(rightMatch);
  }

  String reason = diagnoseMirrorFailure(originBase, originDoor, originBase.getRelative(leftOf(facing)), rightCandidate);
  putMirrorCacheEntry(cacheKey, MirrorCacheEntry.miss(now, reason), now);
  return MirrorSearchResult.failure(reason);
  }

  /**
   * Finds a corner partner door that is adjacent with perpendicular facing.
   *
   * <p>A valid corner partner has the same material and a facing direction that is
   * perpendicular to the origin door's facing, while being placed adjacent to the
   * origin (side-adjacent or diagonal).</p>
   *
   * @param origin the clicked or triggered door block (upper or lower half)
   * @return a detailed lookup result
   */
  public static MirrorSearchResult analyzeCornerDoorPartner(Block origin) {
  Block originBase = toLowerDoorBlock(origin);
  if (originBase == null) {
    return MirrorSearchResult.failure("origin_is_not_door");
  }

  Door originDoor = (Door) originBase.getBlockData();
  Block[] candidates = {
    originBase.getRelative(1, 0, 0),
    originBase.getRelative(-1, 0, 0),
    originBase.getRelative(0, 0, 1),
    originBase.getRelative(0, 0, -1),
    originBase.getRelative(1, 0, 1),
    originBase.getRelative(1, 0, -1),
    originBase.getRelative(-1, 0, 1),
    originBase.getRelative(-1, 0, -1)
  };

  for (Block candidate : candidates) {
    Block match = matchingCornerDoor(originBase, originDoor, candidate);
    if (match != null) {
    return MirrorSearchResult.success(match);
    }
  }

  return MirrorSearchResult.failure("corner_not_found");
  }

  /**
   * Invalidates the mirror cache for the given door block coordinates.
   *
   * @param block the door block to invalidate
   */
  public static void invalidateMirrorCacheAt(Block block) {
  if (block == null) {
    return;
  }
  UUID worldId = block.getWorld().getUID();
  int x = block.getX();
  int y = block.getY();
  int z = block.getZ();
  invalidateMirrorCacheAt(worldId, x, y, z);
  invalidateMirrorCacheAt(worldId, x, y - 1, z);
  }

  /**
   * Invalidates the mirror cache for the given coordinate.
   *
   * @param worldId world UUID
   * @param x X coordinate
   * @param y Y coordinate
   * @param z Z coordinate
   */
  public static void invalidateMirrorCacheAt(UUID worldId, int x, int y, int z) {
  MirrorCacheKey key = new MirrorCacheKey(worldId, x, y, z);
  MIRROR_CACHE.remove(key);
  }

  /**
   * Invalidates mirror cache entries near the given door block coordinates.
   *
   * <p>Clears the cache for the door itself plus adjacent bases that could pair
   * with it in a standard double-door configuration.</p>
   *
   * @param block the door block to invalidate around
   */
  public static void invalidateMirrorCacheNear(Block block) {
  if (block == null) {
    return;
  }
  UUID worldId = block.getWorld().getUID();
  int baseX = block.getX();
  int baseY = block.getY();
  int baseZ = block.getZ();
  for (int dx = -1; dx <= 1; dx++) {
    for (int dz = -1; dz <= 1; dz++) {
    invalidateMirrorCacheAt(worldId, baseX + dx, baseY, baseZ + dz);
    invalidateMirrorCacheAt(worldId, baseX + dx, baseY - 1, baseZ + dz);
    }
  }
  }

  private static boolean isMirrorCacheEntryFresh(long now, MirrorCacheEntry entry) {
  return (now - entry.timestampMillis()) <= mirrorCacheTtlMillis;
  }

  private static void putMirrorCacheEntry(MirrorCacheKey key, MirrorCacheEntry entry, long now) {
  MIRROR_CACHE.put(key, entry);
  trimMirrorCache(now);
  }

  private static void trimMirrorCache(long now) {
  if (MIRROR_CACHE.size() <= MIRROR_CACHE_MAX_SIZE
    && (now - lastMirrorCacheTrimMillis) < MIRROR_CACHE_TRIM_INTERVAL_MILLIS) {
    return;
  }
  lastMirrorCacheTrimMillis = now;
  for (Map.Entry<MirrorCacheKey, MirrorCacheEntry> entry : MIRROR_CACHE.entrySet()) {
    if (!isMirrorCacheEntryFresh(now, entry.getValue())) {
    MIRROR_CACHE.remove(entry.getKey(), entry.getValue());
    }
  }
  while (MIRROR_CACHE.size() > MIRROR_CACHE_MAX_SIZE) {
    MirrorCacheKey oldestKey = null;
    long oldestTimestamp = Long.MAX_VALUE;
    for (Map.Entry<MirrorCacheKey, MirrorCacheEntry> entry : MIRROR_CACHE.entrySet()) {
    if (entry.getValue().timestampMillis() < oldestTimestamp) {
      oldestTimestamp = entry.getValue().timestampMillis();
      oldestKey = entry.getKey();
    }
    }
    if (oldestKey == null) {
    return;
    }
    MIRROR_CACHE.remove(oldestKey);
  }
  }

  /**
   * Finds connected blocks with the same material using BFS.
   *
   * @param origin the origin block
   * @param maxDistance max BFS depth in block steps
   * @return connected same-material blocks excluding origin
   */
  public static Set<Block> findConnectedDoors(Block origin, int maxDistance) {
  Set<Block> result = new HashSet<>();
  if (origin == null || maxDistance < 1) {
    return result;
  }

  Material originType = origin.getType();
  World world = origin.getWorld();
  ArrayDeque<SearchNode> queue = new ArrayDeque<>();
  Set<Long> visited = new HashSet<>();

  queue.add(new SearchNode(origin, 0));
  visited.add(coordHash(origin));

  while (!queue.isEmpty()) {
    SearchNode node = queue.poll();
    Block current = node.block();
    int depth = node.depth();

    if (depth > 0) {
    result.add(current);
    }

    if (depth >= maxDistance) {
    continue;
    }

    addNeighborIfMatching(originType, world, current.getX() + 1, current.getY(), current.getZ(), depth, visited, queue);
    addNeighborIfMatching(originType, world, current.getX() - 1, current.getY(), current.getZ(), depth, visited, queue);
    addNeighborIfMatching(originType, world, current.getX(), current.getY() + 1, current.getZ(), depth, visited, queue);
    addNeighborIfMatching(originType, world, current.getX(), current.getY() - 1, current.getZ(), depth, visited, queue);
    addNeighborIfMatching(originType, world, current.getX(), current.getY(), current.getZ() + 1, depth, visited, queue);
    addNeighborIfMatching(originType, world, current.getX(), current.getY(), current.getZ() - 1, depth, visited, queue);
  }

  return result;
  }

  private static Block matchingMirroredDoor(Block originBase, Door originDoor, Block candidate) {
  Block candidateBase = toLowerDoorBlock(candidate);
  if (candidateBase == null) {
    return null;
  }
  if (candidateBase.getType() != originBase.getType()) {
    return null;
  }

  Door candidateDoor = (Door) candidateBase.getBlockData();
  if (candidateDoor.getFacing() != originDoor.getFacing()) {
    return null;
  }
  if (candidateDoor.getHinge() == originDoor.getHinge()) {
    return null;
  }

  return candidateBase;
  }

  private static Block matchingCornerDoor(Block originBase, Door originDoor, Block candidate) {
  Block candidateBase = toLowerDoorBlock(candidate);
  if (candidateBase == null) {
    return null;
  }
  if (candidateBase.getType() != originBase.getType()) {
    return null;
  }

  Door candidateDoor = (Door) candidateBase.getBlockData();
  if (!isPerpendicularFacing(originDoor.getFacing(), candidateDoor.getFacing())) {
    return null;
  }

  return candidateBase;
  }

  private static String diagnoseMirrorFailure(Block originBase, Door originDoor, Block leftCandidate, Block rightCandidate) {
  String leftReason = diagnoseCandidate(originBase, originDoor, leftCandidate, "left");
  String rightReason = diagnoseCandidate(originBase, originDoor, rightCandidate, "right");
  return leftReason + ";" + rightReason;
  }

  private static String diagnoseCandidate(Block originBase, Door originDoor, Block candidate, String side) {
  Block candidateBase = toLowerDoorBlock(candidate);
  if (candidateBase == null) {
    return side + "_not_door";
  }
  if (candidateBase.getType() != originBase.getType()) {
    return side + "_different_material";
  }

  Door candidateDoor = (Door) candidateBase.getBlockData();
  if (candidateDoor.getFacing() != originDoor.getFacing()) {
    return side + "_different_facing";
  }
  if (candidateDoor.getHinge() == originDoor.getHinge()) {
    return side + "_same_hinge";
  }
  return side + "_unknown";
  }

  private static Block toLowerDoorBlock(Block block) {
  if (block == null) {
    return null;
  }
  if (!(block.getBlockData() instanceof Door doorData)) {
    return null;
  }

  if (doorData.getHalf() == Bisected.Half.BOTTOM) {
    return block;
  }

  Block lower = block.getRelative(BlockFace.DOWN);
  if (!(lower.getBlockData() instanceof Door lowerDoor)) {
    return null;
  }
  return lowerDoor.getHalf() == Bisected.Half.BOTTOM ? lower : null;
  }

  private static BlockFace leftOf(BlockFace facing) {
  return switch (facing) {
    case NORTH -> BlockFace.WEST;
    case SOUTH -> BlockFace.EAST;
    case EAST -> BlockFace.NORTH;
    case WEST -> BlockFace.SOUTH;
    default -> BlockFace.SELF;
  };
  }

  private static BlockFace rightOf(BlockFace facing) {
  return switch (facing) {
    case NORTH -> BlockFace.EAST;
    case SOUTH -> BlockFace.WEST;
    case EAST -> BlockFace.SOUTH;
    case WEST -> BlockFace.NORTH;
    default -> BlockFace.SELF;
  };
  }

  private static boolean isPerpendicularFacing(BlockFace first, BlockFace second) {
  if (first == BlockFace.NORTH || first == BlockFace.SOUTH) {
    return second == BlockFace.EAST || second == BlockFace.WEST;
  }
  if (first == BlockFace.EAST || first == BlockFace.WEST) {
    return second == BlockFace.NORTH || second == BlockFace.SOUTH;
  }
  return false;
  }

  private static void addNeighborIfMatching(
    Material originType,
    World world,
    int x,
    int y,
    int z,
    int currentDepth,
    Set<Long> visited,
    ArrayDeque<SearchNode> queue
  ) {
  long neighborKey = coordHash(x, y, z);
  if (visited.contains(neighborKey)) {
    return;
  }

  Block neighbor = world.getBlockAt(x, y, z);
  if (neighbor.getType() != originType) {
    return;
  }

  visited.add(neighborKey);
  queue.add(new SearchNode(neighbor, currentDepth + 1));
  }

  private static long coordHash(Block block) {
  return coordHash(block.getX(), block.getY(), block.getZ());
  }

  private static long coordHash(int x, int y, int z) {
  // Mirrors Mojang's BlockPos-style bit packing to avoid collisions in normal world ranges.
  long lx = ((long) x & 0x3FFFFFFL) << 38;
  long lz = ((long) z & 0x3FFFFFFL) << 12;
  long ly = (long) y & 0xFFFL;
  return lx | lz | ly;
  }

  private record SearchNode(Block block, int depth) {
  }

  /**
   * Detailed result for mirrored-partner lookup.
   *
   * @param partner the matched partner block, null when not found
   * @param reason failure reason key, empty when success
   */
  public record MirrorSearchResult(Block partner, String reason) {
  /**
   * Checks whether a partner was found.
   *
   * @return true when partner is non-null
   */
  public boolean found() {
    return partner != null;
  }

  private static MirrorSearchResult success(Block partner) {
    return new MirrorSearchResult(partner, "");
  }

  private static MirrorSearchResult failure(String reason) {
    return new MirrorSearchResult(null, reason == null ? "not_found" : reason);
  }
  }

  private record MirrorCacheKey(UUID worldId, int x, int y, int z) {
  }

  private record MirrorCacheEntry(long timestampMillis, boolean found, int partnerX, int partnerY, int partnerZ, String reason) {
  private static MirrorCacheEntry found(long now, Block partner) {
    return new MirrorCacheEntry(now, true, partner.getX(), partner.getY(), partner.getZ(), "");
  }

  private static MirrorCacheEntry miss(long now, String reason) {
    return new MirrorCacheEntry(now, false, 0, 0, 0, reason == null ? "not_found" : reason);
  }
  }
}
