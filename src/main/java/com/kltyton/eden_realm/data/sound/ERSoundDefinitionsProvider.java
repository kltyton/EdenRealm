package com.kltyton.eden_realm.data.sound;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.registry.ERSoundEvents;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

public final class ERSoundDefinitionsProvider extends SoundDefinitionsProvider {
    private static final String SUBTITLE_PREFIX = "subtitles.eden_realm.entity.moss_stone_colossus.";
    private static final String SOUND_PREFIX = "entity/moss_stone_colossus/";

    public ERSoundDefinitionsProvider(PackOutput output) {
        super(output, ERConstants.MOD_ID);
    }

    @Override
    public void registerSounds() {
        add(ERSoundEvents.MOSS_STONE_COLOSSUS_STEP, definition()
                .subtitle(SUBTITLE_PREFIX + "step")
                .with(
                        sound(ERConstants.id(SOUND_PREFIX + "step_1")),
                        sound(ERConstants.id(SOUND_PREFIX + "step_2")),
                        sound(ERConstants.id(SOUND_PREFIX + "step_3")),
                        sound(ERConstants.id(SOUND_PREFIX + "step_4")),
                        sound(ERConstants.id(SOUND_PREFIX + "step_5")),
                        sound(ERConstants.id(SOUND_PREFIX + "step_6"))));
        add(ERSoundEvents.MOSS_STONE_COLOSSUS_AMBIENT, definition()
                .subtitle(SUBTITLE_PREFIX + "ambient")
                .with(sound(ERConstants.id(SOUND_PREFIX + "ambient"))));
        add(ERSoundEvents.MOSS_STONE_COLOSSUS_HURT, definition()
                .subtitle(SUBTITLE_PREFIX + "hurt")
                .with(sound(ERConstants.id(SOUND_PREFIX + "hurt"))));
        add(ERSoundEvents.MOSS_STONE_COLOSSUS_DEATH, definition()
                .subtitle(SUBTITLE_PREFIX + "death")
                .with(sound(ERConstants.id(SOUND_PREFIX + "death"))));
    }
}
