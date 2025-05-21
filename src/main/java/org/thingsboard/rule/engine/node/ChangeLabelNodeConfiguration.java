package org.thingsboard.rule.engine.node;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

/**
 * Configuration for the ChangeLabelNode.
 * Defines how the new label is specified and the target entity type.
 */
@Data
public class ChangeLabelNodeConfiguration implements NodeConfiguration<ChangeLabelNodeConfiguration> {

    /**
     * Enum to define the source of the new label.
     */
    public enum LabelSource {
        STATIC,      // Label is a static string
        MESSAGE_METADATA, // Label is from message metadata
        MESSAGE_DATA // Label is from message data (e.g., a JSON field)
    }

    private LabelSource labelSource;
    private String staticLabelValue; // Used if labelSource is STATIC
    private String labelNameOrPattern; // Used if labelSource is MESSAGE_METADATA or MESSAGE_DATA. Can be a simple name or a pattern.
    private String targetEntityType;   // Optional: if empty, try to infer from originator. E.g., "DEVICE", "ASSET"

    @Override
    public ChangeLabelNodeConfiguration defaultConfiguration() {
        ChangeLabelNodeConfiguration configuration = new ChangeLabelNodeConfiguration();
        configuration.setLabelSource(LabelSource.STATIC);
        configuration.setStaticLabelValue("NewLabel");
        configuration.setLabelNameOrPattern("");
        configuration.setTargetEntityType(""); // Default to inferred
        return configuration;
    }
}
