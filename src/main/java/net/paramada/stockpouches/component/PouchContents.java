package net.paramada.stockpouches.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

/** Immutable pouch state. The end of the list is the next variant to extract. */
public record PouchContents(List<PouchEntry> entries, int selectedIndex) {

    public static final int MAX_DISTINCT_ENTRIES = 1024;
    public static final PouchContents EMPTY = new PouchContents(List.of(), -1);

    private static final Codec<List<PouchEntry>> ENTRIES_CODEC = PouchEntry.CODEC
            .listOf(0, MAX_DISTINCT_ENTRIES);

    public static final Codec<PouchContents> CODEC = RecordCodecBuilder.<PouchContents>create(instance -> instance.group(
            ENTRIES_CODEC.optionalFieldOf("entries", List.of()).forGetter(PouchContents::entries),
            Codec.INT.optionalFieldOf("selected", -1).forGetter(PouchContents::selectedIndex)
    ).apply(instance, PouchContents::new)).validate(PouchContents::validate);

    public static final StreamCodec<RegistryFriendlyByteBuf, PouchContents> STREAM_CODEC = StreamCodec.composite(
            PouchEntry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_DISTINCT_ENTRIES)), PouchContents::entries,
            ByteBufCodecs.VAR_INT, PouchContents::selectedIndex,
            PouchContents::new
    );

    public PouchContents(List<PouchEntry> entries) {
        this(entries, -1);
    }

    public PouchContents {
        entries = List.copyOf(entries);
        if (entries.size() > MAX_DISTINCT_ENTRIES) {
            throw new IllegalArgumentException("Too many distinct pouch entries");
        }
        if (selectedIndex < -1 || selectedIndex >= entries.size()) {
            selectedIndex = -1;
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int distinctEntries() {
        return entries.size();
    }

    public long totalItems() {
        long total = 0;
        for (PouchEntry entry : entries) {
            if (Long.MAX_VALUE - total < entry.amount()) {
                return Long.MAX_VALUE;
            }
            total += entry.amount();
        }
        return total;
    }

    /**
     * Inserts as much of {@code source} as can be represented. Re-inserting an
     * existing variant moves it to the end, which makes extraction LIFO.
     */
    public InsertResult insert(ItemStack source) {
        if (source.isEmpty()) {
            return new InsertResult(this, 0);
        }
        return insert(ItemStackTemplate.fromNonEmptyStack(source));
    }

    /** Pure variant used by the runtime adapter and unit tests alike. */
    public InsertResult insert(ItemStackTemplate source) {
        if (source.count() <= 0) {
            return new InsertResult(this, 0);
        }

        int existingIndex = findMatchingEntry(source);
        if (existingIndex < 0 && entries.size() >= MAX_DISTINCT_ENTRIES) {
            return new InsertResult(this, 0);
        }

        long current = existingIndex < 0 ? 0 : entries.get(existingIndex).amount();
        int accepted = (int) Math.min((long) source.count(), Long.MAX_VALUE - current);
        if (accepted <= 0) {
            return new InsertResult(this, 0);
        }

        List<PouchEntry> updated = new ArrayList<>(entries);
        if (existingIndex >= 0) {
            updated.remove(existingIndex);
        }
        updated.add(PouchEntry.fromTemplate(source, current + accepted));
        return new InsertResult(new PouchContents(updated, -1), accepted);
    }

    public ExtractResult extractLastStack() {
        if (entries.isEmpty()) {
            return new ExtractResult(this, ItemStack.EMPTY);
        }

        PouchEntry selected = entries.get(extractionIndex());
        ExtractEntryResult result = extractLast(selected.template().create().getMaxStackSize());
        return new ExtractResult(result.contents(), result.extracted()
                .map(entry -> entry.createStack((int) entry.amount()))
                .orElse(ItemStack.EMPTY));
    }

    /** Extracts at most one legal stack without constructing an ItemStack. */
    public ExtractEntryResult extractLast(int maxStackSize) {
        if (entries.isEmpty() || maxStackSize <= 0) {
            return new ExtractEntryResult(this, Optional.empty());
        }

        int lastIndex = extractionIndex();
        PouchEntry entry = entries.get(lastIndex);
        int extracted = (int) Math.min(entry.amount(), maxStackSize);
        List<PouchEntry> updated = new ArrayList<>(entries);
        long remainder = entry.amount() - extracted;
        if (remainder == 0) {
            updated.remove(lastIndex);
        } else {
            updated.set(lastIndex, new PouchEntry(entry.template(), remainder));
        }
        int nextSelection = remainder == 0 ? -1 : lastIndex;
        return new ExtractEntryResult(
                new PouchContents(updated, nextSelection),
                Optional.of(new PouchEntry(entry.template(), extracted))
        );
    }

    public PouchContents withSelectedIndex(int index) {
        return new PouchContents(entries, index);
    }

    /** Entries ordered by amount for presentation and scroll selection. */
    public List<IndexedEntry> entriesByAmount() {
        List<IndexedEntry> sorted = new ArrayList<>(entries.size());
        for (int index = 0; index < entries.size(); index++) {
            sorted.add(new IndexedEntry(index, entries.get(index)));
        }
        sorted.sort((left, right) -> Long.compare(right.entry().amount(), left.entry().amount()));
        return List.copyOf(sorted);
    }

    public PouchContents cycleSelection(int direction) {
        List<IndexedEntry> sorted = entriesByAmount();
        if (sorted.isEmpty() || direction == 0) {
            return this;
        }
        int currentRank = -1;
        for (int rank = 0; rank < sorted.size(); rank++) {
            if (sorted.get(rank).index() == selectedIndex) {
                currentRank = rank;
                break;
            }
        }
        int nextRank = currentRank < 0
                ? 0
                : Math.floorMod(currentRank + Integer.signum(direction), sorted.size());
        return withSelectedIndex(sorted.get(nextRank).index());
    }

    private int extractionIndex() {
        return selectedIndex >= 0 && selectedIndex < entries.size() ? selectedIndex : entries.size() - 1;
    }

    private int findMatchingEntry(ItemStackTemplate stack) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).matches(stack)) {
                return index;
            }
        }
        return -1;
    }

    private static DataResult<PouchContents> validate(PouchContents contents) {
        Set<ItemStackTemplate> identities = new HashSet<>();
        for (PouchEntry entry : contents.entries) {
            if (!identities.add(entry.template())) {
                return DataResult.error(() -> "Pouch contents contain duplicate item variants");
            }
        }
        if (contents.selectedIndex < -1 || contents.selectedIndex >= contents.entries.size()) {
            return DataResult.error(() -> "Selected pouch entry is outside the contents");
        }
        return DataResult.success(contents);
    }

    public record InsertResult(PouchContents contents, int inserted) {}

    public record IndexedEntry(int index, PouchEntry entry) {}

    public record ExtractEntryResult(PouchContents contents, Optional<PouchEntry> extracted) {}

    public record ExtractResult(PouchContents contents, ItemStack extracted) {}
}
