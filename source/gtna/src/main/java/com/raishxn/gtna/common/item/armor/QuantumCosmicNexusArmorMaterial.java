package com.raishxn.gtna.common.item.armor;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNAMaterials;

import java.util.List;
import java.util.Map;

public final class QuantumCosmicNexusArmorMaterial {

    public static final Holder<ArmorMaterial> INSTANCE = Holder.direct(new ArmorMaterial(
            Map.of(
                    ArmorItem.Type.BOOTS, 10,
                    ArmorItem.Type.LEGGINGS, 20,
                    ArmorItem.Type.CHESTPLATE, 24,
                    ArmorItem.Type.HELMET, 10,
                    ArmorItem.Type.BODY, 24),
            200,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(ChemicalHelper.get(TagPrefix.ingot, GTNAMaterials.Echoite).getItem()),
            List.of(new ArmorMaterial.Layer(GTNACORE.id("quantum_cosmic_nexus_armor"))),
            100.0F,
            1.0F));

    private QuantumCosmicNexusArmorMaterial() {}
}
