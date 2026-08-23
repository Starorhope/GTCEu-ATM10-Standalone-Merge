package com.raishxn.gtna.common.item.armor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.raishxn.gtna.utils.GTNATooltips;

import java.util.List;

public class QuantumCosmicNexusArmorItem extends ArmorItem {

    public QuantumCosmicNexusArmorItem(Type type, Properties properties) {
        super(QuantumCosmicNexusArmorMaterial.INSTANCE, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(GTNATooltips.desc("item.gtna.quantum_cosmic_nexus_armor.tooltip"));
        tooltip.add(GTNATooltips.important("item.gtna.quantum_cosmic_nexus_armor.tooltip.power"));
    }
}
