/*
 * Copyright (c) 2019, 2022, Gluon and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.fxom;

import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoubleArrayPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.ListValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 *
 *
 */
class FXOMRefresher {

    public void refresh(final FXOMDocument document) {
        String fxmlText = null;
        try {
            fxmlText = document.getFxmlText(false);
            final var newDocument
                    = new FXOMDocument(fxmlText,
                    document.getLocation(),
                    document.getClassLoader(),
                    document.getResources());
            final var backup = new TransientStateBackup(document);
            // if the refresh should not take place (e.g. due to an error), remove a property from intrinsic
            if (newDocument.getSceneGraphRoot() == null && newDocument.getFxomRoot() == null) {
                removeIntrinsicProperty(document);
            } else {
                refreshDocument(document, newDocument);
            }
            backup.restore();
            synchronizeDividerPositions(document);
        } catch (final RuntimeException | IOException x) {
            final var sb = new StringBuilder();
            sb.append("Bug in ");
            sb.append(getClass().getSimpleName());
            if (fxmlText != null) {
                try {
                    final var fxmlFile = File.createTempFile("DTL-5996-", ".fxml");
                    try (final var pw = new PrintWriter(fxmlFile, StandardCharsets.UTF_8)) {
                        pw.write(fxmlText);
                        sb.append(": FXML dumped in ");
                        sb.append(fxmlFile.getPath());
                    }
                } catch (final IOException xx) {
                    sb.append(": no FXML dumped");
                }
            } else {
                sb.append(": no FXML dumped");
            }
            throw new IllegalStateException(sb.toString(), x);
        }
    }

    private void removeIntrinsicProperty(final FXOMDocument document) {
        final var fxomRoot = (FXOMInstance) document.getFxomRoot();
        if (fxomRoot != null) {
            final var propertyC = (FXOMPropertyC) fxomRoot.getProperties().get(new PropertyName("children"));
            if (propertyC.getValues().getFirst() instanceof final FXOMIntrinsic fxomIntrinsic) {
                fxomIntrinsic.removeCharsetProperty();
            }
        }
    }

    /*
     * Private (stylesheet)
     */

    private void refreshDocument(final FXOMDocument currentDocument, final FXOMDocument newDocument) {
        // Transfers scene graph object from newDocument to currentDocument
        currentDocument.setSceneGraphRoot(newDocument.getSceneGraphRoot());
        // Transfers display node from newDocument to currentDocument
        currentDocument.setDisplayNode(newDocument.getDisplayNode());
        // Transfers display stylesheets from newDocument to currentDocument
        currentDocument.setDisplayStylesheets(newDocument.getDisplayStylesheets());
        // Simulates Scene's behavior : automatically adds "root" styleclass if
        // if the scene graph root is a Parent instance or wraps a Parent instance
        if (currentDocument.getSceneGraphRoot() instanceof final Parent rootParent) {
            rootParent.getStyleClass().addFirst("root");
        } else if (currentDocument.getSceneGraphRoot() instanceof Scene
                || currentDocument.getSceneGraphRoot() instanceof Window) {
            final var displayNode = currentDocument.getDisplayNode();
            if (displayNode != null && displayNode instanceof Parent) {
                displayNode.getStyleClass().addFirst("root");
            }
        }
        // Recurses
        if (currentDocument.getFxomRoot() != null) {
            refreshFxomObject(currentDocument.getFxomRoot(), newDocument.getFxomRoot());
        }
    }


    private void refreshFxomObject(final FXOMObject currentObject, final FXOMObject newObject) {
        assert currentObject != null;
        assert newObject != null;
        assert currentObject.getClass() == newObject.getClass();
        currentObject.setSceneGraphObject(newObject.getSceneGraphObject());
        if (currentObject instanceof FXOMInstance) {
            refreshFxomInstance((FXOMInstance) currentObject, (FXOMInstance) newObject);
        } else if (currentObject instanceof FXOMCollection) {
            refreshFxomCollection((FXOMCollection) currentObject, (FXOMCollection) newObject);
        } else if (currentObject instanceof FXOMIntrinsic) {
            refreshFxomIntrinsic((FXOMIntrinsic) currentObject, (FXOMIntrinsic) newObject);
        } else {
            assert false : "Unexpected fxom object " + currentObject;
        }

//        assert currentObject.equals(newObject) : "currentValue=" + currentObject +
//                                               "  newValue=" + newObject;
    }


    private void refreshFxomInstance(final FXOMInstance currentInstance, final FXOMInstance newInstance) {
        assert currentInstance != null;
        assert newInstance != null;
        assert currentInstance.getClass() == newInstance.getClass();
        currentInstance.setDeclaredClass(newInstance.getDeclaredClass());
        final var currentNames = currentInstance.getProperties().keySet();
        final var newNames = newInstance.getProperties().keySet();
        assert currentNames.equals(newNames);
        for (final var name : currentNames) {
            final var currentProperty = currentInstance.getProperties().get(name);
            final var newProperty = newInstance.getProperties().get(name);
            refreshFxomProperty(currentProperty, newProperty);
        }
    }

    private void refreshFxomCollection(final FXOMCollection currentCollection, final FXOMCollection newCollection) {
        assert currentCollection != null;
        assert newCollection != null;
        currentCollection.setDeclaredClass(newCollection.getDeclaredClass());
        refreshFxomObjects(currentCollection.getItems(), newCollection.getItems());
    }

    private void refreshFxomIntrinsic(final FXOMIntrinsic currentIntrinsic, final FXOMIntrinsic newIntrinsic) {
        assert currentIntrinsic != null;
        assert newIntrinsic != null;
        currentIntrinsic.setSourceSceneGraphObject(newIntrinsic.getSourceSceneGraphObject());
        currentIntrinsic.getProperties().clear();
        currentIntrinsic.fillProperties(newIntrinsic.getProperties());
    }

    private void refreshFxomProperty(final FXOMProperty currentProperty, final FXOMProperty newProperty) {
        assert currentProperty != null;
        assert newProperty != null;
        assert currentProperty.getName().equals(newProperty.getName());
        if (currentProperty instanceof FXOMPropertyT) {
            assert newProperty instanceof FXOMPropertyT;
            assert ((FXOMPropertyT) currentProperty).getValue().equals(((FXOMPropertyT) newProperty).getValue());
        } else {
            assert currentProperty instanceof FXOMPropertyC;
            assert newProperty instanceof FXOMPropertyC;
            final var currentPC = (FXOMPropertyC) currentProperty;
            final var newPC = (FXOMPropertyC) newProperty;
            refreshFxomObjects(currentPC.getValues(), newPC.getValues());
        }
    }


    private void refreshFxomObjects(final List<FXOMObject> currentObjects, final List<FXOMObject> newObjects) {
        assert currentObjects != null;
        assert newObjects != null;
        assert currentObjects.size() == newObjects.size();
        for (int i = 0, count = currentObjects.size(); i < count; i++) {
            final var currentObject = currentObjects.get(i);
            final var newObject = newObjects.get(i);
            if (currentObject instanceof FXOMIntrinsic || newObject instanceof FXOMIntrinsic) {
                handleRefreshIntrinsic(currentObject, newObject);
            } else {
                refreshFxomObject(currentObject, newObject);
            }
        }
    }

    private void handleRefreshIntrinsic(final FXOMObject currentObject, final FXOMObject newObject) {
        if (currentObject instanceof FXOMIntrinsic && newObject instanceof FXOMIntrinsic) {
            refreshFxomObject(currentObject, newObject);
        } else if (newObject instanceof FXOMIntrinsic) {
            final var fxomInstance = getFxomInstance((FXOMIntrinsic) newObject);
            refreshFxomObject(currentObject, fxomInstance);
        } else if (currentObject instanceof FXOMIntrinsic) {
            final var fxomInstance = getFxomInstance((FXOMIntrinsic) currentObject);
            refreshFxomObject(fxomInstance, newObject);
        }
    }

    private FXOMInstance getFxomInstance(final FXOMIntrinsic intrinsic) {
        final var fxomInstance = new FXOMInstance(intrinsic.getFxomDocument(), intrinsic.getGlueElement());
        fxomInstance.setSceneGraphObject(intrinsic.getSourceSceneGraphObject());
        fxomInstance.setDeclaredClass(intrinsic.getClass());
        if (!intrinsic.getProperties().isEmpty()) {
            fxomInstance.fillProperties(intrinsic.getProperties());
        }
        return fxomInstance;
    }
    
    /*
     * The case of SplitPane.dividerPositions property
     * -----------------------------------------------
     * 
     * When user adds a child to a SplitPane, this adds a new entry in
     * SplitPane.children property but also adds a new value to 
     * SplitPane.dividerPositions by side-effect.
     * 
     * The change in SplitPane.dividerPositions is performed at scene graph
     * level by FX. Thus it is unseen by FXOM. 
     * 
     * So in that case we perform a special operation which copies value of 
     * SplitPane.dividerPositions into FXOMProperty representing 
     * dividerPositions in FXOM.
     */

    private void synchronizeDividerPositions(final FXOMDocument document) {
        final var fxomRoot = document.getFxomRoot();
        if (fxomRoot != null) {
            final var metadata
                    = Metadata.getMetadata();
            final var dividerPositionsName
                    = new PropertyName("dividerPositions");
            final var candidates
                    = fxomRoot.collectObjectWithSceneGraphObjectClass(SplitPane.class);

            for (final var fxomObject : candidates) {
                if (fxomObject instanceof final FXOMInstance fxomInstance) {
                    assert fxomInstance.getSceneGraphObject() instanceof SplitPane;
                    final var splitPane
                            = (SplitPane) fxomInstance.getSceneGraphObject();
                    splitPane.layout();
                    final var vpm
                            = metadata.queryValueProperty(fxomInstance, dividerPositionsName);
                    assert vpm instanceof ListValuePropertyMetadata
                            : "vpm.getClass()=" + vpm.getClass().getSimpleName();
                    final var davpm
                            = (DoubleArrayPropertyMetadata) vpm;
                    davpm.synchronizeWithSceneGraphObject(fxomInstance);
                }
            }
        }
    }


//    
//    
//    private void reloadStylesheets(final Parent p) {
//        assert p != null;
//        assert p.getScene() != null;
//        
//        if (p.getStylesheets().isEmpty() == false) {
//            final List<String> stylesheets = new ArrayList<>();
//            stylesheets.addAll(p.getStylesheets());
////            p.getStylesheets().clear();
////            p.impl_processCSS(true);
//            p.getStylesheets().setAll(stylesheets);
////            p.impl_processCSS(true);
//        }
//        for (Node child : p.getChildrenUnmodifiable()) {
//            if (child instanceof Parent) {
//                reloadStylesheets((Parent)child);
//            }
//        }
//        
//    }
}
