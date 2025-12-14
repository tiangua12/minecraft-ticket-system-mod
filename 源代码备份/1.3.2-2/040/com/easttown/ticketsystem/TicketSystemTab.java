// TicketSystemTab.java
package com.easttown.ticketsystem;

import com.easttown.ticketsystem.init.ItemInit;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class TicketSystemTab {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            TicketSystemMod.MODID);

    public static final RegistryObject<CreativeModeTab> TICKET_TAB = TABS.register("ticket_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ticketsystem"))
                    .icon(() -> new ItemStack(ItemInit.TICKET.get()))
                    .displayItems((params, output) -> {
                        output.accept(ItemInit.TICKET_MACHINE_ITEM.get());
                        output.accept(ItemInit.TICKET.get());
                        output.accept(ItemInit.ADMIN_KEY.get());
                        output.accept(ItemInit.COPPER_COIN.get());
                        output.accept(ItemInit.IRON_COIN.get());
                        output.accept(ItemInit.DIAMOND_COIN.get());
                        output.accept(ItemInit.GOLD_COIN.get());
                        output.accept(ItemInit.NETHERITE_COIN.get());
                        output.accept(ItemInit.EMERALD_COIN.get());
                        output.accept(ItemInit.GATE_ITEM.get());
                        output.accept(ItemInit.ITEM9875IS0.get());
                        output.accept(ItemInit.REISSUE_MACHINE_ITEM.get());
                        output.accept(ItemInit.REIMBURSEMENT_VOUCHER.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}