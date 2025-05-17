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

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class ModifyCacheHintJob extends ModifySelectionJob {

    private int subJobCount = 0;
    private final PropertyName cachePN = new PropertyName("cache"); //NOI18N
    private final PropertyName cacheHintPN = new PropertyName("cacheHint"); //NOI18N

    public ModifyCacheHintJob(final ValuePropertyMetadata propertyMetadata, final Object newValue, final EditorController editorController) {
        super(propertyMetadata, newValue, editorController);
        assert cacheHintPN.equals(propertyMetadata.getName());
    }

    @Override
    protected List<Job> makeSubJobs() {

        final List<Job> result = new ArrayList<>();
        final Set<FXOMInstance> candidates = new HashSet<>();
        final var selection = getEditorController().getSelection();
        if (selection.getGroup() instanceof final ObjectSelectionGroup osg) {
            for (final var fxomObject : osg.getItems()) {
                if (fxomObject instanceof FXOMInstance) {
                    candidates.add((FXOMInstance) fxomObject);
                }
            }
        } else {
            assert selection.getGroup() == null : "Add implementation for " + selection.getGroup();
        }

        // Add ModifyObject jobs
        for (final var fxomInstance : candidates) {
            // ModifyObject job for the cacheHint property
            final var subJob1 = new ModifyObjectJob(
                    fxomInstance, propertyMetadata, newValue, getEditorController());
            if (subJob1.isExecutable()) {
                result.add(subJob1);
                subJobCount++;
            }
            // ModifyObject job for the cache property
            if (!"DEFAULT".equals(newValue)) { //NOI18N
                final var cacheVPM
                        = Metadata.getMetadata().queryValueProperty(fxomInstance, cachePN);
                final var subJob2 = new ModifyObjectJob(
                        fxomInstance, cacheVPM, Boolean.TRUE, getEditorController());
                if (subJob2.isExecutable()) {
                    result.add(subJob2);
                }
            }
        }

        return result;
    }

    @Override
    protected String makeDescription() {
        final String result = switch (subJobCount) {
            case 0 -> "Unexecutable Set"; //NOI18N
            case 1 -> // Single selection
                    getSubJobs().getFirst().getDescription();
            default -> I18N.getString("label.action.edit.set.n",
                    propertyMetadata.getName().toString(),
                    subJobCount);
        };

        return result;
    }
}
