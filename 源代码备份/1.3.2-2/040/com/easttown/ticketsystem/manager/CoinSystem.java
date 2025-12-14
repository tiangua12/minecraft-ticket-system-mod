package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.config.CoinConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class CoinSystem {
    // 硬币价值映射（按从低到高排序）
    private static final Map<String, Integer> coinValues = new LinkedHashMap<>();
    
    static {
        // 初始化硬币价值（以铜币为单位）
        coinValues.put(CoinConfig.COPPER_COIN_ITEM.get(), 1);
        coinValues.put(CoinConfig.IRON_COIN_ITEM.get(), CoinConfig.getCopperToIronRate());
        coinValues.put(CoinConfig.GOLD_COIN_ITEM.get(), 
            CoinConfig.getIronToGoldRate() * CoinConfig.getCopperToIronRate());
        coinValues.put(CoinConfig.EMERALD_COIN_ITEM.get(), 
            CoinConfig.getGoldToEmeraldRate() * CoinConfig.getIronToGoldRate() * CoinConfig.getCopperToIronRate());
        coinValues.put(CoinConfig.DIAMOND_COIN_ITEM.get(), 
            CoinConfig.getEmeraldToDiamondRate() * CoinConfig.getGoldToEmeraldRate() * 
            CoinConfig.getIronToGoldRate() * CoinConfig.getCopperToIronRate());
        coinValues.put(CoinConfig.NETHERITE_COIN_ITEM.get(), 
            CoinConfig.getDiamondToNetheriteRate() * CoinConfig.getEmeraldToDiamondRate() * 
            CoinConfig.getGoldToEmeraldRate() * CoinConfig.getIronToGoldRate() * 
            CoinConfig.getCopperToIronRate());
    }
    
    // 计算最优硬币组合
    public static Map<String, Integer> calculateOptimalCoins(int amount) {
        Map<String, Integer> result = new HashMap<>();
        int remaining = amount;
        
        // 从高级到低级计算
        List<String> coins = new ArrayList<>(coinValues.keySet());
        Collections.reverse(coins);
        
        for (String coinId : coins) {
            int value = coinValues.get(coinId);
            if (remaining >= value) {
                int count = remaining / value;
                result.put(coinId, count);
                remaining %= value;
            }
        }
        
        return result;
    }
    
    // 检查玩家是否有足够硬币
    public static boolean hasSufficientCoins(Player player, int price) {
        return getPlayerCopperValue(player) >= price;
    }
    
    // 获取玩家硬币总价值（铜币）
    public static int getPlayerCopperValue(Player player) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : coinValues.entrySet()) {
            String coinId = entry.getKey();
            Item coinItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(coinId));
            if (coinItem != null) {
                int count = countItems(player.getInventory(), coinItem);
                total += count * entry.getValue();
            }
        }
        return total;
    }
    
    // 扣除硬币并返回实际扣除的硬币（考虑找零）
    public static Map<String, Integer> deductWithChange(Player player, int price) {
        int playerValue = getPlayerCopperValue(player);
        if (playerValue < price) return Collections.emptyMap();
        
        // 先移除全部硬币
        for (Map.Entry<String, Integer> entry : coinValues.entrySet()) {
            String coinId = entry.getKey();
            Item coinItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(coinId));
            if (coinItem != null) {
                removeItems(player.getInventory(), coinItem, countItems(player.getInventory(), coinItem));
            }
        }
        
        // 计算并给予找零
        int change = playerValue - price;
        Map<String, Integer> changeCoins = calculateOptimalCoins(change);
        
        for (Map.Entry<String, Integer> entry : changeCoins.entrySet()) {
            String coinId = entry.getKey();
            Item coinItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(coinId));
            if (coinItem != null && entry.getValue() > 0) {
                giveItems(player, coinItem, entry.getValue());
            }
        }
        
        // 返回实际扣除的硬币
        return calculateOptimalCoins(price);
    }
    
    // 格式化差额为可读字符串
    public static String formatDeficit(int deficitCopper) {
        if (deficitCopper <= 0) return "0";
        
        Map<String, Integer> coins = calculateOptimalCoins(deficitCopper);
        StringBuilder sb = new StringBuilder();
        
        List<String> coinTypes = Arrays.asList(
            CoinConfig.NETHERITE_COIN_ITEM.get(),
            CoinConfig.DIAMOND_COIN_ITEM.get(),
            CoinConfig.EMERALD_COIN_ITEM.get(),
            CoinConfig.GOLD_COIN_ITEM.get(),
            CoinConfig.IRON_COIN_ITEM.get(),
            CoinConfig.COPPER_COIN_ITEM.get()
        );
        
        for (String coinType : coinTypes) {
            if (coins.containsKey(coinType) && coins.get(coinType) > 0) {
                if (sb.length() > 0) sb.append(" + ");
                sb.append(coins.get(coinType)).append(" ").append(getCoinName(coinType));
            }
        }
        
        return sb.toString();
    }
    
    // 获取硬币名称
    public static String getCoinName(String coinId) {
        if (coinId.equals(CoinConfig.COPPER_COIN_ITEM.get())) return "铜币";
        if (coinId.equals(CoinConfig.IRON_COIN_ITEM.get())) return "铁币";
        if (coinId.equals(CoinConfig.GOLD_COIN_ITEM.get())) return "金币";
        if (coinId.equals(CoinConfig.EMERALD_COIN_ITEM.get())) return "绿宝石币";
        if (coinId.equals(CoinConfig.DIAMOND_COIN_ITEM.get())) return "钻石币";
        if (coinId.equals(CoinConfig.NETHERITE_COIN_ITEM.get())) return "下界合金币";
        return "硬币";
    }
    
    // 获取硬币价值（以铜币为单位）
    public static int getCoinValue(String coinId) {
        return coinValues.getOrDefault(coinId, 0);
    }

    // 检查是否为硬币
    public static boolean isCoin(String coinId) {
        return coinValues.containsKey(coinId);
    }
    
    // 统计物品数量
    private static int countItems(Inventory inventory, Item item) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    // 移除物品
    private static void removeItems(Inventory inventory, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
                if (remaining <= 0) break;
            }
        }
    }
    
    // 给予物品
    public static void giveItems(Player player, Item item, int amount) {
        while (amount > 0) {
            int stackSize = Math.min(item.getMaxStackSize(), amount);
            ItemStack stack = new ItemStack(item, stackSize);
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
            amount -= stackSize;
        }
    }
}
