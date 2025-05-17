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
package com.oracle.javafx.scenebuilder.kit.editor.job;

import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.RelocateNodeJob;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNodes;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;

/**
 *
 */
public class DuplicateSelectionJob extends BatchSelectionJob {

    private final static double offset = 10;
    final Map<FXOMObject, FXOMObject> newFxomObjects = new LinkedHashMap<>();

    public DuplicateSelectionJob(final EditorController editorController) {
        super(editorController);
    }

    @Override
    protected List<Job> makeSubJobs() {
        final List<Job> result = new LinkedList<>();

        if (canDuplicate()) { // (1)
            
            final var selection = getEditorController().getSelection();
            final var asg = selection.getGroup();
            assert asg instanceof ObjectSelectionGroup; // Because of (1)
            final var osg = (ObjectSelectionGroup) asg;
            assert osg.hasSingleParent(); // Because of (1)
            final var targetObject = osg.getAncestor();
            assert targetObject != null; // Because of (1)
            final var targetDocument = getEditorController().getFxomDocument();
            for (final var selectedObject : osg.getSortedItems()) {
                final var newDocument = FXOMNodes.newDocument(selectedObject);
                final var newObject = newDocument.getFxomRoot();
                newObject.moveToFxomDocument(targetDocument);
                assert newDocument.getFxomRoot() == null;
                newFxomObjects.put(selectedObject, newObject);
            }
            assert !newFxomObjects.isEmpty(); // Because of (1)

            // Build InsertAsSubComponent jobs
            final var targetMask = new DesignHierarchyMask(targetObject);
            if (targetMask.isAcceptingSubComponent(newFxomObjects.keySet())) {
                var index = 0;
                for (final var entry : newFxomObjects.entrySet()) {
                    final var selectedFxomObject = entry.getKey();
                    final var newFxomObject = entry.getValue();
                    final var insertSubJob = new InsertAsSubComponentJob(
                            newFxomObject,
                            targetObject,
                            targetMask.getSubComponentCount() + index++,
                            getEditorController());
                    result.add(insertSubJob);
                    final var selectedSceneGraphObject = selectedFxomObject.getSceneGraphObject();
                    // Relocate duplicated objects if needed
                    if (selectedSceneGraphObject instanceof Node) {
                        final var selectedNode = (Node) selectedSceneGraphObject;
                        final double newLayoutX = Math.round(selectedNode.getLayoutX() + offset);
                        final double newLayoutY = Math.round(selectedNode.getLayoutY() + offset);
                        assert newFxomObject instanceof FXOMInstance;
                        final var relocateSubJob = new RelocateNodeJob(
                                (FXOMInstance) newFxomObject,
                                newLayoutX,
                                newLayoutY,
                                getEditorController());
                        result.add(relocateSubJob);
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected String makeDescription() {
        final String result;
        assert !newFxomObjects.values().isEmpty();
        if (newFxomObjects.values().size() == 1) {
            result = makeSingleSelectionDescription();
        } else {
            result = makeMultipleSelectionDescription();
        }

        return result;
    }

    @Override
    protected AbstractSelectionGroup getNewSelectionGroup() {
        assert newFxomObjects != null; // But possibly empty
        if (newFxomObjects.isEmpty()) {
            return null;
        } else {
            return new ObjectSelectionGroup(newFxomObjects.values(), newFxomObjects.values().iterator().next(), null);
        }
    }

    private boolean canDuplicate() {
        final var fxomDocument = getEditorController().getFxomDocument();
        if (fxomDocument == null) {
            return false;
        }
        final var selection = getEditorController().getSelection();
        if (selection.isEmpty()) {
            return false;
        }
        final var rootObject = fxomDocument.getFxomRoot();
        if (selection.isSelected(rootObject)) {
            return false;
        }
        final var asg = selection.getGroup();
        if (!(asg instanceof ObjectSelectionGroup)) {
            return false;
        }
        final var osg = (ObjectSelectionGroup) asg;
        for (final var fxomObject : osg.getItems()) {
            if (fxomObject.getSceneGraphObject() == null) { // Unresolved custom type
                return false;
            }
        }
        return osg.hasSingleParent();
    }

    private String makeSingleSelectionDescription() {
        final String result;

        final var newObject = newFxomObjects.values().iterator().next();
        if (newObject instanceof FXOMInstance) {
            final var sceneGraphObject = newObject.getSceneGraphObject();
            if (sceneGraphObject != null) {
                result = I18N.getString("label.action.edit.duplicate.1", sceneGraphObject.getClass().getSimpleName());
            } else {
                result = I18N.getString("label.action.edit.duplicate.unresolved");
            }
        } else if (newObject instanceof FXOMCollection) {
            result = I18N.getString("label.action.edit.duplicate.collection");
        } else {
            assert false;
            result = I18N.getString("label.action.edit.duplicate.1", newObject.getClass().getSimpleName());
        }

        return result;
    }

    private String makeMultipleSelectionDescription() {
        return I18N.getString("label.action.edit.duplicate.n", newFxomObjects.values().size());
    }
}
