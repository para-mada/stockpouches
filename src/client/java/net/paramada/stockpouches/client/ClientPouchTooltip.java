package net.paramada.stockpouches.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.paramada.stockpouches.component.PouchContents;
import net.paramada.stockpouches.component.PouchEntry;
import net.paramada.stockpouches.component.PouchTooltip;

import java.util.List;
import java.util.Locale;

public final class ClientPouchTooltip implements ClientTooltipComponent {

    private static final int COLUMNS = 3;
    private static final int PAGE_SIZE = 9;
    private static final int SLOT_SIZE = 24;
    private static final int PADDING = 4;
    private static final int ITEM_BACKGROUND = 0xB5242128;
    private static final int SELECTED_BACKGROUND = 0xAA353138;

    private final PouchContents contents;

    public ClientPouchTooltip(PouchTooltip tooltip) {
        this.contents = tooltip.contents();
    }

    @Override
    public int getHeight(Font font) {
        return visibleRows() * SLOT_SIZE + PADDING * 2;
    }

    @Override
    public int getWidth(Font font) {
        return visibleColumns() * SLOT_SIZE + PADDING * 2;
    }

    @Override
    public boolean showTooltipWithItemInHand() {
        return true;
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        List<PouchContents.IndexedEntry> sorted = contents.entriesByAmount();
        int selectedRank = selectedRank(sorted);
        int pageStart = selectedRank < 0 ? 0 : (selectedRank / PAGE_SIZE) * PAGE_SIZE;
        int visibleCount = Math.min(PAGE_SIZE, sorted.size() - pageStart);
        int gridX = x + PADDING;

        for (int cell = 0; cell < visibleCount; cell++) {
            int slotX = gridX + (cell % COLUMNS) * SLOT_SIZE;
            int slotY = y + PADDING + (cell / COLUMNS) * SLOT_SIZE;

            int rank = pageStart + cell;
            PouchContents.IndexedEntry indexed = sorted.get(rank);
            PouchEntry entry = indexed.entry();
            int background = indexed.index() == contents.selectedIndex()
                    ? SELECTED_BACKGROUND
                    : ITEM_BACKGROUND;
            drawRoundedSlot(graphics, slotX, slotY, background);

            graphics.item(entry.createStack(1), slotX + 4, slotY + 3);
            String count = abbreviated(entry.amount());
            graphics.text(font, count, slotX + SLOT_SIZE - font.width(count), slotY + 14, 0xFFFFFFFF, true);
        }
    }

    /** Draws a slot with each one-pixel corner omitted for a pixel-art rounded edge. */
    private static void drawRoundedSlot(GuiGraphicsExtractor graphics, int x, int y, int color) {
        int left = x + 1;
        int top = y + 1;
        int right = x + SLOT_SIZE - 1;
        int bottom = y + SLOT_SIZE - 1;
        graphics.fill(left + 1, top, right - 1, top + 1, color);
        graphics.fill(left, top + 1, right, bottom - 1, color);
        graphics.fill(left + 1, bottom - 1, right - 1, bottom, color);
    }

    private int visibleEntryCount() {
        List<PouchContents.IndexedEntry> sorted = contents.entriesByAmount();
        int selectedRank = selectedRank(sorted);
        int pageStart = selectedRank < 0 ? 0 : (selectedRank / PAGE_SIZE) * PAGE_SIZE;
        return Math.max(1, Math.min(PAGE_SIZE, sorted.size() - pageStart));
    }

    private int visibleColumns() {
        return Math.min(COLUMNS, visibleEntryCount());
    }

    private int visibleRows() {
        return (visibleEntryCount() + COLUMNS - 1) / COLUMNS;
    }

    private int selectedRank(List<PouchContents.IndexedEntry> sorted) {
        for (int rank = 0; rank < sorted.size(); rank++) {
            if (sorted.get(rank).index() == contents.selectedIndex()) {
                return rank;
            }
        }
        return -1;
    }

    private static String abbreviated(long amount) {
        if (amount < 1_000) {
            return Long.toString(amount);
        }
        if (amount < 1_000_000) {
            return compact(amount / 1_000.0, "k");
        }
        if (amount < 1_000_000_000) {
            return compact(amount / 1_000_000.0, "m");
        }
        if (amount < 1_000_000_000_000L) {
            return compact(amount / 1_000_000_000.0, "b");
        }
        return compact(amount / 1_000_000_000_000.0, "t");
    }

    private static String compact(double value, String suffix) {
        return String.format(Locale.ROOT, value >= 100 ? "%.0f%s" : "%.1f%s", value, suffix);
    }
}
