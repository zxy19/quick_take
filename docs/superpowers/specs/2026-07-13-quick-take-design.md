# Quick Take Mod — 设计规范

## 概述

一个仅客户端 NeoForge 模组，提供类 Spotlight 的快速物品选择界面。在创造模式下通过快捷键或输入字符打开，支持拼音匹配（需 JEC 模组）。

- Minecraft: 1.26.1.2
- NeoForge: 26.1.2.78
- Java: 25
- 客户端模组: `@Mod(dist = Dist.CLIENT)`

## 触发方式

| 触发方式 | 事件 | 说明 |
|---------|------|------|
| 按 R 键 | `ClientTickEvent.Post` + `KeyMapping.consumeClick()` | 仅创造模式 |
| 创造物品栏输入字符 | `ScreenEvent.CharacterTyped.Post` | 搜索框已处理则 Post 不触发 |

## 文件结构

```
src/main/java/studio/fantasyit/quick_take/
├── QuickTake.java                    # 主入口，@Mod(dist = Dist.CLIENT)
├── integration/
│   ├── Integration.java             # 判断 mod 是否安装
│   └── JechIntegration.java         # JEC 拼音匹配调用
├── key/
│   └── QuickTakeKeyMapping.java     # 键盘绑定（默认 R）
├── screen/
│   └── QuickTakeScreen.java         # Spotlight 界面
├── helper/
│   ├── ItemMatcher.java             # 物品匹配算法
│   └── InventoryHandler.java        # 物品操作
└── event/
    └── ClientEventHandler.java      # 事件监听
```

## 各组件设计

### QuickTake.java

- `@Mod(value = QuickTake.MODID, dist = Dist.CLIENT)` 仅客户端
- 构造函数中注册 `RegisterKeyMappingsEvent` 和 `ClientEventHandler`
- 清理所有模板代码（示例方块、物品、标签页）

### QuickTakeKeyMapping.java

- `KeyMapping` 默认 R 键（`GLFW.GLFW_KEY_R`），分类 `CREATIVE`
- 在 `RegisterKeyMappingsEvent` 中注册

### QuickTakeScreen.java

**布局**：
- 全屏半透明黑色背景（alpha ≈ 128）
- 搜索栏（EditBox）：宽度 `screen.width * 0.4`，居中，高 20px
- 列表区域：同宽度，位于搜索栏下方 4px，最多可见 9 行，每行 18px
- 每行：16x16 物品图标 + 物品名称 + 焦点高亮条
- 右侧滚动条（与创造物品栏风格一致）

**交互**：
- 任意字符 → 追加到搜索栏，实时过滤
- 退格 → 删除搜索栏末尾字符
- 上箭头 → 焦点上移（循环，顶部到底部）
- 下箭头 → 焦点下移（循环，底部到顶部）
- Enter → 选择当前焦点物品 → 执行 InventoryHandler → 关闭界面
- Esc → 关闭界面

**数据流**：
- `init()` 时收集所有 `CreativeModeTab.allTabs()` 的 `getDisplayItems()` → `allItems`
- 搜索栏变化 → `ItemMatcher.matches()` 过滤 → `filteredItems`
- 焦点索引 `selectedIndex`，滚动偏移 `scrollOffset`
- 打开时搜索栏自动聚焦，显示全部物品

### ItemMatcher.java

**方法签名**：`public static boolean matches(ItemStack stack, String pattern)`

**算法**：
1. pattern 为空/空白 → 返回 true
2. 空格分割 pattern → subPatterns[]
3. 获取匹配文本：显示名称(小写)、每行 tooltip 文本(小写)、注册 ID(小写)
4. 对每个 subPattern：
   - 在匹配文本列表中查找子序列匹配 → 成功 → 继续
   - 委托 `JechIntegration.tryMatch(stack, subPattern)` → 成功 → 继续
   - 失败 → 返回 false
5. 全部成功 → 返回 true

### JechIntegration.java

**方法签名**：`public static boolean tryMatch(ItemStack stack, String pattern)`

1. 检查 `Integration.isJechLoaded()` → 否 → 返回 false
2. `Match.contains(物品显示名称, pattern)` → true → 返回 true
3. 遍历 tooltip 行 → `Match.contains(tooltip行, pattern)` → true → 返回 true
4. 返回 false

### Integration.java

**方法**：`public static boolean isJechLoaded()` → `ModList.get().isLoaded("jecharacters")`

### InventoryHandler.java

**方法签名**：`public static void selectItem(Minecraft mc, ItemStack target)`

**同步方式**：封装 `setItemAndSync(mc, inventoryIndex, stack)` 方法，同时调用 `getInventory().setItem()` 和 `gameMode.handleCreativeModeItemAdd()`。Container menu 槽位映射：
- 快捷栏 inventory index 0-8 → container slot 36-44
- 背包 inventory index 9-35 → container slot 9-35

**5 步流程**：

```
1. 遍历快捷栏(0-8) → 找到相同物品 → mc.player.getInventory().selected = slot → 返回

2. 遍历背包(0-35) → 找到 → 跳到步骤 4

3. 未找到 → 找空背包槽位(9-35)
   a. 有 → setItemAndSync(emptySlot, target)
   b. 无 → 扔掉手持物品，setItemAndSync(currentHotbar, target) → 返回

4. 找空快捷栏槽位(0-8)
   a. 有 → setItemAndSync(hotbarSlot, target)，selected = hotbarSlot → 返回
   b. 无 → 交换：currentHotbar 物品 ↔ target 所在背包槽位
           setItemAndSync(currentHotbar, target)
           setItemAndSync(backpackSlot, oldItem)
```

### ClientEventHandler.java

```java
@EventBusSubscriber(modid = ModId, value = Dist.CLIENT)
```

- `ClientTickEvent.Post`：检测 `KeyMapping.consumeClick()` → 仅创造模式 → 打开 QuickTakeScreen
- `ScreenEvent.CharacterTyped.Post`：当前 Screen 是 CreativeModeInventoryScreen → 打开 QuickTakeScreen（传入字符作为初始搜索文本）

### 语言文件

```json
{
  "key.categories.quick_take": "Quick Take",
  "key.quick_take.open": "Open Quick Take"
}
```

## 清理内容

删除以下模板代码：
- `EXAMPLE_BLOCK`、`EXAMPLE_BLOCK_ITEM`、`EXAMPLE_ITEM`、`EXAMPLE_TAB`
- `BLOCKS`、`ITEMS`、`CREATIVE_MODE_TABS` DeferredRegister
- `commonSetup`、`addCreative`、`onServerStarting` 方法
- `Config.java` 中示例配置项