// Exact runtime regression guard for the empty-ingredient failures caused by
// the UEV-MAX circuit tags, the progression GTNA supplies for them, and the
// machine recipes whose old generators silently collided or skipped entries.
ServerEvents.afterRecipes(event => {
    const ids = []
    const expectedRecipeCount = 561
    const highTiers = ['uev', 'uiv', 'uxv', 'opv', 'max']
    const componentTiers = ['uhv', 'uev', 'uiv', 'uxv', 'opv']
    const components = [
        'electric_motor', 'electric_pump', 'electric_piston', 'conveyor_module',
        'robot_arm', 'sensor', 'emitter', 'field_generator'
    ]
    const standardMachines = [
        'alloy_smelter', 'arc_furnace', 'assembler', 'autoclave', 'bender',
        'brewery', 'canner', 'centrifuge', 'chemical_bath', 'chemical_reactor',
        'compressor', 'cutter', 'distillery', 'electric_furnace', 'electrolyzer',
        'electromagnetic_separator', 'extractor', 'extruder', 'fermenter',
        'fluid_heater', 'fluid_solidifier', 'forge_hammer', 'forming_press',
        'gas_collector', 'laser_engraver', 'lathe', 'macerator', 'mixer',
        'ore_washer', 'packer', 'sifter', 'thermal_centrifuge', 'wiremill'
    ]

    highTiers.forEach(tier => {
        ids.push(`gtceu:assembly_line/gtna_${tier}_circuit`)
        ids.push(`gtceu:assembly_line/gtna_machine_casing_${tier}`)
        ids.push(`gtceu:shaped/${tier}_1a_energy_converter`)
        ids.push(`gtceu:shaped/${tier}_4a_energy_converter`)
        ids.push(`gtceu:shaped/${tier}_8a_energy_converter`)
        ids.push(`gtceu:shaped/${tier}_16a_energy_converter`)
        ids.push(`gtceu:shaped/${tier}_charger_4x`)
    })

    ;['uev', 'uiv', 'uxv', 'opv'].forEach(tier => {
        standardMachines.forEach(machine => ids.push(`gtceu:shaped/${tier}_${machine}`))
    })
    ;['uhv', 'uev', 'uiv', 'uxv', 'opv'].forEach(tier => {
        ids.push(`gtceu:shaped/${tier}_circuit_assembler`)
        ids.push(`gtceu:shaped/${tier}_scanner`)
    })

    componentTiers.forEach(tier => {
        components.forEach(component => ids.push(`gtceu:assembly_line/gtna_${component}_${tier}`))
    })

    // Survival paths for the Nexus handheld utilities.
    ids.push('gtceu:assembler/gtna_nexus_linker')
    ids.push('gtceu:assembler/gtna_quantum_network_terminal')
    ids.push('gtceu:assembly_line/gtna_nexus_structure_terminal')

    // High-tier utility hatches are researched from the immediately preceding
    // hatch. MAX recipes deliberately run at OpV voltage.
    const utilityHatchTiers = ['uhv', 'uev', 'uiv', 'uxv', 'opv', 'max']
    utilityHatchTiers.forEach(tier => {
        ids.push(`gtceu:assembly_line/gtna_accelerate_hatch_${tier}`)
        ids.push(`gtceu:assembly_line/gtna_overclock_hatch_${tier}`)
    })
    highTiers.forEach(tier => ids.push(`gtceu:assembly_line/gtna_thread_hatch_${tier}`))
    ;['uhv', 'uev', 'uiv', 'uxv', 'opv'].forEach(tier => {
        ids.push(`gtceu:assembly_line/gtna_parallel_hatch_${tier}`)
    })
    highTiers.forEach(tier => ids.push(`gtceu:assembly_line/nexus_capacitor_${tier}`))

    // Every registered wireless hatch must have one executable assembler
    // progression recipe: 14 voltage tiers x 11 amperages x input/output.
    const wirelessTiers = [
        'lv', 'mv', 'hv', 'ev', 'iv', 'luv', 'zpm',
        'uv', 'uhv', 'uev', 'uiv', 'uxv', 'opv', 'max'
    ]
    wirelessTiers.forEach(tier => {
        for (let ampExp = 0; ampExp <= 10; ampExp++) {
            const amps = Math.pow(4, ampExp)
            ids.push(`gtceu:assembler/wireless_energy_in_${amps}a_${tier}`)
            ids.push(`gtceu:assembler/wireless_energy_out_${amps}a_${tier}`)
        }
    })

    // Echoite's same-item compressor inputs used to collapse in RecipeDB.
    // Keep the two truly single-input operations in the compressor and use
    // distinct programming circuits in the two-input packer for the rest.
    ;[
        'compress_echoite_ingot_to_double_ingot',
        'compress_echoite_double_ingot_to_quadruple_ingot'
    ].forEach(recipe => ids.push(`gtceu:compressor/${recipe}`))
    ;[
        'pack_echoite_ingot_to_triple_ingot',
        'pack_echoite_ingot_to_quadruple_ingot',
        'pack_echoite_ingot_to_quintuple_ingot',
        'pack_echoite_ingot_to_block'
    ].forEach(recipe => ids.push(`gtceu:packer/${recipe}`))

    const retiredIds = [
        'gtceu:compressor/compress_echoite_ingot_to_triple_ingot',
        'gtceu:compressor/compress_echoite_ingot_to_quadruple_ingot',
        'gtceu:compressor/compress_echoite_ingot_to_quintuple_ingot',
        'gtceu:compressor/compress_echoite_to_block'
    ]

    const invalid = []
    if (ids.length !== expectedRecipeCount) {
        invalid.push(`audit_recipe_id_count=${ids.length},expected=${expectedRecipeCount}`)
    }
    ids.forEach(id => {
        const count = event.countRecipes({ id: id })
        if (count !== 1) invalid.push(`${id}=${count}`)
    })
    retiredIds.forEach(id => {
        const count = event.countRecipes({ id: id })
        if (count !== 0) invalid.push(`retired:${id}=${count}`)
    })

    const circuitItems = highTiers.map(tier => `gtna:${tier}_circuit`)
    const missingItems = circuitItems.filter(id => Item.of(id).isEmpty())
    if (invalid.length === 0 && missingItems.length === 0) {
        console.info(`[Codex GTNA Recipe Audit] PASS ${ids.length}/${expectedRecipeCount} recipes and ${circuitItems.length}/${circuitItems.length} circuit items`)
    } else {
        console.error(`[Codex GTNA Recipe Audit] FAIL ${invalid.concat(missingItems).join(', ')}`)
    }
})
