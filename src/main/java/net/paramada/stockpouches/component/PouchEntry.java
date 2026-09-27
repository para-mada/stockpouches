package net.paramada.stockpouches.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

/** A component-aware item variant and its amount inside a pouch. */
public record PouchEntry(ItemStackTemplate template, long amount) {

    private static final Codec<Long> POSITIVE_LONG = Codec.LONG.validate(value -> value > 0
            ? DataResult.success(value)
            : DataResult.error(() -> "Pouch entry amount must be positive"));

    public static final Codec<PouchEntry> CODEC = RecordCodecBuilder.<PouchEntry>create(instance -> instance.group(
            ItemStackTemplate.CODEC.fieldOf("item").forGetter(PouchEntry::template),
            POSITIVE_LONG.fieldOf("amount").forGetter(PouchEntry::amount)
    ).apply(instance, PouchEntry::new)).validate(PouchEntry::validate);

    public static final StreamCodec<RegistryFriendlyByteBuf, PouchEntry> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC, PouchEntry::template,
            ByteBufCodecs.VAR_LONG, PouchEntry::amount,
            PouchEntry::new
    );

    public PouchEntry {
        if (template.count() != 1) {
            template = template.withCount(1);
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Pouch entry amount must be positive");
        }
    }

    public static PouchEntry fromStack(ItemStack stack, long amount) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Cannot create a pouch entry from an empty stack");
        }
        return new PouchEntry(ItemStackTemplate.fromNonEmptyStack(stack, 1), amount);
    }

    public static PouchEntry fromTemplate(ItemStackTemplate template, long amount) {
        return new PouchEntry(template.withCount(1), amount);
    }

    public boolean matches(ItemStackTemplate candidate) {
        return template.equals(candidate.withCount(1));
    }

    public ItemStack createStack(int count) {
        return template.create().copyWithCount(count);
    }

    private static DataResult<PouchEntry> validate(PouchEntry entry) {
        if (entry.template.count() != 1) {
            return DataResult.error(() -> "Pouch entry item count must be one");
        }
        if (entry.amount <= 0) {
            return DataResult.error(() -> "Pouch entry amount must be positive");
        }
        return DataResult.success(entry);
    }
}
