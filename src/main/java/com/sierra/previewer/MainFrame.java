package com.sierra.previewer;

import com.sierra.previewer.engine.RenderingEngine;
import com.sierra.previewer.model.RenderError;
import com.sierra.previewer.model.RenderResult;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * The main application window for the Sierra UI Previewer. This class combines
 * all subsystems: - Editor (RSyntaxTextArea) - Visualization (previewPanel) -
 * Control (debounceTimer) - Error Handling (JOptionPane)
 */
public class MainFrame extends JFrame {

    // --- Subsystems ---
    private final RenderingEngine renderingEngine;
    private final Timer debounceTimer;

    // --- UI Components ---
    private RSyntaxTextArea editorPane;
    private JPanel previewPanel; // Visualization Subsystem
    private JSplitPane splitPane;
    private JLabel statusBar;

    public MainFrame() {
        super("Sierra UI Previewer");
        this.renderingEngine = new RenderingEngine();

        // 1. Setup Editor Subsystem (Section II)
        setupEditorPane();

        // 2. Setup Visualization Subsystem (Section V)
        setupPreviewPane();

        // 3. Setup Main Layout
        setupMainLayout();

        // 4. Setup Control Subsystem (Section III)
        this.debounceTimer = setupDebounceTimer();

        // 5. Wire editor events to the control layer
        editorPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                debounceTimer.restart();
                statusBar.setText("Typing...");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounceTimer.restart();
                statusBar.setText("Typing...");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Style changes, not relevant for text
            }
        });

        // Trigger an initial render
        triggerRender();
    }

    private void setupEditorPane() {
        editorPane = new RSyntaxTextArea(25, 80);
        editorPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        editorPane.setCodeFoldingEnabled(true);
        editorPane.setAntiAliasingEnabled(true);
    }

    private void setupPreviewPane() {
        previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel welcomeLabel = new JLabel("Preview will appear here.", SwingConstants.CENTER);
        welcomeLabel.setForeground(Color.GRAY);
        previewPanel.add(welcomeLabel, BorderLayout.CENTER);
    }

    private void setupMainLayout() {
        RTextScrollPane editorScrollPane = new RTextScrollPane(editorPane);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorScrollPane, previewPanel);
        splitPane.setDividerLocation(0.5);
        splitPane.setResizeWeight(0.5);

        statusBar = new JLabel("Ready.");
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Implements the debounce mechanism
     */
    private Timer setupDebounceTimer() {
        // Create the timer with the recommended 750ms delay
        Timer timer = new Timer(750, (e) -> triggerRender());
        timer.setRepeats(false); // Make it a one-shot timer
        return timer;
    }

    /**
     * Kicks off the rendering process on a background thread.
     */
    private void triggerRender() {
        statusBar.setText("Rendering...");
        String xmlText = editorPane.getText();

        // Use SwingWorker for background rendering
        RenderWorker worker = new RenderWorker(xmlText, renderingEngine, this::displayRenderResult);
        worker.execute();
    }

    /**
     * This is the callback that runs on the EDT when the SwingWorker is done.
     * It handles the result from the rendering engine.
     */
    private void displayRenderResult(RenderResult result) {
        switch (result) {
            case RenderResult.Success success -> {
                previewPanel.removeAll();
                JComponent component = success.component();
                System.out.println("Got back: " + component.getClass().getName());
                previewPanel.add(success.component(), BorderLayout.CENTER);
                previewPanel.revalidate();
                previewPanel.repaint();
                
                // Update frame title
                this.setTitle("Sierra UI Previewer");
                statusBar.setText("Render successful.");
                
            }
            case RenderResult.Error error -> {
                String errorMessage = error.details().toString();
                System.out.println(errorMessage);
                statusBar.setText("Error: " + errorMessage);
                
                // Show the modal error dialog as specified
                JOptionPane.showMessageDialog(
                        this,
                        errorMessage,
                        "Render Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            default -> {
            }
        }
    }

    /**
     * The SwingWorker implementation that runs the rendering on a background
     * thread.
     */
    private static class RenderWorker extends SwingWorker<RenderResult, Void> {

        private final String xmlText;
        private final RenderingEngine engine;
        private final Consumer<RenderResult> callback; // To be called on EDT

        RenderWorker(String xmlText, RenderingEngine engine, Consumer<RenderResult> callback) {
            this.xmlText = xmlText;
            this.engine = engine;
            this.callback = callback;
        }

        @Override
        protected RenderResult doInBackground() throws Exception {
            // This runs on a background thread
            return engine.render(xmlText);
        }

        @Override
        protected void done() {
            // This runs on the EDT (Section V.2)
            try {
                RenderResult result = get();
                callback.accept(result);
            } catch (InterruptedException | ExecutionException e) {
                System.out.println(e);
                callback.accept(new RenderResult.Error(new RenderError(e.getMessage(), e)));
            }
        }
    }
}
