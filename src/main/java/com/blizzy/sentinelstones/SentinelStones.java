package com.blizzy.sentinelstones;

import org.slf4j.Logger;

import com.blizzy.sentinelstones.block.ProtectionStoneBlock;
import com.blizzy.sentinelstones.commands.SentinelStonesCommands;
import com.blizzy.sentinelstones.protection.BeaconEffectHandler;
import com.blizzy.sentinelstones.protection.BeaconProtectionEvents;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.ModList;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SentinelStones.MODID)
public class SentinelStones {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "sentinelstones";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "sentinelstones" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "sentinelstones" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "sentinelstones" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /* */
    // Creates a new Block with the id "sentinelstones:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "sentinelstones:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);
    // Sentinel Stone
    public static final DeferredBlock<Block> SENTINEL_STONE =
            BLOCKS.registerBlock(
                "sentinel_stone",
                ProtectionStoneBlock::new,
                BlockBehaviour.Properties.of().explosionResistance(0.0f).destroyTime(1.0f)
            );
    
    public static final DeferredItem<BlockItem> SENTINEL_STONE_ITEM =
            ITEMS.registerSimpleBlockItem(
                "sentinel_stone", SENTINEL_STONE
            );

    // Creates a new food item with the id "sentinelstones:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // Creates a creative tab with the id "sentinelstones:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.sentinelstones")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> SENTINEL_STONE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                //output.accept(EXAMPLE_ITEM.get());// Add the example item to the tab. For your own tabs, this method is preferred over the event
                output.accept(SENTINEL_STONE.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public SentinelStones(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (SentinelStones) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
        // Add Commands.
        //modEventBus.addListener(SentinelStonesCommands::registerCommands);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        NeoForge.EVENT_BUS.addListener(SentinelStonesCommands::registerCommands);
        NeoForge.EVENT_BUS.addListener(BeaconProtectionEvents::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(BeaconProtectionEvents::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(BeaconProtectionEvents::onBlockInteract);
        NeoForge.EVENT_BUS.addListener(BeaconProtectionEvents::onExplosionDetonate);
        NeoForge.EVENT_BUS.addListener(BeaconEffectHandler::onPlayerTick);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
        if (ModList.get().isLoaded("create")) {
            event.enqueueWork(() -> {
                try {
                    Class<?> compatibility = Class.forName(
                            "com.blizzy.sentinelstones.compat.create.CreateCompatibility"
                    );

                    compatibility.getMethod("register").invoke(null);

                    LOGGER.info("Create compatibility enabled.");
                } catch (ReflectiveOperationException exception) {
                    throw new RuntimeException(
                            "Failed to initialize Create compatibility.",
                            exception
                    );
                }
            });
        }
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("Sentinel Stones loaded.");
    }

}
