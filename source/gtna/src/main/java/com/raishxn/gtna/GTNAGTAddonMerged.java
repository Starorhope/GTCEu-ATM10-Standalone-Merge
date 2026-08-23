package com.raishxn.gtna;

import com.gregtechceu.gtceu.api.addon.GTAddon;

/**
 * GTCEu addon entry point used when GTNA is embedded in GTCEu's physical mod file.
 *
 * <p>GTCEu filters addon annotations against the owning logical mod id. The standalone
 * {@link GTNAGTAddon} remains bound to {@code gtna}; this entry point is selected only when
 * the same implementation is carried by the single-logical-mod {@code gtceu} bundle.</p>
 */
@GTAddon("gtceu")
public final class GTNAGTAddonMerged extends GTNAGTAddon {}
