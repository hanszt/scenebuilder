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

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class FitToParentSelectionJob extends BatchDocumentJob {

    public FitToParentSelectionJob(final EditorController editorController) {
        super(editorController);
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
        } else if (selection.getGroup() instanceof GridSelectionGroup) {
            // GridPane rows / columns are selected : FitToParentSelectionJob is meaningless
            // Just do nothing
        } else {
            assert selection.getGroup() == null :
                    "Add implementation for " + selection.getGroup();
        }

        for (final var candidate : candidates) {
            final var subJob
                    = new FitToParentObjectJob(candidate, getEditorController());
            if (subJob.isExecutable()) {
                result.add(subJob);
            } else {
                // Should we do best effort here ?
                // Do not clear subJobs list but keep only executable ones
                result.clear(); // -> isExecutable() will return false
                break;
            }
        }
        return result;
    }

    @Override
    protected String makeDescription() {
        final String result = switch (getSubJobs().size()) {
            case 0 -> "Unexecutable Fit To Parent"; // NO18N
            case 1 -> getSubJobs().getFirst().getDescription();
            default -> makeMultipleSelectionDescription();
        };
        return result;
    }

    private String makeMultipleSelectionDescription() {
        final var result = new StringBuilder();
        result.append("Fit To Parent ");
        result.append(getSubJobs().size());
        result.append(" Objects");
        return result.toString();
    }
}
