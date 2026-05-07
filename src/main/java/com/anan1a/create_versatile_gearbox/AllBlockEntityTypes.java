package com.anan1a.create_versatile_gearbox;

import com.anan1a.create_versatile_gearbox.content.versatile_gearbox.VersatileGearboxBlockEntity;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import net.neoforged.bus.api.IEventBus;

public class AllBlockEntityTypes {

    private static final CreateRegistrate REGISTRATE = Registers.registrate();

    public static final BlockEntityEntry<VersatileGearboxBlockEntity> VERSATILE_GEARBOX = REGISTRATE
            .blockEntity("versatile_gearbox", VersatileGearboxBlockEntity::new)
            .validBlocks(AllBlocks.VERSATILE_GEARBOX)
            .register();

    public static void register(IEventBus modEventBus) {
    }
}