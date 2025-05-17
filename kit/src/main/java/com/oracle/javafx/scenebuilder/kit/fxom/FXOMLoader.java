/*
 * Copyright (c) 2017, 2024, Gluon and/or its affiliates.
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

import com.oracle.javafx.scenebuilder.kit.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.ErrorDialog;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import javafx.fxml.LoadListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;


/**
 *
 *
 */
class FXOMLoader implements LoadListener {

    private final FXOMDocument document;
    private final Consumer<Exception> knownErrorsHandler;
    private TransientNode currentTransientNode;
    private GlueCursor glueCursor;
    
    /*
     * FXOMLoader
     */

    public FXOMLoader(final FXOMDocument document) {
        this(document, FXOMLoader::showErrorDialog);
    }

    FXOMLoader(final FXOMDocument document, final Consumer<Exception> knownErrorsHandler) {
        assert document != null;
        assert document.getGlue().getRootElement() != null;
        this.document = document;
        this.knownErrorsHandler = Objects.requireNonNull(knownErrorsHandler);
    }

    public void load(final String fxmlText) throws java.io.IOException {
        assert fxmlText != null;

        final ClassLoader classLoader;
        if (document.getClassLoader() != null) {
            classLoader = document.getClassLoader();
        } else {
            classLoader = FXMLLoader.getDefaultClassLoader();
        }

        final var fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(document.getLocation());
        fxmlLoader.setResources(new ResourceKeyCollector(document.getResources()));
        fxmlLoader.setClassLoader(new TransientClassLoader(classLoader));
        fxmlLoader.setLoadListener(this);
        Deprecation.setStaticLoad(fxmlLoader, true);

        final var utf8 = StandardCharsets.UTF_8;
        try (final InputStream is = new ByteArrayInputStream(fxmlText.getBytes(utf8))) {
            glueCursor = new GlueCursor(document.getGlue());
            currentTransientNode = null;
            assert is.markSupported();
            is.reset();
            setSceneGraphRoot(fxmlLoader.load(is));
        } catch (final RuntimeException | IOException x) {
            handleFxmlLoadingError(x);
        }
    }

    private void handleFxmlLoadingError(final Exception x) throws IOException {
        if (x.getCause() != null) {
            handleKnownCauses(x);
        } else {
            handleUnknownAndMissingCauses(x);
        }
    }

    private void handleUnknownAndMissingCauses(final Exception x) throws IOException {
        throw new IOException(x);
    }

    private void handleKnownCauses(final Exception x) throws IOException {
        if (x.getCause() instanceof XMLStreamException) {
            knownErrorsHandler.accept(x);
        } else {                    
            handleUnknownAndMissingCauses(x);
        }
    }

    private static void showErrorDialog(final Exception x) {
        final var errorDialog = new ErrorDialog(null);
        errorDialog.setMessage(I18N.getString("alert.open.failure.charset.not.found"));
        errorDialog.setDetails(I18N.getString("alert.open.failure.charset.not.found.details"));
        errorDialog.setDebugInfo(x.getCause().toString());
        errorDialog.setTitle(I18N.getString("alert.title.open"));
        errorDialog.showAndWait();
    }

    private void setSceneGraphRoot(final Object sceneGraphRoot) {
        document.setSceneGraphRoot(sceneGraphRoot);
        document.setDisplayNode(null);
        document.setDisplayStylesheets(Collections.emptyList());

        if (sceneGraphRoot instanceof Scene) {
            final var scene = (Scene) sceneGraphRoot;
            document.setDisplayNode(scene.getRoot());
            document.setDisplayStylesheets(scene.getStylesheets());
            scene.setRoot(new Pane()); // ensure displayNode is only part of one scene
        } else if (sceneGraphRoot instanceof Window) {
            final var window = (Window) sceneGraphRoot;
            if (window.getScene() != null) {
                document.setDisplayNode(window.getScene().getRoot());
                document.setDisplayStylesheets(window.getScene().getStylesheets());
                window.getScene().setRoot(new Pane()); // ensure displayNode is only part of one scene
            }
        }
    }

    public FXOMDocument getDocument() {
        return document;
    }
    
    
    /*
     * LoadListener
     */

    @Override
    public void readImportProcessingInstruction(final String data) {
    }

    @Override
    public void readLanguageProcessingInstruction(final String data) {
    }

    @Override
    public void readComment(final String string) {
    }

    @Override
    public void beginInstanceDeclarationElement(final Class<?> declaredClass) {
        assert declaredClass != null;
        assert glueCursor.getCurrentElement().getTagName().equals(PropertyName.makeClassFullName(declaredClass)) ||
               glueCursor.getCurrentElement().getTagName().equals(declaredClass.getCanonicalName());

        final var transientInstance
                = new TransientObject(currentTransientNode,
                declaredClass, glueCursor.getCurrentElement());

        currentTransientNode = transientInstance;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginUnknownTypeElement(final String unknownClassName) {
        assert unknownClassName != null;
        assert glueCursor.getCurrentElement().getTagName().equals(unknownClassName);

        final var transientInstance
                = new TransientObject(currentTransientNode,
                unknownClassName, glueCursor.getCurrentElement());

        currentTransientNode = transientInstance;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginIncludeElement() {
        assert glueCursor.getCurrentElement().getTagName().equals("fx:include");

        final var transientIntrinsic
                = new TransientIntrinsic(currentTransientNode,
                FXOMIntrinsic.Type.FX_INCLUDE, glueCursor.getCurrentElement());

        currentTransientNode = transientIntrinsic;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginReferenceElement() {
        assert glueCursor.getCurrentElement().getTagName().equals("fx:reference");

        final var transientIntrinsic
                = new TransientIntrinsic(currentTransientNode,
                FXOMIntrinsic.Type.FX_REFERENCE, glueCursor.getCurrentElement());

        currentTransientNode = transientIntrinsic;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginCopyElement() {
        assert glueCursor.getCurrentElement().getTagName().equals("fx:copy");

        final var transientIntrinsic
                = new TransientIntrinsic(currentTransientNode,
                FXOMIntrinsic.Type.FX_COPY, glueCursor.getCurrentElement());

        currentTransientNode = transientIntrinsic;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginRootElement() {
        assert glueCursor.getCurrentElement().getTagName().equals("fx:root");

        final var transientInstance
                = new TransientObject(currentTransientNode,
                glueCursor.getCurrentElement());

        currentTransientNode = transientInstance;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginPropertyElement(final String name, final Class<?> staticClass) {
        assert name != null;

        final var transientProperty
                = new TransientProperty(currentTransientNode,
                    new PropertyName(name, staticClass),
                    glueCursor.getCurrentElement());

        currentTransientNode = transientProperty;
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginUnknownStaticPropertyElement(final String string) {
        currentTransientNode = new TransientIgnored(currentTransientNode);
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginScriptElement() {
        currentTransientNode = new TransientIgnored(currentTransientNode);
        glueCursor.moveToNextElement();
    }

    @Override
    public void beginDefineElement() {
        currentTransientNode = new TransientIgnored(currentTransientNode);
        glueCursor.moveToNextElement();
    }

    @Override
    public void readInternalAttribute(final String attrName, final String attrValue) {
        assert currentTransientNode instanceof TransientObject ||
               currentTransientNode instanceof TransientIntrinsic;

        if (attrName.equals("type")) {
            assert currentTransientNode instanceof TransientObject;
            final var transientObject = (TransientObject) currentTransientNode;
            transientObject.setFxRootType(attrValue);
        }
    }

    @Override
    public void readPropertyAttribute(final String name, final Class<?> staticClass, final String fxmlValue) {
        assert currentTransientNode instanceof TransientObject
                || currentTransientNode instanceof TransientIntrinsic
                || currentTransientNode instanceof TransientProperty;

        assert name != null;

        final var pname = new PropertyName(name, staticClass);
        final var fxomProperty = new FXOMPropertyT(document, pname, null, null, fxmlValue);

        if (currentTransientNode instanceof TransientObject) {
            final var transientInstance = (TransientObject) currentTransientNode;
            transientInstance.getProperties().add(fxomProperty);
        } else if (currentTransientNode instanceof TransientProperty) {
            final var transientProperty = (TransientProperty) currentTransientNode;
            transientProperty.getCollectedProperties().add(fxomProperty);
        } else if(currentTransientNode instanceof  TransientIntrinsic) {
            final var transientIntrinsic = (TransientIntrinsic) currentTransientNode;
            transientIntrinsic.getProperties().add(fxomProperty);
        }
    }

    @Override
    public void readUnknownStaticPropertyAttribute(final String string, final String string1) {
        // TODO(elp) : implement FXOMLoader.readUnknownStaticPropertyAttribute.
    }

    @Override
    public void readEventHandlerAttribute(final String name, final String hashStatement) {
        // Same as readPropertyAttribute() 
        readPropertyAttribute(name, null, hashStatement);
    }

    @Override
    public void endElement(final Object sceneGraphObject) {

        currentTransientNode.setSceneGraphObject(sceneGraphObject);

        if (currentTransientNode instanceof TransientObject) {
            final var currentInstance = (TransientObject) currentTransientNode;
            final var currentFxomObject = currentInstance.makeFxomObject(document);
            final var currentParent = currentInstance.getParentNode();
            if (currentParent instanceof TransientProperty) {
                final var parentProperty = (TransientProperty) currentParent;
                parentProperty.getValues().add(currentFxomObject);
            } else if (currentParent instanceof TransientObject) {
                final var parentInstance = (TransientObject) currentParent;
                parentInstance.getCollectedItems().add(currentFxomObject);
            } else if (currentParent instanceof TransientIgnored) {
                // currentObject is an object inside an fx:define section
                // Nothing to do for now
            } else {
                assert currentParent == null;
                document.updateRoots(currentFxomObject, currentFxomObject.getSceneGraphObject());
            }

        } else if (currentTransientNode instanceof TransientIntrinsic) {
            final var currentIntrinsic = (TransientIntrinsic) currentTransientNode;
            final var currentFxomIntrinsic = currentIntrinsic.makeFxomIntrinsic(document);
            final var currentParent = currentIntrinsic.getParentNode();

            if (currentParent instanceof TransientProperty) {
                final var parentProperty = (TransientProperty) currentParent;
                parentProperty.getValues().add(currentFxomIntrinsic);
            } else if (currentParent instanceof TransientObject) {
                final var parentInstance = (TransientObject) currentParent;
                parentInstance.getCollectedItems().add(currentFxomIntrinsic);
            } else if (currentParent instanceof TransientIgnored) {
                // currentObject is an object inside an fx:define section
                // Nothing to do for now
            } else {
                assert currentParent == null;
                document.updateRoots(currentFxomIntrinsic, currentFxomIntrinsic.getSceneGraphObject());
            }
        } else if (currentTransientNode instanceof TransientProperty) {
            final var currentProperty = (TransientProperty) currentTransientNode;
            final var currentParent = currentProperty.getParentNode();
            final var currentFxomProperty = currentProperty.makeFxomProperty(document);
            assert currentParent instanceof TransientObject;
            if (currentParent instanceof TransientObject){
            final var parentObject = (TransientObject) currentParent;
            parentObject.getProperties().add(currentFxomProperty);
        }
        else if(currentParent instanceof TransientIntrinsic) {
                final var transientIntrinsic = (TransientIntrinsic) currentParent;
                transientIntrinsic.getProperties().add(currentFxomProperty);
            }

            // We ignore sceneGraphObject
        } else {
                assert currentTransientNode instanceof TransientIgnored;
                // Nothing to do in this case
        }

        currentTransientNode = currentTransientNode.getParentNode();
    }
}
