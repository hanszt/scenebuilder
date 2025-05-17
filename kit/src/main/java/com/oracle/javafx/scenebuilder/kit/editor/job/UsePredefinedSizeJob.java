/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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

import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController.Size;
import com.oracle.javafx.scenebuilder.kit.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;

/**
 * Job to use for setting the size of the given FXOMObject; when not provided
 * deal with the top level item of the layout. The job will set the preferred
 * width and height to the given value while min and max width and height are
 * set to Region.USE_PREF_SIZE.
 * No action is taken unless the FXOMObject is an instance of Region or WebView.
 */
public class UsePredefinedSizeJob extends Job {

    private final List<ModifyObjectJob> subJobs = new ArrayList<>();
    private String description; // final but initialized lazily
    private final Size size;
    private final EditorController editorController;
    private final FXOMObject fxomObject;

    public UsePredefinedSizeJob(final EditorController editorController, final Size size, final FXOMObject fxomObject) {
        super(editorController);
        this.editorController = editorController;
        this.size = size;
        this.fxomObject = fxomObject;
        buildSubJobs();
    }

    public UsePredefinedSizeJob(final EditorController editorController, final Size size) {
        super(editorController);
        this.editorController = editorController;
        this.size = size;
        if (editorController.getFxomDocument() == null) {
            this.fxomObject = null;
        } else {
            var fxomObject = editorController.getFxomDocument().getFxomRoot();

            if (fxomObject != null && fxomObject.getSceneGraphObject() instanceof Scene) {
                // Set the size of the scene's root
                final var mask = new DesignHierarchyMask(fxomObject);
                fxomObject = mask.getAccessory(DesignHierarchyMask.Accessory.ROOT);
                assert fxomObject != null;
            }

            this.fxomObject = fxomObject;
        }
        buildSubJobs();
    }
    
    /*
     * Job
     */
    @Override
    public boolean isExecutable() {
        return !subJobs.isEmpty();
    }

    @Override
    public void execute() {
        final var fxomDocument = editorController.getFxomDocument();
        fxomDocument.beginUpdate();
        for (final var subJob : subJobs) {
            subJob.execute();
        }
        fxomDocument.endUpdate();
    }

    @Override
    public void undo() {
        final var fxomDocument = editorController.getFxomDocument();
        fxomDocument.beginUpdate();
        for (var i = subJobs.size() - 1; i >= 0; i--) {
            subJobs.get(i).undo();
        }
        fxomDocument.endUpdate();
    }

    @Override
    public void redo() {
        final var fxomDocument = editorController.getFxomDocument();
        fxomDocument.beginUpdate();
        for (final var subJob : subJobs) {
            subJob.redo();
        }
        fxomDocument.endUpdate();
    }

    @Override
    public String getDescription() {
        if (description == null) {
            description = I18N.getString("job.set.size",
                    JobUtils.getStringFromDouble(getWidthFromSize(size)),
                    JobUtils.getStringFromDouble(getHeightFromSize(size)));
        }
        return description;
    }

    private void buildSubJobs() {

        if (editorController.getFxomDocument() != null && (fxomObject instanceof final FXOMInstance fxomInstance)) {
            final var sceneGraphObject = fxomInstance.getSceneGraphObject();
            
            if (sceneGraphObject instanceof WebView
                    || sceneGraphObject instanceof Region) {
                subJobs.addAll(modifyHeightJobs(fxomInstance));
                subJobs.addAll(modifyWidthJobs(fxomInstance));
            }
        }
    }

    private List<ModifyObjectJob> modifyHeightJobs(final FXOMInstance candidate) {
        final List<ModifyObjectJob> result = new ArrayList<>();

        final var maxHeight = new PropertyName("maxHeight"); //NOI18N
        final var minHeight = new PropertyName("minHeight"); //NOI18N
        final var prefHeight = new PropertyName("prefHeight"); //NOI18N

        final var maxHeightVPM
                = Metadata.getMetadata().queryValueProperty(candidate, maxHeight);
        final var minHeightVPM
                = Metadata.getMetadata().queryValueProperty(candidate, minHeight);
        final var prefHeightVPM
                = Metadata.getMetadata().queryValueProperty(candidate, prefHeight);

        final var maxHeightJob = new ModifyObjectJob(
                candidate, maxHeightVPM, Region.USE_PREF_SIZE, editorController);
        final var minHeightJob = new ModifyObjectJob(
                candidate, minHeightVPM, Region.USE_PREF_SIZE, editorController);
        final var prefHeightJob = new ModifyObjectJob(
                candidate, prefHeightVPM, getHeightFromSize(size), editorController);

        if (maxHeightJob.isExecutable()) {
            result.add(maxHeightJob);
        }
        if (minHeightJob.isExecutable()) {
            result.add(minHeightJob);
        }
        if (prefHeightJob.isExecutable()) {
            result.add(prefHeightJob);
        }
        return result;
    }

    private List<ModifyObjectJob> modifyWidthJobs(final FXOMInstance candidate) {
        final List<ModifyObjectJob> result = new ArrayList<>();

        final var maxWidth = new PropertyName("maxWidth"); //NOI18N
        final var minWidth = new PropertyName("minWidth"); //NOI18N
        final var prefWidth = new PropertyName("prefWidth"); //NOI18N

        final var maxWidthVPM
                = Metadata.getMetadata().queryValueProperty(candidate, maxWidth);
        final var minWidthVPM
                = Metadata.getMetadata().queryValueProperty(candidate, minWidth);
        final var prefWidthVPM
                = Metadata.getMetadata().queryValueProperty(candidate, prefWidth);

        final var maxWidthJob = new ModifyObjectJob(
                candidate, maxWidthVPM, Region.USE_PREF_SIZE, editorController);
        final var minWidthJob = new ModifyObjectJob(
                candidate, minWidthVPM, Region.USE_PREF_SIZE, editorController);
        final var prefWidthJob = new ModifyObjectJob(
                candidate, prefWidthVPM, getWidthFromSize(size), editorController);

        if (maxWidthJob.isExecutable()) {
            result.add(maxWidthJob);
        }
        if (minWidthJob.isExecutable()) {
            result.add(minWidthJob);
        }
        if (prefWidthJob.isExecutable()) {
            result.add(prefWidthJob);
        }
        return result;
    }
    
    private double getWidthFromSize(final Size size) {
        assert size != Size.SIZE_PREFERRED;
        
        if (size == Size.SIZE_DEFAULT) {
            return editorController.getDefaultRootContainerWidth();
        }

        final var sizeString = size.toString();
        return Double.parseDouble(sizeString.substring(5, sizeString.indexOf('x'))); //NOI18N
    }
    
    private double getHeightFromSize(final Size size) {
        assert size != Size.SIZE_PREFERRED;
        
        if (size == Size.SIZE_DEFAULT) {
            return editorController.getDefaultRootContainerHeight();
        }

        final var sizeString = size.toString();
        return Double.parseDouble(sizeString.substring(sizeString.indexOf('x') + 1, sizeString.length())); //NOI18N
    }
}
