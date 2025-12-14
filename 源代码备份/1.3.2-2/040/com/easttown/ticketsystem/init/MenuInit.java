package com.easttown.ticketsystem.init;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.screen.GateConfigMenu;
import com.easttown.ticketsystem.screen.TicketMachineMenu;
import com.easttown.ticketsystem.screen.terminal.menu.TerminalMainMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuInit {
    public static final DeferredRegister<MenuType<?>> MENUS = 
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, TicketSystemMod.MODID);
    
    public static final RegistryObject<MenuType<TicketMachineMenu>> TICKET_MACHINE_MENU = 
        MENUS.register("ticket_machine_menu", 
            () -> IForgeMenuType.create((windowId, inv, data) -> 
                new TicketMachineMenu(windowId, inv, data)));
    
    // 添加闸机配置菜单注册
    public static final RegistryObject<MenuType<GateConfigMenu>> GATE_CONFIG_MENU =
        MENUS.register("gate_config_menu",
            () -> IForgeMenuType.create((windowId, inv, data) ->
                new GateConfigMenu(windowId, inv, data)));

    // 旅行服务终端主菜单
    public static final RegistryObject<MenuType<TerminalMainMenu>> TERMINAL_MAIN_MENU =
        MENUS.register("terminal_main_menu",
            () -> IForgeMenuType.create((windowId, inv, data) ->
                new TerminalMainMenu(windowId, inv, data)));

}
