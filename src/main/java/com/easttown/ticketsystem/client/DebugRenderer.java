// DebugRenderer.java (辅助渲染)
package com.easttown.ticketsystem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;

public class DebugRenderer {
    public static void renderAABB(PoseStack poseStack, MultiBufferSource buffer, 
                                 AABB aabb, int packedLight, 
                                 float r, float g, float b, float a) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.LINES);
        
        double minX = aabb.minX;
        double minY = aabb.minY;
        double minZ = aabb.minZ;
        double maxX = aabb.maxX;
        double maxY = aabb.maxY;
        double maxZ = aabb.maxZ;
        
        // 绘制底部
        renderLine(vertexConsumer, poseStack, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        renderLine(vertexConsumer, poseStack, minX, minY, minZ, minX, minY, maxZ, r, g, b, a);
        renderLine(vertexConsumer, poseStack, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        renderLine(vertexConsumer, poseStack, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, a);
        
        // 绘制顶部
        renderLine(vertexConsumer, poseStack, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        renderLine(vertexConsumer, poseStack, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
        renderLine(vertexConsumer, poseStack, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        renderLine(vertexConsumer, poseStack, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        
        // 绘制侧边
        renderLine(vertexConsumer, poseStack, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        renderLine(vertexConsumer, poseStack, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        renderLine(vertexConsumer, poseStack, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
        renderLine(vertexConsumer, poseStack, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
    }
    
    private static void renderLine(VertexConsumer consumer, PoseStack poseStack, 
                                  double x1, double y1, double z1,
                                  double x2, double y2, double z2,
                                  float r, float g, float b, float a) {
        consumer.vertex(poseStack.last().pose(), (float) x1, (float) y1, (float) z1)
                .color(r, g, b, a)
                .normal(0, 1, 0)
                .endVertex();
        consumer.vertex(poseStack.last().pose(), (float) x2, (float) y2, (float) z2)
                .color(r, g, b, a)
                .normal(0, 1, 0)
                .endVertex();
    }
}
