/*
 * Copyright (c) 2016, 2022 Gluon and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treeview;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyItem;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;

import static javafx.geometry.Orientation.HORIZONTAL;

import javafx.scene.control.Cell;
import javafx.scene.control.Control;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;

/**
 * Hierarchy panel controller based on the TreeView control.
 */
public class HierarchyTreeViewController extends AbstractHierarchyPanelController {

    @FXML
    protected TreeView<HierarchyItem> treeView;

    public HierarchyTreeViewController(final EditorController editorController) {
        super(HierarchyTreeViewController.class.getResource("HierarchyTreeView.fxml"), editorController); //NOI18N
    }

    @Override
    public Control getPanelControl() {
        return treeView;
    }

    @Override
    public ObservableList<TreeItem<HierarchyItem>> getSelectedItems() {
        return treeView.getSelectionModel().getSelectedItems();
    }

    @Override
    protected void initializePanel() {
        assert treeView != null;
        super.initializePanel();
        // Initialize and configure tree view
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // Cell factory
        treeView.setCellFactory(p -> new HierarchyTreeCell<>(HierarchyTreeViewController.this));
        // We do not use the platform editing feature because 
        // editing is started on selection + simple click instead of double click
        treeView.setEditable(false);
        
        treeView.setOnKeyPressed(event -> {
            
            if (event.getCode() == KeyCode.ESCAPE) {
            	// on ESC we select the parent of current selected item
                final var selectionModel = treeView.getSelectionModel();
                final var item = selectionModel.getSelectedItem();
                if (item != null) {
                    final var parent = item.getParent();
                    if (parent != null) {
                        selectionModel.clearSelection();
                        selectionModel.select(parent);
                    }
                }
            }
        });
    }

    @Override
    protected void updatePanel() {
        if (treeView != null) {
            // First update rootTreeItem + children TreeItems 
            updateTreeItems();
            // Then update the TreeTableView with the updated rootTreeItem
            stopListeningToTreeItemSelection();
            treeView.setRoot(rootTreeItem);
            startListeningToTreeItemSelection();
        }
    }

    @Override
    protected void clearSelection() {
        assert treeView != null;
        treeView.getSelectionModel().clearSelection();
    }

    @Override
    protected void select(final TreeItem<HierarchyItem> treeItem) {
        assert treeView != null;
        // The select method of TreeView selection model will expand the selected TreeItem.
        // Keep the current expanded value to set it back after selection.
        final var isExpanded = treeItem.isExpanded();
        treeView.getSelectionModel().select(treeItem);
        treeItem.setExpanded(isExpanded);
    }

    @Override
    public void scrollTo(final TreeItem<HierarchyItem> treeItem) {
        assert treeView != null;
        treeView.scrollTo(treeView.getRow(treeItem));
    }

    @Override
    public Cell<?> getCell(final TreeItem<?> treeItem) {
        assert treeView != null;
        final var treeCell
                = HierarchyTreeViewUtils.getTreeCell(treeView, treeItem);
        return treeCell;
    }

    /**
     * Returns the Y coordinate of the panel content TOP. Used to define the
     * zone for auto scrolling.
     *
     * @return the Y coordinate of the panel content TOP
     */
    @Override
    public double getContentTopY() {
        final var bounds = treeView.getLayoutBounds();
        final var point = treeView.localToParent(bounds.getMinX(), bounds.getMinY());
        return point.getY();
    }

    /**
     * Returns the Y coordinate of the panel content BOTTOM. Used to define the
     * zone for auto scrolling.
     *
     * @return the Y coordinate of the panel content BOTTOM
     */
    @Override
    public double getContentBottomY() {
        final var bounds = treeView.getLayoutBounds();
        final var point = treeView.localToParent(bounds.getMinX(), bounds.getMinY());
        final var topY = point.getY();
        final var height = bounds.getHeight();
        final var horizontalScrollBar = getScrollBar(HORIZONTAL);
        final double bottomY;
        if (horizontalScrollBar != null && horizontalScrollBar.isVisible()) {
            bottomY = topY + height - horizontalScrollBar.getLayoutBounds().getHeight();
        } else {
            bottomY = topY + height;
        }
        return bottomY;
    }

    @Override
    protected void startListeningToTreeItemSelection() {
        treeView.getSelectionModel().getSelectedItems().addListener(treeItemSelectionListener);
    }

    @Override
    protected void stopListeningToTreeItemSelection() {
        treeView.getSelectionModel().getSelectedItems().removeListener(treeItemSelectionListener);
    }

    @Override
    protected void startEditingDisplayInfo() {
        // Start inline editing the display info on ENTER key
        final List<TreeItem<HierarchyItem>> selectedTreeItems
                = treeView.getSelectionModel().getSelectedItems();
        if (selectedTreeItems.size() == 1) {
            final var selectedTreeItem = selectedTreeItems.getFirst();
            final var item = selectedTreeItem.getValue();
            final var option = getDisplayOption();
            if (item != null
                && !item.isResourceKey(option) // Do not allow inline editing of the I18N value
                && item.hasDisplayInfo(option)) {
                final var tc = HierarchyTreeViewUtils.getTreeCell(treeView, selectedTreeItem);
                assert tc instanceof HierarchyTreeCell;
                final var htc = (HierarchyTreeCell<?>) tc;
                htc.startEditingDisplayInfo();
            }
        }
    }

    /**
     * *************************************************************************
     * Parent ring
     * *************************************************************************
     */
    @Override
    public void clearBorderColor() {
        assert treeView != null;
        final var cells = HierarchyTreeViewUtils.getTreeCells(treeView);
        assert cells != null;
        for (final var node : cells) {
            assert node instanceof Cell;
            clearBorderColor((Cell<?>) node);
        }
    }

    @Override
    public void updateParentRing() {
        assert treeView != null;
        
        // Do not update parent ring while performing some operations 
        // like DND within the hierarchy panel
        if (!isParentRingEnabled()) {
            return;
        }

        final var treeCells = HierarchyTreeViewUtils.getTreeCells(treeView);
        final List<TreeItem<HierarchyItem>> selectedTreeItems = treeView.getSelectionModel().getSelectedItems();

        // First clear previous parent ring if any
        clearBorderColor();

        // Dirty selection
        for (final var selectedTreeItem : selectedTreeItems) {
            if (selectedTreeItem == null) {
                return;
            }
        }

        // Then update parent ring if selection is not empty
        if (!selectedTreeItems.isEmpty()) {

            // Single selection is ROOT TreeItem => no parent ring
            final var treeItemRoot = treeView.getRoot();
            if (selectedTreeItems.size() == 1 && selectedTreeItems.getFirst() == treeItemRoot) {
                return;
            }

            final int treeCellTopIndex;
            final int treeCellBottomIndex;

            // TOP TreeItem is the common parent TreeItem
            final var treeItemTop
                    = HierarchyTreeViewUtils.getCommonParentTreeItem(selectedTreeItems);
            final var treeCellTop
                    = HierarchyTreeViewUtils.getTreeCell(treeCells, treeItemTop);
            if (treeCellTop != null) {
                setBorder(treeCellTop, BorderSide.TOP_RIGHT_LEFT);
                treeCellTopIndex = treeCellTop.getIndex();
            } else {
                treeCellTopIndex = 0;
            }

            // BOTTOM TreeItem is the last child of the common parent TreeItem
            final var size = treeItemTop.getChildren().size();
            assert size >= 1;
            final var treeItemBottom = treeItemTop.getChildren().get(size - 1);
            final var treeCellBottom = HierarchyTreeViewUtils.getTreeCell(treeCells, treeItemBottom);
            if (treeCellBottom != null) {
                setBorder(treeCellBottom, BorderSide.RIGHT_BOTTOM_LEFT);
                treeCellBottomIndex = treeCellBottom.getIndex();
            } else {
                treeCellBottomIndex = treeCells.size() - 1;
            }

            // MIDDLE TreeItems
            for (final var node : treeCells) {
                assert node instanceof TreeCell;
                final var treeCell = (TreeCell<?>) node;
                final var index = treeCell.getIndex();
                if (index > treeCellTopIndex && index < treeCellBottomIndex) {
                    setBorder(treeCell, BorderSide.RIGHT_LEFT);
                }
            }
        }
    }

    @Override
    public void updatePlaceHolder() {
        assert treeView != null;
        final var cells = HierarchyTreeViewUtils.getTreeCells(treeView);
        assert cells != null;
        for (final var node : cells) {
            assert node instanceof HierarchyTreeCell;
            final var cell = (HierarchyTreeCell<?>) node;
            cell.updatePlaceHolder();
        }
    }
}
