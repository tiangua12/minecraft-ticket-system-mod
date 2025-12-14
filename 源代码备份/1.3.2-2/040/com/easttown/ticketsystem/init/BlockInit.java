package com.easttown.ticketsystem.init;

import com.easttown.ticketsystem.TicketSystemMod;
//import com.easttown.ticketsystem.block.GateBlock;
//import com.easttown.ticketsystem.block.TicketMachineBlock;
import com.easttown.ticketsystem.block.GateBlock;
import com.easttown.ticketsystem.block.TicketMachineBlock;
import com.easttown.ticketsystem.block.ReissueMachineBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockInit {
    public static final DeferredRegister<Block> BLOCKS = 
        DeferredRegister.create(ForgeRegistries.BLOCKS, TicketSystemMod.MODID);
    
    public static final RegistryObject<Block> TICKET_MACHINE = BLOCKS.register("ticket_machine", 
        () -> new TicketMachineBlock(BlockBehaviour.Properties.of()
            .strength(3.5f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
    ));
    
    // 添加闸机方块的注册
    public static final RegistryObject<Block> GATE = BLOCKS.register("gate", 
        () -> new GateBlock(BlockBehaviour.Properties.of()
            .strength(3.5f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .noOcclusion()
    ));
    
    // 添加补票机器方块
public static final RegistryObject<Block> REISSUE_MACHINE = BLOCKS.register("reissue_machine",
    () -> new ReissueMachineBlock(BlockBehaviour.Properties.of()
        .mapColor(MapColor.METAL)
        .strength(5.0f, 6.0f)
        .sound(SoundType.METAL)
        .requiresCorrectToolForDrops()
        .noOcclusion()
));

}
