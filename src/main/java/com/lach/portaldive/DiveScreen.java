package com.lach.portaldive;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;

/** Stands in for the vanilla "Loading terrain" screen while the dive plays. */
public class DiveScreen extends ReceivingLevelScreen {
    private final BooleanSupplier ready;

    public DiveScreen(BooleanSupplier ready, Reason reason) {
        super(ready, reason);
        this.ready = ready;
    }

    @Override
    public void tick() {
        if (Dive.shouldFinish(ready.getAsBoolean())) {
            Dive.arrive();
            this.onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        Dive.renderDive(g, width, height);
    }
}
