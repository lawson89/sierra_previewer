package com.sierra.previewer.engine;

import com.sierra.previewer.model.RenderError;
import com.sierra.previewer.model.RenderResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.swing.*;
import org.httprpc.sierra.UILoader;

public class RenderingEngine {

    public RenderingEngine() {

    }

    /**
     * Renders the given XML text by first saving it to a target file.
     * If targetPath is not null, it saves to that file.
     * If targetPath is null, it saves to a temporary file.
     * * @param xmlText The XML content to render.
     * @param targetPath The file path to save the XML content to. Can be null.
     * @return The result of the rendering operation.
     */
    public RenderResult render(String xmlText, Path targetPath) { // MODIFIED SIGNATURE
        if (xmlText == null || xmlText.isBlank()) {
            return new RenderResult.Success(new JPanel());
        }
        
        Path savePath = targetPath;

        try {
            if (savePath == null) {
                // If no file is open, create a temporary file as fallback
                savePath = Files.createTempFile("sierrapreview", ".xml");
                // Only delete temporary files on exit
                savePath.toFile().deleteOnExit(); 
            }
            
            // Write the content to the chosen path (either the open file or the temp file)
            // This is the core change: saving to the targetPath instead of always a temp file.
            Files.write(savePath, xmlText.getBytes("UTF-8"), 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING, 
                    StandardOpenOption.WRITE);

            System.out.println("Data saved for rendering to file: " + savePath.toAbsolutePath());

            JComponent rootComponent = UILoader.load(savePath);
            return new RenderResult.Success(rootComponent);
        } catch (IOException e) {
            return new RenderResult.Error(new RenderError(e.getMessage(), e));
        }
    }
}