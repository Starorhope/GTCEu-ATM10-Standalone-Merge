package yuuki1293.pccard;

import appeng.api.ids.AEComponents;
import appeng.crafting.pattern.EncodedProcessingPattern;
import com.gregtechceu.gtceu.common.data.item.GTDataComponents;
import net.minecraft.world.item.ItemStack;
import net.pedroksl.advanced_ae.common.definitions.AAEComponents;
import net.pedroksl.advanced_ae.common.patterns.EncodedAdvProcessingPattern;
import net.pedroksl.ae2addonlib.util.NullableDirection;

import java.util.ArrayList;

public class TagUtils {
    /**
     * get inputs itemStacks from Pattern.<br>
     * circuit will be deleted.
     * return 0 ~ 31
     */
    public static int extractCircuitNumber(ItemStack stack) {
        var component = stack.get(AEComponents.ENCODED_PROCESSING_PATTERN);
        if (component != null) {
            var inputs = new ArrayList<>(component.sparseInputs());
            for (int i = 0; i < inputs.size(); i++) {
                var input = inputs.get(i);
                if (input == null) continue;

                var number = input.what().get(GTDataComponents.CIRCUIT_CONFIG.get());
                if (number != null) {
                    // Keep the sparse slot layout intact. AE2 treats null as an unused pattern slot.
                    inputs.set(i, null);
                    stack.set(AEComponents.ENCODED_PROCESSING_PATTERN,
                        new EncodedProcessingPattern(inputs, new ArrayList<>(component.sparseOutputs())));
                    return number;
                }
            }
        }

        var advancedComponent = stack.get(AAEComponents.ENCODED_ADV_PROCESSING_PATTERN);
        if (advancedComponent == null) return -1;

        var inputs = new ArrayList<>(advancedComponent.sparseInputs());
        var outputs = new ArrayList<>(advancedComponent.sparseOutputs());
        var directions = new ArrayList<>(advancedComponent.directionList());
        // Keep malformed/legacy components safe to rewrite: AdvancedAE indexes directions
        // alongside the input list when decoding the pattern.
        while (directions.size() < inputs.size()) {
            directions.add(NullableDirection.NULLDIR);
        }
        while (directions.size() > inputs.size()) {
            directions.removeLast();
        }
        for (int i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            if (input == null) continue;

            var number = input.what().get(GTDataComponents.CIRCUIT_CONFIG.get());
            if (number != null) {
                inputs.remove(i);
                directions.remove(i);
                stack.set(AAEComponents.ENCODED_ADV_PROCESSING_PATTERN,
                    new EncodedAdvProcessingPattern(inputs, outputs, directions));
                return number;
            }
        }

        return -1;
    }
}
