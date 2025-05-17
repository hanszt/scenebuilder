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

import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class FXOMCloner {
    
    private final FXOMDocument targetDocument;
    private final FxIdCollector fxIdCollector;
    private FXOMObject clonee;
    private final Set<String> addedFxIds = new HashSet<>();
    
    public FXOMCloner(final FXOMDocument targetDocument) {
        assert targetDocument != null;
        this.targetDocument = targetDocument;
        this.fxIdCollector = new FxIdCollector(targetDocument);
    }
    
    public FXOMDocument getTargetDocument() {
        return targetDocument;
    }
    
    public FXOMObject clone(final FXOMObject clonee) {
        return clone(clonee, false /* preserveCloneFxId */);
    }
    
    public FXOMObject clone(final FXOMObject clonee, final boolean preserveCloneeFxId) {
        assert clonee != null;
        assert addedFxIds.isEmpty();
        
        this.clonee = clonee;
        
        // Creates a deep clone of 'clonee'
        final var result = cloneObject(this.clonee);
        addedFxIds.clear();
        
        // Renames fxid in the clone so that there is no naming
        // conflict when the clone is hooked to its target document.
        renameFxIds(result, preserveCloneeFxId);
        
        return result;
    }
    
    
    /*
     * Private
     */
    
    private FXOMObject cloneObject(final FXOMObject fxomObject) {
        final FXOMObject result;
        
        if (fxomObject instanceof FXOMCollection) {
            result = cloneCollection((FXOMCollection) fxomObject);
        } else if (fxomObject instanceof FXOMInstance) {
            result = cloneInstance((FXOMInstance) fxomObject);
        } else if (fxomObject instanceof FXOMIntrinsic) {
            result = cloneIntrinsic((FXOMIntrinsic) fxomObject);
        } else {
            throw new RuntimeException(getClass().getSimpleName()
                    + " needs some additional implementation"); //NOI18N
        }
        
        return result;
    }
    
    
    private FXOMCollection cloneCollection(final FXOMCollection source) {
        assert source != null;
        
        final var result = new FXOMCollection(
                targetDocument,
                source.getDeclaredClass());
        
        for (final var sourceItem : source.getItems()) {
            final var newItem = cloneObject(sourceItem);
            newItem.addToParentCollection(-1, result);
        }
        
        result.setFxConstant(source.getFxConstant());
        result.setFxController(source.getFxController());
        result.setFxFactory(source.getFxFactory());
        result.setFxId(source.getFxId());
        result.setFxValue(source.getFxValue());
        
        return result;
    }
    
    
    private FXOMInstance cloneInstance(final FXOMInstance source) {
        
        assert source != null;
        
        final FXOMInstance result;
        if (source.getDeclaredClass() == null) {
            assert source.getSceneGraphObject() == null; // source is unresolved
            result = new FXOMInstance(
                    targetDocument,
                    source.getGlueElement().getTagName());
        } else {
            result = new FXOMInstance(
                    targetDocument,
                    source.getDeclaredClass());
        }
        
        for (final var e : source.getProperties().entrySet()) {
            final var newProperty = cloneProperty(e.getValue());
            // Note: cloneProperty() may 
            if (newProperty != null) {
                newProperty.addToParentInstance(-1, result);
            }
        }
        
        result.setFxConstant(source.getFxConstant());
        result.setFxController(source.getFxController());
        result.setFxFactory(source.getFxFactory());
        result.setFxId(source.getFxId());
        result.setFxValue(source.getFxValue());
        
        return result;
    }
    
    
    private FXOMObject cloneIntrinsic(final FXOMIntrinsic source) {
        assert source != null;
        
        final boolean shallowClone;
        final FXOMObject sourceObject;
        switch(source.getType()) {
            case FX_REFERENCE:
            case FX_COPY:
                final var sourceFxId = source.getSource();
                assert sourceFxId != null;
                sourceObject = clonee.getFxomDocument().searchWithFxId(sourceFxId);
                if (isInsideClonee(sourceObject) || addedFxIds.contains(sourceFxId)) {
                    shallowClone = true;
                } else {
                    shallowClone = false;
                    addedFxIds.add(sourceFxId);
                }
                break;
            default:
                sourceObject = null;
                shallowClone = true;
                break;
        }
        
        final FXOMObject result;
        if (shallowClone) {
            // We clone the intrinsic itself
            result = new FXOMIntrinsic(
                    targetDocument,
                    source.getType(),
                    source.getSource());

            result.setFxConstant(source.getFxConstant());
            result.setFxController(source.getFxController());
            result.setFxFactory(source.getFxFactory());
            result.setFxId(source.getFxId());
            result.setFxValue(source.getFxValue());
        }
        else {
            assert sourceObject != null;
            
            // We clone the target of the intrinsic
            result = cloneObject(sourceObject);
        }
        
        
        return result;
    }
    
    
    private FXOMProperty cloneProperty(final FXOMProperty source) {
        final FXOMProperty result;
        
        if (source instanceof FXOMPropertyC) {
            result = clonePropertyC((FXOMPropertyC) source);
        } else if (source instanceof FXOMPropertyT) {
            result = clonePropertyT((FXOMPropertyT) source);
        } else {
            throw new RuntimeException(getClass().getSimpleName()
                    + " needs some additional implementation"); //NOI18N
        }
        
        return result;
    }

    public FXOMPropertyC clonePropertyC(final FXOMPropertyC source) {
        assert source != null;
        
        final var result = new FXOMPropertyC(
                targetDocument,
                source.getName());
        
        for (final var sourceValue : source.getValues()) {
            final var newValue = cloneObject(sourceValue);
            newValue.addToParentProperty(-1, result);
        }
        
        return result;
    }
    
    public FXOMProperty clonePropertyT(final FXOMPropertyT source) {
        assert source != null;
        
        final boolean shallowClone;
        final FXOMObject sourceObject;
        final var pv = new PrefixedValue(source.getValue());
        if (pv.isExpression()) {
            final var sourceFxId = pv.getSuffix();
            assert sourceFxId != null;
            sourceObject = clonee.getFxomDocument().searchWithFxId(sourceFxId);
            assert sourceObject != null : "sourceFxId=" + sourceFxId;
            if (isInsideClonee(sourceObject) || addedFxIds.contains(sourceFxId)) {
                shallowClone = true;
            } else {
                shallowClone = false;
                addedFxIds.add(sourceFxId);
            }
        } else {
            shallowClone = true;
            sourceObject = null;
        }
        
        final FXOMProperty result;
        if (shallowClone) {
            result = new FXOMPropertyT(
                    targetDocument,
                    source.getName(),
                    source.getValue());
        } else {
            assert sourceObject != null;
            if (FXOMNodes.isWeakReference(source)) {
                result = null;
            } else {
                result = new FXOMPropertyC(
                        targetDocument,
                        source.getName(),
                        cloneObject(sourceObject));
            }
        }
        
        return result;
    }
    
    private boolean isInsideClonee(final FXOMObject object) {
        assert object != null;
        return (object == clonee) || object.isDescendantOf(clonee);
    }    
    
    private void renameFxIds(final FXOMObject clone, final boolean preserveCloneeFxId) {
        
        final var fxIds = clone.collectFxIds();
        
        if (preserveCloneeFxId && (clonee.getFxId() != null)) {
            // We don't apply renaming to the fx:id of the clonee
            assert clonee.getFxId().equals(clone.getFxId());
            assert fxIds.get(clonee.getFxId()) == clone;
            fxIds.remove(clonee.getFxId());
        }
        
        for (final var e : fxIds.entrySet()) {
            final var candidateFxId = e.getKey();
            final var declarer = e.getValue();
            
            final var renamedFxId = fxIdCollector.importFxId(candidateFxId);
            
            if (!renamedFxId.equals(candidateFxId)) {
                
                /*
                 * We renamed candidateFxId as renamedFxId 
                 *  1) on the declarer object
                 *  2) in each fx:reference/fx:copy object
                 *  3) in each xxx="$candidateFxId" expression property //NOI18N
                 */
                
                // 1)
                declarer.setFxId(renamedFxId);

                // 2)
                for (final var reference : clone.collectReferences(candidateFxId)) {
                    assert reference.getSource().equals(candidateFxId);
                    reference.setSource(renamedFxId);
                }

                // 3)
                final var pv = new PrefixedValue(PrefixedValue.Type.EXPRESSION, renamedFxId);
                final var newValue = pv.toString();
                for (final var reference : FXOMNodes.collectReferenceExpression(clone, candidateFxId)) {
                    reference.setValue(newValue);
                }
            }
        }
        
    }
}
