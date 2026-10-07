package com.kltyton.eden_realm.common.entity.passive.villager;

import java.util.Objects;
import java.util.function.BiFunction;

/** Keeps the sound engine behind a client-installed factory, outside common class loading. */
public interface VillagerSpeechPlayback {
    boolean isPlaying();

    void stop();

    VillagerSpeechPlayback NONE = new VillagerSpeechPlayback() {
        @Override public boolean isPlaying() { return false; }
        @Override public void stop() { }
    };

    final class ClientFactory {
        private static BiFunction<PlainsVillager, Integer, VillagerSpeechPlayback> factory = (entity, line) -> NONE;

        private ClientFactory() { }

        public static void install(BiFunction<PlainsVillager, Integer, VillagerSpeechPlayback> clientFactory) {
            factory = Objects.requireNonNull(clientFactory);
        }

        public static VillagerSpeechPlayback play(PlainsVillager entity, int line) {
            return factory.apply(entity, line);
        }
    }
}
