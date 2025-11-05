package com.sierra.previewer.engine;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

/**
 * Implements the TypeConverter utility.
 * Converts string attribute values into the proper Java types for setters.
 */
public class TypeConverter {

    public static Object convert(String value, Class<?> targetType) {
        try {
            if (targetType == String.class) {
                return value;
            }
            
            if (targetType == int.class || targetType == Integer.class) {
                // Check if the value is a constant name (case-insensitive)
                if (value.matches("[a-zA-Z_]+")) { 
                    return convertConstant(value);
                }
                // Otherwise, parse the number
                return Integer.parseInt(value);
            }
            
            if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value);
            }

            if (targetType == Dimension.class) {
                String[] parts = value.split(",");
                if (parts.length == 2) {
                    // Standard format: width,height (e.g., "100,20")
                    return new Dimension(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
                } else if (parts.length == 1) {
                    // Handles single-value dimension for spacer/size (e.g., "8")
                    int size = Integer.parseInt(value.trim());
                    return new Dimension(size, size);
                }
            }
            
            if (targetType == Color.class) {
                return Color.decode(value); 
            }
            
            if (targetType == Font.class) {
                return Font.decode(value);
            }
            
            throw new IllegalArgumentException("Unsupported type: " + targetType.getName());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert '" + value + "' to " + targetType.getName(), e);
        }
    }

    /**
     * Handles common Swing constants by converting the input value to uppercase.
     */
    private static int convertConstant(String value) {
        return switch (value.toUpperCase()) {
            // JFrame constants
            case "EXIT_ON_CLOSE" -> JFrame.EXIT_ON_CLOSE;
            case "DISPOSE_ON_CLOSE" -> JFrame.DISPOSE_ON_CLOSE;
            case "HIDE_ON_CLOSE" -> JFrame.HIDE_ON_CLOSE;
            case "DO_NOTHING_ON_CLOSE" -> JFrame.DO_NOTHING_ON_CLOSE;
            
            // SwingConstants alignment
            case "CENTER" -> SwingConstants.CENTER;
            case "LEFT" -> SwingConstants.LEFT;
            case "RIGHT" -> SwingConstants.RIGHT;
            case "TOP" -> SwingConstants.TOP;
            case "BOTTOM" -> SwingConstants.BOTTOM;
            
            // ListSelectionModel constants
            case "SINGLE" -> ListSelectionModel.SINGLE_SELECTION;
            case "SINGLE_INTERVAL" -> ListSelectionModel.SINGLE_INTERVAL_SELECTION;
            case "MULTIPLE", "MULTIPLE_INTERVAL" -> ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;

            default -> throw new IllegalArgumentException("Unknown constant: " + value);
        };
    }
}