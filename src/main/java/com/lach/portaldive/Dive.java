package com.lach.portaldive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Dive state machine: windup (standing in a portal), dive (loading screen), brake (arrival). */
public final class Dive {
    enum Phase { IDLE, DIVE, BRAKE }

    static final SoundEvent JUMP = SoundEvent.createVariableRangeEvent(PortalDive.id("jump"));
    static final SoundEvent CRUISE = SoundEvent.createVariableRangeEvent(PortalDive.id("cruise"));
    static final SoundEvent BRAKE = SoundEvent.createVariableRangeEvent(PortalDive.id("brake"));

    static final float V_WINDUP = 1.4f, V_MAX = 9f, ACCEL = 0.6f;

    static final Starfield STARS = new Starfield();
    static Phase phase = Phase.IDLE;
    static long phaseStart;
    static float windup;            // 0..1 from the portal spin effect
    static float startSpeed;
    @Nullable static ResourceKey<Level> lastDim;
    @Nullable static CruiseSound cruise;
    static long lastFrame;
    @Nullable static ResourceKey<Level> fromDim;
    static long destSeenAt;

    private Dive() {}

    static float seconds() { return (System.nanoTime() - phaseStart) / 1e9f; }

    // ------------------------------------------------------------------ triggers

    @SubscribeEvent
    static void onScreenOpen(ScreenEvent.Opening e) {
        if (!(e.getNewScreen() instanceof ReceivingLevelScreen next) || next instanceof DiveScreen) {
            if (phase == Phase.DIVE && !(e.getNewScreen() instanceof ReceivingLevelScreen)) arrive();
            return;
        }
        if (!DiveConfig.ENABLED.get()) return;
        if (next.getClass() != ReceivingLevelScreen.class && !DiveConfig.REPLACE_MOD_SCREENS.get()) return; // keep another mod's own screen
        boolean join = lastDim == null;
        boolean portal = next.reason != ReceivingLevelScreen.Reason.OTHER;
        if (join ? !DiveConfig.ON_JOIN.get() : portal ? !DiveConfig.PORTALS.get() : !DiveConfig.OTHER_TRANSITIONS.get()) return;
        if (phase != Phase.DIVE) begin();
        e.setNewScreen(new DiveScreen(next.levelReceived, next.reason));
    }

    static void begin() {
        startSpeed = Math.max(STARS.speed, 0.2f);
        fromDim = lastDim;
        destSeenAt = 0;
        phase = Phase.DIVE;
        phaseStart = System.nanoTime();
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(JUMP, 1f, volume()));
        if (cruise != null) cruise.fadeOut();
        cruise = new CruiseSound(CRUISE, volume());
        mc.getSoundManager().play(cruise);
    }

    static boolean shouldFinish(boolean ready) {
        float t = seconds();
        return (ready && t >= DiveConfig.MIN_SECONDS.get()) || t >= DiveConfig.MAX_SECONDS.get();
    }

    static void arrive() {
        if (phase != Phase.DIVE) return;
        phase = Phase.BRAKE;
        phaseStart = System.nanoTime();
        if (cruise != null) { cruise.fadeOut(); cruise = null; }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(BRAKE, 1f, volume()));
    }

    static float volume() { return DiveConfig.VOLUME.get().floatValue(); }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) lastDim = mc.level.dimension();
        if (phase == Phase.DIVE && !(mc.screen instanceof DiveScreen)) arrive();
        if (phase == Phase.BRAKE && seconds() > DiveConfig.BRAKE_SECONDS.get()) phase = Phase.IDLE;
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        lastDim = null;
        phase = Phase.IDLE;
        windup = 0;
        if (cruise != null) { cruise.fadeOut(); cruise = null; }
    }

    @SubscribeEvent
    static void onSound(PlaySoundEvent e) {
        if (phase != Phase.IDLE && e.getSound() != null && DiveConfig.MUTE_VANILLA_TRAVEL.get()
                && e.getSound().getLocation().equals(SoundEvents.PORTAL_TRAVEL.getLocation())) {
            e.setSound(null);
        }
    }

    // ------------------------------------------------------------------ simulation

    @SubscribeEvent
    static void onFrame(RenderFrameEvent.Pre e) {
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : (now - lastFrame) / 1e9f;
        lastFrame = now;
        Minecraft mc = Minecraft.getInstance();
        float target = 0;
        if (DiveConfig.ENABLED.get() && DiveConfig.PORTAL_WINDUP.get() && mc.player != null && phase == Phase.IDLE) {
            target = net.minecraft.util.Mth.lerp(e.getPartialTick().getGameTimeDeltaPartialTick(false),
                    mc.player.oSpinningEffectIntensity, mc.player.spinningEffectIntensity);
        }
        windup = phase == Phase.IDLE ? target : 0;
        STARS.ensure(DiveConfig.STARS.get());
        switch (phase) {
            case IDLE -> { STARS.speed = V_WINDUP * windup * windup; STARS.swirl = 0.4f * windup; }
            case DIVE -> {
                float k = Math.min(seconds() / ACCEL, 1f);
                STARS.speed = Starfield.lerp(startSpeed, V_MAX, k * k * k) + (k >= 1 ? 0.6f * (float) Math.sin(seconds() * 3) : 0);
                STARS.swirl = 0.25f;
            }
            case BRAKE -> {
                float t = seconds();
                STARS.speed = V_MAX * (float) Math.exp(-t * 5.5f);
                STARS.swirl = 0.25f * (float) Math.exp(-t * 3);
            }
        }
        STARS.step(dt);
    }

    // ------------------------------------------------------------------ rendering

    /** Full-screen tunnel, drawn by {@link DiveScreen}. */
    static void renderDive(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        float t = seconds();
        int tint = diveTint(mc);
        float shakeAmt = shake() * (punch(t) * 6f + 0.6f);
        float cx = w / 2f + jitter(t, 1) * shakeAmt, cy = h / 2f + jitter(t, 2) * shakeAmt;
        float k = Math.min(t / ACCEL, 1f);
        float focal = Math.min(w, h) * 0.5f * Starfield.lerp(1f, 0.72f, k * k);
        float scale = 1f / (float) mc.getWindow().getGuiScale() * 2f;

        QuadBatch add = QuadBatch.additive(g);
        Starfield.glow(add, cx, cy, Math.max(w, h) * 0.75f, tint, 0.22f + 0.08f * (float) Math.sin(t * 2.3f));
        Starfield.glow(add, cx, cy, Math.min(w, h) * 0.32f, tint, 0.3f * k);
        Starfield.glow(add, cx, cy, Math.min(w, h) * 0.07f, 0xFFFFFF, 0.55f * k);
        STARS.draw(add, cx, cy, focal, tint, 1f, scale, true);
        float flash = punch(t);
        if (flash > 0) Starfield.rect(add, 0, 0, w, h, Starfield.argb(flash * 0.55f, rgb(tint, 16), rgb(tint, 8), rgb(tint, 0)));
        add.draw();

        QuadBatch norm = QuadBatch.normal(g);
        float r = (float) Math.hypot(w, h) * 0.5f;
        Starfield.ring(norm, w / 2f, h / 2f, r * 0.55f, r * 1.05f, 0x00000000, 0xC0000000);
        norm.draw();

        if (DiveConfig.SHOW_DESTINATION.get() && mc.level != null && t > 0.3f) {
            float a = Math.min((t - 0.3f) / 0.5f, 1f);
            int alpha = Math.max(4, (int) (a * 200)) << 24;
            Component name = dimName(mc.level.dimension());
            g.drawCenteredString(mc.font, Component.translatable("portaldive.diving"), w / 2, h - 46, alpha | 0x9A9A9A);
            g.drawCenteredString(mc.font, name, w / 2, h - 34, alpha | (tint & 0xFFFFFF | 0x404040));
        }
    }

    /** HUD layer: windup streaks while in a portal, braking streaks + flash after arrival. */
    static void renderOverlay(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (phase == Phase.DIVE || mc.level == null) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float scale = 1f / (float) mc.getWindow().getGuiScale() * 2f;
        int tint = tint(mc.level.dimension());
        float focal = Math.min(w, h) * 0.5f;
        if (phase == Phase.IDLE) {
            if (windup <= 0.01f) return;
            QuadBatch add = QuadBatch.additive(g);
            STARS.draw(add, w / 2f, h / 2f, focal, tint, windup * windup * 0.8f, scale, false);
            add.draw();
            QuadBatch dark = QuadBatch.normal(g);
            float rr = (float) Math.hypot(w, h) * 0.5f;
            Starfield.ring(dark, w / 2f, h / 2f, rr * 0.35f, rr * 1.05f, 0, Starfield.argb(0.6f * windup * windup, 0, 0, 0));
            dark.draw();
            return;
        }
        float t = seconds(), d = DiveConfig.BRAKE_SECONDS.get().floatValue();
        float fade = (float) Math.pow(Math.max(0, 1 - t / d), 1.6);
        QuadBatch add = QuadBatch.additive(g);
        Starfield.glow(add, w / 2f, h / 2f, Math.max(w, h) * 0.75f, tint, 0.25f * fade);
        STARS.draw(add, w / 2f, h / 2f, focal * Starfield.lerp(1f, 0.72f, fade), tint, fade, scale, true);
        add.draw();

        QuadBatch norm = QuadBatch.normal(g);
        float r = (float) Math.hypot(w, h) * 0.5f;
        Starfield.ring(norm, w / 2f, h / 2f, r * 0.55f, r * 1.05f, 0, Starfield.argb(0.75f * fade, 0, 0, 0));
        float flash = (float) Math.exp(-t * 6f);
        if (flashOn()) Starfield.rect(norm, 0, 0, w, h, Starfield.argb(0.9f * flash, 1, 1, 1));
        else Starfield.rect(norm, 0, 0, w, h, Starfield.argb(0.25f * flash, rgb(tint, 16), rgb(tint, 8), rgb(tint, 0)));
        norm.draw();
    }

    // ------------------------------------------------------------------ camera

    @SubscribeEvent
    static void onFov(ViewportEvent.ComputeFov e) {
        double kick = DiveConfig.FOV_KICK.get() * Minecraft.getInstance().options.fovEffectScale().get();
        if (kick <= 0) return;
        double mult = 1;
        if (phase == Phase.IDLE && windup > 0) mult = 1 + 0.12 * kick * windup * windup;
        else if (phase == Phase.BRAKE) {
            float t = seconds();
            mult = 1 + 0.45 * kick * Math.exp(-t * 4.5) * Math.cos(t * 7.5);
        }
        if (mult != 1) e.setFOV(Math.min(e.getFOV() * mult, 170));
    }

    @SubscribeEvent
    static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        float s = shake();
        if (s <= 0) return;
        if (phase == Phase.BRAKE) {
            float t = seconds();
            float amp = s * (float) Math.exp(-t * 3.5f);
            float jolt = -3.5f * s * (float) (Math.exp(-t * 6) * Math.cos(t * 14));
            e.setPitch(e.getPitch() + jolt + jitter(t, 3) * 1.2f * amp);
            e.setYaw(e.getYaw() + jitter(t, 4) * 1.2f * amp);
            e.setRoll(e.getRoll() + jitter(t, 5) * 2.5f * amp);
        } else if (phase == Phase.IDLE && windup > 0.3f) {
            float t = System.nanoTime() / 1e9f, amp = s * (windup - 0.3f) * 0.5f;
            e.setPitch(e.getPitch() + jitter(t, 6) * amp);
            e.setRoll(e.getRoll() + jitter(t, 7) * amp);
        }
    }

    // ------------------------------------------------------------------ helpers

    static float shake() {
        return (float) (DiveConfig.SHAKE.get() * Minecraft.getInstance().options.screenEffectScale().get());
    }

    static boolean flashOn() {
        return DiveConfig.FLASH.get() && !Minecraft.getInstance().options.hideLightningFlash().get();
    }

    /** Burst at the moment of the jump: 0 before, peaks at the end of acceleration, then decays. */
    static float punch(float t) {
        float d = t - ACCEL;
        return d < -0.08f ? 0 : d < 0 ? (d + 0.08f) / 0.08f : (float) Math.exp(-d * 5f);
    }

    /** Smooth pseudo-random wobble in [-1, 1]. */
    static float jitter(float t, int seed) {
        return (float) (Math.sin(t * (31 + seed * 7.3) + seed) * 0.5 + Math.sin(t * (53 + seed * 3.1) + seed * 2) * 0.3
                + Math.sin(t * (17 + seed) + seed * 5) * 0.2);
    }

    /** Leaves in the old dimension's colour and blends into the destination's once the new level is known. */
    static int diveTint(Minecraft mc) {
        ResourceKey<Level> to = mc.level == null ? null : mc.level.dimension();
        if (fromDim == null) return tint(to);
        int from = tint(fromDim);
        if (to == null || to.equals(fromDim)) return from;
        if (destSeenAt == 0) destSeenAt = System.nanoTime();
        float k = Math.min((System.nanoTime() - destSeenAt) / 1e9f / 0.6f, 1f);
        k = k * k * (3 - 2 * k);
        int dest = tint(to);
        return (int) Starfield.lerp(from >> 16 & 255, dest >> 16 & 255, k) << 16
                | (int) Starfield.lerp(from >> 8 & 255, dest >> 8 & 255, k) << 8
                | (int) Starfield.lerp(from & 255, dest & 255, k);
    }

    static float rgb(int c, int shift) { return (c >> shift & 255) / 255f; }

    private static List<? extends String> cachedList;
    private static final Map<ResourceLocation, Integer> COLOURS = new HashMap<>();

    static int tint(@Nullable ResourceKey<Level> dim) {
        if (dim == null) return 0x8FCBFF;
        List<? extends String> list = DiveConfig.COLOURS.get();
        if (list != cachedList) {
            cachedList = list;
            COLOURS.clear();
            for (String s : list) {
                int eq = s.lastIndexOf('=');
                ResourceLocation id = eq > 0 ? ResourceLocation.tryParse(s.substring(0, eq)) : null;
                if (id == null) continue;
                try { COLOURS.put(id, Integer.parseInt(s.substring(eq + 1), 16)); } catch (NumberFormatException ignored) {}
            }
        }
        Integer c = COLOURS.get(dim.location());
        if (c != null) return c;
        float hue = (dim.location().hashCode() & 0xFFFF) / 65535f;
        return java.awt.Color.HSBtoRGB(hue, 0.6f, 1f) & 0xFFFFFF;
    }

    static Component dimName(ResourceKey<Level> dim) {
        ResourceLocation id = dim.location();
        String key = "portaldive.dimension." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        if (I18n.exists(key)) return Component.translatable(key);
        String vanillaKey = "dimension." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        if (I18n.exists(vanillaKey)) return Component.translatable(vanillaKey);
        StringBuilder b = new StringBuilder();
        for (String part : id.getPath().substring(id.getPath().lastIndexOf('/') + 1).split("_")) {
            if (part.isEmpty()) continue;
            if (!b.isEmpty()) b.append(' ');
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return Component.literal(b.toString());
    }
}
