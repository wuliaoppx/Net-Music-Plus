# Net Music Plus — 模组平台描述 / Mod Listing Description

> 用于 CurseForge、Modrinth 的 Description 字段。中文在前，English 在后，可直接整段粘贴。

---

## 中文

### 简介

Net Music Plus 是 [NetMusic](https://github.com/TartaricAcid/NetMusic)（网络音乐机）的附属模组，为其提供扩音附魔与便携播放设备。

网络音乐机默认把音乐播放给 64 格内的玩家。装上本模组后，可以使用扩音附魔把这段距离提升到 128、192 或 256 格；也可以合成一台 MP3 随身听，把唱片揣在身上随身播放，并在物品栏上方查看当前曲目与歌词。

### 内容

**扩音附魔（I / II / III）**

- 可附魔于网络音乐机与 MP3 随身听。
- 三个等级分别把声音传播距离放大到 2 倍、3 倍、4 倍，即约 128、192、256 格。
- 在铁砧上用附魔书打到方块物品上，同名附魔叠加时等级 +1，最高 III 级。
- 方块被破坏时等级随之写回掉落物，重新放置后继续生效。
- 安装 Jade 时，看向方块会显示当前等级与对应格数。

**MP3 随身听**

- 默认 20 个唱片位，每个唱片扩容件增加 10 格，最多 3 个，上限 50 格。
- 播放模式支持循环、单曲、随机；播放方式支持外放与仅自己，外放半径受扩音附魔影响。
- 潜行加右键或按快捷键打开唱片仓界面，界面一行 10 格。
- 快捷键支持长按暂停/继续、单击下一首、双击上一首，另有一个直接打开界面的快捷键，两项默认均未绑定。
- 物品栏上方显示曲目与歌词，位置可拖动，提供暗夜、木质、霓虹三种主题。
- 可放入 Curios 或 Trinkets 的随身听槽位（位于胸甲栏位置），放在背包中同样生效。

### 合成

全部为无序合成。

- 扩音附魔书 I：音符盒 + 铜锭
- 扩音附魔书 II：音符盒 + 铜锭 + 紫水晶碎片
- 扩音附魔书 III：音符盒 + 铜锭 + 紫水晶碎片 + 回响碎片
- MP3 随身听：网络音乐机 + 锁链
- 唱片扩容件：箱子 + 铁锭

### 支持版本

| Minecraft | Forge | Fabric | NeoForge |
|---|---|---|---|
| 1.20.1 | 1.0.2 | 1.0.2 | — |
| 1.21.1 | — | 1.3.2 | 1.2.9 |
| 26.1.2 | — | 1.0.3 | 1.0.3 |

### 前置与联动

- **必需**：NetMusic 1.5.2 及以上。
- **可选**：Jade 15.10.6 及以上（显示扩音等级）。
- **可选**：Curios（Forge 5.14.1+ / NeoForge 9.5.1+）或 Trinkets（3.7.2+ / 3.10.0+ / 4.0.1+），提供随身听饰品槽。

### 配置

- Forge / NeoForge：`config/netmusicplus-common.toml`
- Fabric：`config/netmusicplus.json`
- HUD 设置：`config/netmusicplus-hud.json`

可调整三个扩音等级的倍率（默认 2.0 / 3.0 / 4.0）与随身听外放的基础半径（默认 64.0）。

### 其他

- 只放大声音传播距离，不改变实际音量。
- 与 NetMusic 的唱片、音乐机合成链兼容，不修改其原有配方与物品。
- 使用 MIT 许可证。NetMusic 由 Tartaric Acid 开发，同为 MIT 许可证。
- 本模组的代码、数据文件与文档由 AI 编程助手生成，经审核并在游戏内测试后发布。

---

## English

### About

Net Music Plus is an add-on for [NetMusic](https://github.com/TartaricAcid/NetMusic). It adds an amplifier enchantment for the music player and a portable MP3 player.

A NetMusic music player reaches players within 64 blocks. With this mod you can raise that distance to 128, 192 or 256 blocks, or craft an MP3 player that carries up to 50 discs and shows the current track and lyrics above your hotbar.

### Features

**Amplifier (I / II / III)**

- Applies to the NetMusic music player and the MP3 player.
- Levels scale the audible distance to ×2, ×3 and ×4 — about 128, 192 and 256 blocks.
- Apply the enchanted book to the block item on an anvil; the same enchantment adds one level, capped at III.
- The level is written back to the dropped item when the block is broken and restored when placed again.
- With Jade installed, looking at the block shows the current level and range.

**MP3 player**

- 20 disc slots by default; each disc expander adds 10 slots, up to 3 expanders (50 slots).
- Play modes: loop, single, shuffle. Output: out loud (radius affected by Amplifier) or personal.
- Open the disc bay by sneaking and right-clicking, or with a key bind. 10 slots per row.
- Key binds for hold-to-pause, single-press next and double-press previous, plus one that opens the GUI; both are unbound by default.
- Shows the track and lyrics above the hotbar; draggable position; Night, Wood and Neon themes.
- Works from the Curios / Trinkets trinket slot (positioned over the chestplate slot) or straight from the inventory.

### Recipes

All shapeless.

- Amplifier Book I: Note Block + Copper Ingot
- Amplifier Book II: Note Block + Copper Ingot + Amethyst Shard
- Amplifier Book III: Note Block + Copper Ingot + Amethyst Shard + Echo Shard
- MP3 Player: NetMusic music player + Chain
- Disc Expander: Chest + Iron Ingot

### Versions

| Minecraft | Forge | Fabric | NeoForge |
|---|---|---|---|
| 1.20.1 | 1.0.2 | 1.0.2 | — |
| 1.21.1 | — | 1.3.2 | 1.2.9 |
| 26.1.2 | — | 1.0.3 | 1.0.3 |

### Requirements

- **Required**: NetMusic 1.5.2 or newer.
- **Optional**: Jade 15.10.6 or newer, to display the amplifier level.
- **Optional**: Curios (Forge 5.14.1+ / NeoForge 9.5.1+) or Trinkets (3.7.2+ / 3.10.0+ / 4.0.1+) for the trinket slot.

### Configuration

`config/netmusicplus-common.toml` on Forge / NeoForge, `config/netmusicplus.json` on Fabric, `config/netmusicplus-hud.json` for the HUD. The three level multipliers (2.0 / 3.0 / 4.0) and the base out-loud radius (64.0) are configurable.

### Notes

- Only the audible distance is changed; loudness stays untouched.
- The mod does not modify NetMusic's items or recipes.
- MIT licensed. NetMusic is developed by Tartaric Acid and is also MIT licensed.
- The code, data files and documentation were produced with an AI coding assistant, then reviewed and tested in-game.
