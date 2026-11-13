package com.lach.portaldive;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = PortalDive.MODID, dist = Dist.CLIENT)
public class PortalDive {
    public static final String MODID = "portaldive";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public PortalDive(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, DiveConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(id("dive_overlay"), Dive::renderOverlay));
        NeoForge.EVENT_BUS.register(Dive.class);
    }
}
