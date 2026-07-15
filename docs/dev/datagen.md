# Datagen

生成输出目录：`src/generated/resources`

运行命令：

```bat
.\gradlew.bat runData
```

## Provider

- `ERDataGenerators`：集中注册 datagen provider。当前 `runData` 使用 NeoForge 的 `clientData` run，一次性注册客户端资源和服务端数据 provider，避免拆分运行时清理同一个输出目录。
- `EREnglishLanguageProvider`：`en_us`。
- `ERChineseLanguageProvider`：`zh_cn`。
- `ERModelProvider`：统一生成 blockstates、block models、item models、client item definitions，并让 26.2 `ModelProvider` 校验本模组注册项是否遗漏模型。
- `ERRecipeProvider`：生成 crafting recipes。
- `ERLootTableProvider` / `ERBlockLootSubProvider`：生成 block loot tables。
- `ERBlockTagsProvider` / `ERItemTagsProvider` / `EREntityTypeTagsProvider`：生成 block/item/entity type tags。
- `ERWorldgenProvider`：worldgen datagen 预留入口，当前不输出 JSON。

## 当前生成内容

- 语言：创造标签页、22 套树木基础内容、101 个地形/植物/珊瑚方块及其 89 个方块物品。
- 模型：原有完整木材家族，以及地形 cube/柱体/顶底模型、单一草方块的六组加权随机外观、草径、耕地、陆生植物、浮萍、两种水下海草和 6 套珊瑚模型。
- 物品渲染定义：所有注册物品的 client item definition。
- 配方：原有完整木材家族，以及原岩砖、雕纹/苔化/柱体变体、三组砂岩合成与切石、两种矿石的熔炼和高炉配方。
- 掉落：新增方块均由 datagen 管理；原岩无精准采集时掉碎岩，矿石使用原版式矿物掉落，草/草径/耕地回落伊甸泥土，两种海草使用原版式剪刀掉落，活珊瑚遵循精准采集规则，墙上珊瑚扇共享落地扇 loot table。
- 标签：原有木材标签，以及工具/挖掘等级、`dirt`、`sand`、植被支撑、耕地、水下骨粉、海龟食物、矿石和珊瑚相关 block/item tags。
- 当前 `runData` 最近一次 HashCache 新文件集为 3989；模型生成后共有 541 个 blockstate 和 529 个 client item definition，双语翻译审计缺失数为 0。
- 木制派生方块模型复用对应 `<wood>_planks.png`。木架模型引用 `<wood>_shelf.png`；22 套木架贴图已从 `参考资料/展示架` 同步，不再使用通用占位符。
- 伊甸草方块的六组自定义分层模型由 `ERGrassModelGenerator` 和 `ERCustomBlockModels` 生成，不手写 JSON；物品 tint 定义也由 `ERModelProvider` 生成。

## 人工资源与占位图

- `src/main/resources/assets/eden_realm/textures/colormap/grass.png` 来自 `参考资料/SaturatedGrass!(1.2v)`，是运行时使用的专用 256x256 色图。
- `参考资料/方块` 中已有的地形、植物和珊瑚 PNG 已同步到 `textures/block`。
- 6 种珊瑚的 12 张活体/失活 `*_coral_fan.png` 已替换为正式贴图，6 种失活珊瑚及其珊瑚块也已同步最新重绘版本；泡泡草和蓝庭海草贴图已接入对应注册项。
- 目前仅剩 1 张通用占位图：`eden_grass_block_snow.png`。
- 海岸、琥珀、绿洲三套切制/雕纹砂岩暂不复制新的占位 PNG：模型由 datagen 保留各自的切制/雕纹侧面，并引用本模组对应的 `smooth_<系列>_sandstone` 作为顶/底端面；后续六个正式端面贴图完成后再替换。平滑、普通和水蚀砂岩模型保持原样。
- `eden_grass_block_decoration_2.png` 含 4 个半透明像素；若需要完全 cutout 的硬边装饰，应在最终美术阶段将其 alpha 修正为 0 或 255。
- `参考资料/Simple Grass Flowers v1.9.6` 只作为单方块加权模型逻辑参考；其包内没有独立积雪草侧面 PNG，也没有任何珊瑚纹理。原包 `snowy=true` 直接引用原版 `minecraft:block/grass_block_snow` 模型。

## 规则

- 不手写可 datagen 的 JSON。
- 修改 provider 后必须运行 `runData`。
- `src/generated/resources` 是生成产物，不手工编辑。
- 贴图、音效等人工资产允许放在 `src/main/resources/assets/<modid>`。
