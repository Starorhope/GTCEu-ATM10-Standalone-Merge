package com.raishxn.gtna.client.renderer;

import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class RenderBufferHelper {

    public static void renderCylinder(PoseStack poseStack, VertexConsumer buffer, float x, float y, float z,
                                      float radius, float height, int sides, float red, float green, float blue,
                                      float alpha) {
        Matrix4f mat = poseStack.last().pose();
        float angleStep = (float) ((Math.PI * 2D) / (double) sides);

        for (int i = 0; i < sides; ++i) {
            float angle1 = (float) i * angleStep;
            float angle2 = (float) (i + 1) * angleStep;
            float cosAngle1 = Mth.cos(angle1);
            float sinAngle1 = Mth.sin(angle1);
            float cosAngle2 = Mth.cos(angle2);
            float sinAngle2 = Mth.sin(angle2);
            buffer.addVertex(mat, x + cosAngle1 * radius, y, z + sinAngle1 * radius)
                    .setColor(red, green, blue, alpha);
            buffer.addVertex(mat, x + cosAngle2 * radius, y, z + sinAngle2 * radius)
                    .setColor(red, green, blue, alpha);
            buffer.addVertex(mat, x + cosAngle2 * radius, y + height, z + sinAngle2 * radius)
                    .setColor(red, green, blue, alpha);
            buffer.addVertex(mat, x + cosAngle1 * radius, y, z + sinAngle1 * radius)
                    .setColor(red, green, blue, alpha);
            buffer.addVertex(mat, x + cosAngle2 * radius, y + height, z + sinAngle2 * radius)
                    .setColor(red, green, blue, alpha);
            buffer.addVertex(mat, x + cosAngle1 * radius, y + height, z + sinAngle1 * radius)
                    .setColor(red, green, blue, alpha);
        }
    }
}
