package com.sierra.previewer; // Use the same package as MainFrame

import java.util.Arrays;
import java.util.List;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;

/**
 * Provides autocompletion for Sierra DSL XML based on the sierra.dtd structure.
 */
public class SierraXMLCompletionProvider extends DefaultCompletionProvider implements CompletionProvider {

    // --- Extracted Elements (Tags) from sierra.dtd ---
    private static final List<String> XML_ELEMENTS = Arrays.asList(
            "activity-indicator", "image-pane", "color-chooser", "combo-box", "label", "list",
            "progress-bar", "scroll-pane", "separator", "slider", "spinner", "table", "tree",
            "spacer", "text-pane", "button", "text-area", "text-field", "toggle-button",
            "check-box", "formatted-text-field", "password-field", "radio-button", "menu-button",
            "number-field", "stack-panel", "validated-text-field", "column-panel", "row-panel",
            "suggestion-picker", "date-picker", "time-picker"
    );

    // --- Extracted Common Attributes from %UILoader, %JComponent, etc. ---
    private static final List<String> XML_ATTRIBUTES = Arrays.asList(
            "name", "group", "border", "padding", "weight", "size", "style", "styleClass",
            "background", "enabled", "focusable", "font", "foreground", "visible",
            "alignmentX", "alignmentY", "autoscrolls", "doubleBuffered", "opaque", "toolTipText",
            "actionCommand", "text", "horizontalAlignment", "verticalAlignment", "icon",
            "maximumRowCount", "value"
    );

    public SierraXMLCompletionProvider() {
        // Set up the autocompletion activation characters (for XML)
        // Auto-complete triggers on letters, '<' for new tags, and ' ' for attributes.
        setAutoActivationRules(true, "< ");

        // Add all tags as completions (with an example description)
        for (String tag : XML_ELEMENTS) {
            BasicCompletion c = new BasicCompletion(this, tag, "Sierra UI Element: <" + tag + ">");
            addCompletion(c);
        }

        // Add all attributes as completions
        for (String attr : XML_ATTRIBUTES) {
            // Using a different type/description to distinguish attributes from tags
            BasicCompletion c = new BasicCompletion(this, attr, "Sierra Attribute");
            addCompletion(c);
        }
    }
}
