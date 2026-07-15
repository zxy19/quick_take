package studio.fantasyit.quick_take.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.quick_take.helper.InventoryHandler;
import studio.fantasyit.quick_take.helper.ItemCache;
import studio.fantasyit.quick_take.helper.ItemMatcher;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class QuickTakeScreen extends Screen {
    private static final int LIST_WIDTH = 200;
    private static final int ROW_HEIGHT = 18;
    private static final int MAX_VISIBLE_ROWS = 9;
    private static final int SEARCH_HEIGHT = 20;
    private static final int GAP = 4;

    private EditBox searchBox;
    private final List<ItemMatcher.ItemMatchEntry> entries = new ArrayList<>();
    private final List<ItemStack> filteredItems = new ArrayList<>();
    private int selectedIndex;
    private int scrollOffset;
    private final String initialText;
    private int filterVersion;
    private ExecutorService executor;
    private Future<?> currentTask;

    public QuickTakeScreen() {
        this("");
    }

    public QuickTakeScreen(String initialText) {
        super(Component.empty());
        this.initialText = initialText;
    }

    @Override
    protected void init() {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "QuickTake-Filter");
                t.setDaemon(true);
                return t;
            });
        }
        int listX = (this.width - LIST_WIDTH) / 2;
        int listY = (this.height - (SEARCH_HEIGHT + GAP + MAX_VISIBLE_ROWS * ROW_HEIGHT)) / 2;

        this.searchBox = new EditBox(this.font, listX, listY, LIST_WIDTH, SEARCH_HEIGHT, Component.empty());
        this.searchBox.setFocused(true);
        this.setFocused(this.searchBox);
        this.addRenderableWidget(this.searchBox);

        if (!initialText.isEmpty()) {
            this.searchBox.setValue(initialText);
        }

        if (entries.isEmpty()) {
            entries.addAll(ItemCache.getOrBuild(minecraft));
        }

        filteredItems.clear();
        for (var entry : entries) {
            filteredItems.add(entry.stack());
        }
        selectedIndex = 0;
        scrollOffset = 0;
    }

    @Override
    public void onClose() {
        cancelTask();
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        super.extractRenderState(graphics, mouseX, mouseY, a);

        int listX = (this.width - LIST_WIDTH) / 2;
        int listTop = searchBox.getY() + SEARCH_HEIGHT + GAP;
        int listBottom = listTop + MAX_VISIBLE_ROWS * ROW_HEIGHT;

        graphics.enableScissor(listX, listTop, listX + LIST_WIDTH, listBottom);

        for (int i = 0; i < filteredItems.size(); i++) {
            int rowY = listTop + i * ROW_HEIGHT - scrollOffset;
            if (rowY + ROW_HEIGHT < listTop || rowY > listBottom) {
                continue;
            }
            ItemStack stack = filteredItems.get(i);
            int rowX = listX + 2;
            if (i == selectedIndex) {
                graphics.fill(listX, rowY, listX + LIST_WIDTH, rowY + ROW_HEIGHT, 0x80FFFFFF);
            }
            graphics.item(stack, rowX, rowY + 1);
            graphics.itemDecorations(this.font, stack, rowX, rowY + 1);
            graphics.textRenderer().accept(rowX + 18, rowY + 5, stack.getHoverName());
        }

        graphics.disableScissor();

        if (filteredItems.size() > MAX_VISIBLE_ROWS) {
            int scrollbarX = listX + LIST_WIDTH - 6;
            int trackHeight = MAX_VISIBLE_ROWS * ROW_HEIGHT;
            float ratio = (float) MAX_VISIBLE_ROWS / filteredItems.size();
            int thumbHeight = Math.max(8, (int) (trackHeight * ratio));
            float maxScroll = filteredItems.size() * ROW_HEIGHT - MAX_VISIBLE_ROWS * ROW_HEIGHT;
            float scrollRatio = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;
            int thumbY = listTop + (int) (scrollRatio * (trackHeight - thumbHeight));
            graphics.fill(scrollbarX, listTop, scrollbarX + 4, listBottom, 0x40FFFFFF);
            graphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0xC0FFFFFF);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchBox.isFocused()) {
            if (this.searchBox.keyPressed(event)) {
                updateFilter();
                return true;
            }
        }
        InputConstants.Key key = InputConstants.getKey(event);
        if (key.getValue() == InputConstants.KEY_UP) {
            navigateUp();
            return true;
        }
        if (key.getValue() == InputConstants.KEY_DOWN) {
            navigateDown();
            return true;
        }
        if (key.getValue() == InputConstants.KEY_RETURN || key.getValue() == InputConstants.KEY_NUMPADENTER) {
            selectCurrent();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchBox.isFocused()) {
            if (this.searchBox.charTyped(event)) {
                updateFilter();
                return true;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int listX = (this.width - LIST_WIDTH) / 2;
            int listTop = searchBox.getY() + SEARCH_HEIGHT + GAP;
            double mouseX = event.x();
            double mouseY = event.y();
            if (mouseX >= listX && mouseX <= listX + LIST_WIDTH && mouseY >= listTop && mouseY <= listTop + MAX_VISIBLE_ROWS * ROW_HEIGHT) {
                int clickedIndex = (int) (mouseY - listTop + scrollOffset) / ROW_HEIGHT;
                if (clickedIndex >= 0 && clickedIndex < filteredItems.size()) {
                    selectedIndex = clickedIndex;
                    selectCurrent();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = filteredItems.size() * ROW_HEIGHT - MAX_VISIBLE_ROWS * ROW_HEIGHT;
        if (maxScroll > 0) {
            scrollOffset = Mth.clamp(scrollOffset - (int) scrollY * ROW_HEIGHT, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void navigateUp() {
        if (filteredItems.isEmpty()) return;
        if (selectedIndex > 0) {
            selectedIndex--;
        } else {
            selectedIndex = filteredItems.size() - 1;
        }
        ensureVisible();
    }

    private void navigateDown() {
        if (filteredItems.isEmpty()) return;
        if (selectedIndex < filteredItems.size() - 1) {
            selectedIndex++;
        } else {
            selectedIndex = 0;
        }
        ensureVisible();
    }

    private void selectCurrent() {
        if (!filteredItems.isEmpty()) {
            InventoryHandler.selectItem(this.minecraft, filteredItems.get(selectedIndex));
        }
        this.onClose();
    }

    private void ensureVisible() {
        int maxScroll = filteredItems.size() * ROW_HEIGHT - MAX_VISIBLE_ROWS * ROW_HEIGHT;
        int itemTop = selectedIndex * ROW_HEIGHT;
        int itemBottom = itemTop + ROW_HEIGHT;
        if (itemTop < scrollOffset) {
            scrollOffset = itemTop;
        } else if (itemBottom > scrollOffset + MAX_VISIBLE_ROWS * ROW_HEIGHT) {
            scrollOffset = itemBottom - MAX_VISIBLE_ROWS * ROW_HEIGHT;
        }
        scrollOffset = Math.clamp(scrollOffset, 0, Math.max(0, maxScroll));
    }

    private void updateFilter() {
        String text = this.searchBox.getValue();
        if (text.isBlank()) {
            cancelTask();
            filterVersion++;
            filteredItems.clear();
            for (var entry : entries) {
                filteredItems.add(entry.stack());
            }
            selectedIndex = 0;
            scrollOffset = 0;
            return;
        }
        cancelTask();
        final int version = ++filterVersion;
        final List<ItemMatcher.ItemMatchEntry> entryList = List.copyOf(entries);
        currentTask = executor.submit(() -> {
            List<AbstractMap.SimpleEntry<ItemStack, Float>> phase1 = new ArrayList<>();
            for (var entry : entryList) {
                if (Thread.interrupted()) return;
                float score = ItemMatcher.matchPriorityName(entry, text);
                if (score > 0) {
                    phase1.add(new AbstractMap.SimpleEntry<>(entry.stack(), score));
                }
            }
            phase1.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
            final List<AbstractMap.SimpleEntry<ItemStack, Float>> phase1Result = List.copyOf(phase1);
            minecraft.execute(() -> {
                if (version != filterVersion) return;
                filteredItems.clear();
                for (var e : phase1Result) {
                    filteredItems.add(e.getKey());
                }
                selectedIndex = 0;
                scrollOffset = 0;
            });

            if (Thread.interrupted()) return;
            List<AbstractMap.SimpleEntry<ItemStack, Float>> phase2 = new ArrayList<>();
            for (var entry : entryList) {
                if (Thread.interrupted()) return;
                if (phase1.stream().anyMatch(e -> e.getKey() == entry.stack())) {
                    continue;
                }
                float score = ItemMatcher.matchPriorityId(entry, text);
                if (score > 0) {
                    phase2.add(new AbstractMap.SimpleEntry<>(entry.stack(), score));
                }
            }
            phase2.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
            final List<AbstractMap.SimpleEntry<ItemStack, Float>> phase2Result = List.copyOf(phase2);
            minecraft.execute(() -> {
                if (version != filterVersion) return;
                for (var e : phase2Result) {
                    filteredItems.add(e.getKey());
                }
            });

            if (Thread.interrupted()) return;
            List<AbstractMap.SimpleEntry<ItemStack, Float>> phase3 = new ArrayList<>();
            for (var entry : entryList) {
                if (Thread.interrupted()) return;
                if (phase1.stream().anyMatch(e -> e.getKey() == entry.stack())
                        || phase2.stream().anyMatch(e -> e.getKey() == entry.stack())) {
                    continue;
                }
                float score = ItemMatcher.matchPriorityTooltip(entry, text);
                if (score > 0) {
                    phase3.add(new AbstractMap.SimpleEntry<>(entry.stack(), score));
                }
            }
            phase3.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
            final List<AbstractMap.SimpleEntry<ItemStack, Float>> phase3Result = List.copyOf(phase3);
            minecraft.execute(() -> {
                if (version != filterVersion) return;
                for (var e : phase3Result) {
                    filteredItems.add(e.getKey());
                }
            });
        });
    }

    private void cancelTask() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
    }
}