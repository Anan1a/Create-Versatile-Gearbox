package com.anan1a.create_versatile_gearbox.foundation.gui;

import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.gui.AllIcons;

import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;

/**
 * 模组自定义图标，引用 {@code assets/create_versatile_gearbox/textures/gui/icons.png}。
 * <p>
 * 图集大小 128×128，每个图标 16×16（8 列 × 8 行）。
 * UV 计算使用 {@link #ICON_ATLAS_SIZE} = 128（父类 {@link AllIcons} 为 256）。
 */
public class CVGIcons extends AllIcons {

    public static final ResourceLocation ICON_ATLAS =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/gui/icons.png");
    public static final int ICON_ATLAS_SIZE = 128;

    private static int gridX = 0, gridY = -1;
    private final int px, py;

    public static final CVGIcons
        I_NONE = newRow(),
        I_ABSOLUTE_SPEED = next(),
        I_ABSOLUTE_MULTIPLIER = next(),
        I_RELATIVE_SPEED = next(),
        I_RELATIVE_MULTIPLIER = next(),

        I_NONE_BOLD = newRow(),
        I_ABSOLUTE_SPEED_BOLD = next(),
        I_ABSOLUTE_MULTIPLIER_BOLD = next(),
        I_RELATIVE_SPEED_BOLD = next(),
        I_RELATIVE_MULTIPLIER_BOLD = next();

    public CVGIcons(int x, int y) {
        super(x, y);
        this.px = x * 16;
        this.py = y * 16;
    }

    private static CVGIcons next() {
        return new CVGIcons(++gridX, gridY);
    }

    private static CVGIcons newRow() {
        return new CVGIcons(gridX = 0, ++gridY);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void bind() {
        RenderSystem.setShaderTexture(0, ICON_ATLAS);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(ICON_ATLAS, x, y, 0, px, py, 16, 16, ICON_ATLAS_SIZE, ICON_ATLAS_SIZE);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(PoseStack ms, MultiBufferSource buffer, int color) {
        VertexConsumer builder = buffer.getBuffer(RenderType.text(ICON_ATLAS));
        Matrix4f matrix = ms.last().pose();
        Color rgb = new Color(color);
        int light = LightTexture.FULL_BRIGHT;

        Vec3 vec1 = new Vec3(0, 0, 0);
        Vec3 vec2 = new Vec3(0, 1, 0);
        Vec3 vec3 = new Vec3(1, 1, 0);
        Vec3 vec4 = new Vec3(1, 0, 0);

        float u1 = px * 1f / ICON_ATLAS_SIZE;
        float u2 = (px + 16) * 1f / ICON_ATLAS_SIZE;
        float v1 = py * 1f / ICON_ATLAS_SIZE;
        float v2 = (py + 16) * 1f / ICON_ATLAS_SIZE;

        vertex(builder, matrix, vec1, rgb, u1, v1, light);
        vertex(builder, matrix, vec2, rgb, u1, v2, light);
        vertex(builder, matrix, vec3, rgb, u2, v2, light);
        vertex(builder, matrix, vec4, rgb, u2, v1, light);
    }

    @OnlyIn(Dist.CLIENT)
    private void vertex(VertexConsumer builder, Matrix4f matrix, Vec3 vec, Color rgb, float u, float v, int light) {
        builder.addVertex(matrix, (float) vec.x, (float) vec.y, (float) vec.z)
                .setColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), 255)
                .setUv(u, v)
                .setLight(light);
    }
}
