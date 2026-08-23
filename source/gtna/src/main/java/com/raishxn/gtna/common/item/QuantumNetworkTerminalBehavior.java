package com.raishxn.gtna.common.item;

import com.gregtechceu.gtceu.api.mui.IItemUIHolder;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;

import brachy.modularui.factory.PlayerInventoryGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.utils.serialization.network.ByteBufAdapters;
import brachy.modularui.value.sync.GenericSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;

public class QuantumNetworkTerminalBehavior implements IItemUIHolder {

    public static final QuantumNetworkTerminalBehavior INSTANCE = new QuantumNetworkTerminalBehavior();

    protected QuantumNetworkTerminalBehavior() {}

    @Override
    public ModularPanel<?> buildUI(PlayerInventoryGuiData<?> data, PanelSyncManager syncManager, UISettings settings) {
        GenericSyncValue<RegistryFriendlyByteBuf, Component> status = GenericSyncValue
                .<RegistryFriendlyByteBuf, Component>builder(Component.class)
                .adapter(ByteBufAdapters.COMPONENT)
                .getter(() -> QuantumTerminalUI.createStatus(data.getUsedItemStack(), data.getPlayer()))
                .build();
        syncManager.syncValue("quantum_network_status", status);

        return new ModularPanel<>("quantum_network_terminal")
                .size(310, 280)
                .background(GTGuiTextures.BACKGROUND)
                .child(new TextWidget<>(status::getValue)
                        .maxWidth(292)
                        .left(9).top(8));
    }
}
