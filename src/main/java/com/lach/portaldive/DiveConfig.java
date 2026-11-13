package com.lach.portaldive;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class DiveConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = B.comment("Replace the dimension loading screen with the hyperspace dive.")
            .define("enabled", true);
    public static final ModConfigSpec.BooleanValue PORTALS = B.comment("Dive on Nether and End portal travel.")
            .define("portals", true);
    public static final ModConfigSpec.BooleanValue OTHER_TRANSITIONS = B.comment("Dive on other dimension changes (commands, modded portals, respawning in another dimension).")
            .define("otherTransitions", true);
    public static final ModConfigSpec.BooleanValue ON_JOIN = B.comment("Dive when joining a world or server.")
            .define("onJoin", false);
    public static final ModConfigSpec.BooleanValue PORTAL_WINDUP = B.comment("Stars start streaking while you stand in a Nether portal.")
            .define("portalWindup", true);
    public static final ModConfigSpec.DoubleValue MIN_SECONDS = B.comment("Shortest dive, so fast loads still feel like a jump.")
            .defineInRange("minSeconds", 1.4, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue MAX_SECONDS = B.comment("Give up waiting for chunks after this long (vanilla uses 30).")
            .defineInRange("maxSeconds", 30.0, 2.0, 120.0);
    public static final ModConfigSpec.DoubleValue BRAKE_SECONDS = B.comment("Length of the hyper-braking arrival.")
            .defineInRange("brakeSeconds", 1.6, 0.2, 5.0);
    public static final ModConfigSpec.DoubleValue SHAKE = B.comment("Screen shake strength. Also scaled by the vanilla Screen Effects slider.")
            .defineInRange("shake", 1.0, 0.0, 3.0);
    public static final ModConfigSpec.DoubleValue FOV_KICK = B.comment("FOV kick on arrival. Also scaled by the vanilla FOV Effects slider.")
            .defineInRange("fovKick", 1.0, 0.0, 3.0);
    public static final ModConfigSpec.BooleanValue FLASH = B.comment("White flash on arrival. Turn off if flashes bother you (also off when Hide Lightning Flashes is on).")
            .define("flash", true);
    public static final ModConfigSpec.IntValue STARS = B.comment("Number of star streaks.")
            .defineInRange("stars", 700, 100, 3000);
    public static final ModConfigSpec.DoubleValue VOLUME = B.comment("Dive sound volume.")
            .defineInRange("volume", 0.8, 0.0, 1.0);
    public static final ModConfigSpec.BooleanValue MUTE_VANILLA_TRAVEL = B.comment("Mute the vanilla portal travel sound while diving.")
            .define("muteVanillaTravel", true);
    public static final ModConfigSpec.BooleanValue SHOW_DESTINATION = B.comment("Show the destination name during the dive.")
            .define("showDestination", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> COLOURS = B.comment(
                    "Tunnel colour per dimension, as dimension=RRGGBB. Unlisted dimensions get a colour from their id.")
            .defineListAllowEmpty("colours", List.of(
                    "minecraft:overworld=8FCBFF", "minecraft:the_nether=FF5A2A", "minecraft:the_end=B36BFF"),
                    () -> "namespace:path=FFFFFF", o -> o instanceof String s && s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+=[0-9a-fA-F]{6}"));

    public static final ModConfigSpec SPEC = B.build();

    private DiveConfig() {}
}
