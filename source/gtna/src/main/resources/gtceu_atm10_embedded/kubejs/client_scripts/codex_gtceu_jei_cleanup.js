// Older development builds registered GregStar-only parts under kubejs:*.
// This KubeJS + JEI version implements removeEntries and
// removeEntriesCompletely through the same JEI runtime ingredient removal.
// Recipe slots retain their already-typed ingredients, so this callback is
// only used for aliases or entries that must not appear in the side list.
//
// GTNA now supplies a complete UEV-MAX casing and component progression, so
// the real post-UHV machine casings must remain visible in the recipe viewer.
const $GTNAConfigHolder = Java.loadClass('com.raishxn.gtna.config.ConfigHolder')

RecipeViewerEvents.removeEntries('item', event => {
    const legacyGregStarIds = [
        'star_housing',
        'absolute_reaction_plating',
        'star_compression_module',
        'superthermal_transference_coil',
        'cable_of_hyperconductivity',
        'greg_star_shard',
        'doge_coin',
        'micro_universe_catalyst',
        'micro_universe_drill_ship',
        'micro_universe_energy_transmitter',
        'micro_universe_focus_lens',
        'inert_nether_star',
        'creative_polonium_cell',
        'creative_plutonium_cell',
        'infinite_chlorine_cell',
        'infinite_sulfuric_acid_cell',
        'infinite_hydrogen_cell',
        'infinite_oxygen_cell',
        'infinite_styrene_butadiene_rubber_cell',
        'infinite_lava_cell',
        'infinite_polytetrafluoroethylene_cell',
        'infinite_polybenzimidazole_cell',
        'infinite_ethylene_cell',
        'infinite_lubricant_cell',
        'infinite_air_cell',
        'infinite_nether_air_cell',
        'infinite_ender_air_cell',
        'infinite_fluorine_cell',
        'infinite_soldering_alloy_cell'
    ]
    const hiddenEntries = legacyGregStarIds.map(id => `kubejs:${id}`)

    // NORMAL mode intentionally disables the five restricted GTNA families.
    // Hide every disabled family from the ingredient list; otherwise JEI shows
    // dozens of unobtainable machines whose conditional recipes are absent.
    const tiers = [
        'ulv', 'lv', 'mv', 'hv', 'ev', 'iv', 'luv',
        'zpm', 'uv', 'uhv', 'uev', 'uiv', 'uxv', 'opv', 'max'
    ]
    const voltageTiers = tiers.slice(1)
    const addRestrictedGroup = (group, ids) => {
        if (!$GTNAConfigHolder.isRestrictedGroupAllowed(group)) {
            ids.forEach(id => hiddenEntries.push(id))
        }
    }

    if ($GTNAConfigHolder.shouldHideRestrictedItemsFromJei()) {
        addRestrictedGroup('infinityCovers', [
            'gtna:infinite_steam_singleblock_cover',
            'gtna:infinite_electric_singleblock_cover'
        ])
        addRestrictedGroup('quantumCosmicNexusArmor', [
            'gtna:quantum_cosmic_nexus_helmet',
            'gtna:quantum_cosmic_nexus_chestplate',
            'gtna:quantum_cosmic_nexus_leggings',
            'gtna:quantum_cosmic_nexus_boots'
        ])
        addRestrictedGroup('realityRipper', ['gtna:reality_ripper_sword'])
        // Build these arrays as expressions instead of block-local declarations.
        // Rhino reuses the event callback scope and otherwise reports a false
        // "redeclaration of var" when JEI fires this callback during reload.
        addRestrictedGroup('infiniteInputParts', ['gtna:infinite_steam_input_bus']
            .concat(voltageTiers.map(tier => `gtna:infinite_input_bus_${tier}`))
            .concat(voltageTiers.map(tier => `gtna:infinite_input_hatch_${tier}`)))

        addRestrictedGroup('outputBoostParts', ['gtna:output_boost_steam_output_bus']
            .concat(voltageTiers.map(tier => `gtna:output_boost_hatch_${tier}`))
            .concat(voltageTiers.map(tier => `gtna:output_boost_item_bus_${tier}`))
            .concat(voltageTiers.map(tier => `gtna:output_boost_fluid_hatch_${tier}`)))
    }

    // Development controllers and unfinished/internal structure parts are not
    // survival progression entries and must not masquerade as missing recipes.
    hiddenEntries.push(
        'gtna:duration_tester',
        'gtna:nexus_hypercore_casing',
        'gtna:matrix_module_i',
        'gtna:matrix_module_ii',
        'gtna:matrix_module_iii',
        'gtna:matrix_module_iv',
        'gtna:restraint_device'
    )

    // Optional debug/internal entries can be completely absent from the item
    // registry under the current config. KubeJS rejects an entire remove call
    // if even one ID is missing, so pass only IDs that actually exist.
    var codexGtnaExistingHiddenEntries = hiddenEntries.filter(id => Item.exists(id))
    var codexGtnaAbsentHiddenEntryCount = hiddenEntries.length - codexGtnaExistingHiddenEntries.length
    event.remove(codexGtnaExistingHiddenEntries)
    console.info(
        `[Codex GT Viewer] processed ${hiddenEntries.length} candidates; ` +
        `removed ${codexGtnaExistingHiddenEntries.length}; absent ${codexGtnaAbsentHiddenEntryCount}`
    )
})
