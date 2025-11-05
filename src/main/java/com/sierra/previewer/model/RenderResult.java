package com.sierra.previewer.model;

import javax.swing.JComponent;

/**
 * A sealed interface representing the outcome of the rendering process.
 * It will be either a Success or an Error.
 */
public sealed interface RenderResult {
    /**
     * Represents a successful render.
     *
     * @param component The root JComponent to display.
     */
    record Success(JComponent component) implements RenderResult {}

    /**
     * Represents a failed render.
     *
     * @param details The RenderError object.
     */
    record Error(RenderError details) implements RenderResult {}
}