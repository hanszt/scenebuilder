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

package com.oracle.javafx.scenebuilder.kit.editor.panel.content.util;

import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;

/**
 *
 */
public class BoundsUtils {

    BoundsUtils() {
        // no-op
    }

    public static Bounds makeBounds(final Point2D p1, final Point2D p2) {
        return new BoundingBox(
                Math.min(p1.getX(), p2.getX()),
                Math.min(p1.getY(), p2.getY()),
                Math.abs(p2.getX() - p1.getX()),
                Math.abs(p2.getY() - p1.getY()));
    }
    
    
    public static boolean equals(final Bounds b1, final Bounds b2) {
        return MathUtils.equals(b1.getMinX(), b2.getMinX()) &&
                MathUtils.equals(b1.getMinY(), b2.getMinY()) &&
                MathUtils.equals(b1.getMaxX(), b2.getMaxX()) &&
                MathUtils.equals(b1.getMaxY(), b2.getMaxY());
                
    }
    
    public static Bounds inset(final Bounds bounds, final double dx, final double dy) {
        final var minX = bounds.getMinX() + dx;
        final var minY = bounds.getMinY() + dy;
        final var maxX = bounds.getMaxX() - dx;
        final var maxY = bounds.getMaxY() - dy;
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }
    
    
    public static EdgeInfo distanceToEdges(final Bounds b, final double x, final double y, final Node node) {
        assert b != null;
        assert node != null;
        assert node.getScene() != null;

        final EdgeInfo result;
        if (b.isEmpty()) {
            result = null;
        } else {
            final var minX = b.getMinX();
            final var minY = b.getMinY();
            final var maxX = b.getMaxX();
            final var maxY = b.getMaxY();
            
            final var p1 = node.localToScene(minX, minY, true /* rootScene */);
            final var p2 = node.localToScene(maxX, minY, true /* rootScene */);
            final var p3 = node.localToScene(maxX, maxY, true /* rootScene */);
            final var p4 = node.localToScene(minX, maxY, true /* rootScene */);

            final var nl = new LineEquation(p1, p2);
            final var el = new LineEquation(p2, p3);
            final var sl = new LineEquation(p3, p4);
            final var wl = new LineEquation(p4, p1);

            final var p = node.localToScene(x, y, true /* rootScene */);
            final var sceneX = p.getX();
            final var sceneY = p.getY();
            final var nh = nl.pointAtOffset(nl.offsetAtPoint(sceneX, sceneY));
            final var eh = el.pointAtOffset(el.offsetAtPoint(sceneX, sceneY));
            final var sh = sl.pointAtOffset(sl.offsetAtPoint(sceneX, sceneY));
            final var wh = wl.pointAtOffset(wl.offsetAtPoint(sceneX, sceneY));

            final var nd = distance(nh, p);
            final var ed = distance(eh, p);
            final var sd = distance(sh, p);
            final var wd = distance(wh, p);

            return new EdgeInfo(nd, ed, sd, wd);
        }
        
        return result;
    }
    
    public static class EdgeInfo {
        private final double northDistance;
        private final double eastDistance;
        private final double southDistance;
        private final double westDistance;

        public EdgeInfo(final double northDistance, final double eastDistance,
                        final double southDistance, final double westDistance) {
            this.northDistance = northDistance;
            this.eastDistance = eastDistance;
            this.southDistance = southDistance;
            this.westDistance = westDistance;
        }

        public double getNorthDistance() {
            return northDistance;
        }

        public double getEastDistance() {
            return eastDistance;
        }

        public double getSouthDistance() {
            return southDistance;
        }

        public double getWestDistance() {
            return westDistance;
        }

        @Override
        public String toString() {
            return "EdgeInfo{" //NOI18N
                    + "northDistance=" + northDistance  //NOI18N
                    + ", eastDistance=" + eastDistance  //NOI18N
                    + ", southDistance=" + southDistance  //NOI18N
                    + ", westDistance=" + westDistance  //NOI18N
                    + '}'; //NOI18N
        }

    }
    
    
    private static double distance(final Point2D p1, final Point2D p2) {
        final var dx = p2.getX() - p1.getX();
        final var dy = p2.getY() - p1.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}
