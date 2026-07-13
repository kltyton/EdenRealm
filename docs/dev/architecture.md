# 架构

## 基础信息

- Mod 名称：伊甸之境 / Eden Realm
- 当前 mod id：`eden_realm`
- 当前基础包：`com.kltyton.eden_realm`
- Minecraft / NeoForge 版本来源：`gradle.properties`

## 入口

- `EdenRealm` 是 NeoForge `@Mod` 主类。
- `ERConstants` 集中保存 mod id、名称和常用 `Identifier` / recipe key helper。
- 主类只做注册入口和 datagen 监听注册，不放内容定义。

## Registry

Registry 按概念拆分到 `registry` 包：

- `ERBlocks`
- `ERItems`
- `ERCreativeTabs`
- `EREntityTypes`
- `ERBlockEntities`
- `ERMobEffects`
- `ERSoundEvents`
- `ERParticleTypes`
- `ERMenuTypes`
- `ERDataComponents`

当前注册 22 套树木基础内容，并通过 `registry/content` 注册 99 个地形、植物和珊瑚方块，其中 87 个具有独立方块物品。`ERWoodSet` 是树木单一数据源；`ERBlockEntry` 统一保存新增内容的 registry id、双语名称、方块 holder 和物品策略。菜单、音效、粒子等 registry 目前保留为空入口。

当前木材基础链路：

- `common/block/ERWoodSet`：树木集合、命名、`BlockSetType`、`WoodType`。
- `common/block/ERStairBlock` / `ERButtonBlock` / `ERPressurePlateBlock`：只公开原版受保护构造器，避免为木制派生方块引入 AT、Mixin 或反射。
- `registry/ERBlocks`：原木、木、去皮原木、去皮木、木板、楼梯、台阶、栅栏、栅栏门、按钮、压力板、木架、树叶、树苗、门、活板门、告示牌、悬挂告示牌。
- `registry/ERItems`：方块物品、带空容器组件的木架物品、告示牌物品、悬挂告示牌物品、普通船、运输船。
- `registry/EREntityTypes`：普通船实体、运输船实体。
- `registry/ERBlockEntities`：不注册自定义告示牌或木架方块实体；通过 NeoForge `BlockEntityTypeAddBlocksEvent` 将 Eden 的告示牌、悬挂告示牌和木架加入原版 `SIGN` / `HANGING_SIGN` / `SHELF` 的 valid block 集合，避免放置和交互时方块实体校验崩溃。
- `common/item/ERBoatItem`：延迟解析 `EntityType` 的船物品，避免 DeferredRegister 注册时序问题。

地形与生态内容链路：

- `registry/content/ERTerrainBlocks`：44 个基础岩石、矿石、泥土/草地、天空、寒地、沼泽、沙与砂岩方块。
- `registry/content/ERPlantBlocks`：7 个基础植物方块。
- `registry/content/ERCoralBlocks`：6 个珊瑚种类，每种 8 个活体/失活及落地/墙上变体，共 48 个方块。
- `registry/ERItems`：从 `ERBlockEntry` 集中派生 87 个方块物品。浮萍使用原版 `PlaceOnWaterBlockItem`；12 个活体/失活珊瑚扇使用 `StandingAndWallBlockItem`，在同一物品内选择落地扇或已有墙扇方块。所有自定义 `BlockItem` 注册统一调用 `Item.Properties#useBlockDescriptionPrefix()`，继续使用 `block.<modid>.*` 双语名称。
- `common/event/ERBlockToolEvents`：通过公开的 `BlockToolModificationEvent` 处理伊甸泥土/草地的铲平和锄耕。
- `common/block/ERGrassBlock`、`ERDirtPathBlock`、`ERFarmlandBlock`：保持退化、草地扩散、骨粉、湿度和踩踏行为落回伊甸泥土，不混入原版泥土链。
- `common/block/ERShapedBushBlock`、`ERShapedDryVegetationBlock`：为五种直立植物提供与贴图非透明范围一致的选择轮廓，并跟随方块随机模型偏移；方块仍沿用原版植物的无物理碰撞属性。
- 腐木菌毯沿用原版 `CarpetBlock` 的 1 像素高度和支撑规则，但设置 `noOcclusion`，避免透明纹理区域错误遮掉下方方块顶面。

## 草色系统

- `client/color/ERGrassColorReloadListener` 从 `eden_realm:textures/colormap/grass.png` 加载并校验 256x256 色图。
- `client/color/ERGrassColors` 定义独立 `ColorResolver` 和方块 tint source，按群系修改后的温度、降水、草色 override 与 modifier 计算颜色。
- `client/color/ERGrassColorSource` 为物品模型提供 `eden_realm:grass` tint codec。
- 单个 `eden_grass_block` 使用该 resolver；草底色与侧面 overlay 使用 `tintindex: 0`，三个顶部装饰层不着色。
- `data/model/ERGrassModelGenerator` 参考 Simple Grass Flowers 的资源包结构，为同一方块生成六组加权随机外观和四向旋转；装饰平面位于 `Y=16.075`，避免与草顶面共面。
- 所有运行时着色类位于 `client` 包；common registry 和方块行为不引用客户端类。

## Access Transformer

`src/main/resources/META-INF/accesstransformer.cfg` 只暴露 `BlockModelGenerators#createShelf(Block, Block)`。原因是 26.2 原版 shelf 的 multipart blockstate/model 生成逻辑没有公开 API，而 shelf 需要 `facing`、`powered`、`side_chain` 的原版状态模型。该 AT 仅用于 `data/model/ERModelProvider` datagen 调用；当前未使用 Mixin 或反射。

## 包结构

- `common`：通用玩法逻辑，按 block、item、entity、effect、menu、recipe、loot、capability、attachment、component 拆分。
- `world`：维度、群系、特性、树、结构、放置、生成、传送门相关逻辑。
- `client`：renderer、model、particle、screen、sound、color 等客户端专用代码。
- `data`：datagen 入口和各类 provider。
- `util`：共享工具和 tag key。

## 客户端边界

`client/ERClientEvents` 只在客户端 Dist 自动订阅，注册船模型层、船 renderer、草色 resolver、tint codec 和色图重载监听器。主类不直接引用 renderer/model/color 类。`data/model` 使用 `net.minecraft.client.*` datagen API，仅用于 datagen。

NeoForge 1.21.4+ 将 data run 拆为 `clientData` / `serverData`。当前项目的标准入口仍是 `runData`，并在一次 `GatherDataEvent.Client` 中注册全量 provider，确保 `src/generated/resources` 同时保留 `assets/` 和 `data/` 输出。

## CI

`.github/workflows/build.yml` 定义 GitHub Actions 自动构筑。自动触发条件为 push 中包含 `src/**` 修改；仅推送 `参考资料/**`、文档或其他非 `src` 文件不会触发自动构筑。工作流使用 Java 25，依次运行 `runData` 和 `build`，并上传 `build/libs/*.jar`。

`.github/workflows/qodana_code_quality.yml` 定义 Qodana 质量检查。自动触发条件同样限制为 push / pull request 中包含 `src/**` 修改；手动 `workflow_dispatch` 不受路径过滤限制。

## 扩展顺序

1. 新内容先加入设计数据库和 registry id 规划。
2. 注册最小内容对象。
3. 同步补 datagen：语言、模型、loot、recipe、tags。
4. 对实体补完整链：EntityType、属性、renderer、物品或 spawn egg、loot、创造标签页。
5. 对世界生成补 datapack registry provider，再跑 `runData` 和 `runClient`。
