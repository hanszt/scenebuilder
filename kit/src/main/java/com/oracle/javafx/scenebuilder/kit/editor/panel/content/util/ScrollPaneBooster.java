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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.util;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

/**
 * This is a wrapper for ScrollPane class. It brings some essential verbs
 * that should be available on ScrollPane.
 * 
 * 
 */
public class ScrollPaneBooster {
    
    public final ScrollPane scrollPane;
    
    public ScrollPaneBooster(final ScrollPane scrollPane) {
        assert scrollPane != null;
        this.scrollPane = scrollPane;
    }
    
    public void scrollTo(final Node node) {
        assert node != null;
    }
    
    public void scrollTo(final Bounds targetRect) {
        final var visibleRect = getContentVisibleRect();
        
        if (!visibleRect.intersects(targetRect)) {
            // targetRect is not visible (not even partially)
            
            final double targetCenterX, targetCenterY, hValue, vValue;
            targetCenterX = (targetRect.getMinX() + targetRect.getMaxX()) / 2.0;
            targetCenterY = (targetRect.getMinY() + targetRect.getMaxY()) / 2.0;
            final var clamp = true;
            hValue = xContentToHValue(targetCenterX, clamp);
            vValue = yContentToVValue(targetCenterY, clamp);
            
            scrollPane.setHvalue(hValue);
            scrollPane.setVvalue(vValue);
        }
    }
    
    public double xContentToHValue(final double x, final boolean clamp) {
        final var xNormalized = xContentToNormalized(x, clamp);
        final var hmin = scrollPane.getHmin();
        final var hmax = scrollPane.getHmax();
       
        return hmin + xNormalized * (hmax - hmin);
    }
    
    public double xContentToNormalized(final double x, final boolean clamp) {
        
        /*
         * viewport     +-----+-----+                 +-----+-----+
         * content      +-----+-----------------------------+-----+
         *                    |
         * x          viewport.width / 2          content.width - viewport.width / 2 
         *             - content.minX                   - content.minX
         * 
         * result             0                             1
         * 
         */
        
        final var contentBounds = scrollPane.getContent().getLayoutBounds();
        final var visibleBounds = getContentVisibleRect();
        final var minX = visibleBounds.getWidth() / 2 - contentBounds.getMinX();
        final var maxX = contentBounds.getWidth() - visibleBounds.getWidth() / 2.0 - contentBounds.getMinX();

        var result = (x - minX) / (maxX - minX);
        
        if (clamp) {
            result = Math.max(Math.min(result, 1.0), 0.0);
        }
        
        return result;
    }
    
    public double yContentToVValue(final double y, final boolean clamp) {
        final var yNormalized = yContentToNormalized(y, clamp);
        final var vmin = scrollPane.getVmin();
        final var vmax = scrollPane.getVmax();
       
        return vmin + yNormalized * (vmax - vmin);
    }
    
    public double yContentToNormalized(final double y, final boolean clamp) {
        final var contentBounds = scrollPane.getContent().getLayoutBounds();
        final var visibleBounds = getContentVisibleRect();
        final var minY = visibleBounds.getHeight() / 2 - contentBounds.getMinY();
        final var maxY = contentBounds.getHeight() - visibleBounds.getHeight() / 2.0 - contentBounds.getMinY();

        var result = (y - minY) / (maxY - minY);
        
        if (clamp) {
            result = Math.max(Math.min(result, 1.0), 0.0);
        }
        
        return result;
    }
    
    public Bounds getContentVisibleRect() {
        final var viewportBounds = scrollPane.getViewportBounds();

        /*
         * ScrollPane.viewportBounds is a strange beast.
         * 
         * When viewport is in top left corner:
         *    viewportBounds.minx = 0
         *    viewportBounds.miny = 0
         * 
         *     +--------------+--------------+
         *     |   viewport   |              |
         *     |              |              |
         *     +--------------+              |
         *     |                             |
         *     |            content          |
         *     +-----------------------------+
         * 
         * When viewport is in bottom/right corner:
         *    viewportBounds.minx = - content width
         *    viewportBounds.miny = - content height
         * 
         *     +-----------------------------+
         *     |            content          |
         *     |                             |
         *     |              +--------------+
         *     |              |   viewport   |
         *     |              |              |
         *     +--------------+--------------+
         *     
         *     
         */
        
        final var contentBounds = scrollPane.getContent().getLayoutBounds();
        final double minX, minY, width, height;
        minX = - viewportBounds.getMinX() - contentBounds.getMinX();
        minY = - viewportBounds.getMinY() - contentBounds.getMinY();
        width  = viewportBounds.getWidth();
        height = viewportBounds.getHeight();
    
        return new BoundingBox(minX, minY, width, height);
    }
}
