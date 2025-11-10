package com.sierra.previewer;

import com.sierra.previewer.engine.RenderingEngine;
import com.sierra.previewer.model.RenderError;
import com.sierra.previewer.model.RenderResult;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

// --- Import correct Sierra classes ---
import org.httprpc.sierra.UILoader;
import org.httprpc.sierra.Outlet;

/**
 * The main application window for the Sierra UI Previewer. UI is defined in
 * MainFrame.xml and loaded by UILoader. This class contains the wiring and
 * business logic.
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

    @Outlet
    private JLabel filePathLabel; // The <label> for the file path

    // --- Manually Created Components ---
    // RSyntaxTextArea is a custom component not in the DTD,
    // so we create it manually and add it to our placeholder.
    private RSyntaxTextArea editorPane;

    private final JFileChooser fileChooser;

    public MainFrame() {
        super("Sierra UI Previewer"); // Title is set here
        this.renderingEngine = new RenderingEngine();

        setContentPane(UILoader.load(this, "MainFrame.xml")); // Assumes file is in resources

        setupMenuBar();
        
        this.fileChooser = new JFileChooser();
        FileNameExtensionFilter xmlFilter = new FileNameExtensionFilter("XML Files (*.xml)", "xml");
        fileChooser.setFileFilter(xmlFilter);
        fileChooser.setAcceptAllFileFilterUsed(false); // Only show XML files

        // Manually set up components not supported by the DTD
        setupCustomEditor();

        previewPanel.setLayout(new BorderLayout());

        this.debounceTimer = setupDebounceTimer();

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
     * Creates and sets the application's menu bar. This is done manually as
     * JMenuBar is not in the Sierra DTD.
     */
    private void setupMenuBar() {
        // 1. Create the main menu bar
        JMenuBar menuBar = new JMenuBar();

        // 2. Create the "File" menu
        JMenu fileMenu = new JMenu("File");

        // 3. Create the "Open" item and add it to "File"
        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> {
            // Show the file chooser
            int result = fileChooser.showOpenDialog(MainFrame.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                loadFile(selectedFile);
            }
        });
        fileMenu.add(openItem);

        // 4. Create the "About" menu
        JMenu aboutMenu = new JMenu("About");

        // 5. Create the "About" item (to go inside the "About" menu)
        // We add an item inside the menu, as the JMenu itself isn't clickable.
        JMenuItem aboutItem = new JMenuItem("About Previewer");
        aboutItem.addActionListener(e -> {
            // Show the requested message box
            JOptionPane.showMessageDialog(
                    this, // Parent component
                    "Sierra DSL Previewer", // Message
                    "About", // Dialog title
                    JOptionPane.INFORMATION_MESSAGE
            );
        });
        aboutMenu.add(aboutItem);

        // 6. Add both menus to the menu bar
        menuBar.add(fileMenu);
        menuBar.add(aboutMenu);

        // 7. Set this menu bar on the JFrame
        this.setJMenuBar(menuBar);
    }

    /**
     * Kicks off a SwingWorker to load a file's content
     * onto a background thread.
     */
    private void loadFile(File file) {
        filePathLabel.setText("Loading " + file.getName() + "...");
        FileLoaderWorker worker = new FileLoaderWorker(file.toPath(), this::displayFileContent);
        worker.execute();
    }

    /**
     * Callback that runs on the EDT after the file is loaded.
     * Updates the UI with the file content and path.
     */
    private void displayFileContent(FileLoadResult result) {
        if (result instanceof FileLoadResult.Success success) {
            editorPane.setText(success.content());
            editorPane.setCaretPosition(0); // Scroll to top
            filePathLabel.setText(success.path().toAbsolutePath().toString());
            
            // Re-render the preview with the new content
            triggerRender(); 
        } else if (result instanceof FileLoadResult.Error error) {
            filePathLabel.setText("Error loading file.");
            JOptionPane.showMessageDialog(
                    this,
                    "Could not read file:\n" + error.exception().getMessage(),
                    "File Load Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    /**
     * A sealed record interface to pass file loading results
     * back to the EDT.
     */
    private sealed interface FileLoadResult {
        record Success(String content, Path path) implements FileLoadResult {}
        record Error(Exception exception) implements FileLoadResult {}
    }

    /**
     * The SwingWorker implementation that reads the file on a background
     * thread.
     */
    private static class FileLoaderWorker extends SwingWorker<FileLoadResult, Void> {

        private final Path filePath;
        private final Consumer<FileLoadResult> callback;

        FileLoaderWorker(Path filePath, Consumer<FileLoadResult> callback) {
            this.filePath = filePath;
            this.callback = callback;
        }

        @Override
        protected FileLoadResult doInBackground() throws Exception {
            // This runs on a background thread
            try {
                String content = Files.readString(filePath);
                return new FileLoadResult.Success(content, filePath);
            } catch (Exception e) {
                return new FileLoadResult.Error(e);
            }
        }

        @Override
        protected void done() {
            // This runs on the EDT
            try {
                FileLoadResult result = get();
                callback.accept(result);
            } catch (InterruptedException | ExecutionException e) {
                callback.accept(new FileLoadResult.Error(e));
            }
        }
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
     * It handles the result from the rendering engine. (Identical to original,
     * and works because we set previewPanel's layout to BorderLayout in the
     * constructor).
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
