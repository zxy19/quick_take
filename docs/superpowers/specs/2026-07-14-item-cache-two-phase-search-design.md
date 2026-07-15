# Item Cache & Two-Phase Search — Design

## Overview

1. Globally cache creative mode item name lists, clear on resource reload, rebuild on screen open.
2. Split search into two phases: name/ID match first (instant UI), then tooltip match (appended later).

## Changes

### 1. New: `helper/ItemCache.java`

Static cache for creative mode item match entries.

```java
public class ItemCache {
    private static List<ItemMatchEntry> cache = null;

    public static List<ItemMatchEntry> getOrBuild(Minecraft mc) {
        if (cache == null) {
            cache = build(mc);
        }
        return cache;
    }

    public static void invalidate() {
        cache = null;
    }

    private static List<ItemMatchEntry> build(Minecraft mc) {
        // Logic moved from QuickTakeScreen.init():
        // - CreativeModeTabs.tryRebuildTabContents()
        // - Iterate all CATEGORY tabs, collect display items
        // - Build ItemMatchEntry with getMatchTexts for each stack
    }
}
```

- Called from `QuickTakeScreen.init()` via `getOrBuild(minecraft)`
- Only rendering thread calls `getOrBuild`, no synchronization needed

### 2. Modify: `ItemMatcher.java`

Add two new methods for phase-specific matching:

```java
// Only match name (texts[0]) and registry ID (texts[size-1])
public static float matchPriorityName(ItemMatchEntry entry, String pattern)

// Only match tooltip lines (texts[1..size-2]), with JechIntegration fallback
public static float matchPriorityTooltip(ItemMatchEntry entry, String pattern)
```

- `matchPriorityName` skips tooltip texts entirely, no Jech fallback
- `matchPriorityTooltip` only checks tooltip texts, with Jech fallback
- Existing `matchPriority` and `matches` remain unchanged

### 3. Modify: `QuickTakeScreen.java`

**init()**: Replace entries building with `entries = ItemCache.getOrBuild(minecraft)`.

**updateFilter()**: Two-phase async search in a single background task:

```
cancelTask() → new task:
  Phase 1: for each entry, matchPriorityName() → score > 0 → add to scored list
           sort by score desc
           minecraft.execute(() -> set filteredItems = scored)
  Phase 2: for each entry NOT already in Phase 1 results, matchPriorityTooltip() → score > 0 → add
           sort by score desc
           minecraft.execute(() -> append to filteredItems)
```

- Both phases run sequentially in the same thread
- Phase 1 results appear immediately, Phase 2 appends to end
- New input cancels the task, `filterVersion` prevents stale UI updates

### 4. New: `event/ClientModBusEventHandler.java`

Reload listener via annotation-based event subscriber:

```java
@EventBusSubscriber(modid = QuickTake.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModBusEventHandler {
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((stage, manager, preparer, exec) -> {
            ItemCache.invalidate();
        });
    }
}
```

## Files affected

| File | Action |
|------|--------|
| `helper/ItemCache.java` | **NEW** |
| `event/ClientModBusEventHandler.java` | **NEW** |
| `helper/ItemMatcher.java` | Add `matchPriorityName`, `matchPriorityTooltip` |
| `screen/QuickTakeScreen.java` | Use `ItemCache`, two-phase filter |
| `QuickTake.java` | No changes needed |