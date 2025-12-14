// CoordinateDebugger.java
package com.easttown.ticketsystem.client;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.screen.TicketMachineScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class CoordinateDebugger {
    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Pre event) {
        // 使用全局调试模式
        if (TicketSystemMod.debugMode) {
            Minecraft mc = Minecraft.getInstance();
            int mouseX = (int) mc.mouseHandler.xpos();
            int mouseY = (int) mc.mouseHandler.ypos();
            
            String coords = String.format("Mouse: %d, %d", mouseX, mouseY);
        
            // 添加GUI位置信息
            if (mc.screen instanceof TicketMachineScreen screen) {
                try {
                    // 获取GUI位置
                    java.lang.reflect.Field leftPosField = TicketMachineScreen.class.getDeclaredField("leftPos");
                    leftPosField.setAccessible(true);
                    int leftPos = (int) leftPosField.get(screen);
                    
                    java.lang.reflect.Field topPosField = TicketMachineScreen.class.getDeclaredField("topPos");
                    topPosField.setAccessible(true);
                    int topPos = (int) topPosField.get(screen);
                    
                    coords += String.format(" | GUI: [%d,%d]", leftPos, topPos);
                } catch (Exception e) {
                    coords += " | GUI: [access failed]";
                }
            }
            
            event.getGuiGraphics().drawString(
                mc.font,
                coords,
                10, 30, // 在屏幕左上角显示
                0xFFFFFF,
                false
            );
        }
    }
}
