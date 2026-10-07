package com.kltyton.eden_realm.client.sound.villager;

import com.kltyton.eden_realm.common.entity.passive.villager.PlainsVillager;
import com.kltyton.eden_realm.common.entity.passive.villager.VillagerSpeechPlayback;
import com.kltyton.eden_realm.registry.ERSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;

public final class PlainsVillagerSpeechAudio {
    private PlainsVillagerSpeechAudio() { }

    public static VillagerSpeechPlayback play(PlainsVillager entity, int line) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.distanceToSqr(entity) > 256 || entity.isSilent()) {
            return VillagerSpeechPlayback.NONE;
        }
        var sounds = client.getSoundManager();
        var sound = new EntityBoundSoundInstance(line == 1 ? ERSoundEvents.PLAINS_VILLAGER_REUNION.get()
                : ERSoundEvents.PLAINS_VILLAGER_RELIEF.get(), SoundSource.NEUTRAL, 1, 1, entity,
                entity.getRandom().nextLong());
        SoundEngine.PlayResult result = sounds.play(sound);
        int silentEndTick = entity.tickCount + (line == 1 ? 57 : 70);
        return new VillagerSpeechPlayback() {
            private boolean stopped;

            @Override
            public boolean isPlaying() {
                if (stopped || !entity.isAlive() || entity.isRemoved()) {
                    return false;
                }
                // Muted/disabled audio still permits a readable, duration-bounded bubble.
                return result == SoundEngine.PlayResult.NOT_STARTED
                        ? entity.tickCount < silentEndTick : sounds.isActive(sound);
            }

            @Override
            public void stop() {
                stopped = true;
                sounds.stop(sound);
            }
        };
    }
}
