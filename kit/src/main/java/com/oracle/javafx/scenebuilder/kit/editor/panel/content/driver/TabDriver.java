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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver;

import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AccessoryDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.curve.AbstractCurveEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles.AbstractHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles.TabHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring.AbstractPring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring.TabPring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.resizer.AbstractResizer;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring.AbstractTring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring.TabTring;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Tab;

/**
 *
 */
public class TabDriver extends AbstractDriver {

    public TabDriver(final ContentPanelController contentPanelController) {
        super(contentPanelController);
    }

    /*
     * AbstractDriver
     */
    
    @Override
    public AbstractHandles<?> makeHandles(final FXOMObject fxomObject) {
        assert fxomObject.getSceneGraphObject() instanceof Tab;
        assert fxomObject instanceof FXOMInstance;
        return new TabHandles(contentPanelController, (FXOMInstance) fxomObject);
    }
    
    @Override
    public AbstractTring<?> makeTring(final AbstractDropTarget dropTarget) {
        assert dropTarget != null;
        assert dropTarget.getTargetObject() instanceof FXOMInstance;
        assert dropTarget.getTargetObject().getSceneGraphObject() instanceof Tab;
        return new TabTring(contentPanelController, (FXOMInstance) dropTarget.getTargetObject());
    }

    @Override
    public AbstractPring<?> makePring(final FXOMObject fxomObject) {
        assert fxomObject.getSceneGraphObject() instanceof Tab;
        assert fxomObject instanceof FXOMInstance;
        return new TabPring(contentPanelController, (FXOMInstance) fxomObject);
    }
    
    @Override
    public AbstractResizer<?> makeResizer(final FXOMObject fxomObject) {
        // Resize gesture does not apply to Tab objects
        return null;
    }

    @Override
    public AbstractCurveEditor<?> makeCurveEditor(final FXOMObject fxomObject) {
        return null;
    }
    
    @Override
    public FXOMObject refinePick(final Node hitNode, final double sceneX, final double sceneY, final FXOMObject fxomObject) {
        return fxomObject;
    }

    @Override
    public AbstractDropTarget makeDropTarget(final FXOMObject fxomObject, final double sceneX, final double sceneY) {
        assert fxomObject instanceof FXOMInstance;
        return new AccessoryDropTarget((FXOMInstance) fxomObject, Accessory.CONTENT);
    }

    @Override
    public Node getInlineEditorBounds(final FXOMObject fxomObject) {
        assert fxomObject.getSceneGraphObject() instanceof Tab;
        final var tab = (Tab) fxomObject.getSceneGraphObject();
        final var di = new TabPaneDesignInfoX();
        return di.getTabNode(tab.getTabPane(), tab);
    }

    @Override
    public boolean intersectsBounds(final FXOMObject fxomObject, final Bounds bounds) {
        assert fxomObject.getSceneGraphObject() instanceof Tab;
        
        final var tab = (Tab) fxomObject.getSceneGraphObject();
        final boolean result;
        if (tab.isSelected()) {
            final var tabPane
                    = tab.getTabPane();
            final var sceneGraphNodeBounds
                    = tabPane.localToScene(tabPane.getLayoutBounds(), true /* rootScene */);
            result = sceneGraphNodeBounds.intersects(bounds);
        } else {
            result = false;
        }
        
        return result;
    }
    
}
