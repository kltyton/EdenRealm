# AGENTS.md

## Project

项目名称：伊甸之境 / Eden Realm
项目类型：Minecraft 大型维度模组
目标加载器：NeoForge 26.2
目标：探索、生存、收集、成长、Boss 挑战的大型维度模组。

本文件是给 Codex / 代码智能体的仓库级工作约束。所有任务开始前必须先阅读本文件，并以本文件为最高优先级的项目规则之一。

---

## Non-negotiable Goals

1. 最终结果必须能够成功编译。
2. 最终结果必须能够运行 `runClient`，并至少进入客户端主菜单或给出明确、可复现的客户端启动结果。
3. 智能体允许修改、删除、新增仓库内任何文件，但必须保持变更可审计、可解释、可回滚。
4. 不允许为了“快速通过”而注释掉核心功能、删掉注册、跳过错误、吞异常或降低版本。
5. 不允许把大量无关类堆在同一个目录下。项目结构必须清晰，方便人工审计。
6. 任何 JSON、资源文件、数据包文件，只要 NeoForge / Minecraft datagen 能生成，必须优先使用 datagen。不要手写大批 JSON。
7. 类、方法、字段访问性问题的优先级：

    * 首选：Access Transformer。
    * 其次：Mixin。
    * 最后才考虑反射。
    * 反射必须有明确理由、局部封装、注释说明和失败处理。
8. 以当前仓库的 `build.gradle`、`gradle.properties`、`settings.gradle`、`neoforge.mods.toml` 为实际版本来源。不要凭记忆假设 NeoForge API。
9. 每次较大改动后必须运行合适的 Gradle 检查，不要堆积大量未验证代码。

---

## Permissions

你可以：

* 修改任何 Java、Gradle、TOML、资源、文档文件。
* 删除明显过时、损坏、重复、无用或与当前 NeoForge 26.2 框架冲突的文件。
* 新增包、类、数据生成器、文档、测试和辅助脚本。
* 重构项目结构，只要保持可编译、可运行，并在最终总结中说明迁移关系。

你不应该：

* 删除用户手工资产，例如贴图、模型、音效、设计文档，除非它们明确损坏或被替代。
* 引入不必要的第三方依赖。
* 修改 Gradle 到不兼容版本。
* 使用网络下载不明脚本。
* 自动提交 Git commit，除非用户明确要求。
* 隐藏编译错误、运行错误或数据包加载错误。

---

## Build and Verification Commands

优先使用仓库自带 Gradle Wrapper。

Windows：

```bat
.\gradlew.bat --version
.\gradlew.bat clean
.\gradlew.bat runData
.\gradlew.bat build
.\gradlew.bat runClient
```

Linux / macOS：

```bash
./gradlew --version
./gradlew clean
./gradlew runData
./gradlew build
./gradlew runClient
```

如果当前项目没有 `runData` 任务，先检查 Gradle 配置，不要直接放弃 datagen。
如果运行环境没有 GUI，仍然必须完成 `runData` 和 `build`，并明确说明 `runClient` 的阻塞原因、已执行命令、日志位置和下一步人工验证命令。

`runClient` 成功标准：

* 客户端启动过程中没有 registry、datapack、mixin、classloading、resource reload 崩溃。
* 能进入主菜单，或至少能证明客户端初始化成功并且没有异常退出。
* 如果启动后持续运行，不要让任务无限挂起；在确认无崩溃后可停止进程并记录结果。

---

## Project Structure Rules

基础包名以仓库现有包名为准。若项目尚未确定包名，使用：

```text
com.edenrealm
```

推荐结构：

```text
src/main/java/<base_package>/
  EdenRealm.java
  ERConstants.java

  registry/
    ERBlocks.java
    ERItems.java
    ERCreativeTabs.java
    EREntityTypes.java
    ERBlockEntities.java
    ERMobEffects.java
    ERSoundEvents.java
    ERParticleTypes.java
    ERMenuTypes.java
    ERDataComponents.java

  common/
    block/
    item/
    entity/
      passive/
      neutral/
      hostile/
      boss/
    effect/
    menu/
    recipe/
    loot/
    capability/
    attachment/
    component/

  world/
    biome/
    dimension/
    feature/
    foliage/
    tree/
    structure/
    placement/
    spawn/
    portal/

  client/
    renderer/
      blockentity/
      entity/
    model/
    particle/
    screen/
    sound/
    color/

  data/
    ERDataGenerators.java
    lang/
    model/
    loot/
    recipe/
    tag/
    worldgen/

  mixin/
    Only add mixins when AT/API is not enough.

  util/
```

资源结构：

```text
src/main/resources/
  META-INF/
    neoforge.mods.toml
    accesstransformer.cfg
  assets/<modid>/
    lang/
    textures/
    models/
    blockstates/
    sounds.json
  data/<modid>/
    Only hand-written data that cannot reasonably be datagenned.

src/generated/resources/
  Generated JSON output only. Do not hand edit.
```

文档结构：

```text
docs/
  design/
    eden_database.md
  dev/
    architecture.md
    datagen.md
    validation.md
```

要求：

* Registry 类按 Minecraft / NeoForge 概念分类，不要创建一个巨大的 `ModRegistry`。
* 生物按被动、中立、敌对、Boss 分类。
* 世界生成按 biome、feature、tree、structure、dimension 分类。
* client-only 代码必须放在 `client` 包，不得被 common 初始化路径直接 classload。
* 数据生成器按 lang、model、loot、recipe、tag、worldgen 分类。
* 大型内容列表先进入文档或 catalog，不要一次性硬编码到一个巨型类里。

---

## Naming Rules

* `modid` 使用小写英文、数字、下划线。若项目尚未指定，使用 `edenrealm`。
* Registry ID 使用 `snake_case`。
* Java 类使用清晰英文名，不使用拼音。
* 用户可见名称可以同时提供 `zh_cn` 和 `en_us` 翻译。
* Boss、群系、生物、植物等设计名应保留在文档和语言文件中。
* 不要使用中文作为 registry id、文件名或资源路径。

示例：

```text
ice_crystal_forest
silver_frost_forest
sky_plateau
ancient_forest
starshine_flower
ice_crystal_pine_log
tree_spirit_king
mermaid_queen
sunflower_monarch
moss_stone_colossus
```

---

## Access, Mixins, and Reflection

访问 Minecraft 或 NeoForge 内部成员时按以下顺序处理：

1. 先查找公开 API、事件、扩展点、DeferredRegister、datapack registry、capability / attachment 等正规方式。
2. 如果只是访问性不足，使用 Access Transformer。
3. 如果需要改变方法行为，且没有事件或 API，才使用 Mixin。
4. 反射只允许用于极小范围兼容性逻辑，并必须：

    * 封装在专用 helper 类中。
    * 有清晰注释说明为什么 AT / Mixin 不可行。
    * 有异常处理。
    * 不在 tick 高频路径中使用。
    * 不吞掉关键错误。

Access Transformer 文件默认位置：

```text
src/main/resources/META-INF/accesstransformer.cfg
```

每条 AT 必须有注释说明用途和调用点。不要添加无用 AT。

Mixin 要求：

* 只为不可避免的行为修改添加。
* 每个 Mixin 类顶部说明目的。
* 避免宽泛 injection。
* 避免和其他模组高冲突的注入点。
* 必须验证 `runClient` 不因 Mixin apply 失败而崩溃。

---

## Datagen Rules

强制优先 datagen 的内容：

* blockstates
* item models / block models
* client item definitions
* language files
* recipes
* loot tables
* tags
* biome tags
* damage type / trim / painting 等数据注册内容
* biome modifiers
* configured features
* placed features
* structures / structure sets / template pools
* dimension / dimension type / worldgen 相关 JSON
* advancements
* data maps / global loot modifiers

允许手写的内容：

* `neoforge.mods.toml`
* `accesstransformer.cfg`
* Mixin config
* 少量无法合理 datagen 的特殊资源
* 贴图、音效、模型源文件等人工资产
* 临时占位资源，但必须在最终总结中标记

规则：

* 生成文件输出到 `src/generated/resources`，不要手动编辑生成结果。
* 如果修改了 datagen provider，必须运行 `runData`。
* 如果手写 JSON 是不得已，必须说明原因。
* 不要复制粘贴大量相似 JSON。
* datagen provider 必须拆分清楚，不要把所有生成逻辑写进一个超大类。

---

## NeoForge / Minecraft Coding Rules

* 使用 DeferredRegister / DeferredHolder 等当前 NeoForge 推荐注册方式。
* 区分 mod event bus 和 game event bus。
* 注册、common setup、client setup 必须放在正确生命周期。
* 避免静态初始化顺序问题。
* RegistryObject / DeferredHolder 不得在注册完成前强行取值。
* 不要在 common 代码直接引用 Minecraft client-only 类。
* Entity 属性、渲染器、spawn placement、egg、loot、tag 必须分别注册。
* Biome、feature、structure、dimension 先以最小可运行框架建立，再逐步填内容。
* 大型 Boss AI、复杂世界生成、网络同步不要在框架阶段一次性完成。
* 日志使用 mod logger，不要大量 `System.out.println`。
* 配置、网络、数据组件等系统先建立清晰入口，避免散落各处。

---

## Content Direction

项目内容来自“伊甸之境开发总数据库”。当前规划包括：

* 30+ 群系
* 40+ 树木
* 100+ 植物
* 60+ 生物
* 8+ Boss
* 20+ 结构

初始框架阶段不要试图一次性完整实现全部内容。应先建立：

1. 可编译可启动的 NeoForge 26.2 基础框架。
2. 清晰 registry 分类。
3. datagen 管线。
4. 一个或少量示例方块 / 物品 / 创造模式标签页。
5. 内容数据库文档。
6. 为群系、植物、生物、Boss、结构预留清晰扩展位置。
7. 可持续迭代的世界生成架构。

优先落地的内容分类：

* 群系：冰晶森林、天空台地、远古森林、幽光沼泽、蓝庭海谷、曙光平原、岩苔高原。
* 树木：冰晶松、星辉树、古灵树、幽灯树、圣辉树、岩脊松。
* 生物：冰晶蝶、云兽、冠羽猿、潮汐蟹、发光水母。
* Boss：树灵之王、向日葵花王、人鱼女王、苔石巨像。
* 结构：树桩、星潮遗庭、向日葵花园、巨像遗迹。

---

## Framework-first Development Policy

当用户要求“先搭框架”时，只做框架，不要过度实现玩法。

框架阶段应该包括：

* Mod 主类。
* 常量类。
* Registry 类。
* 基础 creative tab。
* 1-3 个可验证示例方块 / 物品。
* DataGen 入口和 provider 拆分。
* `zh_cn` / `en_us` 语言生成。
* 基础 blockstate / model / item model 生成。
* 基础 loot table / recipe / tag 生成。
* docs 目录和内容数据库文档。
* 可后续扩展的 biome / entity / boss / structure 包结构。

框架阶段不应该包括：

* 复杂 Boss 战。
* 完整 30+ 群系世界生成。
* 复杂实体 AI。
* 复杂网络协议。
* 大量未验证 placeholder JSON。
* 大量无法编译的半成品类。

---

## Documentation Requirements

每次较大变更后更新相关文档：

```text
docs/design/eden_database.md
docs/dev/architecture.md
docs/dev/datagen.md
docs/dev/validation.md
```

文档至少说明：

* 当前已实现内容。
* 当前只是占位的内容。
* 如何运行 datagen。
* 如何编译。
* 如何运行客户端。
* 下一步建议。

---

## Review Checklist Before Final Response

最终回复用户前必须检查：

* `git status` 或等效方式确认改了哪些文件。
* `runData` 是否执行；如果没有，说明原因。
* `build` 是否通过。
* `runClient` 是否执行；如果不能执行，说明环境限制和人工验证命令。
* 是否有手写 JSON；如有，说明原因。
* 是否添加了 AT / Mixin / 反射；如有，说明原因。
* 是否存在 client-only classloading 风险。
* 是否存在 registry 初始化顺序风险。
* 是否更新文档。
* 是否保留了清晰目录结构。

最终回复格式：

```text
完成内容：
- ...

验证：
- ... 命令：结果

重要变更文件：
- ...

风险 / 后续：
- ...
```

不要只说“完成了”。必须给出可审计结果。
---

## Source Code Discovery & No-Guess Policy (CRITICAL)

### 1. 强制源码扫描规则

当智能体需要查找 Minecraft / NeoForge / MCP / 游戏源码时：

* **禁止直接假设类路径存在**
* **禁止凭记忆写类路径**
* 如果常规路径不存在（例如 `net.minecraft.world.entity.monster.Zombie`）：

必须执行：

> 在本地 官方源码路径 / MCP / decompiled cache 中进行目录扫描，而不是放弃或猜测路径

MCP示例标准路径：

```
G:\weituo\mcp\minecraft-dev-cache\decompiled\26.2\mojmap\
```

必须支持递归查找，例如：

* entity
* monster
* zombie
* Zombie.java

📌 规则：

* 找不到 ≠ 不存在
* 必须“扫描整个源码树”
* 必须优先 官方源码(因为在高版本官方已取消混淆,源码可以直接读取!)，其次MCP cache

---

### 2. Source Priority Order（源码优先级）

所有原版代码参考必须遵循：

1. 官方 NeoForge / Mojmap decompiled source（最高优先级）
2. MCP / minecraft-dev-cache
3. 仅当以上都不存在时才允许参考 API 文档或类似实现

🚫 禁止：

* ChatGPT 记忆路径
* 猜测 package
* 编造类结构

---

### 3. 自动注册完整性规则（CRITICAL GAMEPLAY INTEGRITY）

当新增以下内容时：

* Entity（生物）
* Item（物品）
* Block（方块）
* BlockEntity
* Spawn Egg
* Creative Tab 内容
* Attribute / DataComponent

必须自动完成：

#### 3.1 注册链完整性

必须确保：

* 已注册到 DeferredRegister
* 已正确加入 Mod Event Bus
* 已连接到对应 registry 类（如 ERItems / EREntities）
* 不允许“只注册一半”

---

#### 3.2 自动补全内容（MANDATORY）

如果新增 Entity：

必须同时生成：

* Spawn Egg（如果适用）
* EntityType 注册
* Attribute 注册（如果适用）
* Renderer（client）
* Spawn rules（如果适用）
* Loot table（如果适用）
* **Creative Tab 加入**

如果新增 Item：

必须同时生成：

* Creative Tab 添加
* Language entry（zh_cn + en_us via datagen）
* Model JSON（via datagen）
* Tag（如果属于分类物品）

如果新增 Block：

必须同时生成：

* BlockState
* BlockModel
* ItemBlock
* Loot table
* Tag（if applicable）
* Creative Tab

---

### 4. 自动注册风格约束（LIKE VANILLA STYLE）

注册必须满足：

* 使用集中式 register 方法（允许多个 registry 类）
* 避免分散式 new + static 初始化
* 优先结构：

```
ERItems.register()
ERBlocks.register()
ERBiomes.register()
```

或：

```
DeferredRegister.create(...)
```

但必须“统一入口”

🚫 禁止：

* 零散 RegistryObject 分布全项目
* 在构造函数里注册
* static block hack 注册

---

### 5. 不确定性处理规则（ANTI-HALLUCINATION POLICY）

当遇到：

* 找不到类
* 找不到方法
* 找不到字段
* Minecraft 版本差异

必须：


1. 先 Mojmap decompile 搜索
2. 再 NeoForge source 搜索
3. 再 MCP cache 搜索
4. 最后才允许 fallback 到“结构推断”

🚫 禁止直接猜 API

---

### 6. 安全失败策略

如果仍然找不到源码：

必须输出：

* 已搜索路径
* 搜索关键词
* 未找到原因
* 替代方案（如 mixin / AT）

而不是“假装存在”

---
