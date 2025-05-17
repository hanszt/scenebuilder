/* 
 * Copyright (c) 2022, 2024, Gluon and/or its affiliates.
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

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument.FXOMDocumentSwitch;
import com.oracle.javafx.scenebuilder.kit.fxom.glue.GlueInstruction;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ImagePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignImage;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.JavaLanguage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

/**
 * This class groups static utility methods which operate on FXOMNode and
 * subclasses (a bit like Collection and Collections).
 * 
 * 
 */
public class FXOMNodes {
    
    FXOMNodes() {
        // no-op
    }

    /**
     * Sorts the specified set of objects according their location in
     * the fxom document. Objets are sorted according depth first order.
     * In particular, if objects all have the same parent, then the resulting 
     * list will be sorted by indexes.
     * 
     * @param objects a set of fxom objects (never null but possibly empty)
     * @return the list of objets sorted by position in the fxom document
     */
    public static List<FXOMObject> sort(final Set<FXOMObject> objects) {
        final List<FXOMObject> result;
        
        assert objects != null;
        
        if (objects.isEmpty()) {
            result = Collections.emptyList();
        } else if (objects.size() == 1) {
            result = Collections.singletonList(objects.iterator().next());
        } else {
            final var object0 = objects.iterator().next();
            final var fxomDocument = object0.getFxomDocument();
            assert fxomDocument != null;
            result = new ArrayList<>();
            sort(fxomDocument.getFxomRoot(), objects, result);
        }
        
        return result;
    }
    
    
    /**
     * Flattens a set of fxom objects.
     * A set of fxom objects is declared "flat" if each object member 
     * of the set has no ancestor member of the set.
     * 
     * @param objects a set of fxom objects (never null)
     * @return the flat set of objects.
     */
    public static Set<FXOMObject> flatten(final Set<FXOMObject> objects) {
        final Set<FXOMObject> result = new HashSet<>();
        
        assert objects != null;
        
        for (final var o : objects) {
            if (lookupAncestor(o, objects) == null) {
                result.add(o);
            }
        }
        
        return result;
    }
    
    
    /**
     * Returns null or the first ancestor of "obj" which belongs to "candidates".
     * @param obj an fxom object (never null)
     * @param candidates a set of fxom object (not null and not empty)
     * @return null or the first ancestor of "obj" which belongs to "candidates".
     */
    public static FXOMObject lookupAncestor(final FXOMObject obj, final Set<FXOMObject> candidates) {
        assert obj != null;
        assert candidates != null;
        assert !candidates.isEmpty();

        var result = obj.getParentObject();
        while ((result != null) && (!candidates.contains(result))) {
            result = result.getParentObject();
        }
        
        return result;
    }
    
    
    
    public static FXOMObject newObject(final FXOMDocument targetDocument, final File file)
            throws IOException {
        assert targetDocument != null;
        assert file != null;
        FXOMObject result = null;
        if (file.getAbsolutePath().endsWith(".fxml")) { //NOI18N
            final var fxmlText
                    = FXOMDocument.readContentFromURL(file.toURI().toURL());
            final var transientDoc = new FXOMDocument(
                    fxmlText,
                    targetDocument.getLocation(),
                    targetDocument.getClassLoader(),
                    targetDocument.getResources(),
                    FXOMDocumentSwitch.NORMALIZED);
            result = transientDoc.getFxomRoot();
            if (result != null) {
                result.moveToFxomDocument(targetDocument);
            }
        } else {
            // Try load the file as an image
            final var fileURL = file.toURI().toURL().toString();
            final var image = new Image(fileURL);
            if (!image.isError()) {
                final var transientDoc
                        = makeFxomDocumentFromImageURL(image, 200.0);
                result = transientDoc.getFxomRoot();
                if (result != null) {
                    result.moveToFxomDocument(targetDocument);
                }
            } else {
                try {
                    final var media = new Media(fileURL);
                    if (media.getError() == null) {
                        final var transientDoc
                                = makeFxomDocumentFromMedia(media, 200.0);
                        result = transientDoc.getFxomRoot();
                        if (result != null) {
                            result.moveToFxomDocument(targetDocument);
                        }
                    } else {
                        throw new IOException(media.getError());
                    }
                } catch(final MediaException x) {
                    throw new IOException(x);
                }
            }
        }

        return result;
    }
    
    public static FXOMIntrinsic newInclude(final FXOMDocument targetDocument, final File file)
            throws IOException {
        assert targetDocument != null;
        assert targetDocument.getLocation() != null;
        assert file != null;
        FXOMIntrinsic result = null;
        if (file.getAbsolutePath().endsWith(".fxml")) { //NOI18N
            final var fxmlURL = file.toURI().toURL();
            final var fxmlText = FXOMDocument.readContentFromURL(fxmlURL);
            final var transientDoc = new FXOMDocument(
                    fxmlText,
                    fxmlURL,
                    targetDocument.getClassLoader(),
                    targetDocument.getResources(),
                    FXOMDocumentSwitch.NORMALIZED);
            if (transientDoc.getFxomRoot() != null) {
                final var pv
                        = PrefixedValue.makePrefixedValue(fxmlURL, targetDocument.getLocation());
                assert pv.isDocumentRelativePath();
                assert pv.toString().startsWith(FXMLLoader.RELATIVE_PATH_PREFIX);
                final var includeRef
                        = pv.toString().substring(FXMLLoader.RELATIVE_PATH_PREFIX.length());
                result = new FXOMIntrinsic(targetDocument, FXOMIntrinsic.Type.FX_INCLUDE, includeRef);
                result.setSourceSceneGraphObject(transientDoc.getFxomRoot().getSceneGraphObject());
            }
        }

        return result;
    }
    
    public static FXOMDocument newDocument(final FXOMObject source) {
        assert source != null;
        
        final var result = new FXOMDocument();
        
        /*
         * If source's document contains unresolved objects,
         * then clones import instructions from the source document
         * to the new document.
         */
        final var sourceDocument
                = source.getFxomDocument();
        assert sourceDocument.getFxomRoot() != null; // contains at least source
        final var unresolvedObjects
                = collectUnresolvedObjects(sourceDocument.getFxomRoot());
        if (!unresolvedObjects.isEmpty()) {
            // Copy all the imports from the source document to the new document
            final var sourceGlue = sourceDocument.getGlue();
            final var resultGlue = result.getGlue();
            for (final var i : sourceGlue.collectInstructions("import")) {
                final var ci = new GlueInstruction(resultGlue, i.getTarget(), i.getData());
                resultGlue.getHeader().add(ci);
            }
        }
        
        /*
         * Clones source to the new document
         */
        final var cloner = new FXOMCloner(result);
        final var sourceClone = cloner.clone(source);
        
        /*
         * Setup new document : sourceClone is the root, 
         * same location, same class loader.
         */
        result.beginUpdate();
        result.setLocation(sourceDocument.getLocation());
        result.setClassLoader(sourceDocument.getClassLoader());
        result.setFxomRoot(sourceClone);
        if (result.getFxomRoot() instanceof FXOMInstance) {
            trimStaticProperties((FXOMInstance) result.getFxomRoot());
        }
        result.endUpdate();
        
        return result;
    }


    public static void updateProperty(final FXOMInstance fxomInstance, final FXOMProperty sourceProperty) {
        assert fxomInstance != null;
        assert sourceProperty != null;
        assert sourceProperty.getFxomDocument() == fxomInstance.getFxomDocument();
        
        final var currentProperty = fxomInstance.getProperties().get(sourceProperty.getName());
        if (currentProperty == null) {
            sourceProperty.addToParentInstance(-1, fxomInstance);
        } else if ((currentProperty instanceof final FXOMPropertyT currentPropertyT)
                && (sourceProperty instanceof final FXOMPropertyT newPropertyT)) {
            updateProperty(currentPropertyT, newPropertyT);
        } else if ((currentProperty instanceof final FXOMPropertyC currentPropertyC)
                && (sourceProperty instanceof final FXOMPropertyC newPropertyC)) {
            updateProperty(currentPropertyC, newPropertyC);
        } else {
            final var index = currentProperty.getIndexInParentInstance();
            currentProperty.removeFromParentInstance();
            sourceProperty.addToParentInstance(index, fxomInstance);
        }
    }
    
    
    public static void updateProperty(final FXOMPropertyT fxomProperty, final FXOMPropertyT sourceProperty) {
        assert fxomProperty != null;
        assert sourceProperty != null;
        assert fxomProperty.getName().equals(sourceProperty.getName());
        fxomProperty.setValue(sourceProperty.getValue());
    }
    
    
    public static void updateProperty(final FXOMPropertyC fxomProperty, final FXOMPropertyC sourceProperty) {
        assert fxomProperty != null;
        assert sourceProperty != null;
        assert fxomProperty.getName().equals(sourceProperty.getName());
        
        final List<FXOMObject> currentValues = new ArrayList<>();
        currentValues.addAll(fxomProperty.getValues());
        final List<FXOMObject> sourceValues = new ArrayList<>();
        sourceValues.addAll(sourceProperty.getValues());
        
        final var currentCount = currentValues.size();
        final var newCount = sourceValues.size();
        final var updateCount = Math.min(currentCount, newCount);
        
        // Update items
        for (var i = 0; i < updateCount; i++) {
            final var currentValue = currentValues.get(i);
            final var newValue = sourceValues.get(i);
            if ((currentValue instanceof final FXOMInstance currentInstance) &&
                (newValue instanceof final FXOMInstance newInstance)) {
                if (currentInstance.getDeclaredClass() == newInstance.getDeclaredClass()) {
                    updateInstance(currentInstance, newInstance);
                } else {
                    replacePropertyValue(currentValue, newValue);
                }
            } else if ((currentValue instanceof final FXOMCollection currentCollection) &&
                       (newValue instanceof final FXOMCollection newCollection)) {
                updateCollection(currentCollection, newCollection);
            } else if ((currentValue instanceof final FXOMIntrinsic currentIntrinsic) &&
                       (newValue instanceof final FXOMIntrinsic newIntrinsic)) {
                updateIntrinsic(currentIntrinsic, newIntrinsic);
            } else {
                replacePropertyValue(currentValue, newValue);
           }
        }
        
        if (currentCount < newCount) {
            // Add new items
            for (var i = currentCount; i < newCount; i++) {
                final var newValue = sourceValues.get(i);
                newValue.addToParentProperty(-1, fxomProperty);
            }
        } else {
            // Delete old items
            for (var i = newCount; i < currentCount; i++) {
                final var currentValue = currentValues.get(i);
                currentValue.removeFromParentProperty();
            }
        }
    }
    
    
    public static void updateInstance(final FXOMInstance fxomInstance, final FXOMInstance sourceInstance) {
        assert fxomInstance != null;
        assert sourceInstance != null;
        assert fxomInstance.getFxomDocument() == sourceInstance.getFxomDocument();
        assert fxomInstance.getDeclaredClass() == sourceInstance.getDeclaredClass();
        
        // Compute obsolete properties.
        // It must be done here because sourceInstance is going to mutate.
        final Set<PropertyName> obsoleteNames = new HashSet<>();
        obsoleteNames.addAll(fxomInstance.getProperties().keySet());
        obsoleteNames.removeAll(sourceInstance.getProperties().keySet());

        // Update properties
        final Set<FXOMProperty> sourceProperties = new HashSet<>(sourceInstance.getProperties().values());
        for (final var sourceProperty : sourceProperties) {
            updateProperty(fxomInstance, sourceProperty);
        }
        // Remove obsolete properties
        for (final var pn : obsoleteNames) {
            final var fxomProperty = fxomInstance.getProperties().get(pn);
            assert fxomProperty != null;
            assert fxomProperty.getParentInstance() == fxomInstance;
            fxomProperty.removeFromParentInstance();
        }
        
        fxomInstance.setFxConstant(sourceInstance.getFxConstant());
        fxomInstance.setFxValue(sourceInstance.getFxValue());
        fxomInstance.setFxFactory(sourceInstance.getFxFactory());
    }
    
    
    public static void updateCollection(final FXOMCollection fxomCollection, final FXOMCollection sourceCollection) {
        assert fxomCollection != null;
        assert sourceCollection != null;
        assert fxomCollection.getFxomDocument() == sourceCollection.getFxomDocument();
        
        final var currentCount = fxomCollection.getItems().size();
        final var sourceCount = sourceCollection.getItems().size();
        final var updateCount = Math.min(currentCount, sourceCount);
        
        // Update items
        for (var i = 0; i < updateCount; i++) {
            final var currentValue = fxomCollection.getItems().get(i);
            final var newValue = sourceCollection.getItems().get(i);
            if ((currentValue instanceof final FXOMInstance currentInstance) &&
                (newValue instanceof final FXOMInstance newInstance)) {
                updateInstance(currentInstance, newInstance);
            } else if ((currentValue instanceof final FXOMCollection currentCollection) &&
                       (newValue instanceof final FXOMCollection newCollection)) {
                updateCollection(currentCollection, newCollection);
            } else if ((currentValue instanceof final FXOMIntrinsic currentIntrinsic) &&
                       (newValue instanceof final FXOMIntrinsic newIntrinsic)) {
                updateIntrinsic(currentIntrinsic, newIntrinsic);
            } else {
                final var index = currentValue.getIndexInParentProperty();
                assert index != -1;
                currentValue.removeFromParentCollection();
                newValue.addToParentCollection(index, fxomCollection);
            }
        }
        
        if (currentCount < sourceCount) {
            // Add new items
            final var addCount = sourceCount - currentCount;
            for (var i = 0; i < addCount; i++) {
                final var newValue = sourceCollection.getItems().get(i);
                newValue.addToParentCollection(-1, fxomCollection);
            }
        } else {
            // Delete old items
            final var removeCount = currentCount - sourceCount;
            for (var i = 0; i < removeCount; i++) {
                final var currentValue = fxomCollection.getItems().get(sourceCount);
                currentValue.removeFromParentProperty();
            }
        }
        
        fxomCollection.setFxConstant(sourceCollection.getFxConstant());
        fxomCollection.setFxValue(sourceCollection.getFxValue());
        fxomCollection.setFxFactory(sourceCollection.getFxFactory());
    }
    
    
    public static void updateIntrinsic(final FXOMIntrinsic fxomIntrinsic, final FXOMIntrinsic sourceIntrinsic) {
        assert fxomIntrinsic != null;
        assert sourceIntrinsic != null;
        assert fxomIntrinsic.getFxomDocument() != sourceIntrinsic.getFxomDocument();
        assert fxomIntrinsic.getType() == sourceIntrinsic.getType();
        
        fxomIntrinsic.setSource(sourceIntrinsic.getSource());
        fxomIntrinsic.setFxConstant(sourceIntrinsic.getFxConstant());
        fxomIntrinsic.setFxValue(sourceIntrinsic.getFxValue());
        fxomIntrinsic.setFxFactory(sourceIntrinsic.getFxFactory());
    }
    
    
    public static List<FXOMPropertyT> collectReferenceExpression(final FXOMObject fxomRoot, final String fxId) {
        assert fxomRoot != null;
        assert fxId != null;
        
        final List<FXOMPropertyT> result = new ArrayList<>();
        
        for (final var p : fxomRoot.collectPropertiesT()) {
            final var pv = new PrefixedValue(p.getValue());
            if (pv.isExpression()) {
                /*
                 * p is an FXOMPropertyT like this:
                 * 
                 * <.... property="$id" .... />
                 */
                final var id = pv.getSuffix();
                if (id.equals(fxId)) {
                    result.add(p);
                }
            }
        }
        
        return result;
    }
    
    
    public static List<FXOMObject> collectUnresolvedObjects(final FXOMObject fxomObject) {
        final List<FXOMObject> result = new ArrayList<>();
        
        for (final var o : serializeObjects(fxomObject)) {
            if (o.getSceneGraphObject() == null) {
                result.add(o);
            }
        }
        
        return result;
    }
    
    
    public static List<FXOMObject> serializeObjects(final FXOMObject fxomObject) {
        final List<FXOMObject> result = new ArrayList<>();
        
        serializeObjects(fxomObject, result);
        assert !result.isEmpty();
        assert result.getFirst() == fxomObject;
        
        return result;
    }
   
    public static void removeToggleGroups(final Map<String, FXOMObject> fxIdMap) {
        assert fxIdMap != null;
        
        for (final var fxId : new HashSet<>(fxIdMap.keySet())) {
            final var fxomObject = fxIdMap.get(fxId);
            if (fxomObject.getSceneGraphObject() instanceof ToggleGroup) {
                fxIdMap.remove(fxId);
            }
        }
    }
    
    
    public static String extractReferenceSource(final FXOMNode node) {
        final String result;
        
        if (node instanceof final FXOMIntrinsic intrinsic) {
            result = switch (intrinsic.getType()) {
                case FX_REFERENCE, FX_COPY -> intrinsic.getSource();
                default -> null;
            };
        } else if (node instanceof final FXOMPropertyT property) {
            final var pv = new PrefixedValue(property.getValue());
            if (pv.isExpression() && JavaLanguage.isIdentifier(pv.getSuffix())) {
                result = pv.getSuffix();
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        
        return result;
    }
    
    
    private static final PropertyName toggleGroupName = new PropertyName("toggleGroup");
    
    public static boolean isToggleGroupReference(final FXOMNode node) {
        final boolean result;
        
        if (extractReferenceSource(node) == null) {
            result = false;
        } else {
            if (node instanceof final FXOMIntrinsic intrinsic) {
                final FXOMProperty parentProperty = intrinsic.getParentProperty();
                if (parentProperty == null) {
                    result = false;
                } else {
                    result = parentProperty.getName().equals(toggleGroupName);
                }
            } else if (node instanceof final FXOMPropertyT property) {
                result = property.getName().equals(toggleGroupName);
            } else {
                result = false;
            }
        }
        
        return result;
    }
    
    
    public static FXOMPropertyC makeToggleGroup(final FXOMDocument fxomDocument, final String fxId) {
        final var toggleGroup = new FXOMInstance(fxomDocument, ToggleGroup.class);
        toggleGroup.setFxId(fxId);
        return new FXOMPropertyC(fxomDocument, toggleGroupName, toggleGroup);
    }
    
    
    public static boolean isWeakReference(final FXOMNode node) {
        final boolean result;
        
        if (node instanceof final FXOMIntrinsic intrinsic) {
            switch(intrinsic.getType()) {
                case FX_REFERENCE:
                case FX_COPY:
                    if (intrinsic.getParentProperty() != null) {
                        final var propertyName = intrinsic.getParentProperty().getName();
                        if (propertyName.getResidenceClass() == null) {
                            result = getWeakPropertyNames().contains(propertyName.getName());
                        } else {
                            result = false;
                        }
                    } else {
                        result = false;
                    }
                    break;
                default:
                    result = false;
            }
        } else if (node instanceof final FXOMPropertyT property) {
            final var pv = new PrefixedValue(property.getValue());
            if (pv.isExpression() && JavaLanguage.isIdentifier(pv.getSuffix())) {
                final var propertyName = property.getName();
                if (propertyName.getResidenceClass() == null) {
                    result = getWeakPropertyNames().contains(propertyName.getName());
                } else {
                    result = false;
                }
            } else {
                result = false;
            }
        } else {
            result = false;
        }
        
        return result;
    }
    
    
    private static Set<String> weakPropertyNames;
    
    public static synchronized Set<String> getWeakPropertyNames() {
        
        if (weakPropertyNames == null) {
            weakPropertyNames = new HashSet<>();
            weakPropertyNames.add("labelFor");
            weakPropertyNames.add("expandedPane");
            weakPropertyNames.add("clip");
        }
        
        return weakPropertyNames;
    }
    
    
    /*
     * Private
     */
    
    private static void sort(final FXOMObject from,
                             final Set<FXOMObject> objects, final List<FXOMObject> result) {
        
        if (objects.contains(from)) {
            result.add(from);
        }
        
        if (from instanceof final FXOMCollection collection) {
            for (final var item : collection.getItems()) {
                sort(item, objects, result);
            }
        } else if (from instanceof final FXOMInstance instance) {
            final List<PropertyName> propertyNames
                    = new ArrayList<>(instance.getProperties().keySet());
            Collections.sort(propertyNames);
            for (final var name : propertyNames) {
                final var property = instance.getProperties().get(name);
                assert property != null;
                if (property instanceof final FXOMPropertyC propertyC) {
                    for (final var v : propertyC.getValues()) {
                        sort(v, objects, result);
                    }
                }
            }
        } else {
            assert from instanceof FXOMIntrinsic
                    : "Unexpected FXOMObject subclass " + from.getClass();
        }
    }

    
    private static void trimStaticProperties(final FXOMInstance fxomInstance) {
        final List<FXOMProperty> properties = 
                new ArrayList<>(fxomInstance.getProperties().values());
        for (final var p : properties) {
            if (p.getName().getResidenceClass() != null) {
                // This is a static property : we remove it.
                p.removeFromParentInstance();
            }
        }
    }
    
    
    private static void replacePropertyValue(final FXOMObject replacee, final FXOMObject replacement) {
        assert replacee.getIndexInParentProperty() != -1;
        
        final var replaceeIndex = replacee.getIndexInParentProperty();
        assert replaceeIndex != -1;
        replacement.addToParentProperty(replaceeIndex, replacee.getParentProperty());
        replacee.removeFromParentProperty();
    }

    private static FXOMDocument makeFxomDocumentFromImageURL(
        final Image image, final double fitSize) throws IOException {

        assert image != null;
        assert fitSize > 0.0;
        
        final var imageWidth = image.getWidth();
        final var imageHeight = image.getHeight();
        
        final double fitWidth, fitHeight;
        final var imageSize = Math.max(imageWidth, imageHeight);
        if (imageSize < fitSize) {
            fitWidth = 0;
            fitHeight = 0;
        } else {
            final var widthScale  = fitSize / imageSize;
            final var heightScale = fitSize / imageHeight;
            final var scale = Math.min(widthScale, heightScale);
            fitWidth = Math.floor(imageWidth * scale);
            fitHeight = Math.floor(imageHeight * scale);
        }
        
        return makeFxomDocumentFromImageURL(image, fitWidth, fitHeight);
    }
    
    private static FXOMDocument makeFxomDocumentFromImageURL(
        final Image image, final double fitWidth, final double fitHeight) {
        
        final var result = new FXOMDocument();
        final var imageView = new FXOMInstance(result, ImageView.class);

        final var imageName = new PropertyName("image"); //NOI18N
        final var fitWidthName = new PropertyName("fitWidth"); //NOI18N
        final var fitHeightName = new PropertyName("fitHeight"); //NOI18N

        final var imageViewMeta
                = Metadata.getMetadata().queryComponentMetadata(ImageView.class);
        final var imagePropMeta
                = imageViewMeta.lookupProperty(imageName);
        final var fitWidthPropMeta
                = imageViewMeta.lookupProperty(fitWidthName);
        final var fitHeightPropMeta
                = imageViewMeta.lookupProperty(fitHeightName);

        assert imagePropMeta instanceof ImagePropertyMetadata;
        assert fitWidthPropMeta instanceof DoublePropertyMetadata;
        assert fitHeightPropMeta instanceof DoublePropertyMetadata;

        final var imageMeta
                = (ImagePropertyMetadata) imagePropMeta;
        final var fitWidthMeta
                = (DoublePropertyMetadata) fitWidthPropMeta;
        final var fitHeightMeta
                = (DoublePropertyMetadata) fitHeightPropMeta;

        imageMeta.setValue(imageView, new DesignImage(image));
        fitWidthMeta.setValue(imageView, fitWidth);
        fitHeightMeta.setValue(imageView, fitHeight);
        
        result.setFxomRoot(imageView);
        
        return result;
    }

    private static FXOMDocument makeFxomDocumentFromMedia(
        final Media media, final double fitSize) throws IOException {

        assert media != null;
        assert fitSize > 0.0;
        
        final double mediaWidth = media.getWidth();
        final double mediaHeight = media.getHeight();
        
        final double fitWidth, fitHeight;
        final var mediaSize = Math.max(mediaWidth, mediaHeight);
        if (mediaSize < fitSize) {
            fitWidth = 0;
            fitHeight = 0;
        } else {
            final var widthScale  = fitSize / mediaSize;
            final var heightScale = fitSize / mediaHeight;
            final var scale = Math.min(widthScale, heightScale);
            fitWidth = Math.floor(mediaWidth * scale);
            fitHeight = Math.floor(mediaHeight * scale);
        }
        
        return makeFxomDocumentFromMedia(media, fitWidth, fitHeight);
    }
    
    private static FXOMDocument makeFxomDocumentFromMedia(
        final Media media, final double fitWidth, final double fitHeight) {
        
        /*
         * <MediaView fitWidth="200" fitHeight="2003 >
         *   <mediaPlayer>
         *     <MediaPlayer cycleCount="-1">
         *       <media>
         *         <Media>
         *           <source>
         *              <URL value="file:/Users/elp/Dekstop/blah.flv" />
         *           </source>
         *         <Media/>
         *       </media>
         *     </MediaPlayer>
         *   </mediaPlayer>
         * </MediaView>
         */
        
        final var result = new FXOMDocument();
        
        /*
         * URL
         */
        final var valueName
                = new PropertyName("value"); //NOI18N
        final var valueProperty
                = new FXOMPropertyT(result, valueName, media.getSource());
        final var urlInstance
                = new FXOMInstance(result, URL.class);
        valueProperty.addToParentInstance(-1, urlInstance);

        /*
         * Media
         */
        final var sourceName
                = new PropertyName("source"); //NOI18N
        final var sourceProperty
                = new FXOMPropertyC(result, sourceName, urlInstance);
        final var mediaInstance
                = new FXOMInstance(result, Media.class);
        sourceProperty.addToParentInstance(-1, mediaInstance);

        /*
         * MediaPlayer
         */
        final var mediaName
                = new PropertyName("media"); //NOI18N
        final var mediaProperty
                = new FXOMPropertyC(result, mediaName, mediaInstance);
        final var mediaPlayerInstance
                = new FXOMInstance(result, MediaPlayer.class);
        mediaProperty.addToParentInstance(-1, mediaPlayerInstance);
        
        /*
         * MediaView
         */
        final var mediaPlayerName
                = new PropertyName("mediaPlayer"); //NOI18N
        final var mediaPlayerProperty
                = new FXOMPropertyC(result, mediaPlayerName, mediaPlayerInstance);
        final var fitWidthName
                = new PropertyName("fitWidth"); //NOI18N
        final var fitWidthProperty
                = new FXOMPropertyT(result, fitWidthName, String.valueOf(fitWidth));
        final var fitHeightName
                = new PropertyName("fitHeight"); //NOI18N
        final var fitHeightProperty
                = new FXOMPropertyT(result, fitHeightName, String.valueOf(fitHeight));
        final var mediaView
                = new FXOMInstance(result, MediaView.class);
        mediaPlayerProperty.addToParentInstance(-1, mediaView);
        fitWidthProperty.addToParentInstance(-1, mediaView);
        fitHeightProperty.addToParentInstance(-1, mediaView);
        
        result.setFxomRoot(mediaView);
        
        return result;
    }

    
    private static void serializeObjects(final FXOMObject fxomObject, final List<FXOMObject> result) {
        assert fxomObject != null;
        assert result != null;
        
        result.add(fxomObject);
        
        if (fxomObject instanceof final FXOMInstance fxomInstance) {
            for (final var p : fxomInstance.getProperties().values()) {
                if (p instanceof final FXOMPropertyC pc) {
                    for (final var v : pc.getValues()) {
                        serializeObjects(v, result);
                    }
                }
            }
        } else if (fxomObject instanceof final FXOMCollection fxomCollection) {
            for (final var i : fxomCollection.getItems()) {
                serializeObjects(i, result);
            }
        }
    }
}
