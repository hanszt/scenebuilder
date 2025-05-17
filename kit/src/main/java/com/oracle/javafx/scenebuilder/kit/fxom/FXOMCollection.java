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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * 
 */
public class FXOMCollection extends FXOMObject {
    
    private final List<FXOMObject> items = new ArrayList<>();
    private Class<?> declaredClass;

    FXOMCollection(
            final FXOMDocument fxomDocument,
            final GlueElement glueElement,
            final Class<?> declaredClass,
            final Object sceneGraphObject,
            final List<FXOMObject> items) {
        super(fxomDocument, glueElement, sceneGraphObject);
        
        assert (declaredClass != null);
        assert (declaredClass.getSimpleName().equals(glueElement.getTagName()));
        assert sceneGraphObject instanceof Collection;
        assert items != null;
        
        this.declaredClass = declaredClass;
        for (final var i : items) {
            this.items.add(i);
            i.setParentCollection(this);
        }
    }
    
    
    FXOMCollection(
            final FXOMDocument fxomDocument,
            final Class<?> declaredClass) {
        super(fxomDocument, declaredClass.getSimpleName());
        this.declaredClass = declaredClass;
    }
    
    public Class<?> getDeclaredClass() {
        return declaredClass;
    }

    public void setDeclaredClass(final Class<?> declaredClass) {
        this.declaredClass = declaredClass;
    }

    public List<FXOMObject> getItems() {
        return Collections.unmodifiableList(items);
    }    
    
    
    /*
     * FXOMObject
     */

    @Override
    public List<FXOMObject> getChildObjects() {
        return Collections.unmodifiableList(items);
    }


    @Override
    public FXOMObject searchWithSceneGraphObject(final Object sceneGraphObject) {
        FXOMObject result;
        
        result = super.searchWithSceneGraphObject(sceneGraphObject);
        if (result == null) {
            final var it = items.iterator();
            while ((result == null) && it.hasNext()) {
                final var item = it.next();
                result = item.searchWithSceneGraphObject(sceneGraphObject);
            }
        }
        
        return result;
    }

    @Override
    public FXOMObject searchWithFxId(final String fxId) {
        FXOMObject result;
        
        result = super.searchWithFxId(fxId);
        if (result == null) {
            final var it = items.iterator();
            while ((result == null) && it.hasNext()) {
                final var item = it.next();
                result = item.searchWithFxId(fxId);
            }
        }
        
        return result;
    }
    
    @Override
    protected void collectDeclaredClasses(final Set<Class<?>> result) {
        assert result != null;
        
        result.add(declaredClass);
        
        for (final var i : items) {
            i.collectDeclaredClasses(result);
        }
    }

    @Override
    protected void collectProperties(final PropertyName propertyName, final List<FXOMProperty> result) {
        assert propertyName != null;
        assert result != null;
        
        for (final var i : items) {
            i.collectProperties(propertyName, result);
        }
    }

    @Override
    protected void collectNullProperties(final List<FXOMPropertyT> result) {
        assert result != null;
        
        for (final var i : items) {
            i.collectNullProperties(result);
        }
    }

    @Override
    protected void collectPropertiesT(final List<FXOMPropertyT> result) {
        assert result != null;
        
        for (final var i : items) {
            i.collectPropertiesT(result);
        }
    }

    @Override
    protected void collectReferences(final String source, final List<FXOMIntrinsic> result) {
        for (final var i : items) {
            i.collectReferences(source, result);
        }
    }

    @Override
    protected void collectReferences(final String source, final FXOMObject scope, final List<FXOMNode> result) {
        if ((scope == null) || (scope != this)) {
            for (final var i : items) {
                i.collectReferences(source, scope, result);
            }
        }
    }

    @Override
    protected void collectIncludes(final String source, final List<FXOMIntrinsic> result) {
        for (final var i : items) {
            i.collectIncludes(source, result);
        }
    }

    @Override
    protected void collectFxIds(final Map<String, FXOMObject> result) {
        final var fxId = getFxId();
        if (fxId != null) {
            result.put(fxId, this);
        }
        
        for (final var i : items) {
            i.collectFxIds(result);
        }
    }

    @Override
    protected void collectObjectWithSceneGraphObjectClass(final Class<?> sceneGraphObjectClass, final List<FXOMObject> result) {
        if (getSceneGraphObject() != null) {
            if (getSceneGraphObject().getClass() == sceneGraphObjectClass) {
                result.add(this);
            }
            for (final var i : items) {
                i.collectObjectWithSceneGraphObjectClass(sceneGraphObjectClass, result);
            }
        }
    }

    @Override
    protected void collectEventHandlers(final List<FXOMPropertyT> result) {
        if (getSceneGraphObject() != null) {
            for (final var i : items) {
                i.collectEventHandlers(result);
            }
        }
    }

    /*
     * FXOMNode
     */
    
    @Override
    protected void changeFxomDocument(final FXOMDocument destination) {
        
        super.changeFxomDocument(destination);
        for (final var i : items) {
            i.changeFxomDocument(destination);
        }
    }

    @Override
    public void documentLocationWillChange(final URL newLocation) {
        for (final var i : items) {
            i.documentLocationWillChange(newLocation);
        }
    }
    
    
    /*
     * Package
     */
    
    /* Reserved to FXOMObject.addToParentCollection() private use */
    void addValue(final int index, final FXOMObject item) {
        assert item != null;
        assert item.getParentCollection() == this;
        assert !items.contains(item);
        if (index == -1) {
            items.add(item);
        } else {
            items.add(index, item);
        }
    }
    
    /* Reserved to FXOMObject.removeFromParentCollection() private use */
    void removeValue(final FXOMObject item) {
        assert item != null;
        assert item.getParentProperty() == null;
        assert items.contains(item);
        items.remove(item);
    }
}
