package com.lach.portaldive;

import java.util.Random;

/** Star tunnel simulation and geometry. Version independent: emits coloured quads into a {@link Sink}. */
public final class Starfield {
    public interface Sink {
        /** Corners in order, colours ARGB per corner. */
        void quad(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3);
    }

    static final float NEAR = 0.04f, FAR = 4f;
    private final Random rnd = new Random();
    private float[] theta = new float[0], rad = new float[0], z = new float[0], white = new float[0];

    public float speed;   // z units per second
    public float swirl;   // radians per second

    void ensure(int n) {
        if (theta.length == n) return;
        theta = new float[n]; rad = new float[n]; z = new float[n]; white = new float[n];
        for (int i = 0; i < n; i++) respawn(i, NEAR + rnd.nextFloat() * (FAR - NEAR));
    }

    private void respawn(int i, float depth) {
        theta[i] = rnd.nextFloat() * (float) (Math.PI * 2);
        float r = rnd.nextFloat();
        rad[i] = 0.18f + 1.4f * r * r;
        z[i] = depth;
        white[i] = 0.08f + rnd.nextFloat() * rnd.nextFloat() * 0.85f;
    }

    public void step(float dt) {
        if (dt <= 0) return;
        dt = Math.min(dt, 0.1f);
        for (int i = 0; i < z.length; i++) {
            z[i] -= speed * dt;
            theta[i] += swirl * dt * (0.6f + 0.8f * white[i]);
            if (z[i] < NEAR) respawn(i, FAR - rnd.nextFloat() * 0.3f);
        }
    }

    /**
     * @param cx,cy  centre on screen
     * @param focal  projection scale in pixels
     * @param tint   RGB tunnel colour
     * @param alpha  overall opacity 0..1
     * @param scale  gui scale hint for line widths
     */
    public void draw(Sink s, float cx, float cy, float focal, int tint, float alpha, float scale, boolean chroma) {
        if (alpha <= 0.002f) return;
        float trailK = 0.055f;
        float tr = (tint >> 16 & 255) / 255f, tg = (tint >> 8 & 255) / 255f, tb = (tint & 255) / 255f;
        for (int i = 0; i < z.length; i++) {
            float zh = z[i];
            float zt = Math.min(zh + Math.max(speed * trailK, 0.0015f), FAR);
            float cos = (float) Math.cos(theta[i]), sin = (float) Math.sin(theta[i]);
            float hx = cx + cos * rad[i] / zh * focal, hy = cy + sin * rad[i] / zh * focal;
            float tx = cx + cos * rad[i] / zt * focal, ty = cy + sin * rad[i] / zt * focal;
            float depth = 1f - (zh - NEAR) / (FAR - NEAR);
            float a = alpha * depth * depth * (0.55f + 0.45f * white[i]);
            if (a < 0.01f) continue;
            float w = Math.min(0.6f + 1.4f * (0.35f / zh), 4.5f) * scale;
            float wm = white[i];
            int head = argb(a, lerp(tr, 1, wm), lerp(tg, 1, wm), lerp(tb, 1, wm));
            int tail = argb(0, tr, tg, tb);
            line(s, tx, ty, hx, hy, w, tail, head);
            if (chroma && speed > 2f) {
                float k = Math.min((speed - 2f) / 6f, 1f) * 0.5f;
                float off = 1.012f;
                line(s, cx + (tx - cx) * off, cy + (ty - cy) * off, cx + (hx - cx) * off, cy + (hy - cy) * off, w, 0, argb(a * k, 1f, 0.2f, 0.2f));
                off = 0.988f;
                line(s, cx + (tx - cx) * off, cy + (ty - cy) * off, cx + (hx - cx) * off, cy + (hy - cy) * off, w, 0, argb(a * k, 0.2f, 0.4f, 1f));
            }
        }
    }

    /** Soft radial glow (additive). */
    public static void glow(Sink s, float cx, float cy, float radius, int rgb, float alpha) {
        if (alpha <= 0.002f) return;
        int in = argb(alpha, (rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
        int out = in & 0x00FFFFFF;
        ring(s, cx, cy, 0, radius, in, out);
    }

    /** Ring from r0 to r1 with colours c0 (inner) and c1 (outer). */
    public static void ring(Sink s, float cx, float cy, float r0, float r1, int c0, int c1) {
        int seg = 48;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            float c0x = (float) Math.cos(a0), c0y = (float) Math.sin(a0), c1x = (float) Math.cos(a1), c1y = (float) Math.sin(a1);
            s.quad(cx + c0x * r0, cy + c0y * r0, c0, cx + c0x * r1, cy + c0y * r1, c1,
                    cx + c1x * r1, cy + c1y * r1, c1, cx + c1x * r0, cy + c1y * r0, c0);
        }
    }

    public static void rect(Sink s, float x0, float y0, float x1, float y1, int c) {
        s.quad(x0, y0, c, x0, y1, c, x1, y1, c, x1, y0, c);
    }

    private static void line(Sink s, float x0, float y0, float x1, float y1, float w, int c0, int c1) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < w) { // dot: stretch to a small square along the ray
            float ux = len > 1e-4f ? dx / len : 1, uy = len > 1e-4f ? dy / len : 0;
            x0 = x1 - ux * w; y0 = y1 - uy * w; dx = x1 - x0; dy = y1 - y0; len = w;
            c0 = c1;
        }
        float nx = -dy / len * w * 0.5f, ny = dx / len * w * 0.5f;
        s.quad(x0 - nx, y0 - ny, c0, x0 + nx, y0 + ny, c0, x1 + nx, y1 + ny, c1, x1 - nx, y1 - ny, c1);
    }

    static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    static int argb(float a, float r, float g, float b) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(float v) { return Math.max(0, Math.min(255, (int) (v * 255f + 0.5f))); }
}
