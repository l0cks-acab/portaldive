package com.lach.portaldive;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Looping hyperspace rumble that fades in, then fades out on arrival. */
final class CruiseSound extends AbstractTickableSoundInstance {
    private final float target;
    private boolean fading;

    CruiseSound(SoundEvent event, float volume) {
        super(event, SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.target = volume;
        this.volume = 0.01f;
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    void fadeOut() { fading = true; }

    @Override
    public void tick() {
        if (fading) {
            volume -= target / 6f;
            if (volume <= 0.01f) stop();
        } else {
            volume = Math.min(target, volume + target / 10f);
        }
    }

    @Override
    public boolean canStartSilent() { return true; }
}
