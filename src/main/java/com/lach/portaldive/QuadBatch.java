package com.lach.portaldive;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/** Collects {@link Starfield} quads into one GUI render state element. */
final class QuadBatch implements Starfield.Sink {
    private final GuiGraphicsExtractor g;
    private final RenderPipeline pipeline;
    private final Matrix3x2f pose;
    private float[] pos = new float[1024];
    private int[] col = new int[512];
    private int n;

    private final boolean premultiply;

    private QuadBatch(GuiGraphicsExtractor g, RenderPipeline pipeline, boolean premultiply) {
        this.g = g;
        this.pipeline = pipeline;
        this.premultiply = premultiply;
        this.pose = new Matrix3x2f(g.pose());
    }

    // GUI_TEXT_HIGHLIGHT blends ONE, ONE: premultiply colour by alpha to get SRC_ALPHA, ONE.
    static QuadBatch additive(GuiGraphicsExtractor g) { return new QuadBatch(g, RenderPipelines.GUI_TEXT_HIGHLIGHT, true); }
    static QuadBatch normal(GuiGraphicsExtractor g) { return new QuadBatch(g, RenderPipelines.GUI, false); }

    private void v(float x, float y, int c) {
        if (n == col.length) { col = Arrays.copyOf(col, n * 2); pos = Arrays.copyOf(pos, n * 4); }
        if (premultiply) {
            float a = (c >>> 24) / 255f;
            c = 0xFF000000 | Math.round((c >> 16 & 255) * a) << 16 | Math.round((c >> 8 & 255) * a) << 8 | Math.round((c & 255) * a);
        }
        pos[n * 2] = x; pos[n * 2 + 1] = y; col[n++] = c;
    }

    @Override
    public void quad(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
        // GUI pipelines cull back faces: keep the winding vanilla uses (negative shoelace area).
        float area = (x0 * y1 - x1 * y0) + (x1 * y2 - x2 * y1) + (x2 * y3 - x3 * y2) + (x3 * y0 - x0 * y3);
        if (area <= 0) { v(x0, y0, c0); v(x1, y1, c1); v(x2, y2, c2); v(x3, y3, c3); }
        else { v(x3, y3, c3); v(x2, y2, c2); v(x1, y1, c1); v(x0, y0, c0); }
    }

    void draw() {
        if (n == 0) return;
        g.guiRenderState.addGuiElement(new Element(pipeline, pose, pos, col, n,
                new ScreenRectangle(0, 0, g.guiWidth(), g.guiHeight())));
        g.nextStratum();
    }

    private record Element(RenderPipeline pipeline, Matrix3x2f pose, float[] pos, int[] col, int n, ScreenRectangle bounds)
            implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer vc) {
            for (int i = 0; i < n; i++) vc.addVertexWith2DPose(pose, pos[i * 2], pos[i * 2 + 1]).setColor(col[i]);
        }

        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public @Nullable ScreenRectangle scissorArea() { return null; }
    }
}
