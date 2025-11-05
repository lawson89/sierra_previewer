package com.sierra.previewer.engine;

import com.sierra.previewer.model.RenderError;
import com.sierra.previewer.model.RenderResult;

import javax.swing.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.awt.*;
import java.io.StringReader;
import java.lang.reflect.Method;

public class RenderingEngine {

    private final ComponentFactory componentFactory = new ComponentFactory();
    private final XMLInputFactory xmlInputFactory;

    public RenderingEngine() {
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    }

    public RenderResult render(String xmlText) {
        if (xmlText == null || xmlText.isBlank()) {
            return new RenderResult.Success(new JPanel());
        }

        XMLStreamReader reader = null;
        try {
            reader = xmlInputFactory.createXMLStreamReader(new StringReader(xmlText));

            while (reader.hasNext()) {
                if (reader.isStartElement()) {
                    break;
                }
                reader.next();
            }

            if (!reader.isStartElement()) {
                throw new Exception("No root element found in XML.");
            }

            Component rootComponent = parseNode(reader, null);

            if (!(rootComponent instanceof JComponent)) {
                throw new Exception("Root element must be a JComponent.");
            }

            return new RenderResult.Success((JComponent) rootComponent);

        } catch (Exception e) {
            int line = -1;
            if (reader != null && reader.getLocation() != null) {
                line = reader.getLocation().getLineNumber();
            }
            return new RenderResult.Error(new RenderError(e.getMessage(), line, e));
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                // ignore close error
            }
        }
    }

    private Component parseNode(XMLStreamReader reader, Container parent) throws Exception {
        if (!reader.isStartElement()) {
            throw new IllegalStateException("parseNode called without a START_ELEMENT");
        }

        String tagName = reader.getLocalName();
        Component component = null;

        if (componentFactory.isLayoutTag(tagName)) {
            if (parent == null) throw new Exception("Layout tag <" + tagName + "> cannot be root.");
            LayoutManager layout = componentFactory.createLayoutManager(tagName);
            applyProperties(layout, reader);
            parent.setLayout(layout);
        } else {
            component = componentFactory.createComponent(tagName);
            applyProperties(component, reader);

            if (parent != null) {
                if (parent instanceof JScrollPane) {
                    ((JScrollPane) parent).setViewportView(component);
                } else {
                    String constraint = getAttribute(reader, "layout.constraint");
                    Object constraintObj = parseLayoutConstraint(constraint);
                    parent.add(component, constraintObj);
                }
            }

            if (component instanceof Container) {
                parseChildren(reader, (Container) component);
            }
        }
        return component;
    }

    private void parseChildren(XMLStreamReader reader, Container parent) throws Exception {
        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                parseNode(reader, parent);
            } else if (reader.isEndElement()) {
                return;
            }
        }
    }

    /**
     * Applies XML attributes, handling special Sierra/FlatLaf properties first,
     * then falling back to generic setter reflection.
     */
    private void applyProperties(Object target, XMLStreamReader reader) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attrName = reader.getAttributeLocalName(i);
            String attrValue = reader.getAttributeValue(i);

            if ("layout.constraint".equals(attrName)) {
                continue;
            }

            // 1. Intercept special Sierra/FlatLaf attributes (putClientProperty or setPreferredSize)
            if (applySpecialAttribute(target, attrName, attrValue)) {
                continue;
            }

            // 2. Fallback to standard setter reflection
            findAndInvokeSetter(target, attrName, attrValue);
        }
    }

    /**
     * Implements the putClientProperty logic based on the Sierra UILoader snippet.
     * @return true if the attribute was handled, false otherwise.
     */
    private boolean applySpecialAttribute(Object target, String name, String value) {
        System.out.println("Apply special attributes " + target.getClass() + " " + name + "=" + value);
        if (!(target instanceof JComponent component)) {
            // Only JComponents can have client properties
            return false;
        }

        switch (name) {
            case "size":
                // Corresponds to component.setPreferredSize(parseSize(value))
                try {
                    Dimension size = (Dimension) TypeConverter.convert(value, Dimension.class);
                    component.setPreferredSize(size);
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to set preferred size for component using 'size=" + value + "'. Check TypeConverter logic.", e);
                }
            case "style":
            case "styleClass":
                // Corresponds to component.putClientProperty("FlatLaf.style", value);
                component.putClientProperty(String.format("FlatLaf.%s", name), value);
                return true;
                
            case "placeholderText":
                // Corresponds to component.putClientProperty("JTextField.placeholderText", value);
                component.putClientProperty(String.format("%s.%s", JTextField.class.getSimpleName(), name), value);
                return true;
                
            case "showClearButton":
                // Corresponds to component.putClientProperty("JTextField.showClearButton", Boolean.valueOf(value));
                component.putClientProperty(String.format("%s.%s", JTextField.class.getSimpleName(), name), Boolean.valueOf(value));
                return true;
                
            case "leadingIcon":
            case "trailingIcon":
                 // Corresponds to component.putClientProperty("JTextField.leadingIcon", getIcon(value));
                 // Assuming getIcon just returns the string value for now as we don't have icon loading logic
                 component.putClientProperty(String.format("%s.%s", JTextField.class.getSimpleName(), name), value);
                 return true;

            default:
                // Not a special attribute
                return false;
        }
    }

    /**
     * Robust reflection logic: tries to convert the attribute value to match any
     * single-argument setter method, solving the primitive type ambiguity.
     */
    private void findAndInvokeSetter(Object target, String attrName, String attrValue) {
        String setterName = "set" + attrName.substring(0, 1).toUpperCase() + attrName.substring(1);
        System.out.println("Invoking " + setterName + " on " + target.getClass());

        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                
                Class<?> paramType = method.getParameterTypes()[0];
                
                try {
                    // Attempt to convert the string value to the setter's parameter type (e.g., String to Font)
                    Object convertedValue = TypeConverter.convert(attrValue, paramType);
                    
                    // If conversion succeeded, invoke the method.
                    method.invoke(target, convertedValue);
                    return; // SUCCESS: Exit method after successful invocation
                    
                } catch (IllegalArgumentException e) {
                    // Conversion failed for this specific parameter type (e.g., trying to convert "8" to boolean).
                    // Continue the loop to try the next possible setter signature (e.g., setSize(Dimension)).
                    System.out.println(e);
                } catch (Exception e) {
                    // Invocation failed (e.g., reflection or security error). Report and stop for this attribute.
                    throw new RuntimeException("Failed to invoke setter '" + setterName + "' on " + target.getClass().getSimpleName() + " for value '" + attrValue + "'", e);
                }
            }
        }
        
        // Fallback for general properties like 'name'
        if ("name".equals(attrName) && target instanceof Component) {
            ((Component) target).setName(attrValue);
            return;
        }
        
        System.out.println("Unable to invoke setter");
    }

    private String getAttribute(XMLStreamReader reader, String name) {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            if (name.equals(reader.getAttributeLocalName(i))) {
                return reader.getAttributeValue(i);
            }
        }
        return null;
    }

    private Object parseLayoutConstraint(String constraint) {
        if (constraint == null) {
            return null;
        }
        return switch (constraint.toUpperCase()) {
            case "CENTER" -> BorderLayout.CENTER;
            case "NORTH" -> BorderLayout.NORTH;
            case "SOUTH" -> BorderLayout.SOUTH;
            case "EAST" -> BorderLayout.EAST;
            case "WEST" -> BorderLayout.WEST;
            default -> constraint;
        };
    }
}