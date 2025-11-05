// src/main/java/com/sierra/previewer/engine/ComponentFactory.java
package com.sierra.previewer.engine;

import java.awt.Component;
import java.awt.LayoutManager;
import java.util.HashMap;
import java.util.Map;
// Use only standard Swing imports
import javax.swing.*;

// Import necessary Sierra components (Layouts and unique widgets only)
import org.httprpc.sierra.ActivityIndicator;
import org.httprpc.sierra.ColumnPanel;
import org.httprpc.sierra.DatePicker;
import org.httprpc.sierra.ImagePane;
import org.httprpc.sierra.MenuButton;
import org.httprpc.sierra.NumberField;
import org.httprpc.sierra.RowPanel;
import org.httprpc.sierra.Spacer;
import org.httprpc.sierra.StackPanel;
import org.httprpc.sierra.SuggestionPicker;
import org.httprpc.sierra.TextPane;
import org.httprpc.sierra.TimePicker;
import org.httprpc.sierra.ValidatedTextField;

/**
 * ComponentFactory corrected to use standard JComponents for most tags, while
 * retaining Sierra's custom layouts and unique widgets.
 */
public class ComponentFactory {

    private final Map<String, Class<? extends Component>> componentMap = new HashMap<>();
    private final Map<String, Class<? extends LayoutManager>> layoutMap = new HashMap<>();

    public ComponentFactory() {
        // === Sierra Custom Components (Layouts/Unique Widgets) ===
        componentMap.put("activity-indicator", ActivityIndicator.class);
        componentMap.put("image-pane", ImagePane.class);
        componentMap.put("spacer", Spacer.class); // Unique utility
        componentMap.put("text-pane", TextPane.class);
        componentMap.put("menu-button", MenuButton.class);
        componentMap.put("stack-panel", StackPanel.class);
        componentMap.put("column-panel", ColumnPanel.class); // Layout
        componentMap.put("row-panel", RowPanel.class); // Layout
        componentMap.put("date-picker", DatePicker.class);
        componentMap.put("time-picker", TimePicker.class);
        componentMap.put("suggestion-picker", SuggestionPicker.class);
        componentMap.put("number-field", NumberField.class);
        componentMap.put("validated-text-field", ValidatedTextField.class);

        // === Standard Swing Components ===
        // All standard UI widgets map to their javax.swing counterparts
        componentMap.put("label", JLabel.class);
        componentMap.put("button", JButton.class);
        componentMap.put("text-area", JTextArea.class);
        componentMap.put("text-field", JTextField.class);
        componentMap.put("toggle-button", JToggleButton.class);
        componentMap.put("check-box", JCheckBox.class);
        componentMap.put("password-field", JPasswordField.class);
        componentMap.put("radio-button", JRadioButton.class);

        componentMap.put("color-chooser", JColorChooser.class);
        componentMap.put("combo-box", JComboBox.class);
        componentMap.put("list", JList.class);
        componentMap.put("progress-bar", JProgressBar.class);
        componentMap.put("scroll-pane", JScrollPane.class);
        componentMap.put("separator", JSeparator.class);
        componentMap.put("slider", JSlider.class);
        componentMap.put("spinner", JSpinner.class);
        componentMap.put("table", JTable.class);
        componentMap.put("tree", JTree.class);
        componentMap.put("formatted-text-field", JFormattedTextField.class);

        // Standard Swing Layouts
        layoutMap.put("borderLayout", java.awt.BorderLayout.class);
        layoutMap.put("flowLayout", java.awt.FlowLayout.class);
        layoutMap.put("gridLayout", java.awt.GridLayout.class);
        layoutMap.put("boxLayout", javax.swing.BoxLayout.class);
    }

    public Component createComponent(String tagName) throws Exception {
        Class<? extends Component> componentClass = componentMap.get(tagName);
        if (componentClass == null) {
            if ("panel".equals(tagName)) {
                return new JPanel();
            }
            throw new ClassNotFoundException("Unknown component tag: <" + tagName + ">");
        }
        System.out.println("Creating component -> " + componentClass);
        return componentClass.getConstructor().newInstance();
    }

    public LayoutManager createLayoutManager(String tagName) throws Exception {
        Class<? extends LayoutManager> layoutClass = layoutMap.get(tagName);
        if (layoutClass == null) {
            throw new ClassNotFoundException("Unknown layout tag: <" + tagName + ">");
        }
        return layoutClass.getConstructor().newInstance();
    }

    public boolean isLayoutTag(String tagName) {
        return layoutMap.containsKey(tagName);
    }

    public boolean isSierraPanel(Component comp) {
        return (comp instanceof ColumnPanel || comp instanceof RowPanel || comp instanceof StackPanel);
    }
}
