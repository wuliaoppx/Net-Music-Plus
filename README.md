# Net Music Plus

给 [NetMusic](https://github.com/TartaricAcid/NetMusic)（网络音乐机）追加两样东西：**扩音附魔**，和一台 **MP3 随身听**。
一个仓库承载 6 个版本组合（Forge / Fabric / NeoForge × 1.20.1 / 1.21.1 / 26.1.2）。

## 功能

### 扩音附魔（Amplifier I / II / III）
- 把附魔书在铁砧上打到 `netmusic:music_player` 或 `netmusicplus:mp3_player` 上，声音可听半径变为 **×2 / ×3 / ×4**（原版唱片机 64 格 → 128 / 192 / 256 格）。
- 同名附魔叠加时等级 +1，上限 III。
- 方块被破坏时等级写回掉落物，重新放置后仍然生效。
- 装 Jade 时在方块信息里显示「扩音：II 级（可听 192 格）」。

三本书的配方（无序合成）：

| 等级 | 原料 |
|---|---|
| I | 音符盒 + 铜锭 |
| II | 音符盒 + 铜锭 + 紫水晶碎片 |
| III | 音符盒 + 铜锭 + 紫水晶碎片 + 回响碎片 |

### MP3 随身听
- 20 个唱片位；`mp3_expander`（唱片扩容件）每个 +10 格，最多装 3 个（上限 50 格）。
- 播放模式：循环 / 单曲 / 随机；播放方式：外放（附近玩家都能听到，范围受扩音附魔影响）/ 仅自己。
- HUD 可显示当前曲目与歌词，位置可拖动调整，4 种界面主题。
- 快捷键：长按 = 暂停 / 继续，单击 = 下一首，双击 = 上一首；也可用 `潜行 + 右键` 或快捷键打开唱片柜界面。
- 可放进 Curios（Forge / NeoForge）/ Trinkets（Fabric）的 `mp3` 饰品槽。

## 支持版本

| 目录 | Minecraft | 加载器 | mod 版本 | 构建 JDK |
|---|---|---|---|---|
| `versions/1.20.1-forge` | 1.20.1 | Forge 47.4.28 | 1.0.1 | Gradle 需 JDK 21，ForgeGradle 另需 JDK 17 |
| `versions/1.20.1-fabric` | 1.20.1 | Fabric Loader 0.16.14 / API 0.92.12 | 1.0.1 | 17 |
| `versions/1.21.1-neoforge` | 1.21.1 | NeoForge 21.1.259 | 1.2.8 | 21 |
| `versions/1.21.1-fabric` | 1.21.1 | Fabric Loader 0.20.1 / API 0.116.17 | 1.3.1 | 21 |
| `versions/26.1.2-neoforge` | 26.1.2 | NeoForge 26.1.2.122 | 1.0.2 | 25 |
| `versions/26.1.2-fabric` | 26.1.2 | Fabric Loader 0.20.1 / API 0.155.3 | 1.0.2 | 25 |

## 构建

每个版本目录都是独立的 Gradle 工程。JDK 由 `JAVA_HOME` 决定（`org.gradle.java.home` 不写死在本仓库里）：

```bash
cd versions/1.21.1-fabric
JAVA_HOME=/path/to/jdk-21 ./gradlew build      # Windows: set JAVA_HOME=... 后 gradlew.bat build
```

产物在 `build/libs/` 下。

### 需要自备的编译依赖（`libs/` 不随仓库分发）
把下列 jar 放到对应版本目录的 `libs/` 里：

| 目录 | 需要的 jar |
|---|---|
| `1.20.1-forge` | `netmusic-1.5.2-forge+mc1.20.1.jar`、`curios-forge-5.14.1+1.20.1.jar` |
| `1.20.1-fabric` | `netmusic-1.5.2-fabric+mc1.20.1.jar`、`trinkets-3.7.2.jar`、`cardinal-components-base-5.2.0.jar`、`cardinal-components-entity-5.2.0.jar`、`ForgeConfigAPIPort-v8.0.3-1.20.1-Fabric.jar`（`runClient` 需要） |
| `1.21.1-neoforge` | `netmusic-1.5.2-neoforge+mc1.21.1.jar`、`curios-neoforge-9.5.1+1.21.1.jar`、`Jade-1.21.1-NeoForge-15.10.6.jar` |
| `1.21.1-fabric` | `netmusic-1.5.2-fabric+mc1.21.1.jar`、`trinkets-3.10.0.jar`、`cardinal-components-base-6.1.0.jar`、`cardinal-components-entity-6.1.0.jar`、`Jade-1.21.1-Fabric-15.10.6.jar` |
| `26.1.2-neoforge` | `netmusic-1.5.2-neoforge+mc26.1.jar`（Curios 走 Maven，不用本地 jar） |
| `26.1.2-fabric` | `netmusic-1.5.2-fabric+mc26.1.jar`、`trinkets-4.0.1+26.1.jar`、`yumi-mc-foundation-1.0.1+26.1.jar` |

来源：NetMusic（GitHub Releases / CurseForge / Modrinth）、Curios（CurseForge）、Trinkets（Modrinth）；Cardinal Components、Yumi 可从对应 mod 的嵌套 jar 或 Maven 取。

## 配置

- 扩音倍率与随身听外放半径：Forge / NeoForge 为 `config/netmusicplus-common.toml`，Fabric 为 `config/netmusicplus.json`
  - `amplifier.level_1_multiplier` / `level_2` / `level_3`，默认 `2.0 / 3.0 / 4.0`
  - `mp3.out_loud_radius`，默认 `64.0`
- HUD：`config/netmusicplus-hud.json`（显示曲目 / 歌词、偏移、主题）

## 实现要点

- **可听半径**：原版 `SoundEngine` 用 `max(volume, 1) * attenuationDistance`（`attenuationDistance` 默认 16）算距离，而 NetMusic 的 `NetMusicSound` 把 `volume` 写死成 `4.0` ⇒ 64 格。扩音附魔的作用点就是 `SoundInstance#getVolume()` 的返回值乘倍率——音量本身在 `>1` 时会被 clamp 到 1，所以只放大范围，不变响。
- **播放包广播**：NetMusic 的 `NetworkHandler#sendToNearby` 里广播半径写死 96 格，必须同步按倍率放大，否则超出 96 格的玩家根本收不到播放包。
- **等级持久化**：等级写进方块实体自定义数据（Forge/NeoForge 用 `persistentData`，Fabric 用 mixin 加字段写进原有 NBT），随方块实体更新包同步到客户端，掉落后写回物品。
- 三个 Fabric 版的 NetMusic 创造页签 id 是 `netmusic:netmusic_group`，Forge / NeoForge 版是 `netmusic:netmusic`。

## 许可证

MIT，见 [LICENSE](LICENSE)。

NetMusic 由 Tartaric Acid 开发，使用 MIT 许可证。本仓库不包含 NetMusic 的源码或 jar，构建所需见上文「需要自备的编译依赖」。
