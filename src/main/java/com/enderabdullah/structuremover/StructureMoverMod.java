package com.enderabdullah.structuremover;

import com.enderabdullah.structuremover.block.VehicleAssemblerBlock;
import com.enderabdullah.structuremover.entity.VehicleEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(StructureMoverMod.MOD_ID)
public class StructureMoverMod {
    public static final String MOD_ID = "structuremover";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);

    public static final Supplier<Block> VEHICLE_ASSEMBLER = BLOCKS.register("vehicle_assembler",
            () -> new VehicleAssemblerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(3.0f, 6.0f).requiresCorrectToolForDrops()));

    public static final Supplier<Block> VEHICLE_WHEEL = BLOCKS.register("vehicle_wheel",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(2.0f, 3.0f)));

    public static final Supplier<Block> STEERING_WHEEL = BLOCKS.register("steering_wheel",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD).strength(1.5f, 3.0f)));

    public static final Supplier<Item> VEHICLE_ASSEMBLER_ITEM = ITEMS.register("vehicle_assembler",
            () -> new BlockItem(VEHICLE_ASSEMBLER.get(), new Item.Properties()));
    public static final Supplier<Item> VEHICLE_WHEEL_ITEM = ITEMS.register("vehicle_wheel",
            () -> new BlockItem(VEHICLE_WHEEL.get(), new Item.Properties()));
    public static final Supplier<Item> STEERING_WHEEL_ITEM = ITEMS.register("steering_wheel",
            () -> new BlockItem(STEERING_WHEEL.get(), new Item.Properties()));

    public static final Supplier<EntityType<VehicleEntity>> VEHICLE = ENTITY_TYPES.register("vehicle",
            () -> EntityType.Builder.<VehicleEntity>of(VehicleEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("vehicle"));

    public StructureMoverMod(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(VEHICLE_ASSEMBLER_ITEM.get());
            event.accept(VEHICLE_WHEEL_ITEM.get());
            event.accept(STEERING_WHEEL_ITEM.get());
        }
    }
}
