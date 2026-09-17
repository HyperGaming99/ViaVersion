/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2026 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viaversion.protocols.v26_2to26_3;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.data.MappingDataBase;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.data.version.StructuredDataKeys1_21_11;
import com.viaversion.viaversion.api.minecraft.data.version.StructuredDataKeys26_2;
import com.viaversion.viaversion.api.minecraft.data.version.StructuredDataKeys26_3;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_3;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes26_1;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType26_1;
import com.viaversion.viaversion.api.type.types.misc.ParticleType;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.connection.ProtocolStorablesBase;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.RecipeDisplayRewriter1_21_5;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPacket26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ServerboundPacket26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ServerboundPackets26_3;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPackets26_1;
import com.viaversion.viaversion.protocols.v26_2to26_3.rewriter.BlockItemPacketRewriter26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.rewriter.EntityPacketRewriter26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.storage.ProtocolStorables26_3;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.block.BlockRewriter1_21_5;
import com.viaversion.viaversion.rewriter.text.NBTComponentRewriter;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol26_2To26_3 extends AbstractProtocol<ClientboundPacket26_1, ClientboundPacket26_3, ServerboundPacket26_3, ServerboundPacket26_1> {

    public static final MappingData MAPPINGS = new MappingDataBase("26.2", "26.3");
    private final EntityPacketRewriter26_3 entityRewriter = new EntityPacketRewriter26_3(this);
    private final BlockItemPacketRewriter26_3 itemRewriter = new BlockItemPacketRewriter26_3(this);
    private final BlockRewriter<ClientboundPacket26_1> blockRewriter = new BlockRewriter1_21_5<>(this, ChunkType26_1::new);
    private final ParticleRewriter<ClientboundPacket26_1> particleRewriter = new ParticleRewriter<>(this);
    private final TagRewriter<ClientboundPacket26_1> tagRewriter = new TagRewriter<>(this);
    private final NBTComponentRewriter<ClientboundPacket26_1> componentRewriter = new NBTComponentRewriter<>(this);
    private final RegistryDataRewriter registryDataRewriter = new RegistryDataRewriter(this);
    private final RecipeDisplayRewriter1_21_5<ClientboundPacket26_1> recipeRewriter = new RecipeDisplayRewriter1_21_5<>(this);

    public Protocol26_2To26_3() {
        super(ClientboundPacket26_1.class, ClientboundPacket26_3.class, ServerboundPacket26_3.class, ServerboundPacket26_1.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        // Explode gained a trailing "play sound" boolean in 26.3; the shared particle handler already rewrote the rest
        appendClientbound(ClientboundPackets26_1.EXPLODE, wrapper -> wrapper.write(Types.BOOLEAN, true));

        // Particles were reordered and gained two extra max speed components and a randomization type in 26.3
        replaceClientbound(ClientboundPackets26_1.LEVEL_PARTICLES, wrapper -> {
            final Particle particle = wrapper.read(types().particle());
            final boolean overrideLimiter = wrapper.read(Types.BOOLEAN);
            final boolean alwaysShow = wrapper.read(Types.BOOLEAN);
            final double x = wrapper.read(Types.DOUBLE);
            final double y = wrapper.read(Types.DOUBLE);
            final double z = wrapper.read(Types.DOUBLE);
            final float offsetX = wrapper.read(Types.FLOAT);
            final float offsetY = wrapper.read(Types.FLOAT);
            final float offsetZ = wrapper.read(Types.FLOAT);
            final float maxSpeed = wrapper.read(Types.FLOAT);
            final int count = wrapper.read(Types.INT);

            particleRewriter.rewriteParticle(wrapper.user(), particle);

            wrapper.write(mappedTypes().particle(), particle);
            wrapper.write(Types.BOOLEAN, overrideLimiter);
            wrapper.write(Types.BOOLEAN, alwaysShow);
            wrapper.write(Types.DOUBLE, x);
            wrapper.write(Types.DOUBLE, y);
            wrapper.write(Types.DOUBLE, z);
            wrapper.write(Types.FLOAT, offsetX);
            wrapper.write(Types.FLOAT, offsetY);
            wrapper.write(Types.FLOAT, offsetZ);
            // The single max speed is approximated for each axis
            wrapper.write(Types.FLOAT, maxSpeed);
            wrapper.write(Types.FLOAT, maxSpeed);
            wrapper.write(Types.FLOAT, maxSpeed);
            wrapper.write(Types.VAR_INT, count);
            wrapper.write(Types.VAR_INT, 0); // DEFAULT randomization type
        });

        // Sign editor now takes a sign text slot (back = 0, front = 1) instead of a boolean
        replaceClientbound(ClientboundPackets26_1.OPEN_SIGN_EDITOR, wrapper -> {
            wrapper.passthrough(Types.BLOCK_POSITION1_14); // Position
            final boolean front = wrapper.read(Types.BOOLEAN);
            wrapper.write(Types.VAR_INT, front ? 1 : 0); // Sign text slot
        });

        // Teleport confirmation now carries the player position/rotation
        registerServerbound(ServerboundPackets26_3.ACCEPT_TELEPORTATION, wrapper -> {
            final int id = wrapper.read(Types.VAR_INT);
            wrapper.read(Types.DOUBLE); // X
            wrapper.read(Types.DOUBLE); // Y
            wrapper.read(Types.DOUBLE); // Z
            wrapper.read(Types.FLOAT); // Y rot
            wrapper.read(Types.FLOAT); // X rot
            wrapper.write(Types.VAR_INT, id);
        });

        // Sign update now takes a sign text slot after the lines
        registerServerbound(ServerboundPackets26_3.SIGN_UPDATE, wrapper -> {
            wrapper.passthrough(Types.BLOCK_POSITION1_14); // Position
            final String[] lines = new String[4];
            for (int i = 0; i < 4; i++) {
                lines[i] = wrapper.read(Types.STRING);
            }
            final int slot = wrapper.read(Types.VAR_INT); // Sign text slot
            wrapper.write(Types.BOOLEAN, slot == 1); // Front text
            for (final String line : lines) {
                wrapper.write(Types.STRING, line);
            }
        });

        // Left-clicking while punching is now its own packet; swing the main hand like the old swing packet did
        registerServerbound(ServerboundPackets26_1.SWING, ServerboundPackets26_3.PUNCH, wrapper -> wrapper.write(Types.VAR_INT, 0)); // Main hand

        registerServerbound(ServerboundPackets26_3.SPECTATOR_ACTION, wrapper -> {
            final Integer entityId = wrapper.read(Types.OPTIONAL_VAR_INT);
            if (entityId != null) {
                wrapper.write(Types.VAR_INT, entityRewriter.toOriginalEntityId(wrapper, entityId));
            } else {
                // Left-clicked the air, previously handled by the swing packet
                wrapper.setPacketType(ServerboundPackets26_1.SWING);
                wrapper.write(Types.VAR_INT, 0); // Main hand
            }
        });

        tagRewriter.addEmptyTags(RegistryType.BLOCK, "infiniburn_nether", "infiniburn_end", "infiniburn_overworld"); // from an older version, but servers may have skipped these
    }

    @Override
    protected void onMappingDataLoaded() {
        EntityTypes26_3.initialize(this);
        ParticleType.Fillers.fill26_2(this);
        mappedTypes().structuredData.filler(this).add(StructuredDataKey.CUSTOM_DATA, StructuredDataKey.MAX_STACK_SIZE, StructuredDataKey.MAX_DAMAGE,
            StructuredDataKey.UNBREAKABLE1_21_5, StructuredDataKey.RARITY, StructuredDataKey.TOOLTIP_DISPLAY, StructuredDataKey.DAMAGE_RESISTANT26_1,
            StructuredDataKey.CUSTOM_NAME, StructuredDataKey.LORE, StructuredDataKey.ENCHANTMENTS1_21_5,
            StructuredDataKey.CUSTOM_MODEL_DATA1_21_4, StructuredDataKey.BLOCKS_ATTACKS26_1, StructuredDataKey.PROVIDES_BANNER_PATTERNS26_1,
            StructuredDataKey.REPAIR_COST, StructuredDataKey.CREATIVE_SLOT_LOCK, StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE,
            StructuredDataKey.INTANGIBLE_PROJECTILE, StructuredDataKey.STORED_ENCHANTMENTS1_21_5, StructuredDataKey.DYED_COLOR1_21_5,
            StructuredDataKey.MAP_ID, StructuredDataKey.MAP_DECORATIONS, StructuredDataKey.MAP_POST_PROCESSING,
            StructuredDataKey.POTION_CONTENTS1_21_2, StructuredDataKey.SUSPICIOUS_STEW_EFFECTS, StructuredDataKey.WRITABLE_BOOK_CONTENT,
            StructuredDataKey.WRITTEN_BOOK_CONTENT, StructuredDataKey.TRIM1_21_5, StructuredDataKey.DEBUG_STICK_STATE, StructuredDataKey.ENTITY_DATA1_21_9,
            StructuredDataKey.BUCKET_ENTITY_DATA, StructuredDataKey.BLOCK_ENTITY_DATA1_21_9, StructuredDataKey.INSTRUMENT26_1,
            StructuredDataKey.RECIPES, StructuredDataKey.LODESTONE_TRACKER, StructuredDataKey.FIREWORK_EXPLOSION, StructuredDataKey.FIREWORKS,
            StructuredDataKey.PROFILE1_21_9, StructuredDataKey.NOTE_BLOCK_SOUND, StructuredDataKey.BANNER_PATTERNS, StructuredDataKey.BASE_COLOR,
            StructuredDataKey.POT_DECORATIONS, StructuredDataKey.BLOCK_STATE, StructuredDataKey.BEES1_21_9, StructuredDataKey.LOCK1_21_2,
            StructuredDataKey.CONTAINER_LOOT, StructuredDataKey.TOOL1_21_5, StructuredDataKey.ITEM_NAME, StructuredDataKey.OMINOUS_BOTTLE_AMPLIFIER,
            StructuredDataKey.FOOD1_21_2, StructuredDataKey.JUKEBOX_PLAYABLE26_1, StructuredDataKey.ATTRIBUTE_MODIFIERS1_21_6,
            StructuredDataKey.REPAIRABLE, StructuredDataKey.ENCHANTABLE, StructuredDataKey.CONSUMABLE1_21_2, StructuredDataKey.ATTACK_RANGE,
            StructuredDataKey.USE_COOLDOWN, StructuredDataKey.DAMAGE, StructuredDataKey.EQUIPPABLE1_21_6, StructuredDataKey.ITEM_MODEL,
            StructuredDataKey.GLIDER, StructuredDataKey.TOOLTIP_STYLE, StructuredDataKey.DEATH_PROTECTION, StructuredDataKey.WEAPON,
            StructuredDataKey.POTION_DURATION_SCALE, StructuredDataKey.VILLAGER_VARIANT, StructuredDataKey.WOLF_VARIANT, StructuredDataKey.WOLF_COLLAR,
            StructuredDataKey.FOX_VARIANT, StructuredDataKey.SALMON_SIZE, StructuredDataKey.PARROT_VARIANT, StructuredDataKey.TROPICAL_FISH_PATTERN,
            StructuredDataKey.TROPICAL_FISH_BASE_COLOR, StructuredDataKey.TROPICAL_FISH_PATTERN_COLOR, StructuredDataKey.MOOSHROOM_VARIANT,
            StructuredDataKey.RABBIT_VARIANT, StructuredDataKey.PIG_VARIANT, StructuredDataKey.FROG_VARIANT, StructuredDataKey.HORSE_VARIANT,
            StructuredDataKey.PAINTING_VARIANT, StructuredDataKey.LLAMA_VARIANT, StructuredDataKey.AXOLOTL_VARIANT, StructuredDataKey.CAT_VARIANT,
            StructuredDataKey.CAT_COLLAR, StructuredDataKey.SHEEP_COLOR, StructuredDataKey.SHULKER_COLOR, StructuredDataKey.PROVIDES_TRIM_MATERIAL26_1,
            StructuredDataKey.BREAK_SOUND, StructuredDataKey.COW_VARIANT, StructuredDataKey.CHICKEN_VARIANT26_1, StructuredDataKey.WOLF_SOUND_VARIANT,
            StructuredDataKey.USE_EFFECTS, StructuredDataKey.MINIMUM_ATTACK_CHARGE, StructuredDataKey.DAMAGE_TYPE26_1, StructuredDataKey.PIERCING_WEAPON,
            StructuredDataKey.KINETIC_WEAPON, StructuredDataKey.ZOMBIE_NAUTILUS_VARIANT26_1, StructuredDataKey.ADDITIONAL_TRADE_COST,
            StructuredDataKey.DYE, StructuredDataKey.PIG_SOUND_VARIANT, StructuredDataKey.COW_SOUND_VARIANT, StructuredDataKey.CHICKEN_SOUND_VARIANT, StructuredDataKey.CAT_SOUND_VARIANT);
        super.onMappingDataLoaded();
    }

    @Override
    public ProtocolStorablesBase createStorables() {
        return new ProtocolStorables26_3();
    }

    @Override
    public void init(final UserConnection connection) {
        addEntityTracker(connection);
        addItemHasher(connection);
    }

    @Override
    public MappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter26_3 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter26_3 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public BlockRewriter<ClientboundPacket26_1> getBlockRewriter() {
        return blockRewriter;
    }

    @Override
    public RecipeDisplayRewriter<ClientboundPacket26_1> getRecipeRewriter() {
        return recipeRewriter;
    }

    @Override
    public RegistryDataRewriter getRegistryDataRewriter() {
        return registryDataRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket26_1> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket26_1> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public NBTComponentRewriter<ClientboundPacket26_1> getComponentRewriter() {
        return componentRewriter;
    }

    @Override
    public Types1_20_5<StructuredDataKeys26_2, EntityDataTypes26_1> types() {
        return VersionedTypes.V26_2;
    }

    @Override
    public Types1_20_5<StructuredDataKeys26_3, EntityDataTypes26_1> mappedTypes() {
        return VersionedTypes.V26_3;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket26_1, ClientboundPacket26_3, ServerboundPacket26_3, ServerboundPacket26_1> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets26_1.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets26_3.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets26_3.class, ServerboundConfigurationPackets1_21_9.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets26_1.class, ServerboundConfigurationPackets1_21_9.class)
        );
    }
}
