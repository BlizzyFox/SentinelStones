
package com.blizzy.sentinelstones.protection;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.GlobalPos;

public class BeaconSettlement {

    private final UUID id;
    private UUID founder;

    private final Set<UUID> officers = new HashSet<>();
    private final Set<UUID> members = new HashSet<>();
    private final Set<GlobalPos> beacons = new HashSet<>();

    public BeaconSettlement(UUID id, UUID founder) {
        this.id = id;
        this.founder = founder;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFounder() {
        return founder;
    }

    public Set<UUID> getOfficers() {
        return officers;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public Set<GlobalPos> getBeacons() {
        return beacons;
    }

    public boolean isFounder(UUID player) {
        return founder.equals(player);
    }

    public boolean isOfficer(UUID player) {
        return officers.contains(player);
    }

    public boolean isMember(UUID player) {
        return founder.equals(player)
                || officers.contains(player)
                || members.contains(player);
    }

    public boolean canManageMembers(UUID player) {
        return isFounder(player) || isOfficer(player);
    }

    public boolean addMember(UUID player) {
        if (isMember(player)) {
            return false;
        }

        return members.add(player);
    }

    public boolean removeMember(UUID actor, UUID target) {
        if (actor.equals(target) || isFounder(target)) {
            return false;
        }

        if (isFounder(actor)) {
            if (officers.remove(target)) {
                return true;
            }

            return members.remove(target);
        }

        if (isOfficer(actor)) {
            return members.remove(target);
        }

        return false;
    }

    public boolean promoteToOfficer(UUID actor, UUID target) {
        if (!isFounder(actor) || !members.remove(target)) {
            return false;
        }

        return officers.add(target);
    }

    public boolean demoteOfficer(UUID actor, UUID target) {
        if (!isFounder(actor) || !officers.remove(target)) {
            return false;
        }

        members.add(target);
        return true;
    }

    public boolean transferFounder(UUID actor, UUID target) {
        if (!isFounder(actor) || !isMember(target) || actor.equals(target)) {
            return false;
        }

        UUID previousFounder = founder;

        officers.remove(target);
        members.remove(target);

        founder = target;
        members.add(previousFounder);

        return true;
    }
}
