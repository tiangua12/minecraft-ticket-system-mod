package com.easttown.ticketsystem.init;

import com.easttown.ticketsystem.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.easttown.ticketsystem.*;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
            TicketSystemMod.MODID);

    // 原有物品
    public static final RegistryObject<Item> TICKET_MACHINE_ITEM = ITEMS.register("ticket_machine",
            () -> new BlockItem(BlockInit.TICKET_MACHINE.get(), new Item.Properties()));
    public static final RegistryObject<Item> TICKET = ITEMS.register("ticket",
            () -> new TicketItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ADMIN_KEY = ITEMS.register("admin_key",
            () -> new AdminKeyItem(new Item.Properties().stacksTo(1)));

    // 硬币物品
    public static final RegistryObject<Item> COPPER_COIN = ITEMS.register("copper_coin",
            CopperCoinItem::new);
    public static final RegistryObject<Item> IRON_COIN = ITEMS.register("iron_coin",
            IronCoinItem::new);
    public static final RegistryObject<Item> GOLD_COIN = ITEMS.register("gold_coin",
            GoldCoinItem::new);
    public static final RegistryObject<Item> DIAMOND_COIN = ITEMS.register("diamond_coin",
            DiamondCoinItem::new);
    public static final RegistryObject<Item> EMERALD_COIN = ITEMS.register("emerald_coin",
            EmeraldCoinItem::new);
    public static final RegistryObject<Item> NETHERITE_COIN = ITEMS.register("netherite_coin",
            NetheriteCoinItem::new);
    public static final RegistryObject<Item> ITEM9875IS0 = ITEMS.register("9875is0",
            ITEM9875is0::new);
            // 在ItemInit类中添加
    public static final RegistryObject<Item> REIMBURSEMENT_VOUCHER = ITEMS.register("reimbursement_voucher",
            () -> new ReimbursementVoucherItem(new Item.Properties()));

    
    // 闸机物品
    public static final RegistryObject<Item> GATE_ITEM = ITEMS.register("gate",
            () -> new BlockItem(BlockInit.GATE.get(), new Item.Properties()));
            public static final RegistryObject<Item> REISSUE_MACHINE_ITEM = ITEMS.register("reissue_machine",
        () -> new BlockItem(BlockInit.REISSUE_MACHINE.get(), new Item.Properties()));

    // 添加自定义蛋糕物品
    public static final RegistryObject<Item> CUSTOM_CAKE_ITEM = ITEMS.register("custom_cake",
        () -> new BlockItem(BlockInit.CUSTOM_CAKE.get(), new Item.Properties()));
}
