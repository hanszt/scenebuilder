/*
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

import com.oracle.javafx.scenebuilder.kit.fxom.glue.GlueElement;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;

import java.net.URL;
import java.util.*;

/**
 * FXOM for special elements like includes or references.
 * 
 */
public class FXOMIntrinsic extends FXOMObject {

    private static final String CHARSET_PROPERTY = "charset";
    private static final String SOURCE_PROPERTY = "source";

    public enum Type {
        FX_INCLUDE,
        FX_REFERENCE,
        FX_COPY,
        UNDEFINED
    }
    
    private final Map<PropertyName, FXOMProperty> properties = new LinkedHashMap<>();
    private Object sourceSceneGraphObject;

    
    FXOMIntrinsic(final FXOMDocument document, final GlueElement glueElement, final Object targetSceneGraphObject, final List<FXOMProperty> properties) {
        super(document, glueElement, null);
        this.sourceSceneGraphObject = targetSceneGraphObject;
        for (final var p : properties) {
            this.properties.put(p.getName(), p);
        }
    }
    
    public FXOMIntrinsic(final FXOMDocument document, final Type type, final String source) {
        super(document, makeTagNameFromType(type));
        getGlueElement().getAttributes().put(SOURCE_PROPERTY, source);
    }

    public void addIntrinsicProperty(final FXOMDocument fxomDocument) {
        final var attributes = this.getGlueElement().getAttributes();
        if(attributes.containsKey(CHARSET_PROPERTY)) {
            createAndInsertProperty(attributes, fxomDocument, CHARSET_PROPERTY);
        }
        if(attributes.containsKey(SOURCE_PROPERTY)) {
            createAndInsertProperty(attributes, fxomDocument, SOURCE_PROPERTY);
        }
    }

    private void createAndInsertProperty(final Map<String, String> attributes, final FXOMDocument fxomDocument, final String propertyKey) {
        final var valueString = attributes.get(propertyKey);
        final var propertyName = new PropertyName(propertyKey);
        final FXOMProperty property = new FXOMPropertyT(fxomDocument, propertyName, valueString);
        this.getProperties().put(propertyName, property);
    }

    public void removeCharsetProperty() {
        final var attributes = this.getGlueElement().getAttributes();
        if(attributes.containsKey(CHARSET_PROPERTY)) {
            attributes.remove(CHARSET_PROPERTY);
            final var charsetPropertyName = new PropertyName(CHARSET_PROPERTY);
            this.getProperties().remove(charsetPropertyName);
        }
    }

    public Type getType() {
        return switch (getGlueElement().getTagName()) {
            case "fx:include" -> Type.FX_INCLUDE;
            case "fx:reference" -> Type.FX_REFERENCE;
            case "fx:copy" -> Type.FX_COPY;
            default -> Type.UNDEFINED;
        };
    }
    
    public String getSource() {
        return getGlueElement().getAttributes().get(SOURCE_PROPERTY);
    }

    public void setSource(final String source) {
        if (source == null) {
            getGlueElement().getAttributes().remove(SOURCE_PROPERTY);
        } else {
            getGlueElement().getAttributes().put(SOURCE_PROPERTY, source);
        }
    }
    
    public Object getSourceSceneGraphObject() {
        return sourceSceneGraphObject;
    }

    public void setSourceSceneGraphObject(final Object sourceSceneGraphObject) {
        this.sourceSceneGraphObject = sourceSceneGraphObject;
    }
    
    public Map<PropertyName, FXOMProperty> getProperties() {
        return properties;
    }

    public void fillProperties(final Map<PropertyName, FXOMProperty> properties ) {
        for (final var p : properties.values()) {
            this.properties.put(p.getName(), p);
        }
    }

    public FXOMInstance createFxomInstanceFromIntrinsic() {
        final var fxomInstance = new FXOMInstance(this.getFxomDocument(), this.getGlueElement());
        fxomInstance.setSceneGraphObject(this.getSourceSceneGraphObject());
        fxomInstance.setDeclaredClass(this.getClass());
        if(!this.getProperties().isEmpty()) {
            fxomInstance.fillProperties(this.getProperties());
        }
        return fxomInstance;
    }

    /*
     * FXOMObject
     */

    @Override
    public List<FXOMObject> getChildObjects() {
        // Intrinsics have not children
        return Collections.emptyList();
    }


    @Override
    public FXOMObject searchWithSceneGraphObject(final Object sceneGraphObject) {
        final FXOMObject result;
        
        if (getType() == Type.FX_INCLUDE) {
            result = super.searchWithSceneGraphObject(sceneGraphObject);
        } else {
            result = null;
        }
        
        return result;
    }

    @Override
    public FXOMObject searchWithFxId(final String fxId) {
        final FXOMObject result;
        
        if (getType() == Type.FX_INCLUDE) {
            result = super.searchWithFxId(fxId);
        } else {
            result = null;
        }
        
        return result;
    }

    @Override
    protected void collectDeclaredClasses(final Set<Class<?>> result) {
        // Nothing to collect in this kind of object
    }

    @Override
    protected void collectNullProperties(final List<FXOMPropertyT> result) {
        // Nothing to collect in this kind of object
    }

    @Override
    protected void collectPropertiesT(final List<FXOMPropertyT> result) {
        // Nothing to collect in this kind of object
    }

    @Override
    protected void collectProperties(final PropertyName propertyName, final List<FXOMProperty> result) {
        // Nothing to collect in this kind of object
    }

    @Override
    protected void collectReferences(final String source, final List<FXOMIntrinsic> result) {
        assert result != null;
        
        if ((getType() == Type.FX_REFERENCE) 
                && ((source == null) || source.equals(getSource()))) {
            result.add(this);
        }
    }

    @Override
    protected void collectReferences(final String source, final FXOMObject scope, final List<FXOMNode> result) {
        assert result != null;
        if ((scope != this)) {
            if ((getType() == Type.FX_REFERENCE) 
                    && ((source == null) || source.equals(getSource()))) {
                result.add(this);
            }
        }
    }

    @Override
    protected void collectIncludes(final String source, final List<FXOMIntrinsic> result) {
        assert result != null;
        
        if ((getType() == Type.FX_INCLUDE) 
                && ((source == null) || source.equals(getSource()))) {
            result.add(this);
        }
    }

    @Override
    protected void collectFxIds(final Map<String, FXOMObject> result) {
        final var fxId = getFxId();
        if (fxId != null) {
            result.put(fxId, this);
        }
    }

    @Override
    protected void collectObjectWithSceneGraphObjectClass(final Class<?> sceneGraphObjectClass, final List<FXOMObject> result) {
        // Nothing to collect in this kind of object
    }

    @Override
    protected void collectEventHandlers(final List<FXOMPropertyT> result) {
        // Nothing to collect in this kind of object
    }

    /*
     * FXOMNode
     */
    
    @Override
    public void documentLocationWillChange(final URL newLocation) {
        // Nothing special to do here
    }
    
    
    /*
     * Private
     */
    
    private static String makeTagNameFromType(final Type type) {
        return switch (type) {
            case FX_COPY -> "fx:copy";
            case FX_REFERENCE -> "fx:reference";
            case FX_INCLUDE -> "fx:include";
            case UNDEFINED -> throw new IllegalStateException("Unexpected intrinsic type " + type);
        };
    }
}
