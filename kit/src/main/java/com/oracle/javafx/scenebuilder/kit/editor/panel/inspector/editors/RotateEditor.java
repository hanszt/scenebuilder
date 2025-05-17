/*
 * Copyright (c) 2016, Gluon and/or its affiliates.
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors;

import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;

import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

/**
 * Editor for bounded double properties. (e.g. 0 &lt;= opacity &lt;= 1)
 *
 * 
 */
public class RotateEditor extends PropertyEditor {

    @FXML
    private TextField rotateTf;

    @FXML
    private Button rotatorDial;

    @FXML
    private Button rotatorHandle;

    private Parent root;
    private int roundingFactor = 10; // 1 decimal
    private boolean updateFromRotator = false;

    public RotateEditor(final ValuePropertyMetadata propMeta, final Set<Class<?>> selectedClasses) {
        super(propMeta, selectedClasses);
        initialize();
    }
    
    // Method to please FindBugs
    private void initialize() {
        root = EditorUtils.loadFxml("RotateEditor.fxml", this);

        //
        // Text field
        //
        final EventHandler<ActionEvent> valueListener = event -> {
            if (isHandlingError()) {
                // Event received because of focus lost due to error dialog
                return;
            }
            final var valStr = rotateTf.getText();
            final double valDouble;
            try {
                valDouble = Double.parseDouble(valStr);
            } catch (final NumberFormatException e) {
                handleInvalidValue(valStr);
                return;
            }
            if (!((DoublePropertyMetadata) getPropertyMeta()).isValidValue(valDouble)) {
                handleInvalidValue(valDouble);
                return;
            }
            rotate(valDouble);
            rotateTf.selectAll();
            userUpdateValueProperty(valDouble);

        };
        setNumericEditorBehavior(this, rotateTf, valueListener, false);

        // Select all text when this editor is selected
        rotateTf.setOnMousePressed(event -> rotateTf.selectAll());
        rotateTf.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue) {
                rotateTf.selectAll();
            }
        }));
    }

    @Override
    public Node getValueEditor() {
        return super.handleGenericModes(root);
    }

    @Override
    public Object getValue() {
        return EditorUtils.round(rotatorHandle.getRotate(), roundingFactor);
    }

    @Override
    public void setValue(final Object value) {
        setValueGeneric(value);
        if (isSetValueDone()) {
            return;
        }

        assert (value instanceof Double);
        rotate((Double) value);
    }

    @Override
    public void reset(final ValuePropertyMetadata propMeta, final Set<Class<?>> selectedClasses) {
        super.reset(propMeta, selectedClasses);
//        setValueGeneric(propMeta.getDefaultValueObject());
    }

    @Override
    protected void valueIsIndeterminate() {
        handleIndeterminate(rotateTf);
    }

    @FXML
    void rotatorPressed(final MouseEvent e) {
        rotatorDragged(e);
    }

    @FXML
    void rotatorReleased(final MouseEvent e) {
        userUpdateValueProperty(getValue());
    }

    @FXML
    public void rotatorDragged(final MouseEvent e) {
//        System.out.println("in RotateEditor.rotatorDragged");
        updateFromRotator = true;
        final var p = rotatorDial.getParent();
        final var b = rotatorDial.getLayoutBounds();
        final Double centerX = b.getMinX() + (b.getWidth() / 2);
        final Double centerY = b.getMinY() + (b.getHeight() / 2);
        final var center = p.localToParent(centerX, centerY);
        final var mouse = p.localToParent(e.getX(), e.getY());
        final Double deltaX = mouse.getX() - center.getX();
        final Double deltaY = mouse.getY() - center.getY();
        final Double radians = Math.atan2(deltaY, deltaX);
        rotate(Math.toDegrees(radians));
        userUpdateTransientValueProperty(getValue());
        updateFromRotator = false;
    }

    private void rotate(final Double degrees) {
        rotatorHandle.setRotate(degrees);
        if (updateFromRotator) {
            // Round the value
            rotateTf.setText(EditorUtils.valAsStr(getValue()));
        } else {
            // Do not round the value (more decimals may be required)
            rotateTf.setText(EditorUtils.valAsStr(degrees));
        }
    }

    @Override
    public void requestFocus() {
        EditorUtils.doNextFrame(() -> rotateTf.requestFocus());
    }

}
