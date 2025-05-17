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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class BatchJob extends Job {

    private final List<Job> subJobs = new ArrayList<>();
    private final boolean shouldRefreshSceneGraph;
    private final boolean shouldUpdateSelection;
    private final String description;

    public BatchJob(final EditorController editorController,
                    final boolean shouldRefreshSceneGraph,
                    final boolean shouldUpdateSelection,
                    final String description) {
        super(editorController);
        this.description = description;
        this.shouldRefreshSceneGraph = shouldRefreshSceneGraph;
        this.shouldUpdateSelection = shouldUpdateSelection;
    }
    
    public BatchJob(final EditorController editorController,
                    final boolean shouldRefreshSceneGraph, final String description) {
        super(editorController);
        this.description = description;
        this.shouldRefreshSceneGraph = shouldRefreshSceneGraph;
        this.shouldUpdateSelection = true;
    }
    
     public BatchJob(final EditorController editorController, final String description) {
         super(editorController);
         this.description = description;
         this.shouldRefreshSceneGraph = true;
         this.shouldUpdateSelection = true;
    }
    
   public BatchJob(final EditorController editorController) {
        super(editorController);
        this.description = getClass().getSimpleName();
        this.shouldRefreshSceneGraph = true;
        this.shouldUpdateSelection = true;
    }
    
    public void addSubJob(final Job subJob) {
        assert subJob != null;
        this.subJobs.add(subJob);
    }

    public void addSubJobs(final List<Job> subJobs) {
        assert subJobs != null;
        this.subJobs.addAll(subJobs);
    }

    public void prependSubJob(final Job subJob) {
        assert subJob != null;
        this.subJobs.addFirst(subJob);
    }

    public List<Job> getSubJobs() {
        return Collections.unmodifiableList(subJobs);
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
        final var selection = getEditorController().getSelection();
        final var fxomDocument = getEditorController().getFxomDocument();
        if (shouldUpdateSelection) {
            selection.beginUpdate();
        }
        if (shouldRefreshSceneGraph) {
            fxomDocument.beginUpdate();
        }
        for (final var subJob : subJobs) {
            subJob.execute();
        }
        if (shouldRefreshSceneGraph) {
            fxomDocument.endUpdate();
        }
        if (shouldUpdateSelection) {
            selection.endUpdate();
        }
    }

    @Override
    public void undo() {
        final var selection = getEditorController().getSelection();
        final var fxomDocument = getEditorController().getFxomDocument();
        if (shouldUpdateSelection) {
            selection.beginUpdate();
        }
        if (shouldRefreshSceneGraph) {
            fxomDocument.beginUpdate();
        }
        for (var i = subJobs.size() - 1; i >= 0; i--) {
            subJobs.get(i).undo();
        }
        if (shouldRefreshSceneGraph) {
            fxomDocument.endUpdate();
        }
        if (shouldUpdateSelection) {
            selection.endUpdate();
        }
    }

    @Override
    public void redo() {
        final var selection = getEditorController().getSelection();
        final var fxomDocument = getEditorController().getFxomDocument();
        
        if (shouldUpdateSelection) {
            selection.beginUpdate();
        }
        if (shouldRefreshSceneGraph) {
            fxomDocument.beginUpdate();
        }
        for (final var subJob : subJobs) {
            subJob.redo();
        }
        if (shouldRefreshSceneGraph) {
            fxomDocument.endUpdate();
        }
        if (shouldUpdateSelection) {
            selection.endUpdate();
        }
    }

    @Override
    public String getDescription() {
        return description;
    }
    
}
