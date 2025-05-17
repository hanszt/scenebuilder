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
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.transform.Scale;

/**
 *
 */
public class FitToParentObjectJob extends BatchDocumentJob {

    private final FXOMInstance fxomInstance;
    private final FXOMPropertyC parentProperty;
    private final FXOMInstance parentInstance;

    private enum Sizing {

        HORIZONTAL, VERTICAL
    }

    private enum Anchor {

        TOP, RIGHT, BOTTOM, LEFT
    }

    public FitToParentObjectJob(final FXOMInstance fxomInstance, final EditorController editorController) {
        super(editorController);

        assert fxomInstance != null;
        this.fxomInstance = fxomInstance;
        this.parentProperty = fxomInstance.getParentProperty();
        this.parentInstance = (parentProperty == null) ? null : parentProperty.getParentInstance();
    }

    @Override
    protected List<Job> makeSubJobs() {

        final List<Job> result = new ArrayList<>();

        // Object cannot be root
        if (parentProperty == null) {
            return result; // subJobs is empty => isExecutable will return false
        }
        // Object must be a node
        final var childObject = fxomInstance.getSceneGraphObject();
        if (!(childObject instanceof final Node childNode)) {
            return result; // subJobs is empty => isExecutable will return false
        }
        // Preview version : Node must be resizable (as in SB 1.1)
        // TODO : if the object is not resizable, 
        // update its bounds but do not anchor it.
        if (!childNode.isResizable()) {
            return result; // subJobs is empty => isExecutable will return false
        }
        // Preview version : Parent node must be an AnchorPane (as in SB 1.1)
        // TODO : if the object container is a Pane, 
        // update its bounds but do not anchor it.
        final var parentObject = parentInstance.getSceneGraphObject();
        if (!(parentObject instanceof final AnchorPane parentNode)) {
            return result; // subJobs is empty => isExecutable will return false
        }

        final var childBounds = childNode.getLayoutBounds();
        final var parentBounds = parentNode.getLayoutBounds();
        Scale scale = null;
        for (final var transform : childNode.getTransforms()) {
            if (transform instanceof Scale) {
                scale = (Scale) transform;
            }
        }
        final var scaleX = scale == null ? 1.0 : scale.getX();
        final var scaleY = scale == null ? 1.0 : scale.getY();
        final var sizing = getSizingMask(childNode);
        final var isResizableX = sizing.contains(Sizing.HORIZONTAL);
        final var isResizableY = sizing.contains(Sizing.VERTICAL);
        final var leftAnchorValue = isResizableX ? 0
                : (parentBounds.getWidth() - childBounds.getWidth() * scaleX) / 2.0;
        final var topAnchorValue = isResizableY ? 0
                : (parentBounds.getHeight() - childBounds.getHeight() * scaleY) / 2.0;
        final var prefWidthValue = isResizableX ? parentBounds.getWidth() / scaleY
                : childBounds.getWidth();
        final var prefHeightValue = isResizableY ? parentBounds.getHeight() / scaleY
                : childBounds.getHeight();

        // Modify pref size jobs
        //----------------------------------------------------------------------
        final var prefWidthJob = modifyJob("prefWidth", prefWidthValue);
        if (prefWidthJob.isExecutable()) { // Update if new value differs from old one
            result.add(prefWidthJob);
        }
        final var prefHeightJob = modifyJob("prefHeight", prefHeightValue);
        if (prefHeightJob.isExecutable()) { // Update if new value differs from old one
            result.add(prefHeightJob);
        }

        // Modify Anchors Jobs
        //----------------------------------------------------------------------
        final var leftAnchorJob = modifyAnchorJob(Anchor.LEFT, leftAnchorValue);
        if (leftAnchorJob.isExecutable()) { // Update if new value differs from old one
            result.add(leftAnchorJob);
        }
        final var topAnchorJob = modifyAnchorJob(Anchor.TOP, topAnchorValue);
        if (topAnchorJob.isExecutable()) { // Update if new value differs from old one
            result.add(topAnchorJob);
        }
        if (isResizableX) {
            final var rightAnchorJob = modifyAnchorJob(Anchor.RIGHT, 0.0);
            if (rightAnchorJob.isExecutable()) { // Update if new value differs from old one
                result.add(rightAnchorJob);
            }
        }
        if (isResizableY) {
            final var bottomAnchorJob = modifyAnchorJob(Anchor.BOTTOM, 0.0);
            if (bottomAnchorJob.isExecutable()) { // Update if new value differs from old one
                result.add(bottomAnchorJob);
            }
        }
        return result;
    }
    
    @Override
    protected String makeDescription() {
        final var sb = new StringBuilder();
        sb.append("Fit to Parent ");
        final var sceneGraphObject = fxomInstance.getSceneGraphObject();
        assert sceneGraphObject != null;
        sb.append(sceneGraphObject.getClass().getSimpleName());
        return sb.toString();
    }

    private Job modifyJob(final Class<?> clazz, final String name, final double value) {
        final var pn = new PropertyName(name, clazz);
        final var vpm
                = Metadata.getMetadata().queryValueProperty(fxomInstance, pn);
        final var subJob = new ModifyObjectJob(
                fxomInstance, vpm, value, getEditorController());
        return subJob;
    }

    private Job modifyJob(final String name, final double value) {
        return modifyJob(null, name, value);
    }

    private Job modifyAnchorJob(final Anchor anchor, final double value) {
        final var name = anchor.name().toLowerCase(Locale.ROOT) + "Anchor";
        return modifyJob(AnchorPane.class, name, value);
    }

    private Set<Sizing> getSizingMask(final Node node) {
        final Set<Sizing> result;

        // ScrollBar
        if (node instanceof final ScrollBar scrollBar) {
            result = getSizingMask(scrollBar.getOrientation());
        } //
        // Separator
        else if (node instanceof final Separator separator) {
            result = getSizingMask(separator.getOrientation());
        } //
        // Slider
        else if (node instanceof final Slider slider) {
            result = getSizingMask(slider.getOrientation());
        } //
        else {
            result = EnumSet.of(Sizing.HORIZONTAL, Sizing.VERTICAL);
        }
        return result;
    }

    private Set<Sizing> getSizingMask(final Orientation orientation) {
        assert orientation != null;
        final Set<Sizing> result = switch (orientation) {
            case HORIZONTAL -> EnumSet.of(Sizing.HORIZONTAL);
            case VERTICAL -> EnumSet.of(Sizing.VERTICAL);
            default -> {
                assert false : "unexpected orientation: " + orientation;
                yield null;
            }
        };
        return result;
    }
}
