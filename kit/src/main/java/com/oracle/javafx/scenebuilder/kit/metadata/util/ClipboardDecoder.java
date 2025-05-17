/*
 * Copyright (c) 2022, Gluon and/or its affiliates.
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

package com.oracle.javafx.scenebuilder.kit.metadata.util;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMArchive;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument.FXOMDocumentSwitch;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNodes;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import javafx.scene.input.Clipboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class ClipboardDecoder {

    final Clipboard clipboard;

    public ClipboardDecoder(final Clipboard clipboard) {
        this.clipboard = clipboard;
    }

    public List<FXOMObject> decode(final FXOMDocument targetDocument) {
        assert targetDocument != null;

        List<FXOMObject> result = null;

        // SB_DATA_FORMAT
        if (clipboard.hasContent(ClipboardEncoder.SB_DATA_FORMAT)) {
            final var content = clipboard.getContent(ClipboardEncoder.SB_DATA_FORMAT);
            if (content instanceof final FXOMArchive archive) {
                try {
                    result = archive.decode(targetDocument);
                } catch (final IOException x) {
                    // Do nothing
                }
            }
        }

        // FXML_DATA_FORMAT
        if ((result == null)
            && clipboard.hasContent(ClipboardEncoder.FXML_DATA_FORMAT)) {
            final var content = clipboard.getContent(ClipboardEncoder.FXML_DATA_FORMAT);
            if (content instanceof final String fxmlText) {
                try {
                    final var location = targetDocument.getLocation();
                    final var classLoader = targetDocument.getClassLoader();
                    final var resources = targetDocument.getResources();
                    final var transientDoc
                        = new FXOMDocument(fxmlText, location, classLoader, resources, FXOMDocumentSwitch.NORMALIZED);
                    result = Collections.singletonList(transientDoc.getFxomRoot());
                } catch (final IOException x) {
                    // Do nothing
                }
            }
        }

        // DataFormat.FILES
        if ((result == null) && clipboard.hasFiles()) {
            result = new ArrayList<>();
            for (final var file : clipboard.getFiles()) {
                try {
                    final var newObject = FXOMNodes.newObject(targetDocument, file);
                    // newObject is null when file is empty
                    if (newObject != null) {
                        result.add(newObject);
                    }
                } catch (final IOException x) {
                    // Then we silently ignore this file
                }
            }
        }

        // If nothing is exploitable, we return a list.
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }
}
