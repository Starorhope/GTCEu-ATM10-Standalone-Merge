package com.raishxn.gtna.common.machine.multiMachineBase;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.steam.SteamEnergyRecipeHandler;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeHandlerList;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import com.raishxn.gtna.api.machine.feature.IPatternBufferModeHost;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class SteamMultiMachineBase extends WorkableMultiblockMachine
                                            implements IMuiMachine, IPatternBufferModeHost {

    private final boolean isSteel;

    @Nullable
    protected SteamEnergyRecipeHandler steamEnergy = null;

    public SteamMultiMachineBase(BlockEntityCreationInfo holder, boolean isSteel, Object... args) {
        this(holder, isSteel, new RecipeLogic());
    }

    protected SteamMultiMachineBase(BlockEntityCreationInfo holder, boolean isSteel, RecipeLogic recipeLogic) {
        super(holder, recipeLogic);
        this.isSteel = isSteel;
    }

    protected double getConversionRate() {
        return 1.0;
    }

    @Override
    public void formStructure(String substructureName) {
        super.formStructure(substructureName);
        for (var part : getParts()) {
            if (!PartAbility.STEAM.isApplicable(part.getDefinition().getBlock())) continue;
            var handlers = part.getRecipeHandlers();
            for (var hl : handlers) {
                if (!hl.isValid(IO.IN)) continue;
                for (var fluidHandler : hl.getCapability(FluidRecipeCapability.CAP)) {
                    if (!(fluidHandler instanceof NotifiableFluidTank nft)) continue;
                    if (nft.isFluidValid(0, GTMaterials.Steam.getFluid(1))) {
                        steamEnergy = new SteamEnergyRecipeHandler(nft, getConversionRate());
                        addHandlerList(RecipeHandlerList.of(IO.IN, steamEnergy));
                        return;
                    }
                }
            }
        }
        if (steamEnergy == null) {
            invalidateStructure(substructureName);
        }
    }

    @Override
    public void invalidateStructure(String substructureName) {
        super.invalidateStructure(substructureName);
        this.steamEnergy = null;
    }

    @Override
    public @Nullable String gtna$resolvePatternBufferMode(com.gregtechceu.gtceu.api.recipe.GTRecipe recipe) {
        if (getRecipeTypes().length <= 1) {
            return null;
        }
        return recipe.getType().registryName.toString();
    }

    @Override
    public boolean gtna$applyPatternBufferMode(String modeId, com.gregtechceu.gtceu.api.recipe.GTRecipe recipe) {
        if (modeId == null || modeId.isBlank()) {
            return false;
        }
        for (int i = 0; i < getRecipeTypes().length; i++) {
            if (gtna$matchesModeId(modeId, getRecipeTypes()[i])) {
                if (getActiveRecipeType() != i) {
                    setActiveRecipeType(i);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        widgets.add(MUI2MachineDisplay.syncedLines(syncManager, "gtna_steam_status", this::addDisplayText));
        return widgets;
    }

    public void addDisplayText(List<Component> textList) {
        if (isFormed()) {
            if (steamEnergy != null && steamEnergy.getCapacity() > 0) {
                long steamStored = steamEnergy.getStored();
                textList.add(Component.translatable("gtceu.multiblock.steam.steam_stored", steamStored,
                        steamEnergy.getCapacity()));
            }

            if (recipeLogic.isWaiting()) {
                textList.add(Component.translatable("gtceu.multiblock.steam.low_steam")
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
            }
        }
    }

    /**
     * Compatibility hook for steam machines that still expose their old text-button actions.
     * Their status text is synchronized by MUI2 through {@link #addDisplayText(List)}.
     */
    public void handleDisplayClick(String componentData, ClickData clickData) {}
}
