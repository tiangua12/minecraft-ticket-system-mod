package com.easttown.ticketsystem.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class EasterEggHandler {

    public static boolean checkForEasterEgg(String message, Player player) {
        // 直接检查消息内容
        if (message.equals("Colo9875 is 0!")) {
            triggerEasterEgg(player);
            return true;
        }

        return false;
    }

    private static void triggerEasterEgg(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // 给予铜币奖励 - 使用ItemInit中的注册对象
            ItemStack copperCoin = new ItemStack(
                com.easttown.ticketsystem.init.ItemInit.COPPER_COIN.get()
            );

            if (!player.getInventory().add(copperCoin)) {
                player.drop(copperCoin, false);
            }

            // 给予生命恢复效果
            serverPlayer.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                300, // 15秒 * 20 ticks/秒 = 300 ticks
                1
            ));

            // 播放音效
            serverPlayer.level().playSound(
                null,
                serverPlayer.getX(),
                serverPlayer.getY(),
                serverPlayer.getZ(),
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS,
                1.0f,
                1.0f
            );
        }
    }
}