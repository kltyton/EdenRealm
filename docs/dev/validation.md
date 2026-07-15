# 验证

优先使用 Gradle Wrapper。

## 常用命令

```bat
.\gradlew.bat --version
.\gradlew.bat clean
.\gradlew.bat runData
.\gradlew.bat build
.\gradlew.bat runClient
```

## 当前成功标准

- `runData` 生成语言、模型、配方、loot、tags 到 `src/generated/resources`。
- `build` 成功，无编译错误。
- `runClient` 至少进入客户端主菜单；若持续运行，确认无启动崩溃后可停止进程。

## 检查点

- mod id 与 `gradle.properties` / `neoforge.mods.toml` 一致。
- `@Mod` 使用 `ERConstants.MOD_ID`。
- 方块和物品均通过 DeferredRegister 注册。
- 创造标签页能显示当前示例内容。
- Access Transformer 仅用于暴露必要的 datagen helper；Mixin 和反射默认不使用。
- 没有把完整 Boss、群系、结构硬编码进框架阶段。

## 已知预留

- `ERWorldgenProvider` 当前为空 provider，仅保留 worldgen datagen 扩展点。
- Boss、结构、维度和真实树木世界生成尚未实现。
- 树苗当前只注册可放置基础方块，`TreeGrower` 暂无 configured feature，后续接入 worldgen datagen 后再生长为正式树。

## 本轮验证记录

日期：2026-07-15

- 快进同步远端 `master`：`c012935..1e37f2e`，包含 `8e818fd` 与 `1e37f2e` 两次美术资源提交；远端净变化为 12 张新增珊瑚扇、12 张重绘失活珊瑚/珊瑚块和 2 张新增海草贴图。
- 将 12 张活体/失活珊瑚扇替换进运行时资源；SHA-256 审计确认它们均不再等于 `参考资料/通用占位符纹理.png`。同步 6 种失活珊瑚及其珊瑚块的重绘版本。
- 注册 `bubble_grass`（泡泡草）与 `blue_court_seagrass`（蓝庭海草），补齐方块物品、创造标签页、双语名称、海草模型、剪刀掉落、`minecraft:underwater_bonemeals` 方块标签和 `minecraft:turtle_food` 物品标签。两者只允许完整水源放置，并按贴图透明范围使用 13x5 与 14x15 的选择轮廓。
- `ERSeagrassBlock` 复用本地 26.2 原版 `SeagrassBlock` 的水源、支撑和流体更新语义；当前没有对应高株贴图，因此不实现骨粉长成原版高海草。
- `.\gradlew.bat --no-daemon --console=plain compileJava`：成功。
- `.\gradlew.bat --no-daemon --console=plain runData`：成功；补齐海龟食物标签后的最终 HashCache 为 3989 个文件、最后一次写入 1 个文件。生成资源包含 541 个 blockstate 与 529 个 client item definition。
- `.\gradlew.bat --no-daemon --console=plain build`：成功。
- `runClient`：按用户明确要求不再由自动流程验证，保留给人工客户端验收。
- 通用占位图哈希审计只剩 `eden_grass_block_snow.png`；珊瑚扇不再使用占位图。

日期：2026-07-13

- 只修正用户最终确认的六个端面模型：海岸、琥珀、绿洲三套切制/雕纹砂岩的顶面和底面引用本模组对应的平滑砂岩纹理，各自的切制/雕纹侧面保持不变；三个平滑砂岩自身、普通砂岩和水蚀砂岩模型不改。
- `.\gradlew.bat compileJava`、`.\gradlew.bat runData`、`.\gradlew.bat build`：均成功；生成资源审计确认砂岩模型差异严格只有上述六个，映射全部位于 `eden_realm` 命名空间。
- 隔离执行 `.\gradlew.bat runClient`：成功完成资源重载、声音引擎和 blocks/items/gui 图集加载，主动关闭后为 `BUILD SUCCESSFUL in 18s`；日志未发现缺失资源、模型加载失败、异常或崩溃。
- 根据 `参考资料/方块` 注册 44 个地形方块、7 个植物方块和 6 套共 48 个珊瑚方块，总计 99 个方块；同步注册 87 个方块物品、创造标签页、双语名称、模型、loot、recipe 和 tags。
- 伊甸草方块收敛为唯一 registry id `eden_grass_block`。参考 `Simple Grass Flowers v1.9.6`，同一方块生成基础、两种替换顶面、三种抬高装饰共六组外观，权重为 `120/8/12/1/4/2`，每组包含四向旋转。
- `参考资料/方块` 已有 PNG 全部同步；缺失的雪覆盖侧面与 12 张活体/失活珊瑚扇纹理暂用 `参考资料/通用占位符纹理.png`，共 13 张。
- `ERContentModelGenerators` 拆分为内容编排与 `ERCustomBlockModels` 自定义 JSON 构造，两个类分别为 181 行和 113 行；草方块权重编排独立位于 67 行的 `ERGrassModelGenerator`。
- `.\gradlew.bat runData`：成功；全部 provider 完成，清理 15 个旧草方块生成文件，写入 12 个单方块随机模型相关文件，HashCache 新文件集为 3977。
- 浮萍物品改用原版 `PlaceOnWaterBlockItem`；12 个活体/失活珊瑚扇物品改用 `StandingAndWallBlockItem` 并连接现有墙扇方块。腐木菌毯增加 `noOcclusion`，五种直立植物按贴图非透明范围修正选择轮廓。
- 修复自定义浮萍与珊瑚扇 `BlockItem` 注册未保留 `useBlockDescriptionPrefix()` 的本地化回归；这些物品现在继续查询既有 `block.eden_realm.*` 键，12 个珊瑚扇活体/失活中文名称均已恢复。
- 本地化修复后执行 `.\gradlew.bat compileJava`、`.\gradlew.bat runData` 和 `.\gradlew.bat build`：均成功；`zh_cn` 中 12 个珊瑚扇方块语言键完整，编译产物确认自定义物品属性调用 `useBlockDescriptionPrefix()`。
- 本地化修复后隔离执行 `.\gradlew.bat runClient`：成功完成用户初始化、资源重载、声音引擎和 blocks/items/gui 图集加载，主动关闭后为 `BUILD SUCCESSFUL in 22s`；日志未发现缺失资源、模型加载失败、异常或崩溃。
- 行为修复后再次执行 `.\gradlew.bat runData`：成功；HashCache 3977 个文件，`removed stale: 0`、`written: 0`。
- `.\gradlew.bat build`：成功。
- `.\gradlew.bat runClient`：使用隔离的 `run-validation` 游戏目录成功启动，完成资源重载、进入单人世界和玩家登录，随后正常保存三个维度并退出；Gradle 结果为 `BUILD SUCCESSFUL in 1m 3s`。
- `run-validation/logs/latest.log`：未发现 ERROR、Exception、`Missing resource`、模型加载失败或纹理加载失败，且未产生新 crash report。
- 生成资源审计：539 个 blockstate、527 个 client item definition，旧 `decorated_eden_grass_block_*` 文件与语言键为 0；`zh_cn` / `en_us` 缺失翻译数为 0。
- 本轮没有新增 Access Transformer，未使用 Mixin 或反射；既有 shelf datagen AT 保持不变。

日期：2026-06-23

- 从 `参考资料/展示架` 同步 22 张正式木架贴图，替换 `src/main/resources/assets/eden_realm/textures/block/*_shelf.png` 的通用占位符。
- 兼容参考资料中的旧文件名映射：`炽羽木.png -> blazing_feather_shelf.png`、`雾藤木.png -> mist_vine_shelf.png`、`暮光榴树.png -> twilight_pomegranate_shelf.png`、`天穹柏树.png -> sky_cypress_shelf.png`、`岩脊松树.png -> ridge_pine_shelf.png`。
- 检查 22 个 `<wood>_shelf.png` 文件大小均已变为正式纹理大小，不再是统一占位符大小。

日期：2026-06-23

- 补齐 22 套木架的物品语言键：`item.eden_realm.<wood>_shelf`。原因是木架物品为带 `DataComponents.CONTAINER` 的手动 `BlockItem` 注册，不走普通方块物品的 block description translation key。
- `.\gradlew.bat runData`：成功，`zh_cn` / `en_us` 均已生成 block 与 item 两套 shelf 语言项。

日期：2026-06-23

- 统一参考资料目录命名为 22 个标准树木目录；删除重复目录 `银霜杉`、`炽羽木`、`雾藤木`，并改名 `暮光榴树 -> 暮光树`、`天穹柏树 -> 天穹柏`、`岩脊松树 -> 岩脊松`。
- 保留旧 registry id，统一用户可见中文名：炽羽树、雾藤树、暮光树、天穹柏、岩脊松。
- 修正中文语言 provider 的木材材质名规则：以“树”结尾的树种生成“古灵木架/龙鳞木架/蜜枫木架”，树叶和树苗仍生成“古灵树叶/古灵树苗”。
- `.\gradlew.bat runData`：成功，重新生成 `zh_cn` 等数据，生成文件总数 3496。
- `.\gradlew.bat build`：成功。
- `.\gradlew.bat runClient`：成功，客户端资源加载完成并正常退出；最新日志未发现 `Missing resource`、`ERROR`、`Exception`、`Crash`。

日期：2026-06-23

- 根据 `参考资料/树木的基础方块,物品和实体` 更新木材资源：同步 22 套树木的 block/item/entity/gui 贴图到 `src/main/resources/assets/eden_realm/textures/**`。
- 新增 2 套树木基础内容：岩脊松 `ridge_pine`、蜜枫树 `honey_maple`；同步注册链、创造标签页、语言、模型、配方、loot、tags、船实体。
- 根据参考资料目录命名同步中文显示名：银霜松；保留既有 registry id，避免不必要的数据迁移。
- 修正中文语言 provider 对以“木”结尾名称的重复“木”问题。
- 参考资料中发现两个拼写不一致资产：`雾藤树/door_buttom.png`、`云冠树/trapddor.png`，已分别映射到正确资源路径 `mist_vine_door_bottom.png`、`cloud_crown_trapdoor.png`。
- `.\gradlew.bat runData`：成功，执行 `en_us`、`zh_cn`、model、recipe、loot、block tag、item tag、entity type tag、worldgen placeholder provider；生成文件总数 3496。
- `.\gradlew.bat build`：成功。
- `.\gradlew.bat runClient`：成功，客户端资源加载完成，`Sound engine started`、blocks/items/gui texture atlas 初始化成功，进入本地世界后正常保存退出；最新日志未发现 `Missing resource`、`ERROR`、`Exception`，未产生新 crash report。
- Qodana workflow 已增加 `src/**` 路径过滤：push / pull request 只有包含 `src` 修改才自动运行，手动运行仍保留。

日期：2026-06-23

- 为 20 套树木补齐高版本木材家族缺口：木、去皮木、木架；同步补方块物品、创造标签页、语言、模型、配方、loot、block/item tags。
- 木架使用原版 `ShelfBlock`、原版 `SHELF` 方块实体类型和原版 `wooden_shelves` tag；物品带 `DataComponents.CONTAINER` 空容器组件。
- 新增 `src/main/resources/META-INF/accesstransformer.cfg`，只暴露 `BlockModelGenerators#createShelf(Block, Block)`，用于 datagen 生成原版 multipart shelf 模型；未使用 Mixin 或反射。
- 缺失 20 个 `<wood>_shelf.png`，已先复制 `参考资料/通用占位符纹理.png` 作为临时贴图。
- `.\gradlew.bat compileJava`：首次因 26.2 实际 `BlockTags` 缺少 `LOGS_THAT_BURN` 失败；移除 block 侧该版本不存在的 tag 后成功。
- `.\gradlew.bat runData`：成功，执行 `en_us`、`zh_cn`、model、recipe、loot、block tag、item tag、entity type tag、worldgen placeholder provider；生成文件总数 3184，写入 572 个文件。
- 抽查 `ice_crystal_pine_shelf`：blockstate 为原版 multipart，包含 `facing`、`powered`、`side_chain`；生成 inventory item model、recipe 和 `minecraft:wooden_shelves` tag。
- `.\gradlew.bat build`：成功。
- `.\gradlew.bat runClient`：成功，客户端资源加载完成，`Sound engine started`、blocks/items/gui texture atlas 初始化成功，进入本地世界后正常保存退出；最新日志未发现 `Missing resource`、`ERROR`、`Exception`，未产生新 crash report。

日期：2026-06-23

- 为 20 套树木补齐木制派生方块：楼梯、台阶、栅栏、栅栏门、按钮、压力板；同步补方块物品、创造标签页、语言、模型、配方、loot、block/item tags。
- 检查 20 套 `<wood>_planks.png` 均存在；新增派生方块复用木板贴图，本轮未使用 `参考资料/通用占位符纹理.png`。
- `.\gradlew.bat --no-daemon --console=plain compileJava`：首次因 26.2 实际 `ItemTags` 缺少通用 `STAIRS` / `SLABS` / `FENCES` / `BUTTONS` 常量失败；改为只写 26.2 存在的木制 item tags 后成功。
- `.\gradlew.bat --no-daemon --console=plain runData`：成功，执行 `en_us`、`zh_cn`、model、recipe、loot、block tag、item tag、entity type tag、worldgen placeholder provider；生成文件总数 2620，写入 960 个文件。
- `.\gradlew.bat --no-daemon --console=plain build`：成功。
- `.\gradlew.bat --no-daemon --console=plain runClient`：客户端启动到资源加载完成信号，日志出现 texture atlas `Created:` 与 `Sound engine started`；无新增 crash report；随后主动停止 Gradle/Minecraft 进程。日志扫描未发现新增 `Missing resource`。
- 本轮未使用 Access Transformer、Mixin 或反射。

日期：2026-06-23

- `.\gradlew.bat compileJava`：成功，验证新增树种枚举、NeoForge `BlockEntityTypeAddBlocksEvent` 和 `BlockEntityTypes.SIGN/HANGING_SIGN` API 可编译。
- `.\gradlew.bat runData`：成功，执行 `en_us`、`zh_cn`、model、recipe、loot、block tag、item tag、entity type tag、worldgen placeholder provider；生成文件总数 1663，新增树种后写入 367 个文件。
- `.\gradlew.bat build`：成功。
- `.\gradlew.bat --no-daemon --console=plain runClient`：客户端启动到资源重载阶段，日志显示 Eden Realm、Minecraft、NeoForge 均加载，OpenGL、OpenAL、Sound engine、blocks/items/gui texture atlas 初始化成功；没有新 crash report，随后主动停止进程树。
- 修复 `crash-2026-06-22_19.37.24-client.txt` 指向的悬挂告示牌放置崩溃：通过 NeoForge 事件把 Eden 告示牌加入原版 `SIGN` / `HANGING_SIGN` valid block 集合。
- 新增 4 套树木基础内容：月潮树、暮光树、圣辉树、天穹柏。
- 新增 GitHub Actions 自动构筑：push 中包含 `src/**` 时运行 `runData` 和 `build`；仅推送 `参考资料/**` 不触发。
- 补齐 20 套树种告示牌编辑界面贴图：`textures/gui/signs/<wood>.png` 和 `textures/gui/hanging_signs/<wood>.png`。普通告示牌 GUI 复用参考资料中的 `sign.png`，悬挂告示牌 GUI 使用 `hanging_sign_ui.png`。
- 修复 20 套树种活板门方向性贴图：模型 datagen 不再直接调用 oak/iron 风格的 `createTrapdoor`，改为通过 `BlockFamily` 进入原版 spruce 风格的 orientable trapdoor 生成逻辑；关闭状态会按 `facing` 输出 `y` 旋转。
- 本轮未使用 Access Transformer、Mixin 或反射。

日期：2026-06-22

- `.\gradlew.bat compileJava`：成功。
- `.\gradlew.bat runData`：首次因告示牌语言重复键失败，修正后成功。执行了 `en_us`、`zh_cn`、model、recipe、loot、block tag、item tag、entity type tag、worldgen placeholder provider。
- `.\gradlew.bat build`：成功。
- `.\gradlew.bat --no-daemon runClient`：成功启动到客户端资源加载阶段，日志显示 `Eden Realm 1.0.0 (eden_realm)` 被加载，OpenGL、OpenAL、Sound engine 和 texture atlas 初始化成功；未出现崩溃信号，随后主动停止本次进程树。
- 占位测试内容 `EDEN_CRYSTAL_BLOCK`、`EDEN_CRYSTAL`、`EDEN_STONE` 已从源码、主资源和生成资源移除。
- 本轮未使用 Access Transformer、Mixin 或反射。
