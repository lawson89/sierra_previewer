// src/main/java/com/sierra/previewer/SierraPreviewerApp.java
package com.sierra.previewer;

import javax.swing.*;

public class SierraPreviewerApp {

    public static void main(String[] args) {
        // Run all UI code on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                // Set a modern Look and Feel if available
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Could not set System Look and Feel.");
            }
            
            MainFrame frame = new MainFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);
            frame.setLocationRelativeTo(null); // Center on screen
            frame.setVisible(true);
        });
    }
}