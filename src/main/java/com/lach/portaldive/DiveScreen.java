package com.lach.portaldive;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;

/** Stands in for the vanilla "Loading terrain" screen while the dive plays. */
public class DiveScreen extends LevelLoadingScreen {
    public DiveScreen(LevelLoadTracker tracker, Reason reason) {
        super(tracker, reason);
    }

    @Override
    public void tick() {
        if (Dive.shouldFinish(loadTracker.isLevelReady())) {
            Dive.arrive();
            this.onClose();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fill(0, 0, width, height, 0xFF000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        extractBackground(g, mouseX, mouseY, a);
        Dive.renderDive(g, width, height);
    }
}
