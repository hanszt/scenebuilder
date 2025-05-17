/*
 * Copyright (c) 2024, Gluon and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.report;

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMAssetIndex;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMCollection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNode;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMNodes;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 *
 * 
 */
public class ErrorReport {

    public ErrorReport() {
        // no-op
    }

    private final Map<FXOMNode, List<ErrorReportEntry>> entries = new HashMap<>();
    private final Map<Path, CSSParsingReport> cssParsingReports = new HashMap<>();
    private FXOMDocument fxomDocument;
    private boolean dirty = true;
    
    public void setFxomDocument(final FXOMDocument fxomDocument) {
        this.fxomDocument = fxomDocument;
        forget();
    }
    
    public void forget() {
        this.entries.clear();
        this.dirty = true;
    }
    
    public List<ErrorReportEntry> query(final FXOMObject fxomObject, final boolean recursive) {
        final List<ErrorReportEntry> result;
        
        updateReport();
        
        final List<ErrorReportEntry> collected = new ArrayList<>();
        if (recursive) {
            collectEntries(fxomObject, collected);
        } else {
            if (entries.get(fxomObject) != null) {
                collected.addAll(entries.get(fxomObject));
            }
            if (fxomObject instanceof final FXOMInstance fxomInstance) {
                for (final var fxomProperty : fxomInstance.getProperties().values()) {
                    if (entries.get(fxomProperty) != null) {
                        collected.addAll(entries.get(fxomProperty));
                    }
                }
            }
        }
        
        if (collected.isEmpty()) {
            result = null;
        } else {
            result = collected;
        }
        
        assert (result == null) || (result.size() >= 1);
        
        return result;
    }
    
    public Map<FXOMNode, List<ErrorReportEntry>> getEntries() {
        updateReport();
        return Collections.unmodifiableMap(entries);
    }
    
    public void cssFileDidChange(final Path cssPath) {
        if (cssParsingReports.containsKey(cssPath)) {
            cssParsingReports.remove(cssPath);
            forget();
        }
    }
    
    
    /*
     * Private
     */
    
    
    private void updateReport() {
        if (dirty) {
            assert entries.isEmpty();
            if (fxomDocument != null) {
                verifyAssets();
                verifyUnresolvedObjects();
                verifyBindingExpressions();
            }
            dirty = false;
        }
    }
    
    
    private void verifyAssets() {
        final var assetIndex = new FXOMAssetIndex(fxomDocument);
        for (final var e : assetIndex.getFileAssets().entrySet()) {
            final var assetPath = e.getKey();
            if (!assetPath.toFile().canRead()) {
                final var newEntry
                        = new ErrorReportEntry(e.getValue(), ErrorReportEntry.Type.UNRESOLVED_LOCATION);
                addEntry(e.getValue(), newEntry);
            } else {
                final var assetPathName = assetPath.toString();
                if (assetPathName.toLowerCase(Locale.ROOT).endsWith(".css")) { //NOI18N
                    // assetPath is a CSS file : check its parsing report
                    final var r = getCSSParsingReport(assetPath);
                    assert r != null;
                    if (!r.isEmpty()) {
                        final var newEntry
                                = new ErrorReportEntry(e.getValue(), ErrorReportEntry.Type.INVALID_CSS_CONTENT, r);
                        addEntry(e.getValue(), newEntry);
                    }
                }
            }
        }
    }
    
    private void verifyUnresolvedObjects() {
        for (final var fxomObject : FXOMNodes.serializeObjects(fxomDocument.getFxomRoot())) {
            final Object sceneGraphObject;
            if (fxomObject instanceof final FXOMIntrinsic fxomIntrinsic) {
                sceneGraphObject = fxomIntrinsic.getSourceSceneGraphObject();
            } else {
                sceneGraphObject = fxomObject.getSceneGraphObject();
            }
            if (sceneGraphObject == null) {
                final var newEntry
                        = new ErrorReportEntry(fxomObject, ErrorReportEntry.Type.UNRESOLVED_CLASS);
                addEntry(fxomObject, newEntry);
            }
        }
    }
    
    private void verifyBindingExpressions() {
        for (final var p : fxomDocument.getFxomRoot().collectPropertiesT()) {
            final var pv = new PrefixedValue(p.getValue());
            if (pv.isBindingExpression()) {
                final var newEntry
                        = new ErrorReportEntry(p, ErrorReportEntry.Type.UNSUPPORTED_EXPRESSION);
                addEntry(p, newEntry);
            }
        }
    }
    
    private void addEntry(final FXOMNode fxomNode, final ErrorReportEntry newEntry) {
        var nodeEntries = entries.get(fxomNode);
        if (nodeEntries == null) {
            nodeEntries = new ArrayList<> ();
            entries.put(fxomNode, nodeEntries);
        }
        nodeEntries.add(newEntry);
    }
    
    private CSSParsingReport getCSSParsingReport(final Path assetPath) {
        var result = cssParsingReports.get(assetPath);
        if (result == null) {
            result = new CSSParsingReport(assetPath);
            cssParsingReports.put(assetPath, result);
        }
        return result;
    }
    
    private void collectEntries(final FXOMNode fxomNode, final List<ErrorReportEntry> collected) {
        assert fxomNode != null;
        assert collected != null;
        
        final var nodeEntries = entries.get(fxomNode);
        if (nodeEntries != null) {
            collected.addAll(nodeEntries);
        }
        
        if (fxomNode instanceof final FXOMCollection fxomCollection) {
            for (final var item : fxomCollection.getItems()) {
                collectEntries(item, collected);
            }
        } else if (fxomNode instanceof final FXOMInstance fxomInstance) {
            for (final var fxomProperty : fxomInstance.getProperties().values()) {
                collectEntries(fxomProperty, collected);
            }
        } else if (fxomNode instanceof final FXOMPropertyC fxomPropertyC) {
            for (final var value : fxomPropertyC.getValues()) {
                collectEntries(value, collected);
            }
        }
    }
}
