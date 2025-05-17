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
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ClipboardDecoder;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.input.Clipboard;

/**
 *
 */
public class PasteJob extends BatchSelectionJob {
    
    private FXOMObject targetObject;
    private List<FXOMObject> newObjects;

    public PasteJob(final EditorController editorController) {
        super(editorController);
    }

    @Override
    protected List<Job> makeSubJobs() {
        final List<Job> result = new ArrayList<>();
        
        final var fxomDocument = getEditorController().getFxomDocument();
        if (fxomDocument != null) {

            // Retrieve the FXOMObjects from the clipboard
            final var clipboardDecoder
                    = new ClipboardDecoder(Clipboard.getSystemClipboard());
            newObjects = clipboardDecoder.decode(fxomDocument);
            assert newObjects != null; // But possible empty

            if (!newObjects.isEmpty()) {
                
                // Retrieve the target FXOMObject :
                // If the document is empty (root object is null), then the target 
                // object is null.
                // If the selection is root or is empty, the target object is
                // the root object.
                // Otherwise, the target object is the selection common ancestor.
                if (fxomDocument.getFxomRoot() == null) {
                    targetObject = null;
                } else {
                    final var selection = getEditorController().getSelection();
                    final var rootObject = fxomDocument.getFxomRoot();
                    if (selection.isEmpty() || selection.isSelected(rootObject)) {
                        targetObject = rootObject;
                    } else {
                        targetObject = selection.getAncestor();
                    }
                }
                assert (targetObject != null) || (fxomDocument.getFxomRoot() == null);

                if (targetObject == null) {
                    // Document is empty : only one object can be inserted
                    if (newObjects.size() == 1) {
                        final var newObject0 = newObjects.getFirst();
                        final var subJob = new SetDocumentRootJob(
                                newObject0,
                                getEditorController());
                        result.add(subJob);
                    }
                } else {
                    // Build InsertAsSubComponent jobs
                    final var targetMask = new DesignHierarchyMask(targetObject);
                    if (targetMask.isAcceptingSubComponent(newObjects)) {
                        
                        final double relocateDelta;
                        if (targetMask.isFreeChildPositioning()) {
                            final var pasteJobCount = countPasteJobs();
                            relocateDelta = 10.0 * (pasteJobCount + 1);
                        } else {
                            relocateDelta = 0.0;
                        }
                        for (final var newObject : newObjects) {
                            final var subJob = new InsertAsSubComponentJob(
                                    newObject,
                                    targetObject,
                                    targetMask.getSubComponentCount(),
                                    getEditorController());
                            result.addFirst(subJob);
                            if ((relocateDelta != 0.0) && newObject.isNode()) {
                                final var sceneGraphNode = (Node) newObject.getSceneGraphObject();
                                final var relocateJob = new RelocateNodeJob(
                                        (FXOMInstance) newObject,
                                        sceneGraphNode.getLayoutX() + relocateDelta,
                                        sceneGraphNode.getLayoutY() + relocateDelta,
                                        getEditorController()
                                );
                                result.add(relocateJob);
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }

    
    @Override
    protected String makeDescription() {
        final String result;
        
        if (newObjects.size() == 1) {
            result = makeSingleSelectionDescription();
        } else {
            result = makeMultipleSelectionDescription();
        }
        
        return result;
    }

    @Override
    protected AbstractSelectionGroup getNewSelectionGroup() {
        assert newObjects != null; // But possibly empty
        if (newObjects.isEmpty()) {
            return null;
        } else {
            return new ObjectSelectionGroup(newObjects, newObjects.getFirst(), null);
        }
    }

    /*
     * Private
     */
    private String makeSingleSelectionDescription() {
        final String result;

        assert newObjects.size() == 1;
        final var newObject = newObjects.getFirst();
        if (newObject instanceof FXOMInstance) {
            final var sceneGraphObject = newObject.getSceneGraphObject();
            if (sceneGraphObject != null) {
                result = I18N.getString("label.action.edit.paste.1", sceneGraphObject.getClass().getSimpleName());
            } else {
                result = I18N.getString("label.action.edit.paste.unresolved");
            }
        } else if (newObject instanceof FXOMCollection) {
            result = I18N.getString("label.action.edit.paste.collection");
        } else {
            assert false;
            result = I18N.getString("label.action.edit.paste.1", newObject.getClass().getSimpleName());
        }

        return result;
    }

    private String makeMultipleSelectionDescription() {
        final var objectCount = newObjects.size();
        return I18N.getString("label.action.edit.paste.n", objectCount);
    }
    
    private int countPasteJobs() {
        var result = 0;
        
        final var undoStack = getEditorController().getJobManager().getUndoStack();
        for (final var job : undoStack) {
            if (job instanceof PasteJob) {
                final var pasteJob = (PasteJob) job;
                if (this.targetObject == pasteJob.targetObject) {
                    result++;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        
        return result;
    }
}
