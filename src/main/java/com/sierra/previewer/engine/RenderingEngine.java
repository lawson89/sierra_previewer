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

    public RenderResult render(String xmlText) {
        if (xmlText == null || xmlText.isBlank()) {
            return new RenderResult.Success(new JPanel());
        }

        try {

            Path tempFilePath = Files.createTempFile("sierrapreview", ".xml");

            Files.write(tempFilePath, xmlText.getBytes("UTF-8"), StandardOpenOption.WRITE);

            System.out.println("Data saved to temporary file: " + tempFilePath.toAbsolutePath());

            // Optionally, delete the file on exit
            tempFilePath.toFile().deleteOnExit();

            JComponent rootComponent = UILoader.load(tempFilePath);
            return new RenderResult.Success((JComponent) rootComponent);
        } catch (IOException e) {
            return new RenderResult.Error(new RenderError(e.getMessage(), e));
        }
    }
}
