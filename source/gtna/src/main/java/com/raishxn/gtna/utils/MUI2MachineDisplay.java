package com.raishxn.gtna.utils;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.GenericSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;

import java.util.List;
import java.util.function.Consumer;

/** Helpers for retaining custom machine status text while moving LDLib UIs to MUI2. */
public final class MUI2MachineDisplay {

    private MUI2MachineDisplay() {}

    public static IWidget syncedLines(PanelSyncManager syncManager, String key,
                                      Consumer<List<Component>> lineProvider) {
        GenericSyncValue<RegistryFriendlyByteBuf, Component> value = GenericSyncValue
                .<RegistryFriendlyByteBuf, Component>builder(Component.class)
                .getter(() -> {
                    List<Component> lines = new java.util.ArrayList<>();
                    lineProvider.accept(lines);
                    var result = Component.empty();
                    for (int i = 0; i < lines.size(); i++) {
                        if (i != 0) result.append("\n");
                        result.append(lines.get(i));
                    }
                    return result;
                })
                .deserializer(ComponentSerialization.STREAM_CODEC)
                .serializer(ComponentSerialization.STREAM_CODEC)
                .equalsDefault()
                .copyImmutable()
                .build();
        syncManager.syncValue(key, value);
        return Text.dynamic(value::getValue).asWidget().maxWidth(170);
    }
}
