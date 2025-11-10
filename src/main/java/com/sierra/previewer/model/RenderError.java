// src/main/java/com/sierra/previewer/model/RenderError.java
package com.sierra.previewer.model;

/**
 * A record to hold detailed information about a rendering failure. Implements
 * the error object described in Section VI.
 *
 * @param message A user-friendly error message.
 * @param exception The underlying exception for debugging.
 */
public record RenderError(String message, Exception exception) {

    @Override
    public String toString() {
        return message;
    }
}
