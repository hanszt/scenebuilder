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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;

import com.oracle.javafx.scenebuilder.kit.fxom.glue.GlueDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.sampledata.SampleDataGenerator;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import com.oracle.javafx.scenebuilder.kit.util.URLUtils;

/**
 *
 * 
 */
public class FXOMDocument {
    
    private final GlueDocument glue;
    private URL location;
    private ClassLoader classLoader;
    private ResourceBundle resources;
    private SampleDataGenerator sampleDataGenerator;
    private FXOMObject fxomRoot;
    private Object sceneGraphRoot;
    private Node displayNode;
    private final ArrayList<String> displayStylesheets = new ArrayList<>();
    private final SimpleIntegerProperty sceneGraphRevision = new SimpleIntegerProperty();
    private final SimpleIntegerProperty cssRevision = new SimpleIntegerProperty();
    private SceneGraphHolder sceneGraphHolder;
    private int updateDepth;

    private boolean hasControlsFromExternalPlugin;
    
    private List<Class<?>> initialDeclaredClasses;
    
    /**
     * Creates a new {@link FXOMDocument} from given FXML source. Depending on the
     * use case, the {@link FXOMDocumentSwitch} items can be used to configure the
     * document creation process according to specific needs. E.g. normalization is
     * not enabled by default, thus if required the {@link FXOMDocumentSwitch}
     * {@code NORMALIZED} should be added as constructor argument.
     * 
     * @param fxmlText    FXML source
     * @param location    {@link URL} describing the actual document location
     * @param classLoader {@link ClassLoader} to be used
     * @param resources   {@link ResourceBundle} to be used
     * @param switches    {@link FXOMDocumentSwitch} configuration options to enable
     *                    or disable certain steps in {@link FXOMDocument} creation
     *                    (e.g. enforcing normalization)
     * @throws IOException when the fxmlText cannot be loaded
     */
    public FXOMDocument(final String fxmlText, final URL location, final ClassLoader classLoader, final ResourceBundle resources, final FXOMDocumentSwitch... switches) throws IOException {
        this.glue = new GlueDocument(fxmlText);
        this.location = location;
        this.classLoader = classLoader;
        this.resources = resources;
        initialDeclaredClasses = new ArrayList<>();
        if (this.glue.getRootElement() != null) {
            var fxmlTextToLoad = fxmlText;
            final var availableSwitches = Set.of(switches);
            if (!availableSwitches.contains(FXOMDocumentSwitch.FOR_PREVIEW)) {
                final var fxmlPropertiesDisabler = new FXMLPropertiesDisabler();
                fxmlTextToLoad = fxmlPropertiesDisabler.disableProperties(fxmlText);
            }
            final var loader = new FXOMLoader(this);
            loader.load(fxmlTextToLoad);
            if (availableSwitches.contains(FXOMDocumentSwitch.NORMALIZED)) {
                final var normalizer = new FXOMNormalizer(this);
                normalizer.normalize();
            }
        } else {
            // Document is empty
            assert GlueDocument.isEmptyXmlText(fxmlText);
            // Keeps this.fxomRoot == null
            // Keeps this.sceneGraphRoot == null
        }

        hasControlsFromExternalPlugin = EditorPlatform.hasClassFromExternalPlugin(fxmlText);
    }
        
    public FXOMDocument() {
        this.glue = new GlueDocument();
    }
    
    public GlueDocument getGlue() {
        return glue;
    }
    
    public URL getLocation() {
        return location;
    }

    public Optional<URL> location() {
        return Optional.ofNullable(getLocation());
    }

    public Optional<Path> locationPath() {
        return locationURI()
                // this is used instead of Paths::get to match the original implementation
                .map(File::new)
                .map(File::toPath);
    }

    public Optional<URI> locationURI() {
        if (location == null)
            return Optional.empty();

        try {
            return Optional.ofNullable(location.toURI());
        } catch (final URISyntaxException e) {
            return Optional.empty();
        }
    }

    public void setLocation(final URL location) {
        if (!URLUtils.equals(this.location, location)) {
            beginUpdate();
            if (fxomRoot != null) {
                fxomRoot.documentLocationWillChange(location);
            }
            this.location = location;
            endUpdate();
        }
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(final ClassLoader classLoader) {
        beginUpdate();
        this.classLoader = classLoader;
        endUpdate();
    }    
    
    public List<Class<?>> getInitialDeclaredClasses() {
        return initialDeclaredClasses;
    }

    public ResourceBundle getResources() {
        return resources;
    }

    public void setResources(final ResourceBundle resources) {
        beginUpdate();
        this.resources = resources;
        endUpdate();
    }

    public boolean isSampleDataEnabled() {
        return sampleDataGenerator != null;
    }

    public void setSampleDataEnabled(final boolean sampleDataEnabled) {
        assert !isUpdateOnGoing();
        
        final SampleDataGenerator newSampleDataGenerator;
        if (sampleDataEnabled) {
            newSampleDataGenerator = Objects.requireNonNullElseGet(sampleDataGenerator, SampleDataGenerator::new);
        } else {
            newSampleDataGenerator = null;
        }
        
        if (newSampleDataGenerator != sampleDataGenerator) {
            if (sampleDataGenerator != null) {
                sampleDataGenerator.removeSampleData(fxomRoot);
            }
            sampleDataGenerator = newSampleDataGenerator;
            if (sampleDataGenerator != null) {
                sampleDataGenerator.assignSampleData(fxomRoot);
            }
        }
    }    
    
    public FXOMObject getFxomRoot() {
        return fxomRoot;
    }

    public void setFxomRoot(final FXOMObject fxomRoot) {
        beginUpdate();
        updateRoots(fxomRoot, null);
        endUpdate();
    }
    
    void updateRoots(final FXOMObject fxomRoot, final Object sceneGraphRoot) {
        assert fxomRoot == null || fxomRoot.getFxomDocument() == this;

        this.fxomRoot = fxomRoot;
        if (this.fxomRoot == null) {
            this.glue.setRootElement(null);
        } else {
            this.glue.setRootElement(this.fxomRoot.getGlueElement());
        }
        this.sceneGraphRoot = sceneGraphRoot;
        this.displayNode = null;
        this.displayStylesheets.clear();
    }

    public Object getSceneGraphRoot() {
        return sceneGraphRoot;
    }

    void setSceneGraphRoot(final Object sceneGraphRoot) {
        this.sceneGraphRoot = sceneGraphRoot;
    }

    /**
     * Returns the Node that should be displayed in the editor instead of the scene graph root.
     */
    public Node getDisplayNode() {
        return displayNode;
    }

    public List<String> getDisplayStylesheets() {
        return Collections.unmodifiableList(displayStylesheets);
    }

    void setDisplayStylesheets(final List<String> displayStylesheets) {
        this.displayStylesheets.clear();
        this.displayStylesheets.addAll(displayStylesheets);
    }

    /**
     * Sets the Node that should be displayed in the editor instead of the scene graph root.
     */
    void setDisplayNode(final Node displayNode) {
        this.displayNode = displayNode;
    }

    /**
     * Returns the display node if one is set, otherwise returns the scene graph root.
     */
    public Object getDisplayNodeOrSceneGraphRoot() {
        return displayNode != null ? displayNode : sceneGraphRoot;
    }

    /**
     * Returns the FXML string representation of the FXOMDocument.
     * @param wildcardImports If the FXML should have wildcards in its imports.
     * @return The FXML string representation. This can be empty if current root is null.
     */
    public String getFxmlText(final boolean wildcardImports) {
        final String result;
        if (fxomRoot == null) {
            assert glue.getRootElement() == null;
            assert sceneGraphRoot == null;
            result = "";
        } else {
            assert glue.getRootElement() != null;
            // Note that sceneGraphRoot might be null if fxomRoot is unresolved
            glue.updateIndent();
            final var saver = new FXOMSaver(wildcardImports);
            result = saver.save(this);
        }
        return result;
    }

    public FXOMObject searchWithSceneGraphObject(final Object sceneGraphObject) {
        final FXOMObject result;
        
        if (fxomRoot == null) {
            result = null;
        } else {
            result = fxomRoot.searchWithSceneGraphObject(sceneGraphObject);
        }
        
        return result;
    }
    
    public FXOMObject searchWithFxId(final String fxId) {
        final FXOMObject result;
        
        if (fxomRoot == null) {
            result = null;
        } else {
            result = fxomRoot.searchWithFxId(fxId);
        }
        
        return result;
    }
    
    public Map<String, FXOMObject> collectFxIds() {
        final Map<String, FXOMObject> result;
        
        if (fxomRoot == null) {
            result = Collections.emptyMap();
        } else {
            result = fxomRoot.collectFxIds();
        }
        
        return result;
    }
    
    
    public void beginUpdate() {
        updateDepth++;
    }
    
    public void endUpdate() {
        assert updateDepth >= 1;
        updateDepth--;
        if (updateDepth == 0) {
            refreshSceneGraph();
        }
    }
    
    public boolean isUpdateOnGoing() {
        return updateDepth >= 1;
    }
    
    public void refreshSceneGraph() {
        if (sceneGraphHolder != null) {
            sceneGraphHolder.fxomDocumentWillRefreshSceneGraph(this);
        }
        final var fxomRefresher = new FXOMRefresher();
        fxomRefresher.refresh(this);
        if ((sampleDataGenerator != null) && (fxomRoot != null)) {
            sampleDataGenerator.assignSampleData(fxomRoot);
        }
        if (sceneGraphHolder != null) {
            sceneGraphHolder.fxomDocumentDidRefreshSceneGraph(this);
        }
        sceneGraphRevision.set(sceneGraphRevision.get()+1);
    }
    
    /**
     * Returns the property holding the revision number of the scene graph.
     * refreshSceneGraph() method increments the revision by one each time it
     * refreshes the scene graph.
     * 
     * @return the property holding the revision number of scene graph.
     */
    public ReadOnlyIntegerProperty sceneGraphRevisionProperty() {
        return sceneGraphRevision;
    }
    
    /**
     * Forces this document to reload the specified css stylesheet file.
     * 
     * @param stylesheetPath path of the stylesheet to be reloaded.
     */
    public void reapplyCSS(final Path stylesheetPath) {
        if (sceneGraphRoot instanceof Node) {
            
            /*
             * Normally we should scan for all stylesheets properties which
             * include stylesheetPath and update them.
             * Right now, we use a workaround solution because of bug RT-34863.
             */
            final var contentGroup = ((Node) sceneGraphRoot).getParent();
            if ((contentGroup != null) && (contentGroup.getScene() != null) && stylesheetPath != null) {
                Deprecation.reapplyCSS(contentGroup, stylesheetPath.toUri());
                cssRevision.set(cssRevision.get()+1);
            }
        }
    }
    
    /**
     * Returns the property holding the css revision number.
     * reapplyCSS() method increments the revision by one each time it
     * is invoked.
     * 
     * @return the property holding the css revision number.
     */
    public ReadOnlyIntegerProperty cssRevisionProperty() {
        return cssRevision;
    }
    
    /**
     * Utility method that fetches the text content from a URL.
     * 
     * @param url a URL
     * @return  the text content read from the URL.
     * @throws IOException if something goes wrong
     */
    public static String readContentFromURL(final URL url) throws IOException {
        final var result = new StringBuilder();
        
        try (final var is =url.openConnection().getInputStream()) {
            try (final var r = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                final var buffer = new char[1024];

                var readLength = r.read(buffer, 0, buffer.length);
                while (readLength != -1) {
                    result.append(buffer, 0, readLength);
                    readLength = r.read(buffer, 0, buffer.length);
                }
                
            }
        }
        
        return result.toString();
    }
    
    /**
     * Informs this fxom document that its scene graph is hold by the specified
     * scene graph holder.
     * 
     * @param holder an scene graph holder (should not be null)
     */
    public void beginHoldingSceneGraph(final SceneGraphHolder holder) {
        assert holder != null;
        assert sceneGraphHolder == null;
        sceneGraphHolder = holder;
    }
    
    /**
     * Informs this fxom document that its scene graph i no longer hold.
     */
    public void endHoldingSceneGraph() {
        assert sceneGraphHolder != null;
        sceneGraphHolder = null;
    }

    public boolean hasControlsFromExternalPlugin() {
        return hasControlsFromExternalPlugin;
    }
    
    /**
     * Returns null or the object holding the scene graph of this fxom document.
     * 
     * @return  null or the object holding the scene graph of this fxom document.
     */
    public SceneGraphHolder getSceneGraphHolder() {
        return sceneGraphHolder;
    }
    
    public interface SceneGraphHolder {
        void fxomDocumentWillRefreshSceneGraph(FXOMDocument fxomDocument);
        void fxomDocumentDidRefreshSceneGraph(FXOMDocument fxomDocument);
    }
    
    /**
     * Depending on where the {@link FXOMDocument} shall be used, 
     * it is necessary to configure the {@link FXOMDocument} creation process.
     * The switches here can be used to configure the creation process in the desired way.
     */
    public enum FXOMDocumentSwitch {
        /**
         * When this flag is present, the {@link FXOMDocument} will be normalized (see {@link FXOMNormalizer}).
         */
        NORMALIZED,
        
        /**
         * When this flag is present during {@link FXOMDocument} creation, the
         * {@link FXMLPropertiesDisabler} will be used to modify the FXML source in such
         * a way that the included settings will not interfere Scene Builder's
         * configuration. One possible example here is the option to use the MacOS
         * system menu bar.
         */
        FOR_PREVIEW;
    }
}
