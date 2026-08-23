package yuuki1293.pccard.impl;

/**
 * Blocking modes for the GT ME pattern buffer.
 *
 * <p>The 1.20.1 enum implemented LDLib's selector interface and supplied LDLib textures. GT 8 no longer
 * exposes that UI API, so this port intentionally keeps only the mode data needed by the machine logic.</p>
 */
public enum PatternBufferBlockingMode {

    DISABLED("gui.pccard.pattern_buffer.blocking_mode.off",
        "gui.pccard.pattern_buffer.blocking_mode.off.description"),
    NORMAL("gui.pccard.pattern_buffer.blocking_mode.normal",
        "gui.pccard.pattern_buffer.blocking_mode.normal.description"),
    SMART("gui.pccard.pattern_buffer.blocking_mode.smart",
        "gui.pccard.pattern_buffer.blocking_mode.smart.description"),
    FULL("gui.pccard.pattern_buffer.blocking_mode.full",
        "gui.pccard.pattern_buffer.blocking_mode.full.description");

    public static final PatternBufferBlockingMode[] VALUES = values();
    public static final PatternBufferBlockingMode[] BASIC_VALUES = { DISABLED, NORMAL };

    private final String translationKey;
    private final String descriptionKey;

    PatternBufferBlockingMode(String translationKey, String descriptionKey) {
        this.translationKey = translationKey;
        this.descriptionKey = descriptionKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public String shortLabelKey() {
        return translationKey + ".short";
    }

    public String descriptionKey() {
        return descriptionKey;
    }
}
