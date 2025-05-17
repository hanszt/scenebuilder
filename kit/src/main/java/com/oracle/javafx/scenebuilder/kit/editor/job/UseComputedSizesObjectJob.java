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

import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TableColumnBase;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;

/**
 *
 */
public class UseComputedSizesObjectJob extends BatchDocumentJob {

    private final FXOMInstance fxomInstance;

    public UseComputedSizesObjectJob(final FXOMInstance fxomInstance, final EditorController editorController) {
        super(editorController);
        assert fxomInstance != null;
        this.fxomInstance = fxomInstance;
    }

    @Override
    protected List<Job> makeSubJobs() {

        final List<Job> result = new ArrayList<>();
        final var sceneGraphObject = fxomInstance.getSceneGraphObject();

        // RowConstraints: only height property is meaningfull
        if (sceneGraphObject instanceof RowConstraints) {
            result.addAll(modifyHeightJobs(fxomInstance));
        } //
        // ColumnConstraints: only width property is meaningfull
        else if (sceneGraphObject instanceof ColumnConstraints) {
            result.addAll(modifyWidthJobs(fxomInstance));
        } //
        // Region: both height and width properties are meaningfull
        else if (sceneGraphObject instanceof Region) {
            // First remove anchors if any
            result.addAll(removeAnchorsJobs());
            // Then modify height / width
            result.addAll(modifyHeightJobs(fxomInstance));
            result.addAll(modifyWidthJobs(fxomInstance));
        } //
        // Use computed sizes on ImageView => set the fit size to (0,0)
        else if (sceneGraphObject instanceof ImageView) {
            result.addAll(modifyFitHeightJob(fxomInstance));
            result.addAll(modifyFitWidthJob(fxomInstance));
        } //
        // TableColumnBase: only width property is meaningfull
        else if (sceneGraphObject instanceof TableColumnBase) {
            result.addAll(modifyWidthJobs(fxomInstance));
        }
        return result;
    }

    @Override
    protected String makeDescription() {
        final var sb = new StringBuilder();
        sb.append("Use Computed Sizes on ");
        final var sceneGraphObject = fxomInstance.getSceneGraphObject();
        assert sceneGraphObject != null;
        sb.append(sceneGraphObject.getClass().getSimpleName());
        return sb.toString();
    }

    private List<Job> removeAnchorsJobs() {
        final List<Job> result = new ArrayList<>();
        final var parentObject = fxomInstance.getParentObject();

        if (parentObject != null && parentObject.getSceneGraphObject() instanceof AnchorPane) {
            // switch off AnchorPane Constraints when parent is AnchorPane
            final var topAnchorPN = new PropertyName("topAnchor", AnchorPane.class);
            final var topAnchorVPM
                    = Metadata.getMetadata().queryValueProperty(fxomInstance, topAnchorPN);
            final var rightAnchorPN = new PropertyName("rightAnchor", AnchorPane.class);
            final var rightAnchorVPM
                    = Metadata.getMetadata().queryValueProperty(fxomInstance, rightAnchorPN);
            final var bottomAnchorPN = new PropertyName("bottomAnchor", AnchorPane.class);
            final var bottomAnchorVPM
                    = Metadata.getMetadata().queryValueProperty(fxomInstance, bottomAnchorPN);
            final var leftAnchorPN = new PropertyName("leftAnchor", AnchorPane.class);
            final var leftAnchorVPM
                    = Metadata.getMetadata().queryValueProperty(fxomInstance, leftAnchorPN);
            for (final var vpm : new ValuePropertyMetadata[]{
                topAnchorVPM, rightAnchorVPM, bottomAnchorVPM, leftAnchorVPM}) {

                if (vpm.getValueObject(fxomInstance) != null) {
                    final var subJob = new ModifyObjectJob(
                            fxomInstance, vpm, null, getEditorController());
                    result.add(subJob);
                }
            }
        }
        return result;
    }

    private List<Job> modifyHeightJobs(final FXOMInstance candidate) {
        final List<Job> result = new ArrayList<>();

        final var maxHeight = new PropertyName("maxHeight");
        final var minHeight = new PropertyName("minHeight");
        final var prefHeight = new PropertyName("prefHeight");

        final var maxHeightVPM
                = Metadata.getMetadata().queryValueProperty(candidate, maxHeight);
        final var minHeightVPM
                = Metadata.getMetadata().queryValueProperty(candidate, minHeight);
        final var prefHeightVPM
                = Metadata.getMetadata().queryValueProperty(candidate, prefHeight);

        final var maxHeightJob = new ModifyObjectJob(
                candidate, maxHeightVPM, -1.0, getEditorController());
        final var minHeightJob = new ModifyObjectJob(
                candidate, minHeightVPM, -1.0, getEditorController());
        final var prefHeightJob = new ModifyObjectJob(
                candidate, prefHeightVPM, -1.0, getEditorController());

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

    private List<Job> modifyWidthJobs(final FXOMInstance candidate) {
        final List<Job> result = new ArrayList<>();

        final var maxWidth = new PropertyName("maxWidth");
        final var minWidth = new PropertyName("minWidth");
        final var prefWidth = new PropertyName("prefWidth");

        final var maxWidthVPM
                = Metadata.getMetadata().queryValueProperty(candidate, maxWidth);
        final var minWidthVPM
                = Metadata.getMetadata().queryValueProperty(candidate, minWidth);
        final var prefWidthVPM
                = Metadata.getMetadata().queryValueProperty(candidate, prefWidth);

        final var maxWidthJob = new ModifyObjectJob(
                candidate, maxWidthVPM, -1.0, getEditorController());
        final var minWidthJob = new ModifyObjectJob(
                candidate, minWidthVPM, -1.0, getEditorController());
        final var prefWidthJob = new ModifyObjectJob(
                candidate, prefWidthVPM, -1.0, getEditorController());

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

    private List<Job> modifyFitHeightJob(final FXOMInstance candidate) {
        final List<Job> result = new ArrayList<>();

        final var fitHeight = new PropertyName("fitHeight");
        final var fitHeightVPM
                = Metadata.getMetadata().queryValueProperty(candidate, fitHeight);
        final var fitHeightJob = new ModifyObjectJob(
                candidate, fitHeightVPM, 0.0, getEditorController());

        if (fitHeightJob.isExecutable()) {
            result.add(fitHeightJob);
        }
        return result;
    }

    private List<Job> modifyFitWidthJob(final FXOMInstance candidate) {
        final List<Job> result = new ArrayList<>();

        final var fitWidth = new PropertyName("fitWidth");
        final var fitWidthVPM
                = Metadata.getMetadata().queryValueProperty(candidate, fitWidth);
        final var fitWidthJob = new ModifyObjectJob(
                candidate, fitWidthVPM, 0.0, getEditorController());

        if (fitWidthJob.isExecutable()) {
            result.add(fitWidthJob);
        }
        return result;
    }
}
