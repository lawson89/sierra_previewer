// src/main/java/com/sierra/previewer/model/RenderError.java
package com.sierra.previewer.model;

/**
 * A record to hold detailed information about a rendering failure.
 * Implements the error object described in Section VI.
 *
 * @param message A user-friendly error message.
 * @param lineNumber The line number in the XML where the error occurred (if available).
 * @param exception The underlying exception for debugging.
 */
public record RenderError(String message, int lineNumber, Exception exception) {
    @Override
    public String toString() {
        if (lineNumber > 0) {
            return String.format("[Line %d] %s", lineNumber, message);
        }
        return message;
    }
}