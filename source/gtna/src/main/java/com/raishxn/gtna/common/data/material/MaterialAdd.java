package com.raishxn.gtna.common.data.material;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.*;

import static com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;

public class MaterialAdd {

    public static void init() {
        Bronze.addFlags(GENERATE_SPRING_SMALL, GENERATE_SPRING);
        Beryllium.addFlags(GENERATE_ROD, GENERATE_FRAME);

        // These elemental materials are marker-only in GTCEu 8.0.1, while GTNA uses
        // their tangible forms in progression recipes. Restore only the forms GTNA needs.
        ensureIngot(Rhenium);
        Rhenium.addFlags(GENERATE_PLATE, GENERATE_FRAME, GENERATE_FOIL);
        ensureDust(Hafnium);
        ensureDust(Erbium);
        ensureDust(Zirconium);
        Neutronium.addFlags(GENERATE_FOIL, GENERATE_ROTOR);
        // RTAN has WireProperties, but GTCEu's fine-wire TagPrefix additionally
        // requires this flag. High-tier circuits/components use that form.
        RutheniumTriniumAmericiumNeutronate.addFlags(GENERATE_FINE_WIRE);
    }

    private static void ensureDust(Material material) {
        if (!material.hasProperty(PropertyKey.DUST)) {
            material.setProperty(PropertyKey.DUST, new DustProperty());
        }
    }

    private static void ensureIngot(Material material) {
        ensureDust(material);
        if (!material.hasProperty(PropertyKey.INGOT)) {
            material.setProperty(PropertyKey.INGOT, new IngotProperty());
        }
    }
}
