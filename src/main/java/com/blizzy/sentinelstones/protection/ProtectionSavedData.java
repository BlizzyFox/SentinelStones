
package com.blizzy.sentinelstones.protection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import java.util.Optional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProtectionSavedData extends SavedData {

    private static final String DATA_NAME = "sentinelstones_protection";

    private static final Factory<ProtectionSavedData> FACTORY =
            new Factory<>(
                    ProtectionSavedData::new,
                    ProtectionSavedData::load,
                    null
            );

    // Dimension + position -> owner UUID
    private final Map<StoneKey, UUID> stones = new HashMap<>();

    public ProtectionSavedData() {
    }

    /**
     * Get the world's protection data.
     * Stored on the Overworld, all dimensions share it.
     */
    public static ProtectionSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(FACTORY, DATA_NAME);
    }

    /**
     * Check who owns a particular protection stone.
     */
    public UUID getOwner(ServerLevel level, BlockPos pos) {
        return stones.get(new StoneKey(
                level.dimension().location().toString(),
                pos.immutable()
        ));
    }

    /**
     * Check whether another player's stone overlaps
     * the proposed stone's 3x3x3 protection area.
     *
     * Each stone protects one block in every direction.
     * Therefore, two centers overlap when their distance
     * is <= 2 on all three axes.
     */
    public boolean hasOverlappingClaim(
            ServerLevel level,
            BlockPos pos,
            UUID ignoredOwner
    ) {
        String dimension = level.dimension().location().toString();

        for (Map.Entry<StoneKey, UUID> entry : stones.entrySet()) {

            StoneKey key = entry.getKey();

            if (!key.dimension().equals(dimension)) {
                continue;
            }

            // Ignore the player's previous stone.
            if (entry.getValue().equals(ignoredOwner)) {
                continue;
            }

            BlockPos existing = key.pos();

            int dx = Math.abs(existing.getX() - pos.getX());
            int dy = Math.abs(existing.getY() - pos.getY());
            int dz = Math.abs(existing.getZ() - pos.getZ());

            if (dx <= 2 && dy <= 2 && dz <= 2) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find and unbind a player's existing stone.
     */
    public void unbindPlayer(UUID player) {

        boolean changed = stones.values().removeIf(
                owner -> owner.equals(player)
        );

        if (changed) {
            setDirty();
        }
    }

    /**
     * Bind a stone to a player.
     * Also removes any previously bound stone belonging
     * to that player.
     */
    public void bind(
            ServerLevel level,
            BlockPos pos,
            UUID player
    ) {
        unbindPlayer(player);

        StoneKey key = new StoneKey(
                level.dimension().location().toString(),
                pos.immutable()
        );

        stones.put(key, player);

        setDirty();
    }

    /**
     * Remove ownership from a particular stone.
     */
    public void removeClaim(ServerLevel level, BlockPos pos) {

        StoneKey key = new StoneKey(
                level.dimension().location().toString(),
                pos.immutable()
        );

        if (stones.remove(key) != null) {
            setDirty();
        }
    }

    /**
     * Load saved data from disk.
     */
    public static ProtectionSavedData load(
            CompoundTag tag,
            net.minecraft.core.HolderLookup.Provider registries
    ) {
        ProtectionSavedData data = new ProtectionSavedData();

        ListTag list = tag.getList("stones", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {

            CompoundTag entry = list.getCompound(i);

            String dimension = entry.getString("dimension");

            BlockPos pos = new BlockPos(
                    entry.getInt("x"),
                    entry.getInt("y"),
                    entry.getInt("z")
            );

            UUID owner = entry.getUUID("owner");

            data.stones.put(
                    new StoneKey(dimension, pos),
                    owner
            );
        }

        return data;
    }

    /**
     * Save data to disk.
     */
    @Override
    public CompoundTag save(
            CompoundTag tag,
            net.minecraft.core.HolderLookup.Provider registries
    ) {
        ListTag list = new ListTag();

        for (Map.Entry<StoneKey, UUID> entry : stones.entrySet()) {

            StoneKey key = entry.getKey();
            BlockPos pos = key.pos();

            CompoundTag stoneTag = new CompoundTag();

            stoneTag.putString("dimension", key.dimension());

            stoneTag.putInt("x", pos.getX());
            stoneTag.putInt("y", pos.getY());
            stoneTag.putInt("z", pos.getZ());

            stoneTag.putUUID("owner", entry.getValue());

            list.add(stoneTag);
        }

        tag.put("stones", list);

        return tag;
    }


    public Optional<GlobalPos> getPlayerStone(UUID playerId) {
        for (Map.Entry<StoneKey, UUID> entry : stones.entrySet()) {
            if (entry.getValue().equals(playerId)) {
                StoneKey key = entry.getKey();

                ResourceKey<Level> dimension = ResourceKey.create(
                        Registries.DIMENSION,
                        ResourceLocation.parse(key.dimension())
                );

                return Optional.of(GlobalPos.of(dimension, key.pos()));
            }
        }

        return Optional.empty();
    }

    /**
     * Immutable key for a protection stone.
     */
    private record StoneKey(
            String dimension,
            BlockPos pos
    ) {
    }

    /*
        Check if world location has an owner.
    */
    public UUID getOwnerAt(ServerLevel level, BlockPos targetPos) {
        String dimension = level.dimension().location().toString();

        for (Map.Entry<StoneKey, UUID> entry : stones.entrySet()) {
            StoneKey key = entry.getKey();

            if (!key.dimension().equals(dimension)) {
                continue;
            }

            BlockPos stonePos = key.pos();

            boolean inside =
                    Math.abs(targetPos.getX() - stonePos.getX()) <= 1 &&
                    Math.abs(targetPos.getY() - stonePos.getY()) <= 1 &&
                    Math.abs(targetPos.getZ() - stonePos.getZ()) <= 1;

            if (inside) {
                return entry.getValue();
            }
        }

        return null;
    }
}
