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
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.protocols.v26_2to26_3.Protocol26_2To26_3;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;

/** Rewrites registry element changes introduced by 26.3. */
public final class RegistryDataRewriter26_3 extends RegistryDataRewriter {

    public RegistryDataRewriter26_3(final Protocol26_2To26_3 protocol) {
        super(protocol);
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
    }

    @Override
    public void updateTrimMaterials(final RegistryEntry[] entries) {
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].tag() instanceof final CompoundTag tag && !tag.contains("palette_id")) {
                // 26.3 identifies trim palettes by their registry index.
                tag.put("palette_id", new IntTag(i));
            }
        }
        super.updateTrimMaterials(entries);
    }
}
