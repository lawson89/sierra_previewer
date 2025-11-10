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

// --- Import correct Sierra classes ---
import org.httprpc.sierra.UILoader;
import org.httprpc.sierra.Outlet;

/**
 * The main application window for the Sierra UI Previewer.
 * UI is defined in MainFrame.xml and loaded by UILoader.
 * This class contains the wiring and business logic.
 */
public class MainFrame extends JFrame {

    // --- Subsystems ---
    private final RenderingEngine renderingEngine;
    private final Timer debounceTimer;

    // --- UI Components (Injected by Sierra) ---
    // These fields are populated by UILoader based on the 'name' tags
    // in the MainFrame.xml file, using the @Outlet annotation.
    
    @Outlet
    private JScrollPane editorScrollPane; // The <scroll-pane> placeholder

    @Outlet
    private JPanel previewPanel; // The <column-panel>

    @Outlet
    private JLabel statusBar;

    // --- Manually Created Components ---
    // RSyntaxTextArea is a custom component not in the DTD,
    // so we create it manually and add it to our placeholder.
    private RSyntaxTextArea editorPane;

    public MainFrame() {
        super("Sierra UI Previewer"); // Title is set here
        this.renderingEngine = new RenderingEngine();

        // 1. Load the UI from the declarative .xml file
        // We load the XML definition into the JFrame's content pane.
        // This replaces setupEditorPane(), setupPreviewPane(), and setupMainLayout()
        setContentPane(UILoader.load(this, "MainFrame.xml")); // Assumes file is in resources

        // 2. Manually set up components not supported by the DTD
        setupCustomEditor();

        // 3. **CRITICAL:** The 'displayRenderResult' method relies on BorderLayout.
        // The <column-panel> (previewPanel) loaded by UILoader uses BoxLayout.
        // We must reset the layout manager here so the logic in 
        // displayRenderResult works correctly.
        previewPanel.setLayout(new BorderLayout());

        // 4. Setup Control Subsystem (Identical to original)
        this.debounceTimer = setupDebounceTimer();

        // 5. Wire editor events to the control layer (Identical to original)
        // This works because 'editorPane' was created in setupCustomEditor()
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

        // 6. Trigger an initial render (Identical to original)
        triggerRender();
    }

    /**
     * Creates the custom RSyntaxTextArea and adds it to the
     * <scroll-pane> placeholder that Sierra injected.
     */
    private void setupCustomEditor() {
        editorPane = new RSyntaxTextArea(25, 80);
        editorPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        editorPane.setCodeFoldingEnabled(true);
        editorPane.setAntiAliasingEnabled(true);
        
        // Add our custom editor to the placeholder injected by UILoader
        editorScrollPane.setViewportView(editorPane);
    }

    //
    // --- ALL LOGIC METHODS BELOW ARE 100% IDENTICAL TO YOUR ORIGINAL CODE ---
    //
    // (setupEditorPane, setupPreviewPane, setupMainLayout are removed)
    //

    /**
     * Implements the debounce mechanism (Identical)
     */
    private Timer setupDebounceTimer() {
        // Create the timer with the recommended 750ms delay
        Timer timer = new Timer(750, (e) -> triggerRender());
        timer.setRepeats(false); // Make it a one-shot timer
        return timer;
    }

    /**
     * Kicks off the rendering process on a background thread. (Identical)
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
     * (Identical to original, and works because we set previewPanel's
     * layout to BorderLayout in the constructor).
     */
    private void displayRenderResult(RenderResult result) {
        switch (result) {
            case RenderResult.Success success -> {
                previewPanel.removeAll();
                JComponent component = success.component();
                System.out.println("Got back: " + component.getClass().getName());
                previewPanel.add(success.component(), BorderLayout.CENTER); // This now works
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
     * thread. (Identical)
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