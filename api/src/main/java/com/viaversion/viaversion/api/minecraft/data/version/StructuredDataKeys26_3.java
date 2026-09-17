/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2026 ViaVersion and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.viaversion.viaversion.api.minecraft.data.version;

import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.data.SwingAnimation;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import io.netty.buffer.ByteBuf;

/**
 * Data component keys added in 26.3. Components removed in 26.3 ({@code swing_animation},
 * {@code map_color}) are static constants on {@link StructuredDataKey} and therefore not part
 * of {@link #keys()}.
 */
public class StructuredDataKeys26_3 extends StructuredDataKeys26_2 {

    public final StructuredDataKey<SwingAnimation> attackAnimation;
    public final StructuredDataKey<SwingAnimation> interactAnimation;
    public final StructuredDataKey<Integer> blockTransformer;
    public final StructuredDataKey<Integer> villagerFood;
    public final StructuredDataKey<Object> compostable;
    public final StructuredDataKey<Object[]> cookingFuel;
    public final StructuredDataKey<Object[]> brewingFuel;
    public final StructuredDataKey<Object[]> mobVisibility;
    public final StructuredDataKey<Integer> providesPotteryPattern;
    public final StructuredDataKey<Object[]> signTextFront;
    public final StructuredDataKey<Object[]> signTextBack;
    public final StructuredDataKey<Object> waxed;
    public final StructuredDataKey<Integer> cushionColor;

    public StructuredDataKeys26_3(final VersionedTypesHolder types) {
        super(types);
        this.attackAnimation = add("attack_animation", SwingAnimation.TYPE);
        this.interactAnimation = add("interact_animation", SwingAnimation.TYPE);
        this.blockTransformer = add("block_transformer", Types.VAR_INT); // Holder registry id
        this.villagerFood = add("villager_food", Types.VAR_INT);
        this.compostable = add("compostable", ResolvableIntType.INSTANCE);
        this.cookingFuel = add("cooking_fuel", new FuelType(ResolvableIntType.INSTANCE, ResolvableFloatType.INSTANCE));
        this.brewingFuel = add("brewing_fuel", new FuelType(ResolvableIntType.INSTANCE, ResolvableFloatType.INSTANCE));
        this.mobVisibility = add("mob_visibility", MobVisibilityType.INSTANCE);
        this.providesPotteryPattern = add("provides_pottery_pattern", Types.VAR_INT); // Holder registry id
        this.signTextFront = add("sign_text_front", SignTextType.INSTANCE);
        this.signTextBack = add("sign_text_back", SignTextType.INSTANCE);
        this.waxed = add("waxed", UnitType.INSTANCE);
        this.cushionColor = add("cushion/color", Types.VAR_INT); // DyeColor id
    }

    /**
     * Either an inline int (boolean true + int) or a reference to the context_int_provider registry
     * (boolean false + identifier string).
     */
    static final class ResolvableIntType extends Type<Object> {
        static final ResolvableIntType INSTANCE = new ResolvableIntType();

        private ResolvableIntType() {
            super(Object.class);
        }

        @Override
        public Object read(final ByteBuf buffer) {
            if (buffer.readBoolean()) {
                return Types.INT.read(buffer);
            }
            return Types.STRING.read(buffer);
        }

        @Override
        public void write(final ByteBuf buffer, final Object value) {
            if (value instanceof final Integer integer) {
                buffer.writeBoolean(true);
                Types.INT.write(buffer, integer);
            } else {
                buffer.writeBoolean(false);
                Types.STRING.write(buffer, (String) value);
            }
        }
    }

    /**
     * Either an inline float (boolean true + float) or a registry reference
     * (boolean false + identifier string).
     */
    static final class ResolvableFloatType extends Type<Object> {
        static final ResolvableFloatType INSTANCE = new ResolvableFloatType();

        private ResolvableFloatType() {
            super(Object.class);
        }

        @Override
        public Object read(final ByteBuf buffer) {
            if (buffer.readBoolean()) {
                return Types.FLOAT.read(buffer);
            }
            return Types.STRING.read(buffer);
        }

        @Override
        public void write(final ByteBuf buffer, final Object value) {
            if (value instanceof final Float floatValue) {
                buffer.writeBoolean(true);
                Types.FLOAT.write(buffer, floatValue);
            } else {
                buffer.writeBoolean(false);
                Types.STRING.write(buffer, (String) value);
            }
        }
    }

    /**
     * Two resolvable values: an int (burn time/uses) and a float (speed multiplier).
     */
    static final class FuelType extends Type<Object[]> {
        private final Type<Object> intType;
        private final Type<Object> floatType;

        FuelType(final Type<Object> intType, final Type<Object> floatType) {
            super(Object[].class);
            this.intType = intType;
            this.floatType = floatType;
        }

        @Override
        public Object[] read(final ByteBuf buffer) {
            return new Object[]{intType.read(buffer), floatType.read(buffer)};
        }

        @Override
        public void write(final ByteBuf buffer, final Object[] value) {
            intType.write(buffer, value[0]);
            floatType.write(buffer, value[1]);
        }
    }

    /**
     * Holder set of entity types followed by a visibility float. The holder set is either a named tag
     * (var int 0 + identifier string) or a direct set (var int size + 1, followed by size var int holder ids).
     */
    static final class MobVisibilityType extends Type<Object[]> {
        static final MobVisibilityType INSTANCE = new MobVisibilityType();

        private MobVisibilityType() {
            super(Object[].class);
        }

        @Override
        public Object[] read(final ByteBuf buffer) {
            final int size = Types.VAR_INT.read(buffer) - 1;
            final Object holderSet;
            if (size == -1) {
                holderSet = new Object[]{Boolean.TRUE, Types.STRING.read(buffer)};
            } else {
                final int[] ids = new int[size];
                for (int i = 0; i < size; i++) {
                    ids[i] = Types.VAR_INT.read(buffer);
                }
                holderSet = new Object[]{Boolean.FALSE, ids};
            }
            return new Object[]{holderSet, Types.FLOAT.read(buffer)};
        }

        @Override
        public void write(final ByteBuf buffer, final Object[] value) {
            final Object[] holderSet = (Object[]) value[0];
            if ((Boolean) holderSet[0]) {
                Types.VAR_INT.write(buffer, 0);
                Types.STRING.write(buffer, (String) holderSet[1]);
            } else {
                final int[] ids = (int[]) holderSet[1];
                Types.VAR_INT.write(buffer, ids.length + 1);
                for (final int id : ids) {
                    Types.VAR_INT.write(buffer, id);
                }
            }
            Types.FLOAT.write(buffer, (Float) value[1]);
        }
    }

    /**
     * Sign text: four lines, optional four filtered lines, dye color id and glowing text flag.
     */
    static final class SignTextType extends Type<Object[]> {
        static final SignTextType INSTANCE = new SignTextType();

        private SignTextType() {
            super(Object[].class);
        }

        @Override
        public Object[] read(final ByteBuf buffer) {
            final Tag[] messages = readLines(buffer);
            final Tag[] filtered = buffer.readBoolean() ? readLines(buffer) : null;
            final int color = Types.VAR_INT.read(buffer);
            final boolean glowing = buffer.readBoolean();
            return new Object[]{messages, filtered, color, glowing};
        }

        @Override
        public void write(final ByteBuf buffer, final Object[] value) {
            writeLines(buffer, (Tag[]) value[0]);
            final Tag[] filtered = (Tag[]) value[1];
            buffer.writeBoolean(filtered != null);
            if (filtered != null) {
                writeLines(buffer, filtered);
            }
            Types.VAR_INT.write(buffer, (Integer) value[2]);
            buffer.writeBoolean((Boolean) value[3]);
        }

        private static Tag[] readLines(final ByteBuf buffer) {
            final Tag[] lines = new Tag[4];
            for (int i = 0; i < 4; i++) {
                lines[i] = Types.TEXT_COMPONENT_TAG.read(buffer);
            }
            return lines;
        }

        private static void writeLines(final ByteBuf buffer, final Tag[] lines) {
            for (final Tag line : lines) {
                Types.TEXT_COMPONENT_TAG.write(buffer, line);
            }
        }
    }

    /**
     * Empty unit component.
     */
    static final class UnitType extends Type<Object> {
        static final UnitType INSTANCE = new UnitType();
        private static final Object UNIT = new Object();

        private UnitType() {
            super(Object.class);
        }

        @Override
        public Object read(final ByteBuf buffer) {
            return UNIT;
        }

        @Override
        public void write(final ByteBuf buffer, final Object value) {
            // No data
        }
    }
}
