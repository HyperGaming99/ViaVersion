/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2026 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.viaversion.viaversion.protocols.v26_2to26_3.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.protocols.v26_2to26_3.Protocol26_2To26_3;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;

/** Rewrites registry element changes introduced by 26.3. */
public final class RegistryDataRewriter26_3 extends RegistryDataRewriter {

    public RegistryDataRewriter26_3(final Protocol26_2To26_3 protocol) {
        super(protocol);
        addMissingDamageTypes();
    }

    private void addMissingDamageTypes() {
        addEntries("damage_type",
            damageType("burn_from_stepping"),
            damageType("bypasses_invulnerability"),
            damageType("is_explosion"),
            damageType("is_fall"),
            damageType("is_fire"),
            damageType("is_projectile"));
    }

    private static RegistryEntry damageType(final String name) {
        final CompoundTag tag = new CompoundTag();
        tag.putString("scaling", "never");
        tag.putString("message_id", "generic");
        tag.putFloat("exhaustion", 0);
        return new RegistryEntry("minecraft:" + name, tag);
    }

    @Override
    public void updateEnchantments(final UserConnection connection, final RegistryEntry[] entries) {
        super.updateEnchantments(connection, entries);
        for (final RegistryEntry entry : entries) {
            if (entry.tag() != null) {
                rewriteProviderTypes(entry.tag());
            }
        }
    }

    @Override
    public void updateEnchantmentTerm(final CompoundTag term) {
        // 26.2 calls the discriminator "condition" while 26.3 calls it "type".
        // Run the base rewriter first so its condition-based nested rewrites still apply.
        super.updateEnchantmentTerm(term);

        final StringTag condition = term.removeUnchecked("condition");
        if (condition != null) {
            term.put("type", condition);
        }

        rewriteProviderTypes(term);
    }

    @Override
    public void updateTrimMaterials(final RegistryEntry[] entries) {
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].tag() instanceof final CompoundTag tag && !tag.contains("palette_id")) {
                final StringTag assetName = tag.removeUnchecked("asset_name");
                if (assetName != null) {
                    // 26.3 uses a namespaced palette texture id instead of asset_name.
                    tag.put("palette_id", new StringTag("minecraft:trim/" + assetName.getValue()));
                }
                // Armor-specific overrides moved to equipment assets in 26.3.
                tag.remove("override_armor_assets");
            }
        }
        super.updateTrimMaterials(entries);
    }

    private void rewriteProviderTypes(final Tag tag) {
        if (tag instanceof final CompoundTag compound) {
            for (final Tag child : compound.values()) {
                rewriteProviderTypes(child);
            }
            final StringTag type = compound.getStringTag("type");
            if (type != null && type.getValue().endsWith("_state_provider")) {
                type.setValue(type.getValue()
                    .replace("dual_noise_provider", "dual_noise")
                    .replace("noise_threshold_provider", "noise_threshold")
                    .replace("noise_provider", "noise")
                    .replace("randomized_int_state_provider", "randomized_int")
                    .replace("rule_based_state_provider", "rule_based")
                    .replace("simple_state_provider", "simple")
                    .replace("weighted_state_provider", "weighted")
                    .replace("random_block_provider", "random_block")
                    .replace("rotated_block_provider", "rotated"));
            }
        } else if (tag instanceof final ListTag<?> list) {
            for (final Tag child : list) {
                rewriteProviderTypes(child);
            }
        }
    }
}
