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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver;

import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AccessoryDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring.AbstractTring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring.BorderPaneTring;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;
import javafx.scene.layout.BorderPane;

/**
 *
 */
public class BorderPaneDriver extends AbstractNodeDriver {

    public BorderPaneDriver(final ContentPanelController contentPanelController) {
        super(contentPanelController);
    }

    /*
     * AbstractDriver
     */
     
    @Override
    public AbstractDropTarget makeDropTarget(final FXOMObject fxomObject, final double sceneX, final double sceneY) {
        assert fxomObject.getSceneGraphObject() instanceof BorderPane;
        assert fxomObject instanceof FXOMInstance;
        
        final var fxomInstance = (FXOMInstance) fxomObject;
        final var borderPane = (BorderPane) fxomInstance.getSceneGraphObject();
        final var hitPoint = borderPane.sceneToLocal(sceneX, sceneY, true /* rootScene */);
        final var hitX = hitPoint.getX();
        final var hitY = hitPoint.getY();
        
        final var layoutBounds = borderPane.getLayoutBounds();
        final var centerBounds = BorderPaneTring.computeCenterBounds(borderPane);
        final var topBounds = BorderPaneTring.computeAreaBounds(layoutBounds, centerBounds, Accessory.TOP);
        final var bottomBounds = BorderPaneTring.computeAreaBounds(layoutBounds, centerBounds, Accessory.BOTTOM);
        final var leftBounds = BorderPaneTring.computeAreaBounds(layoutBounds, centerBounds, Accessory.LEFT);
        final var rightBounds = BorderPaneTring.computeAreaBounds(layoutBounds, centerBounds, Accessory.RIGHT);
        
        final Accessory targetAccessory;
        if (centerBounds.contains(hitX, hitY)) {
            targetAccessory = Accessory.CENTER;
        } else if (topBounds.contains(hitX, hitY)) {
            targetAccessory = Accessory.TOP;
        } else if (bottomBounds.contains(hitX, hitY)) {
            targetAccessory = Accessory.BOTTOM;
        } else if (leftBounds.contains(hitX, hitY)) {
            targetAccessory = Accessory.LEFT;
        } else if (rightBounds.contains(hitX, hitY)) {
            targetAccessory = Accessory.RIGHT;
        } else {
            targetAccessory = Accessory.CENTER;
        }
        
        return new AccessoryDropTarget(fxomInstance, targetAccessory);
    }
    
    
    @Override
    public AbstractTring<?> makeTring(final AbstractDropTarget dropTarget) {
        assert dropTarget instanceof AccessoryDropTarget;
        if (!(dropTarget instanceof final AccessoryDropTarget accessoryDropTarget)) {
            return null;
        }
        return new BorderPaneTring(contentPanelController,
                (FXOMInstance) dropTarget.getTargetObject(),
                accessoryDropTarget.getAccessory());
    }
    
}
