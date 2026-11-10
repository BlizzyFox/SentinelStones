package com.blizzy.sentinelstones;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static Set<Block> blacklistedBlocksCache = Collections.emptySet();
	public static Set<Block> allowedWorkstationsCache = Collections.emptySet();
	/* 
    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);
	*/
    public static final ModConfigSpec.BooleanValue EXPLOSION_VULN = BUILDER
            .comment("Should Sentinel Stones be vulnerable to explosives?")
            .define("explosivesDamageSentinelStones", false);

	public static final ModConfigSpec.IntValue SENTINEL_RANGE = BUILDER
			.comment("Range that a Sentinel Stone protects.")
			.defineInRange("sentinelRange", 1, 1, Integer.MAX_VALUE);

	public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_BLOCKS = BUILDER
        .comment("Blacklist these blocks from being placed in the range of a bound beacons magic:")
        .defineListAllowEmpty("blacklistedBlocks", List.of(
            "minecraft:tnt",
            "minecraft:piston",
            "minecraft:sticky_piston",
            "minecraft:dispenser",
            "minecraft:dropper",
            "minecraft:redstone_wire",
            "minecraft:redstone_torch",
            "minecraft:repeater",
            "minecraft:comparator",
            "minecraft:rail",
            "minecraft:powered_rail",
            "minecraft:respawn_anchor",
            "sentinelstones:sentinel_stone",
            "minecraft:water",
            "minecraft:lava"
    	), () -> "", Config::validateItemName);
	
	public static final ModConfigSpec.ConfigValue<List<? extends String>> ALLOWED_WORKSTATIONS = BUILDER
		.comment("Allow any player to access these workstations in the range of a bound beacons magic:")
		.defineListAllowEmpty("allowedWorkstations", List.of(
			"minecraft:crafting_table", "minecraft:furnace", "minecraft:blast_furnace",
            "minecraft:smoker", "minecraft:smithing_table", "minecraft:anvil",
            "minecraft:chipped_anvil", "minecraft:damaged_anvil", "minecraft:enchanting_table",
            "minecraft:stonecutter", "minecraft:loom", "minecraft:cartography_table",
            "minecraft:grindstone", "minecraft:brewing_stand"
		), () -> "", Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

	 // Call this whenever the config loads or reloads to update the sets
    public static void bakeConfig() {
        blacklistedBlocksCache = BLACKLISTED_BLOCKS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .map(BuiltInRegistries.BLOCK::get)
                // Filter out 'air' to ignore invalid entries safely
                .filter(block -> block != BuiltInRegistries.BLOCK.get(BuiltInRegistries.BLOCK.getDefaultKey())) 
                .collect(Collectors.toUnmodifiableSet());

        allowedWorkstationsCache = ALLOWED_WORKSTATIONS.get().stream()
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .map(BuiltInRegistries.BLOCK::get)
                .filter(block -> block != BuiltInRegistries.BLOCK.get(BuiltInRegistries.BLOCK.getDefaultKey()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
