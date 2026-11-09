
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
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.blizzy.sentinelstones.Config;



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

    // Settlement UUID -> Settlement data
    private final Map<UUID, BeaconSettlement> settlements = new HashMap<>();

    // Beacon position -> Settlement UUID
    private final Map<GlobalPos, UUID> beaconSettlements = new HashMap<>();

    public ProtectionSavedData() {
    }
    
    public Collection<BeaconSettlement> getSettlements() {
        return Collections.unmodifiableCollection(settlements.values());
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
        // Load beacon settlements
        ListTag settlementList = tag.getList(
                "settlements",
                Tag.TAG_COMPOUND
        );

        for (int i = 0; i < settlementList.size(); i++) {
            CompoundTag entry = settlementList.getCompound(i);

            if (!entry.hasUUID("id") || !entry.hasUUID("founder")) {
                continue;
            }

            UUID id = entry.getUUID("id");
            UUID founder = entry.getUUID("founder");

            BeaconSettlement settlement = new BeaconSettlement(id, founder);

            // Load Officers
            ListTag officerList = entry.getList(
                    "officers",
                    Tag.TAG_COMPOUND
            );

            for (int j = 0; j < officerList.size(); j++) {
                CompoundTag officer = officerList.getCompound(j);

                if (officer.hasUUID("uuid")) {
                    settlement.getOfficers().add(officer.getUUID("uuid"));
                }
            }

            // Load Members
            ListTag memberList = entry.getList(
                    "members",
                    Tag.TAG_COMPOUND
            );

            for (int j = 0; j < memberList.size(); j++) {
                CompoundTag member = memberList.getCompound(j);

                if (member.hasUUID("uuid")) {
                    settlement.getMembers().add(member.getUUID("uuid"));
                }
            }

            // Load Beacons
            ListTag beaconList = entry.getList(
                    "beacons",
                    Tag.TAG_COMPOUND
            );

            for (int j = 0; j < beaconList.size(); j++) {
                CompoundTag beaconTag = beaconList.getCompound(j);

                try {
                    ResourceLocation dimensionLocation =
                            ResourceLocation.parse(beaconTag.getString("dimension"));

                    ResourceKey<Level> dimension = ResourceKey.create(
                            Registries.DIMENSION,
                            dimensionLocation
                    );

                    BlockPos pos = new BlockPos(
                            beaconTag.getInt("x"),
                            beaconTag.getInt("y"),
                            beaconTag.getInt("z")
                    );

                    GlobalPos beacon = GlobalPos.of(dimension, pos);

                    settlement.getBeacons().add(beacon);
                    data.beaconSettlements.put(beacon, id);

                } catch (IllegalArgumentException exception) {
                    // Ignore malformed beacon positions.
                }
            }

            data.settlements.put(id, settlement);
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

        // Save beacon settlements
        ListTag settlementList = new ListTag();

        for (BeaconSettlement settlement : settlements.values()) {
            CompoundTag entry = new CompoundTag();

            entry.putUUID("id", settlement.getId());
            entry.putUUID("founder", settlement.getFounder());

            // Save Officers
            ListTag officerList = new ListTag();

            for (UUID officerId : settlement.getOfficers()) {
                CompoundTag officer = new CompoundTag();
                officer.putUUID("uuid", officerId);
                officerList.add(officer);
            }

            entry.put("officers", officerList);

            // Save Members
            ListTag memberList = new ListTag();

            for (UUID memberId : settlement.getMembers()) {
                CompoundTag member = new CompoundTag();
                member.putUUID("uuid", memberId);
                memberList.add(member);
            }

            entry.put("members", memberList);

            // Save Beacons
            ListTag beaconList = new ListTag();

            for (GlobalPos beacon : settlement.getBeacons()) {
                CompoundTag beaconTag = new CompoundTag();

                beaconTag.putString(
                        "dimension",
                        beacon.dimension().location().toString()
                );

                BlockPos pos = beacon.pos();

                beaconTag.putInt("x", pos.getX());
                beaconTag.putInt("y", pos.getY());
                beaconTag.putInt("z", pos.getZ());

                beaconList.add(beaconTag);
            }

            entry.put("beacons", beaconList);

            settlementList.add(entry);
        }

        tag.put("settlements", settlementList);

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
        int range = Config.SENTINEL_RANGE.getAsInt();

        for (Map.Entry<StoneKey, UUID> entry : stones.entrySet()) {
            StoneKey key = entry.getKey();

            if (!key.dimension().equals(dimension)) {
                continue;
            }

            BlockPos stonePos = key.pos();

            boolean inside =
                    Math.abs(targetPos.getX() - stonePos.getX()) <= range &&
                    Math.abs(targetPos.getY() - stonePos.getY()) <= range &&
                    Math.abs(targetPos.getZ() - stonePos.getZ()) <= range;

            if (inside) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Create a new settlement with its first beacon.
     * The player who creates it becomes its founder.
     */
    public Optional<BeaconSettlement> createSettlement(
            GlobalPos beacon,
            UUID founder
    ) {
        if (beaconSettlements.containsKey(beacon)) {
            return Optional.empty();
        }

        UUID id = UUID.randomUUID();

        BeaconSettlement settlement = new BeaconSettlement(id, founder);
        settlement.getBeacons().add(beacon);

        settlements.put(id, settlement);
        beaconSettlements.put(beacon, id);

        setDirty();

        return Optional.of(settlement);
    }

    /**
     * Get a settlement by its UUID.
     */
    public BeaconSettlement getSettlement(UUID settlementId) {
        return settlements.get(settlementId);
    }

    /**
     * Find which settlement a beacon belongs to.
     */
    public BeaconSettlement getSettlementAtBeacon(GlobalPos beacon) {
        UUID settlementId = beaconSettlements.get(beacon);

        if (settlementId == null) {
            return null;
        }

        return settlements.get(settlementId);
    }

    /**
     * Add another beacon to an existing settlement.
     */
    public boolean addBeaconToSettlement(
            GlobalPos beacon,
            UUID settlementId
    ) {
        if (beaconSettlements.containsKey(beacon)) {
            return false;
        }

        BeaconSettlement settlement = settlements.get(settlementId);

        if (settlement == null) {
            return false;
        }

        settlement.getBeacons().add(beacon);
        beaconSettlements.put(beacon, settlementId);

        setDirty();
        return true;
    }

    /**
     * Remove a beacon from its settlement.
     * Delete the settlement if it no longer has any beacons.
     */
    public boolean removeBeacon(GlobalPos beacon) {
        UUID settlementId = beaconSettlements.remove(beacon);

        if (settlementId == null) {
            return false;
        }

        BeaconSettlement settlement = settlements.get(settlementId);

        if (settlement != null) {
            settlement.getBeacons().remove(beacon);

            if (settlement.getBeacons().isEmpty()) {
                settlements.remove(settlementId);
            }
        }

        setDirty();
        return true;
    }

    /**
     * Delete an entire settlement.
     * Intended for server operator commands.
     */
    public boolean deleteSettlement(UUID settlementId) {
        BeaconSettlement settlement = settlements.remove(settlementId);

        if (settlement == null) {
            return false;
        }

        for (GlobalPos beacon : settlement.getBeacons()) {
            beaconSettlements.remove(beacon);
        }

        setDirty();
        return true;
    }

    /**
     * Add a member.
     * Founders and Officers can add members.
     */
    public boolean addSettlementMember(
            UUID settlementId,
            UUID actor,
            UUID target
    ) {
        BeaconSettlement settlement = settlements.get(settlementId);

        if (settlement == null || !settlement.canManageMembers(actor)) {
            return false;
        }

        boolean changed = settlement.addMember(target);

        if (changed) {
            setDirty();
        }

        return changed;
    }

    /**
     * Remove a member.
     * Officers can only remove regular members.
     * Founders can also remove Officers.
     */
    public boolean removeSettlementMember(
            UUID settlementId,
            UUID actor,
            UUID target
    ) {
        BeaconSettlement settlement = settlements.get(settlementId);

        if (settlement == null || actor.equals(target)) {
            return false;
        }

        boolean isFounder = settlement.isFounder(actor);
        boolean isOfficer = settlement.isOfficer(actor);

        if (!isFounder && !isOfficer) {
            return false;
        }

        if (settlement.isFounder(target)) {
            return false;
        }

        if (settlement.isOfficer(target) && !isFounder) {
            return false;
        }

        boolean changed = settlement.removeMember(actor, target);

        if (changed) {
            setDirty();
        }

        return changed;
    }

    /**
     * Promote a regular member to Officer.
     * Only the Founder can do this.
     */
    public boolean promoteSettlementOfficer(
            UUID settlementId,
            UUID actor,
            UUID target
    ) {
        BeaconSettlement settlement = settlements.get(settlementId);

        if (settlement == null || !settlement.isFounder(actor)) {
            return false;
        }

        boolean changed = settlement.promoteToOfficer(actor, target);

        if (changed) {
            setDirty();
        }

        return changed;
    }

    /**
     * Demote an Officer to Member.
     * Only the Founder can do this.
     */
    public boolean demoteSettlementOfficer(
            UUID settlementId,
            UUID actor,
            UUID target
    ) {
        BeaconSettlement settlement = settlements.get(settlementId);

        if (settlement == null) {
            return false;
        }

        boolean changed = settlement.demoteOfficer(actor, target);

        if (changed) {
            setDirty();
        }

        return changed;
    }

    /**
     * Transfer founder status to another settlement member.
     */
    public boolean transferSettlementFounder(
            UUID settlementId,
            UUID actor,
            UUID target
    ) {
        BeaconSettlement settlement = settlements.get(settlementId);

        if (settlement == null) {
            return false;
        }

        boolean changed = settlement.transferFounder(actor, target);

        if (changed) {
            setDirty();
        }

        return changed;
    }

}
