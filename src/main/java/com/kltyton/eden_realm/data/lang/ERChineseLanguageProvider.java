package com.kltyton.eden_realm.data.lang;

import com.kltyton.eden_realm.ERConstants;
import com.kltyton.eden_realm.common.block.tree.ERWoodSet;
import com.kltyton.eden_realm.registry.ERBlocks;
import com.kltyton.eden_realm.registry.ERItems;
import com.kltyton.eden_realm.registry.content.block.ERBlockEntry;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class ERChineseLanguageProvider extends LanguageProvider {
    public ERChineseLanguageProvider(PackOutput output) {
        super(output, ERConstants.MOD_ID, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        add("particle.eden_realm.honey_maple_leaves", "黄色蜜枫落叶");
        add("particle.eden_realm.honey_maple_orange_leaves", "橙色蜜枫落叶");
        add("particle.eden_realm.honey_maple_red_leaves", "红色蜜枫落叶");
        add("itemGroup.eden_realm.eden_realm", "伊甸之境");
        add("screen.eden_realm.terrain.title", "伊甸地形工作台");
        add("screen.eden_realm.terrain.biome", "选择群系");
        add("screen.eden_realm.terrain.preview", "地形预览");
        add("screen.eden_realm.terrain.chunks", "区块范围");
        add("screen.eden_realm.terrain.loading", "正在生成预览…");
        add("screen.eden_realm.terrain.refining", "实时预览 · 正在补齐完整区块…");
        add("screen.eden_realm.terrain.macro", "宏观预览 · 放大后自动补齐镜头附近的地形细节。");
        add("screen.eden_realm.terrain.detailLoading", "正在补齐镜头附近的精细地形…");
        add("screen.eden_realm.terrain.detailReady", "近景方块细节已加载");
        add("screen.eden_realm.terrain.wholeLoading", "正在加载全范围逐方块表层…");
        add("screen.eden_realm.terrain.wholeReady", "全范围逐方块表层已加载");
        add("screen.eden_realm.terrain.realtime", "地形随参数实时更新；地貌特征补全中");
        add("screen.eden_realm.terrain.parameters", "独立生成参数");
        add("screen.eden_realm.terrain.seed", "种子");
        add("screen.eden_realm.terrain.default", "默认值");
        add("screen.eden_realm.terrain.reset", "恢复默认");
        add("screen.eden_realm.terrain.export", "导出ZIP数据包");
        add("screen.eden_realm.terrain.back", "取消并退出");
        add("screen.eden_realm.terrain.done", "保存并退出");
        add("screen.eden_realm.terrain.saveTip", "保存当前地形到临时数据包并退出。创建世界时自动安装到该存档的 datapacks 文件夹并加载；再次打开工作台可继续修改。");
        add("screen.eden_realm.terrain.cancelTip", "放弃本次未保存的修改并退出，保留上次保存的数据包。Esc 的作用相同。");
        add("screen.eden_realm.terrain.exportTip", "选择位置导出当前地形的 ZIP 副本，不退出、不保存工作台草稿。服主须将 ZIP 放入服务器存档的 datapacks 文件夹并启用，在创建世界前加载；已有区块不会重新生成。");
        add("screen.eden_realm.terrain.resetTip", "将当前群系的参数恢复为模组默认值；点击保存并退出后才保留此修改。");
        add("screen.eden_realm.terrain.left", "向左旋转");
        add("screen.eden_realm.terrain.right", "向右旋转");
        add("screen.eden_realm.terrain.help", "左键拖动平移，右键拖动旋转，滚轮缩放；植被和结构进入世界后显示。");
        add("screen.eden_realm.terrain.elevationOffset", "地形高度");
        add("screen.eden_realm.terrain.reliefPercent", "起伏程度");
        add("screen.eden_realm.terrain.spacingPercent", "地形疏密");
        add("screen.eden_realm.terrain.shapePercent", "地貌强度");
        add("screen.eden_realm.terrain.shoreIceBlocks", "岸冰宽度");
        add("screen.eden_realm.terrain.generationChancePercent", "生成权重");
        add("screen.eden_realm.terrain.biomeSizePercent", "群系大小");
        add("screen.eden_realm.terrain.error", "操作失败：%s");
        add("screen.eden_realm.terrain.exported", "已导出：%s；放入目标存档的 datapacks 文件夹并启用后生效");
        add("screen.eden_realm.terrain.exportCancelled", "已取消导出，工作台草稿未保存");
        add("screen.eden_realm.terrain.saving", "正在保存地形草稿…");
        add("screen.eden_realm.terrain.exporting", "请选择 ZIP 导出位置…");
        add("screen.eden_realm.terrain.replace", "目标 ZIP 已存在，是否覆盖？");
        add("screen.eden_realm.terrain.installing", "正在准备新世界的地形数据包…");
        add("pack.eden_realm.terrain", "伊甸之境地形参数");
        add("biome.eden_realm.icy_rolling_hills", "冰蓝缓丘");
        add("biome.eden_realm.glacier_meander", "冰河曲湾");
        add("biome.eden_realm.crystal_lake_shore", "晶湖环岸");
        add("biome.eden_realm.blue_ice_plateau", "蓝冰台地");
        add("biome.eden_realm.ice_ridge_valley", "冰脊山谷");
        add("biome.eden_realm.icefall_fjord", "冰瀑峡湾");
        add("biome.eden_realm.frozen_fissure", "冻土裂谷");
        add("biome.eden_realm.cold_spring_lowland", "寒泉洼地");
        add("biome.eden_realm.crystal_stone_plain", "冰晶石原");
        add("biome.eden_realm.ice_crystal_basin", "冰晶盆地");
        add("biome.eden_realm.silver_frost_hills", "银霜缓丘");
        add("biome.eden_realm.frost_stream_valley", "霜溪曲谷");
        add("biome.eden_realm.silver_frost_lakeshore", "银霜湖岸");
        add("biome.eden_realm.frost_rock_plateau", "霜岩台地");
        add("biome.eden_realm.snow_ridge_valley", "雪岭松谷");
        add("biome.eden_realm.silver_frost_basin", "银霜盆地");
        add("biome.eden_realm.cloud_sea_flatlands", "云海平顶");
        add("biome.eden_realm.sky_airspace", "天穹空域");
        add("biome.eden_realm.star_stream_plateau", "星溪台地");
        add("biome.eden_realm.sky_mirror_lake", "天镜湖台");
        add("biome.eden_realm.cloud_island_chain", "云间岛群");
        add("biome.eden_realm.rosy_cloud_terraces", "粉霞阶田");
        add("biome.eden_realm.flower_mirror_lake", "花海镜湖");
        add("dimension.eden_realm.sky_layer", "伊甸之境·天穹层");
        add("dimension.eden_realm.eden_layer", "伊甸之境·主层");
        add("feature.eden_realm.icy_rolling_hills_ice_pine", "冰晶松");
        add("feature.eden_realm.icy_rolling_hills_frost_grass", "霜晶草");
        add("feature.eden_realm.icy_crystal_spire", "冰晶石柱");
        add("feature.eden_realm.icy_crystal_pile", "冰晶石簇");
        add("feature.eden_realm.icy_shore_freeze", "岸边结冰");
        add("feature.eden_realm.frozen_fjord_falls", "冻结冰瀑");
        add("feature.eden_realm.crystal_lake_island", "晶湖小岛");
        add("entity.eden_realm.moss_stone_colossus", "苔石巨像");
        add("entity.eden_realm.plains_villager", "平原村民");
        addItem(ERItems.PLAINS_VILLAGER_SPAWN_EGG, "平原村民刷怪蛋");
        add("dialogue.eden_realm.plains_villager.reunion", "我们又见面了。你的旅途怎么样");
        add("dialogue.eden_realm.plains_villager.relief", "哦，是你啊，我还以为又有活儿了");
        add("entity.eden_realm.falling_fruit", "下落的果实");
        addItem(ERItems.MOSS_STONE_COLOSSUS_SPAWN_EGG, "苔石巨像刷怪蛋");
        addItem(ERItems.TIDE_SONG_COCONUT, "潮歌椰");
        addItem(ERItems.SACRED_LIGHT_FRUIT, "圣辉果");
        addItem(ERItems.CLOUD_CROWN_FRUIT, "云冠果");
        addItem(ERItems.TWILIGHT_POMEGRANATE, "暮光榴果");
        addItem(ERItems.DEWSPIKE_GRAIN, "露穗谷");
        addItem(ERItems.DEWSPIKE_GRAIN_SEEDS, "露穗谷种子");
        addItem(ERItems.STAR_PATTERN_YAM, "星纹薯");
        addItem(ERItems.MOON_CLOVER, "月苜草");
        addItem(ERItems.CRYSTAL_DEW_FRUIT, "晶露果");
        addItem(ERItems.VINE_BEAN, "藤豆");
        addItem(ERItems.MOON_CLOVER_SEEDS, "月苜草种子");
        addItem(ERItems.CRYSTAL_DEW_FRUIT_SEEDS, "晶露果种子");
        addItem(ERItems.VINE_BEAN_SEEDS, "藤豆种子");
        for (var tool : com.kltyton.eden_realm.registry.content.item.ERToolItems.entries()) {
            addItem(tool.item(), tool.chinese());
        }
        add("subtitles.eden_realm.entity.moss_stone_colossus.step", "苔石巨像的脚步声");
        add("subtitles.eden_realm.entity.moss_stone_colossus.ambient", "苔石巨像发出低鸣");
        add("subtitles.eden_realm.entity.moss_stone_colossus.hurt", "苔石巨像受伤");
        add("subtitles.eden_realm.entity.moss_stone_colossus.death", "苔石巨像死亡");

        for (ERWoodSet wood : ERWoodSet.values()) {
            ERBlocks.WoodBlocks blocks = ERBlocks.woodBlocks(wood);
            ERItems.WoodItems items = ERItems.woodItems(wood);
            String name = wood.chineseName();
            String materialName = woodMaterialName(name);
            String leafName = wood == ERWoodSet.HONEY_MAPLE ? "黄色蜜枫树叶" : treePartName(name, "树叶");
            String saplingName = treePartName(name, "树苗");

            addBlock(blocks.log(), name + "原木");
            addBlock(blocks.wood(), materialName);
            addBlock(blocks.strippedLog(), "去皮" + name + "原木");
            addBlock(blocks.strippedWood(), "去皮" + materialName);
            addBlock(blocks.planks(), materialName + "板");
            addBlock(blocks.stairs(), materialName + "楼梯");
            addBlock(blocks.slab(), materialName + "台阶");
            addBlock(blocks.fence(), materialName + "栅栏");
            addBlock(blocks.fenceGate(), materialName + "栅栏门");
            addBlock(blocks.button(), materialName + "按钮");
            addBlock(blocks.pressurePlate(), materialName + "压力板");
            addBlock(blocks.shelf(), materialName + "架");
            addItem(items.shelf(), materialName + "架");
            addBlock(blocks.leaves(), leafName);
            addBlock(blocks.sapling(), saplingName);
            addBlock(blocks.door(), materialName + "门");
            addBlock(blocks.trapdoor(), name + "活板门");
            addBlock(blocks.sign(), name + "告示牌");
            addBlock(blocks.wallSign(), "墙上的" + name + "告示牌");
            addBlock(blocks.hangingSign(), name + "悬挂告示牌");
            addBlock(blocks.wallHangingSign(), "墙上的" + name + "悬挂告示牌");
            addItem(items.boat(), name + "船");
            addItem(items.chestBoat(), name + "运输船");
        }

        addBlock(ERBlocks.HONEY_MAPLE_RED_LEAVES, "红色蜜枫树叶");
        addBlock(ERBlocks.HONEY_MAPLE_ORANGE_LEAVES, "橙色蜜枫树叶");
        for (ERBlockEntry entry : ERBlocks.contentEntries()) {
            addBlock(entry.block(), entry.chineseName());
        }
    }

    private static String woodMaterialName(String name) {
        if (name.endsWith("树")) {
            return name.substring(0, name.length() - 1) + "木";
        }
        return name.endsWith("木") ? name : name + "木";
    }

    private static String treePartName(String name, String part) {
        return name.endsWith("树") ? name.substring(0, name.length() - 1) + part : name + part;
    }
}
