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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.gridpane;

import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring.Quad;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.util.CardinalPoint;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ColorEncoder;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;

/**
 *
 */
class GridPaneMosaic {
    
    public final static double NORTH_TRAY_SIZE = 22;
    public final static double SOUTH_TRAY_SIZE = NORTH_TRAY_SIZE;
    public final static double WEST_TRAY_SIZE = 24;
    public final static double EAST_TRAY_SIZE = WEST_TRAY_SIZE;
    
    private final Group topGroup = new Group();
    private final Path gridPath = new Path();
    private final Group hgapLinesGroup = new Group();
    private final Group vgapLinesGroup = new Group();
    private final Group northTrayGroup = new Group();
    private final Group southTrayGroup = new Group();
    private final Group westTrayGroup = new Group();
    private final Group eastTrayGroup = new Group();
    private final Rectangle targetCellShadow = new Rectangle();
    private final Line targetGapShadowV = new Line();
    private final Line targetGapShadowH= new Line();
    private final Group hgapSensorsGroup = new Group();
    private final Group vgapSensorsGroup = new Group();
    
    private final Quad gridAreaQuad = new Quad();
    private final List<Quad> gridHoleQuads = new ArrayList<>();

    private final String baseStyleClass;
    private final boolean shouldShowTrays;
    private final boolean shouldCreateSensors;
    
    private GridPane gridPane;
    private int columnCount;
    private int rowCount;
    private List<Bounds> cellBounds = new ArrayList<>();
    private final Set<Integer> selectedColumnIndexes = new HashSet<>();
    private final Set<Integer> selectedRowIndexes = new HashSet<>();
    private int targetColumnIndex = -1;
    private int targetRowIndex = -1;
    private int targetGapColumnIndex = -1;
    private int targetGapRowIndex = -1;
    private Color trayColor;
    
    public GridPaneMosaic(final String baseStyleClass, final boolean shouldShowTrays, final boolean shouldCreateSensors) {
        assert baseStyleClass != null;
        
        this.baseStyleClass = baseStyleClass;
        this.shouldShowTrays = shouldShowTrays;
        this.shouldCreateSensors = shouldCreateSensors;
        
        final List<Node> topChildren = topGroup.getChildren();
        topChildren.add(gridPath);              // Mouse transparent
        topChildren.add(hgapLinesGroup);        // Mouse transparent
        topChildren.add(vgapLinesGroup);        // Mouse transparent
        topChildren.add(northTrayGroup);
        topChildren.add(southTrayGroup);
        topChildren.add(westTrayGroup);
        topChildren.add(eastTrayGroup);
        topChildren.add(targetCellShadow);      // Mouse transparent
        topChildren.add(targetGapShadowV);      // Mouse transparent
        topChildren.add(targetGapShadowH);      // Mouse transparent
        topChildren.add(hgapSensorsGroup);
        topChildren.add(vgapSensorsGroup);
        gridAreaQuad.addToPath(gridPath);
        
        gridPath.setMouseTransparent(true);
        gridPath.getStyleClass().add("gap");
        gridPath.getStyleClass().add(baseStyleClass);
        
        hgapLinesGroup.setMouseTransparent(true);
        vgapLinesGroup.setMouseTransparent(true);
        
        targetCellShadow.setMouseTransparent(true);
        targetCellShadow.getStyleClass().add("gap");
        targetCellShadow.getStyleClass().add("selected");
        targetCellShadow.getStyleClass().add(baseStyleClass);
        
        targetGapShadowV.setMouseTransparent(true);
        targetGapShadowV.getStyleClass().add("gap");
        targetGapShadowV.getStyleClass().add("hilit");
        targetGapShadowV.getStyleClass().add(baseStyleClass);
        
        targetGapShadowH.setMouseTransparent(true);
        targetGapShadowH.getStyleClass().add("gap");
        targetGapShadowH.getStyleClass().add("hilit");
        targetGapShadowH.getStyleClass().add(baseStyleClass);
    }

    public Group getTopGroup() {
        return topGroup;
    }

    public GridPane getGridPane() {
        return gridPane;
    }

    public void setGridPane(final GridPane gridPane) {
        this.gridPane = gridPane;
        update();
    }
    
    public void setTrayColor(final Color trayColor) {
        this.trayColor = trayColor;
        if (shouldShowTrays) {
            updateTrayColor();
        }
    }
    
    public void setSelectedColumnIndexes(final Set<Integer> indexes) {
        selectedColumnIndexes.clear();
        selectedColumnIndexes.addAll(indexes);
        update();
    }
    
    public void setSelectedRowIndexes(final Set<Integer> indexes) {
        selectedRowIndexes.clear();
        selectedRowIndexes.addAll(indexes);
        update();
    }

    public void setTargetCell(final int targetColumnIndex, final int targetRowIndex) {
        assert (targetColumnIndex == -1) == (targetRowIndex == -1);
        this.targetColumnIndex = targetColumnIndex;
        this.targetRowIndex = targetRowIndex;
        this.targetGapColumnIndex = -1;
        this.targetGapRowIndex = -1;
        update();
    }
    
    public void setTargetGap(final int targetGapColumnIndex, final int targetGapRowIndex) {
        assert (-1 <= targetGapColumnIndex) && (targetGapColumnIndex <= columnCount);
        assert (-1 <= targetGapRowIndex) && (targetGapRowIndex <= rowCount);
        
        this.targetGapColumnIndex = targetGapColumnIndex;
        this.targetGapRowIndex = targetGapRowIndex;
        this.targetColumnIndex = -1;
        this.targetRowIndex = -1;
        update();
    }
    
    public void update() {
        
        columnCount = Deprecation.getGridPaneColumnCount(gridPane);
        rowCount = Deprecation.getGridPaneRowCount(gridPane);
        if ((columnCount == 0) || (rowCount == 0)) {
            columnCount = rowCount = 0;
        }
        this.cellBounds.clear();
        for (var c = 0; c < columnCount; c++) {
            for (var r = 0; r < rowCount; r++) {
                this.cellBounds.add(Deprecation.getGridPaneCellBounds(gridPane, c, r));
            }
        }
        
        gridAreaQuad.setBounds(gridPane.getLayoutBounds());
        adjustHoleItems();
        adjustHGapLines();
        adjustVGapLines();
        if (shouldShowTrays) {
            adjustTrayItems(northTrayGroup.getChildren(), "north", columnCount);
            adjustTrayItems(southTrayGroup.getChildren(), "south", columnCount);
            adjustTrayItems(westTrayGroup.getChildren(), "west", rowCount);
            adjustTrayItems(eastTrayGroup.getChildren(), "east", rowCount);
        }
        if (shouldCreateSensors) {
            final var hgapSensorCount = Math.max(0, columnCount - 1);
            final var vgapSensorCount = Math.max(0, rowCount - 1);
            adjustGapSensors(hgapSensorsGroup.getChildren(), Cursor.H_RESIZE, hgapSensorCount);
            adjustGapSensors(vgapSensorsGroup.getChildren(), Cursor.V_RESIZE, vgapSensorCount);
        }
        
        if (columnCount >= 1) {
            assert rowCount >= 1;
            
            updateHoleBounds();
            updateHGapLines();
            updateVGapLines();
            
            if (shouldShowTrays) {
                updateNorthTrayBounds();
                updateSouthTrayBounds();
                updateWestTrayBounds();
                updateEastTrayBounds();

                updateSelection(northTrayGroup.getChildren(), selectedColumnIndexes);
                updateSelection(southTrayGroup.getChildren(), selectedColumnIndexes);
                updateSelection(westTrayGroup.getChildren(), selectedRowIndexes);
                updateSelection(eastTrayGroup.getChildren(), selectedRowIndexes);
                
                updateTrayColor();
            }

            if (shouldCreateSensors) {
                updateHGapSensors();
                updateVGapSensors();
            }
        
            
            updateTargetCell();
            updateTargetGap();
        }
    }
    
    
    public List<Node> getNorthTrayNodes() {
        return northTrayGroup.getChildren();
    }
    
    public List<Node> getSouthTrayNodes() {
        return southTrayGroup.getChildren();
    }
    
    public List<Node> getWestTrayNodes() {
        return westTrayGroup.getChildren();
    }
    
    public List<Node> getEastTrayNodes() {
        return eastTrayGroup.getChildren();
    }

    public List<Node> getHgapSensorNodes() {
        return hgapSensorsGroup.getChildren();
    }

    public List<Node> getVgapSensorNodes() {
        return vgapSensorsGroup.getChildren();
    }
    
    
    /*
     * Private
     */
    
    
    private void adjustHoleItems() {
        final var holeCount = columnCount * rowCount;
        
        while (gridHoleQuads.size() < holeCount) {
            final var holeQuad = new Quad(false /* clockwise */); // Counterclockwise !!
            holeQuad.addToPath(gridPath);
            gridHoleQuads.add(holeQuad);
        }
        while (holeCount < gridHoleQuads.size()) {
            final var cellIndex = gridHoleQuads.size() - 1;
            gridHoleQuads.get(cellIndex).removeFromPath(gridPath);
            gridHoleQuads.remove(cellIndex);
        }
    }
    
    
    private void adjustHGapLines() {
        final int hgapLineCount;
        if (gridPane.getHgap() == 0) {
            hgapLineCount = Math.max(0, columnCount-1);
        } else {
            hgapLineCount = 0;
        }
        final List<Node> children = hgapLinesGroup.getChildren();
        while (children.size() < hgapLineCount) {
            children.add(makeGapLine());
        }
        while (children.size() > hgapLineCount) {
            children.removeFirst();
        }
    }
    
    private void adjustVGapLines() {
        final int vgapLineCount;
        if (gridPane.getVgap() == 0) {
            vgapLineCount = Math.max(0, rowCount-1);
        } else {
            vgapLineCount = 0;
        }
        final List<Node> children = vgapLinesGroup.getChildren();
        while (children.size() < vgapLineCount) {
            children.add(makeGapLine());
        }
        while (children.size() > vgapLineCount) {
            children.removeFirst();
        }
    }
    
    private Line makeGapLine() {
        final var result = new Line();
        result.getStyleClass().add("gap");
        result.getStyleClass().add("empty");
        result.getStyleClass().add(baseStyleClass);
        return result;
    }
    
    
    private void adjustTrayItems(final List<Node> trayChildren, final String direction, final int targetCount) {
        
        while (trayChildren.size() < targetCount) {
            final var trayIndex = trayChildren.size();
            trayChildren.add(makeTrayLabel(trayIndex, direction));
        }
        while (targetCount < trayChildren.size()) {
            final var trayIndex = trayChildren.size() - 1;
            trayChildren.remove(trayIndex);
        }
    }
    
    private Label makeTrayLabel(final int num, final String direction) {
        final var result = new Label();
        result.getStyleClass().add("tray");
        result.getStyleClass().add(direction);
        result.getStyleClass().add(baseStyleClass);
        result.setText(String.valueOf(num));
        result.setMinWidth(Region.USE_PREF_SIZE);
        result.setMaxWidth(Region.USE_PREF_SIZE);
        result.setMinHeight(Region.USE_PREF_SIZE);
        result.setMaxHeight(Region.USE_PREF_SIZE);

        if (trayColor != null) {
            final var webColor = ColorEncoder.encodeColorToRGBA(trayColor);
            final var style = "-fx-background-color:" + webColor + ";";//NOI18N
            result.setStyle(style);
        }
        
        return result;
    }
    
    private void updateTrayColor() {
        final String style;
        
        if (trayColor == null) {
            style = "";//NOI18N
        } else {
            final var webColor = ColorEncoder.encodeColorToRGBA(trayColor);
            style = "-fx-background-color:"+ webColor +";";//NOI18N
        }

        adjustTrayStyle(northTrayGroup.getChildren(), style);
        adjustTrayStyle(southTrayGroup.getChildren(), style);
        adjustTrayStyle(westTrayGroup.getChildren(), style);
        adjustTrayStyle(eastTrayGroup.getChildren(), style);
    }
    
    
    private void adjustTrayStyle(final List<Node> trayChildren, final String style) {
        
        for (final var tray : trayChildren) {
            assert tray instanceof Label;
            final var trayLabel = (Label) tray;
            trayLabel.setStyle(style);
        }
    }
    
    
    private void adjustGapSensors(final List<Node> gapSensors, final Cursor cursor, final int targetCount) {
        while (gapSensors.size() < targetCount) {
            gapSensors.add(makeGapSensor(cursor));
        }
        while (targetCount < gapSensors.size()) {
            final var gapIndex = gapSensors.size() - 1;
            gapSensors.remove(gapIndex);
        }
    }
    
    private Line makeGapSensor(final Cursor cursor) {
        final var result = new Line();
        result.setCursor(cursor);
        result.setStroke(Color.TRANSPARENT);

        return result;
    }
    
    
    
    private void updateHGapSensors() {
        final List<Node> children = hgapSensorsGroup.getChildren();
        final var sensorCount = children.size();
        assert (sensorCount == 0) || (sensorCount == columnCount-1);
        for (var i = 0; i < sensorCount; i++) {
            /*
             *                       x0  xm   x1
             *   y0  +----------------+       +-----------------+
             *       |   topLeftCell  |       |   topRightCell  |
             *       +----------------+       +-----------------+
             * 
             *       ...
             * 
             *       +----------------+       +-----------------+
             *       | bottomLeftCell |       |                 |
             *   y1  +----------------+       +-----------------+
             */
            
            final var topLeftCellBounds = getCellBounds(i, 0);
            final var topRightCellBounds = getCellBounds(i + 1, 0);
            final var bottomLeftCellBounds = getCellBounds(i, rowCount - 1);
            final var x0 = topLeftCellBounds.getMaxX();
            final var x1 = topRightCellBounds.getMinX();
            final var xm = (x0 + x1) / 2.0;
            final var y0 = topLeftCellBounds.getMinY();
            final var y1 = bottomLeftCellBounds.getMaxY();
            final var strokeWidth = Math.max(8.0, x1 - x0);
            final var line = (Line) children.get(i);
            line.setStartX(xm);
            line.setStartY(y0);
            line.setEndX(xm);
            line.setEndY(y1);
            line.setStrokeWidth(strokeWidth);
        }
    }
    
    private void updateVGapSensors() {
        final List<Node> children = vgapSensorsGroup.getChildren();
        final var sensorCount = children.size();
        assert (sensorCount == 0) || (sensorCount == rowCount-1);
        for (var i = 0; i < sensorCount; i++) {
            
            /*
             *       x0                                        x1
             *       +----------------+       +-----------------+
             *       |   topLeftCell  |  ...  |   topRightCell  |
             *   y0  +----------------+       +-----------------+
             *   ym 
             *   y1  +----------------+       +-----------------+
             *       | bottomLeftCell |  ...  |                 |
             *       +----------------+       +-----------------+
             */
            
            final var topLeftCellBounds = getCellBounds(0, i);
            final var bottomLeftCellBounds = getCellBounds(0, i + 1);
            final var topRightCellBounds = getCellBounds(columnCount - 1, i);
            final var x0 = topLeftCellBounds.getMinX();
            final var x1 = topRightCellBounds.getMaxX();
            final var y0 = topLeftCellBounds.getMaxY();
            final var y1 = bottomLeftCellBounds.getMinY();
            final var ym = (y0 + y1) / 2.0;
            final var strokeWidth = Math.max(8.0, y1 - y0);
            final var line = (Line) children.get(i);
            line.setStartX(x0);
            line.setStartY(ym);
            line.setEndX(x1);
            line.setEndY(ym);
            line.setStrokeWidth(strokeWidth);
        }
    }
    
    private void updateHoleBounds() {
        for (var c = 0; c < columnCount; c++) {
            for (var r = 0; r < rowCount; r++) {
                final var cb = getCellBounds(c, r);
                gridHoleQuads.get(getCellIndex(c, r)).setBounds(cb);
            }
        }
    }
    
    private void updateHGapLines() {
        final List<Node> children = hgapLinesGroup.getChildren();
        final var lineCount = children.size();
        assert (lineCount == 0) || (lineCount == columnCount-1);
        for (var i = 0; i < lineCount; i++) {
            final var topLeftCellBounds = getCellBounds(i, 0);
            final var topRightCellBounds = getCellBounds(i + 1, 0);
            final var bottomLeftCellBounds = getCellBounds(i, rowCount - 1);
            final var startX = (topLeftCellBounds.getMaxX() + topRightCellBounds.getMinX()) / 2.0;
            final var startY = topLeftCellBounds.getMinY();
            final var endY = bottomLeftCellBounds.getMaxY();
            final var snappedX = Math.round(startX) + 0.5;
            final var line = (Line) children.get(i);
            line.setStartX(snappedX);
            line.setStartY(startY);
            line.setEndX(snappedX);
            line.setEndY(endY);
        }
    }
    
    private void updateVGapLines() {
        final List<Node> children = vgapLinesGroup.getChildren();
        final var lineCount = children.size();
        assert (lineCount == 0) || (lineCount == rowCount-1);
        for (var i = 0; i < lineCount; i++) {
            final var topLeftCellBounds = getCellBounds(0, i);
            final var bottomLeftCellBounds = getCellBounds(0, i + 1);
            final var topRightCellBounds = getCellBounds(columnCount - 1, i);
            final var startX = topLeftCellBounds.getMinX();
            final var startY = (topLeftCellBounds.getMaxY() + bottomLeftCellBounds.getMinY()) / 2.0;
            final var endX = topRightCellBounds.getMaxX();
            final var snappedY = Math.round(startY) + 0.5;
            final var line = (Line) children.get(i);
            line.setStartX(startX);
            line.setStartY(snappedY);
            line.setEndX(endX);
            line.setEndY(snappedY);
        }
    }
    
    
    private void updateNorthTrayBounds() {
        final List<Node> northTrayChildren = northTrayGroup.getChildren();
        assert northTrayChildren.size() == columnCount;
        
        for (var c = 0; c < columnCount; c++) {
            updateNorthTrayBounds(c, (Label)northTrayChildren.get(c));
        }
    }
    
    
    private void updateNorthTrayBounds(final int column, final Label label) {
        final var gb = gridPane.getLayoutBounds();
        final var cb = getCellBounds(column, 0);


        /*
         *            x0                x1
         *            +-----------------+
         *            |     north(c)    |
         * y0  ....---+-----------------+---...
         *            |    padding.top  |
         *     ....---+-----------------+---...
         *            |                 |
         *            |    cell(c, 0)   |
         *            |                 |
         * y1  ....---+-----------------+---...
         */

        final var x0 = cb.getMinX();
        final var x1 = cb.getMaxX();
        final var y0 = gb.getMinY();
        final var y1 = cb.getMaxY();
        assert x0 <= x1;
        assert y0 <= y1;
        
        label.setPrefWidth(x1 - x0);
        label.setPrefHeight(NORTH_TRAY_SIZE);
        
        final Bounds area = new BoundingBox(x0, y0, x1-x0, y1-y0);
        relocateNode(label, area, CardinalPoint.N);
    }
    
    
    private void updateSouthTrayBounds() {
        final List<Node> trayChildren = southTrayGroup.getChildren();
        assert trayChildren.size() == columnCount;
        
        for (var c = 0; c < columnCount; c++) {
            updateSouthTrayBounds(c, (Label)trayChildren.get(c));
        }
    }
    
    
    private void updateSouthTrayBounds(final int column, final Label label) {
        final var gb = gridPane.getLayoutBounds();
        final var cb = getCellBounds(column, 0);


        /*
         *            x0                x1
         * y0  ....---+-----------------+---...
         *            |                 |
         *            |   cell(c, n-1)  |
         *            |                 |
         *     ....---+-----------------+---...
         *            |  padding.bottom |
         * y1  ....---+-----------------+---...
         *            |     south(c)    |
         *            +-----------------+
         */

        final var x0 = cb.getMinX();
        final var x1 = cb.getMaxX();
        final var y0 = cb.getMinY();
        final var y1 = gb.getMaxY();
        assert x0 <= x1;
        assert y0 <= y1;

        label.setPrefWidth(x1 - x0);
        label.setPrefHeight(SOUTH_TRAY_SIZE);
        
        final Bounds area = new BoundingBox(x0, y0, x1-x0, y1-y0);
        relocateNode(label, area, CardinalPoint.S);
    }
    
    
    private void updateWestTrayBounds() {
        final List<Node> trayChildren = westTrayGroup.getChildren();
        assert trayChildren.size() == rowCount;
        
        for (var r = 0; r < rowCount; r++) {
            updateWestTrayBounds(r, (Label)trayChildren.get(r));
        }
    }
    
    
    private void updateWestTrayBounds(final int row, final Label label) {
        final var gb = gridPane.getLayoutBounds();
        final var cb = getCellBounds(0,row);


        /*
         *         x0                    x1
         *         .   .                 . 
         *         .   .                 . 
         *         .   .                 . 
         *         |   |                 |      
         * y0 +----+---+-----------------+...
         *    |    |   |                 |
         *    |    |   |                 |
         *    |    |   |                 |
         *    |    |   |   cell(0, row)  |
         *    |    |   |                 |
         *    |    |   |                 |
         *    |    |   |                 |
         * y1 +----+---+-----------------+...
         *         |   |                 |      
         *         .   .                 . 
         *         .   .                 . 
         *         .   .                 . 
         *      ^    ^
         *      |    |
         *      |    padding.left
         *      |
         *      west(r)
         */

        final var x0 = gb.getMinX();
        final var x1 = cb.getMaxX();
        final var y0 = cb.getMinY();
        final var y1 = cb.getMaxY();
        assert x0 <= x1;
        assert y0 <= y1;

        label.setPrefWidth(y1 - y0);
        label.setPrefHeight(WEST_TRAY_SIZE);
        
        final Bounds area = new BoundingBox(x0, y0, x1-x0, y1-y0);
        relocateNode(label, area, CardinalPoint.W);
    }
    
    
    
    
    private void updateEastTrayBounds() {
        final List<Node> trayChildren = eastTrayGroup.getChildren();
        assert trayChildren.size() == rowCount;
        
        for (var r = 0; r < rowCount; r++) {
            updateEastTrayBounds(r, (Label)trayChildren.get(r));
        }
    }
    
    
    private void updateEastTrayBounds(final int row, final Label label) {
        final var gb = gridPane.getLayoutBounds();
        final var cb = getCellBounds(0,row);


        /*
         *             x0                    x1
         *             .                 .   . 
         *             .                 .   . 
         *             .                 .   . 
         *             |                 |   |      
         * y0          +-----------------+---+----+...
         *             |                 |   |    |
         *             |                 |   |    |
         *             |                 |   |    |
         *             |   cell(0, row)  |   |    |
         *             |                 |   |    |
         *             |                 |   |    |
         *             |                 |   |    |
         * y1          +-----------------+---+----+...
         *             |                 |      
         *             .                 . 
         *             .                 . 
         *             .                 . 
         *                                  ^    ^
         *                                  |    |
         *                                  |    west(r)
         *                                  |    
         *                                  padding.right
         */

        final var x0 = cb.getMinX();
        final var x1 = gb.getMaxX();
        final var y0 = cb.getMinY();
        final var y1 = cb.getMaxY();
        assert x0 <= x1;
        assert y0 <= y1;

        label.setPrefWidth(y1 - y0);
        label.setPrefHeight(EAST_TRAY_SIZE);
        
        final Bounds area = new BoundingBox(x0, y0, x1-x0, y1-y0);
        relocateNode(label, area, CardinalPoint.E);
    }
    
    
    private void relocateNode(final Label node, final Bounds area, final CardinalPoint cp) {
        assert node != null;
        
        final var nodeW = node.getPrefWidth();
        final var nodeH = node.getPrefHeight();
        final var areaW = area.getWidth();
        final var areaH = area.getHeight();
                
        /*
         * From
         *
         *      +----------+
         *      |   node   |--------------------+
         *      +----------+                    |
         *           |                          |
         *           |                          |
         *           |           area           |
         *           |                          |
         *           |                          |
         *           |                          |
         *           +--------------------------+
         *
         *
         * to North
         *                   +----------+
         *                   |   node   |
         *           +-------+----------+-------+
         *           |                          |
         *           |                          |
         *           |                          |   rotation   = 0°
         *           |           area           |   translateX = +areaW/2
         *           |                          |   translateY = -nodeH/2
         *           |                          |
         *           |                          |
         *           +--------------------------+
         * 
         * to South
         *           +--------------------------+
         *           |                          |
         *           |                          |
         *           |                          |   rotation   = 0°
         *           |           area           |   translateX = +areaW/2
         *           |                          |   translateY = +areaW+nodeH/2
         *           |                          |
         *           |                          |
         *           +-------+----------+-------+
         *                   |   node   |
         *                   +----------+
         *
         * to West
         *           +--------------------------+
         *           |                          |
         *      +----+                          |
         *      |    |                          |   rotation   = -90°
         *      |node|           area           |   translateX = -nodeH/2
         *      |    |                          |   translateY = +areaH/2
         *      +----+                          |
         *           |                          |
         *           +--------------------------+
         *
         * to East
         *           +--------------------------+
         *           |                          |
         *           |                          |----+
         *           |                          |    |   rotation   = +90°
         *           |           area           |node|   translateX = +areaW+nodeH/2
         *           |                          |    |   translateY = +areaH/2
         *           |                          |----+
         *           |                          |
         *           +--------------------------+
         */
        
        final double rotation, translateX, translateY;
        switch(cp) {
            case N:
                rotation = 0.0;
                translateX = +areaW/2.0;
                translateY = -nodeH/2.0;
                break;
            case S:
                rotation = 0.0;
                translateX = +areaW/2.0;
                translateY = +areaH + nodeH/2.0;
                break;
            case W:
                rotation = -90.0;
                translateX = -nodeH/2.0;
                translateY = +areaH/2.0;
                break;
            case E:
                rotation = +90.0;
                translateX = +areaW + nodeH/2.0;
                translateY = +areaH/2.0;
                break;
            default:
                assert false;
                rotation = translateX = translateY = 0;
                break;
        }
        
        final var nodeCenterX = nodeW / 2.0;
        final var nodeCenterY = nodeH / 2.0;
        final var layoutDX = area.getMinX() - nodeCenterX + translateX;
        final var layoutDY = area.getMinY() - nodeCenterY + translateY;
        
        node.setLayoutX(layoutDX);
        node.setLayoutY(layoutDY);
        node.setRotate(rotation);
    }
    
    
    
    private void updateSelection(final List<Node> trayChildren, final Set<Integer> selectedIndexes) {
        final var selectedClass = "selected";
        
        for (int i = 0, count = trayChildren.size(); i < count; i++) {
            final List<String> trayStyleClasses = trayChildren.get(i).getStyleClass();
            if (selectedIndexes.contains(i)) {
                if (!trayStyleClasses.contains(selectedClass)) {
                    trayStyleClasses.add(selectedClass);
                }
            } else {
                if (trayStyleClasses.contains(selectedClass)) {
                    trayStyleClasses.remove(selectedClass);
                }
            }
        }
    }
    
    
    private void updateTargetCell() {
        if (targetColumnIndex == -1) {
            assert targetRowIndex == -1;
            targetCellShadow.setVisible(false);
        } else {
            targetCellShadow.setVisible(true);
            final var tb = getCellBounds(targetColumnIndex, targetRowIndex);
            targetCellShadow.setX(tb.getMinX());
            targetCellShadow.setY(tb.getMinY());
            targetCellShadow.setWidth(tb.getWidth());
            targetCellShadow.setHeight(tb.getHeight());
        }
    }
    
    
    private Bounds getCellBounds(final int c, final int r) {
        final var cellIndex = getCellIndex(c, r);
        assert cellIndex < cellBounds.size();
        return cellBounds.get(cellIndex);
    }
    
    private int getCellIndex(final int c, final int r) {
        return c * rowCount + r;
    }
    
    
    private static final double MIN_STROKE_WIDTH = 8;
    
    private void updateTargetGap() {
        
        /*
         * targetGapShadowV
         */
        if (targetGapColumnIndex == -1) {
            targetGapShadowV.setVisible(false);
        } else {
            targetGapShadowV.setVisible(true);
            
            final double startX, startY, endY, strokeWidth;
            if (targetGapColumnIndex < columnCount) {
                final var topCellBounds = getCellBounds(targetGapColumnIndex, 0);
                final var bottomCellBounds = getCellBounds(targetGapColumnIndex, rowCount - 1);
                startY = topCellBounds.getMinY();
                endY = bottomCellBounds.getMaxY();
                if (targetGapColumnIndex == 0) {
                    startX = topCellBounds.getMinX();
                    strokeWidth = MIN_STROKE_WIDTH;
                } else {
                    assert targetGapColumnIndex >= 1;
                    final var leftTopCellBounds = getCellBounds(targetGapColumnIndex - 1, 0);
                    startX = (leftTopCellBounds.getMaxX() + topCellBounds.getMinX()) / 2.0;
                    strokeWidth = Math.abs(leftTopCellBounds.getMaxX() - topCellBounds.getMinX());
                }
            } else {
                final var topCellBounds = getCellBounds(columnCount - 1, 0);
                final var bottomCellBounds = getCellBounds(columnCount - 1, rowCount - 1);
                startX = topCellBounds.getMaxX();
                startY = topCellBounds.getMinY();
                endY = bottomCellBounds.getMaxY();
                strokeWidth = MIN_STROKE_WIDTH;
            }
            targetGapShadowV.setStartX(startX);
            targetGapShadowV.setStartY(startY);
            targetGapShadowV.setEndX(startX);
            targetGapShadowV.setEndY(endY);
            targetGapShadowV.setStrokeWidth(Math.max(strokeWidth, MIN_STROKE_WIDTH));
        }
        
        /*
         * targetGapShadowH
         */
        if (targetGapRowIndex == -1) {
            targetGapShadowH.setVisible(false);
        } else {
            targetGapShadowH.setVisible(true);
            
            final double startX, endX, startY, strokeWidth;
            if (targetGapRowIndex < rowCount) {
                final var leftCellBounds = getCellBounds(0, targetGapRowIndex);
                final var rightCellBounds = getCellBounds(columnCount - 1, targetGapRowIndex);
                startX = leftCellBounds.getMinX();
                endX = rightCellBounds.getMaxX();
                if (targetGapRowIndex == 0) {
                    startY = leftCellBounds.getMinY();
                    strokeWidth = MIN_STROKE_WIDTH;
                } else {
                    assert targetGapRowIndex >= 1;
                    final var aboveLeftCellBounds = getCellBounds(0, targetGapRowIndex - 1);
                    startY = (aboveLeftCellBounds.getMaxY() + leftCellBounds.getMinY()) / 2.0;
                    strokeWidth = Math.abs(aboveLeftCellBounds.getMaxY() - leftCellBounds.getMinY());
                }
            } else {
                final var leftCellBounds = getCellBounds(0, rowCount - 1);
                final var rightCellBounds = getCellBounds(columnCount - 1, rowCount - 1);
                startX = leftCellBounds.getMinX();
                endX = rightCellBounds.getMaxX();
                startY = leftCellBounds.getMaxY();
                strokeWidth = MIN_STROKE_WIDTH;
            }
            targetGapShadowH.setStartX(startX);
            targetGapShadowH.setStartY(startY);
            targetGapShadowH.setEndX(endX);
            targetGapShadowH.setEndY(startY);
            targetGapShadowH.setStrokeWidth(Math.max(strokeWidth, MIN_STROKE_WIDTH));
        }
        
    }
}
