package com.easttown.ticketsystem.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CoinConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // 硬币类型配置
    public static final ForgeConfigSpec.ConfigValue<String> COPPER_COIN_ITEM;
    public static final ForgeConfigSpec.ConfigValue<String> IRON_COIN_ITEM;
    public static final ForgeConfigSpec.ConfigValue<String> GOLD_COIN_ITEM;
    public static final ForgeConfigSpec.ConfigValue<String> EMERALD_COIN_ITEM;
    public static final ForgeConfigSpec.ConfigValue<String> DIAMOND_COIN_ITEM;
    public static final ForgeConfigSpec.ConfigValue<String> NETHERITE_COIN_ITEM;
    
    // 硬币汇率
    public static final ForgeConfigSpec.IntValue COPPER_TO_IRON_RATE;
    public static final ForgeConfigSpec.IntValue IRON_TO_GOLD_RATE;
    public static final ForgeConfigSpec.IntValue GOLD_TO_EMERALD_RATE;
    public static final ForgeConfigSpec.IntValue EMERALD_TO_DIAMOND_RATE;
    public static final ForgeConfigSpec.IntValue DIAMOND_TO_NETHERITE_RATE;
    
    // 每格距离的费用（铜币）
    public static final ForgeConfigSpec.IntValue COST_PER_BLOCK;
    
    static {
        BUILDER.push("硬币系统设置");
        
        // 硬币物品注册名
        COPPER_COIN_ITEM = BUILDER.comment("铜币物品ID")
            .define("copper_coin_item", "ticketsystem:copper_coin");
        IRON_COIN_ITEM = BUILDER.comment("铁币物品ID")
            .define("iron_coin_item", "ticketsystem:iron_coin");
        GOLD_COIN_ITEM = BUILDER.comment("金币物品ID")
            .define("gold_coin_item", "ticketsystem:gold_coin");
        EMERALD_COIN_ITEM = BUILDER.comment("绿宝石币物品ID")
            .define("emerald_coin_item", "ticketsystem:emerald_coin");
        DIAMOND_COIN_ITEM = BUILDER.comment("钻石币物品ID")
            .define("diamond_coin_item", "ticketsystem:diamond_coin");
        NETHERITE_COIN_ITEM = BUILDER.comment("下界合金币物品ID")
            .define("netherite_coin_item", "ticketsystem:netherite_coin");
        
        // 硬币兑换汇率
        COPPER_TO_IRON_RATE = BUILDER.comment("铜币兑换铁币汇率 (1铁币 = X铜币)")
            .defineInRange("copper_to_iron_rate", 10, 1, 1000);
        IRON_TO_GOLD_RATE = BUILDER.comment("铁币兑换金币汇率 (1金币 = X铁币)")
            .defineInRange("iron_to_gold_rate", 10, 1, 1000);
        GOLD_TO_EMERALD_RATE = BUILDER.comment("金币兑换绿宝石币汇率 (1绿宝石币 = X金币)")
            .defineInRange("gold_to_emerald_rate", 10, 1, 1000);
        EMERALD_TO_DIAMOND_RATE = BUILDER.comment("绿宝石币兑换钻石币汇率 (1钻石币 = X绿宝石币)")
            .defineInRange("emerald_to_diamond_rate", 10, 1, 1000);
        DIAMOND_TO_NETHERITE_RATE = BUILDER.comment("钻石币兑换下界合金币汇率 (1下界合金币 = X钻石币)")
            .defineInRange("diamond_to_netherite_rate", 10, 1, 1000);
        
        // 每格距离费用
        COST_PER_BLOCK = BUILDER.comment("每格距离费用 (铜币)")
            .defineInRange("cost_per_block", 1, 1, 100);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    
    public static int getCopperToIronRate() {
        return COPPER_TO_IRON_RATE.get();
    }
    
    public static int getIronToGoldRate() {
        return IRON_TO_GOLD_RATE.get();
    }
    
    public static int getGoldToEmeraldRate() {
        return GOLD_TO_EMERALD_RATE.get();
    }
    
    public static int getEmeraldToDiamondRate() {
        return EMERALD_TO_DIAMOND_RATE.get();
    }
    
    public static int getDiamondToNetheriteRate() {
        return DIAMOND_TO_NETHERITE_RATE.get();
    }
    
    public static int getCostPerBlock() {
        return COST_PER_BLOCK.get();
    }
}