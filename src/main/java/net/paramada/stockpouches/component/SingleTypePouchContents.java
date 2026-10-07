package net.paramada.stockpouches.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable contents for a pouch that stores one exact item variant.
 *
 * <p>The component deliberately does not own a capacity or an acceptance
 * predicate. Those are properties of the pouch item. This lets malformed or
 * future data round-trip without silently truncating it while every mutation
 * still validates the owning pouch's contract.</p>
 */
public final class SingleTypePouchContents {

    public static final int CURRENT_VERSION = 1;
    public static final SingleTypePouchContents EMPTY =
            new SingleTypePouchContents(CURRENT_VERSION, Optional.empty(), 0);

    public static final Codec<SingleTypePouchContents> CODEC = RecordCodecBuilder
            .<SingleTypePouchContents>create(instance -> instance.group(
                    Codec.INT.fieldOf("version").forGetter(SingleTypePouchContents::version),
                    ItemStackTemplate.CODEC.optionalFieldOf("item").forGetter(SingleTypePouchContents::prototype),
                    Codec.INT.optionalFieldOf("amount", 0).forGetter(SingleTypePouchContents::amount)
            ).apply(instance, SingleTypePouchContents::new))
            .validate(SingleTypePouchContents::validateStructure);

    /** Codec-backed to apply the same structural validation to packets and saves. */
    public static final StreamCodec<RegistryFriendlyByteBuf, SingleTypePouchContents> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private final int version;
    private final Optional<ItemStackTemplate> prototype;
    private final int amount;

    private SingleTypePouchContents(int version, Optional<ItemStackTemplate> prototype, int amount) {
        this.version = version;
        this.prototype = Objects.requireNonNull(prototype);
        this.amount = amount;
    }

    public static SingleTypePouchContents of(ItemStack stack, int amount) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("Cannot create pouch contents from an empty stack");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Pouch amount must be positive");
        }
        return new SingleTypePouchContents(
                CURRENT_VERSION,
                Optional.of(ItemStackTemplate.fromNonEmptyStack(stack, 1)),
                amount
        );
    }

    public int version() {
        return version;
    }

    public Optional<ItemStackTemplate> prototype() {
        return prototype;
    }

    public int amount() {
        return amount;
    }

    public boolean isEmpty() {
        return amount == 0 && prototype.isEmpty();
    }

    public boolean isValidFor(int capacity) {
        return capacity > 0
                && validateStructure(this).isSuccess()
                && amount <= capacity;
    }

    public boolean matches(ItemStack candidate) {
        return !candidate.isEmpty()
                && prototype.map(stored -> stored.equals(ItemStackTemplate.fromNonEmptyStack(candidate, 1)))
                .orElse(false);
    }

    public ItemStack createStack(int count) {
        if (count <= 0 || prototype.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return prototype.orElseThrow().create().copyWithCount(count);
    }

    SingleTypePouchContents insertOne(ItemStack source) {
        if (isEmpty()) {
            return of(source, 1);
        }
        return new SingleTypePouchContents(version, prototype, Math.addExact(amount, 1));
    }

    SingleTypePouchContents extractOne() {
        if (amount <= 0) {
            return this;
        }
        return amount == 1 ? EMPTY : new SingleTypePouchContents(version, prototype, amount - 1);
    }

    private static DataResult<SingleTypePouchContents> validateStructure(SingleTypePouchContents contents) {
        if (contents.version != CURRENT_VERSION) {
            return DataResult.error(() -> "Unsupported single-type pouch contents version: " + contents.version);
        }
        if (contents.amount < 0) {
            return DataResult.error(() -> "Pouch amount cannot be negative");
        }
        if (contents.amount == 0 && contents.prototype.isPresent()) {
            return DataResult.error(() -> "Empty pouch contents cannot carry an item prototype");
        }
        if (contents.amount > 0 && contents.prototype.isEmpty()) {
            return DataResult.error(() -> "Non-empty pouch contents require an item prototype");
        }
        if (contents.prototype.isPresent() && contents.prototype.orElseThrow().count() != 1) {
            return DataResult.error(() -> "Pouch item prototype count must be one");
        }
        return DataResult.success(contents);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof SingleTypePouchContents contents
                && version == contents.version
                && amount == contents.amount
                && prototype.equals(contents.prototype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, prototype, amount);
    }

    @Override
    public String toString() {
        return "SingleTypePouchContents[version=" + version
                + ", prototype=" + prototype + ", amount=" + amount + ']';
    }
}
