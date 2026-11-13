package com.lach.portaldive;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/** Draws {@link Starfield} quads straight to the GUI. */
final class QuadBatch implements Starfield.Sink {
    private final Matrix4f pose;
    private final BufferBuilder buf;
    private final boolean additive;

    private QuadBatch(GuiGraphics g, boolean additive) {
        g.flush();
        this.pose = g.pose().last().pose();
        this.additive = additive;
        this.buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    static QuadBatch additive(GuiGraphics g) { return new QuadBatch(g, true); }
    static QuadBatch normal(GuiGraphics g) { return new QuadBatch(g, false); }

    @Override
    public void quad(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
        buf.addVertex(pose, x0, y0, 0).setColor(c0);
        buf.addVertex(pose, x1, y1, 0).setColor(c1);
        buf.addVertex(pose, x2, y2, 0).setColor(c2);
        buf.addVertex(pose, x3, y3, 0).setColor(c3);
    }

    void draw() {
        MeshData mesh = buf.build();
        if (mesh == null) return;
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        if (additive) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(mesh);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }
}
