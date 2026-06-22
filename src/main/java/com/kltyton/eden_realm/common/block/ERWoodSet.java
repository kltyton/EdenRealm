package com.kltyton.eden_realm.common.block;

import com.kltyton.eden_realm.ERConstants;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public enum ERWoodSet {
    ICE_CRYSTAL_PINE("ice_crystal_pine", "Ice Crystal Pine", "冰晶松"),
    SILVER_FROST_FIR("silver_frost_fir", "Silver Frost Fir", "银霜杉"),
    STARSHINE("starshine", "Starshine", "星辉树"),
    ANCIENT_SPIRIT("ancient_spirit", "Ancient Spirit", "古灵树"),
    BLAZING_FEATHER("blazing_feather", "Blazing Feather", "炽羽树"),
    DRAGON_SCALE("dragon_scale", "Dragon Scale", "龙鳞树"),
    GLOOMLIGHT("gloomlight", "Gloomlight", "幽灯树"),
    MIST_VINE("mist_vine", "Mist Vine", "雾藤树"),
    MORNING_DEW("morning_dew", "Morning Dew", "晨露树"),
    AMBER("amber", "Amber", "琥珀树"),
    TIDE_SONG("tide_song", "Tide Song", "潮歌树"),
    SEA_CROWN("sea_crown", "Sea Crown", "海冠树"),
    WIND_CHIME_PINE("wind_chime_pine", "Wind Chime Pine", "风铃松"),
    FIRMAMENT("firmament", "Firmament", "苍穹树"),
    CLOUD_CROWN("cloud_crown", "Cloud Crown", "云冠树"),
    GOLDEN_BEECH("golden_beech", "Golden Beech", "金叶榉树");

    private final String id;
    private final String englishName;
    private final String chineseName;
    private final String registryName;
    private final BlockSetType blockSetType;
    private final WoodType woodType;

    ERWoodSet(String id, String englishName, String chineseName) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.registryName = ERConstants.id(id).toString();
        this.blockSetType = BlockSetType.register(new BlockSetType(this.registryName));
        this.woodType = WoodType.register(new WoodType(this.registryName, this.blockSetType));
    }

    public String id() {
        return id;
    }

    public String englishName() {
        return englishName;
    }

    public String chineseName() {
        return chineseName;
    }

    public String registryName() {
        return registryName;
    }

    public BlockSetType blockSetType() {
        return blockSetType;
    }

    public WoodType woodType() {
        return woodType;
    }

    public String logName() {
        return id + "_log";
    }

    public String strippedLogName() {
        return id + "_stripped_log";
    }

    public String planksName() {
        return id + "_planks";
    }

    public String leavesName() {
        return id + "_leaves";
    }

    public String saplingName() {
        return id + "_sapling";
    }

    public String doorName() {
        return id + "_door";
    }

    public String trapdoorName() {
        return id + "_trapdoor";
    }

    public String signName() {
        return id + "_sign";
    }

    public String wallSignName() {
        return id + "_wall_sign";
    }

    public String hangingSignName() {
        return id + "_hanging_sign";
    }

    public String wallHangingSignName() {
        return id + "_wall_hanging_sign";
    }

    public String boatName() {
        return id + "_boat";
    }

    public String chestBoatName() {
        return id + "_chest_boat";
    }
}
