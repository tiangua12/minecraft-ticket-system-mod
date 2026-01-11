package com.easttown.ticketsystem.init;

import com.easttown.ticketsystem.block.GateBlockEntity;
import com.easttown.ticketsystem.block.TicketMachineBlockEntity;
import com.easttown.ticketsystem.block.ReissueMachineBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.easttown.ticketsystem.*;

public class BlockEntityInit {
    public static final DeferredRegister<
            BlockEntityType<
                    ?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TicketSystemMod.MODID);

    public static final RegistryObject<
            BlockEntityType<
                    TicketMachineBlockEntity>> TICKET_MACHINE = BLOCK_ENTITIES.register("ticket_machine",
            () -> BlockEntityType.Builder.of(
                    TicketMachineBlockEntity::new,
                    BlockInit.TICKET_MACHINE.get()
            ).build(null));

    // 添加闸机方块实体的注册
    public static final RegistryObject<
            BlockEntityType<GateBlockEntity>> GATE = BLOCK_ENTITIES.register("gate",
            () -> BlockEntityType.Builder.of(
                    GateBlockEntity::new,
                    BlockInit.GATE.get()
            ).build(null));
    // 添加补票机器方块实体
    public static final RegistryObject<
            BlockEntityType<
                    ReissueMachineBlockEntity>> REISSUE_MACHINE = BLOCK_ENTITIES.register("reissue_machine",
            () -> BlockEntityType.Builder.of(
                    ReissueMachineBlockEntity::new,
                    BlockInit.REISSUE_MACHINE.get()
            ).build(null));

}
