/*
 * Copyright (c) 2017, 2024 Gluon and/or its affiliates.
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

import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AccessoryDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.ContainerZDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.GridPaneDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.RootDropTarget;
import com.oracle.javafx.scenebuilder.kit.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtils;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyFxIdJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyItem;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.BorderSide;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyDNDController.DroppingMouseLocation;
import com.oracle.javafx.scenebuilder.kit.editor.report.CSSParsingReport;
import com.oracle.javafx.scenebuilder.kit.editor.util.InlineEditController;
import com.oracle.javafx.scenebuilder.kit.editor.util.InlineEditController.Type;
import com.oracle.javafx.scenebuilder.kit.editor.report.ErrorReportEntry;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask.Accessory;

import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.Cursor;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.util.Callback;

/**
 * TreeCells used by the hierarchy TreeView.
 *
 * p
 *
 * @param <T>
 */
public class HierarchyTreeCell<T extends HierarchyItem> extends TreeCell<HierarchyItem> {

    private final AbstractHierarchyPanelController panelController;

    static final String TREE_CELL_GRAPHIC = "tree-cell-graphic";
    public static final String HIERARCHY_FIRST_CELL = "hierarchy-first-cell";
    static final String HIERARCHY_PLACE_HOLDER_LABEL = "hierarchy-place-holder-label";
    static final String HIERARCHY_READWRITE_LABEL = "hierarchy-readwrite-label";
    // Style class used for lookup
    static final String HIERARCHY_TREE_CELL = "hierarchy-tree-cell";

    private final HBox graphic = new HBox();
    private final Label placeHolderLabel = new Label();
    private final Label classNameInfoLabel = new Label();
    private final Label displayInfoLabel = new Label();
    private final ImageView placeHolderImageView = new ImageView();
    private final ImageView classNameImageView = new ImageView();
    private final ImageView warningBadgeImageView = new ImageView();
    private final ImageView includedFileImageView = new ImageView();
    // Stack used to add badges over the top of the node icon
    private final StackPane iconsStack = new StackPane();
    // We use a label to set a tooltip over the node icon 
    // (StackPane does not allow to set tooltips)
    private final Label iconsLabel = new Label();
    private final Tooltip warningBadgeTooltip = new Tooltip();

    // Vertical line used when inserting an item in order to indicate 
    // the parent into which the item will be inserted.
    // Horizontal lines are handled directly by the cell and are built using CSS only.
    //
    // This line will be added to / removed from the skin of the panel control
    // during DND gestures.
    private final Line insertLineIndicator = new Line();

    // Listener for the display option used to update the display info label
    final ChangeListener<DisplayOption> displayOptionListener = (ov, t, t1) -> {
        // Update display info for non empty cells
        if (!isEmpty() && getItem() != null && !getItem().isEmpty()) {
            final var displayInfo = getItem().getDisplayInfo(t1);
            displayInfoLabel.setText(displayInfo);
            displayInfoLabel.setManaged(getItem().hasDisplayInfo(t1));
            displayInfoLabel.setVisible(getItem().hasDisplayInfo(t1));
        }
    };

    public HierarchyTreeCell(final AbstractHierarchyPanelController c) {
        super();
        this.panelController = c;

        iconsStack.getChildren().setAll(
                classNameImageView,
                warningBadgeImageView);
        iconsLabel.setGraphic(iconsStack);
        // RT-31645 : we cannot dynamically update the HBox graphic children 
        // in the cell.updateItem method.
        // We set once the graphic children, then we update the managed property
        // of the children depending on the cell item. 
        graphic.getChildren().setAll(
                includedFileImageView,
                placeHolderImageView,
                iconsLabel,
                placeHolderLabel,
                classNameInfoLabel,
                displayInfoLabel);

        // Add style class used when invoking lookupAll
        this.getStyleClass().add(HIERARCHY_TREE_CELL);

        // Update vertical insert line indicator stroke width
        insertLineIndicator.setStrokeWidth(2.0);

        // CSS
        graphic.getStyleClass().add(TREE_CELL_GRAPHIC);
        updatePlaceHolder();
        displayInfoLabel.getStyleClass().add(HIERARCHY_READWRITE_LABEL);
        placeHolderLabel.getStyleClass().add(HIERARCHY_PLACE_HOLDER_LABEL);
        // Layout
        classNameInfoLabel.setMinWidth(Control.USE_PREF_SIZE);
        displayInfoLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(displayInfoLabel, Priority.ALWAYS);

        panelController.displayOptionProperty().addListener(
                new WeakChangeListener<>(displayOptionListener));

        // Key events
        //----------------------------------------------------------------------
        final EventHandler<KeyEvent> keyEventHandler = e -> filterKeyEvent(e);
        this.addEventFilter(KeyEvent.ANY, keyEventHandler);

        // Mouse events
        //----------------------------------------------------------------------
        final EventHandler<MouseEvent> mouseEventHandler = e -> filterMouseEvent(e);
        this.addEventFilter(MouseEvent.ANY, mouseEventHandler);

        // Drag events
        //----------------------------------------------------------------------
        final var dndController = panelController.getDNDController();

        setOnDragDropped(event -> {
            final var treeItem
                    = HierarchyTreeCell.this.getTreeItem();
            // Forward to the DND controller
            dndController.handleOnDragDropped(treeItem, event);

            // CSS
            panelController.clearBorderColor(HierarchyTreeCell.this);
            // Remove insert line indicator
            panelController.removeFromPanelControlSkin(insertLineIndicator);
        });
        setOnDragEntered(event -> {
            final var treeItem
                    = HierarchyTreeCell.this.getTreeItem();
            // Forward to the DND controller
            dndController.handleOnDragEntered(treeItem, event);
        });
        setOnDragExited(event -> {
            final var treeItem
                    = HierarchyTreeCell.this.getTreeItem();
            final var bounds = HierarchyTreeCell.this.getLayoutBounds();
            final var point = HierarchyTreeCell.this.localToScene(bounds.getMinX(), bounds.getMinY(), true /* rootScene */);
            final DroppingMouseLocation location;
            if (event.getSceneY() <= point.getY()) {
                location = DroppingMouseLocation.TOP;
            } else {
                location = DroppingMouseLocation.BOTTOM;
            }

            // Forward to the DND controller
            dndController.handleOnDragExited(treeItem, event, location);

            // CSS
            panelController.clearBorderColor(HierarchyTreeCell.this);
            // Remove insert line indicator
            panelController.removeFromPanelControlSkin(insertLineIndicator);
        });
        setOnDragOver(event -> {
            final var treeItem
                    = HierarchyTreeCell.this.getTreeItem();
            final var dragController
                    = panelController.getEditorController().getDragController();
            final var location = getDroppingMouseLocation(event);

            // Forward to the DND controller
            dndController.handleOnDragOver(treeItem, event, location); // (1)

            panelController.clearBorderColor();
            // Update vertical insert line indicator stroke color
            final var paint = panelController.getParentRingColor();
            insertLineIndicator.setStroke(paint);
            // Remove insert line indicator
            panelController.removeFromPanelControlSkin(insertLineIndicator);

            // If an animation timeline is running 
            // (auto-scroll when DND to the top or bottom of the Hierarchy),
            // we do not display insert indicators.
            if (panelController.isTimelineRunning()) {
                return;
            }

            // Drop target has been updated because of (1)
            if (dragController.isDropAccepted()) {

                final var dropTarget = dragController.getDropTarget();
                final var dropTargetObject = dropTarget.getTargetObject();
                final TreeItem<?> rootTreeItem = getTreeView().getRoot();

                if (dropTarget instanceof RootDropTarget) {
                    // No visual feedback in case of dropping the root node
                    return;
                }

                //==========================================================
                // ACCESSORIES :
                //
                // No need to handle the insert line indicator.
                // Border is set either on the accessory place holder cell
                // or on the accessory owner cell.
                //==========================================================
                if (dropTarget instanceof final AccessoryDropTarget accessoryDropTarget) {

                    final TreeCell<?> cell;

                    // TreeItem is null when dropping below the datas
                    // => the drop target is the root
                    if (treeItem == null) {
                        cell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), rootTreeItem);
                    } else {
                        final var item = treeItem.getValue();
                        assert item != null;

                        if (item.isPlaceholder()) {
                            cell = HierarchyTreeCell.this;
                        } else if (accessoryDropTarget.getAccessory() == Accessory.GRAPHIC) {
                            // Check if an empty graphic TreeItem has been added
                            final var graphicTreeItem
                                    = dndController.getEmptyGraphicTreeItemFor(treeItem);
                            if (graphicTreeItem != null) {
                                cell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), graphicTreeItem);
                            } else {
                                final var accessoryOwnerTreeItem1
                                        = panelController.lookupTreeItem(dropTargetObject);
                                cell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), accessoryOwnerTreeItem1);
                            }
                        } else {
                            final var accessoryOwnerTreeItem2
                                    = panelController.lookupTreeItem(dropTargetObject);
                            cell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), accessoryOwnerTreeItem2);
                        }
                    }

                    panelController.setBorder(cell, BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                }//
                //==========================================================
                // SUB COMPONENTS :
                //
                // Need to handle the insert line indicator.
                //==========================================================
                else {
                    assert dropTarget instanceof ContainerZDropTarget
                            || dropTarget instanceof GridPaneDropTarget;
                    final TreeItem<?> startTreeItem;
                    final TreeCell<?> startCell;
                    final TreeCell<?> stopCell;

                    // TreeItem is null when dropping below the datas
                    // => the drop target is the root
                    if (treeItem == null) {
                        if (rootTreeItem.isLeaf() || !rootTreeItem.isExpanded()) {
                            final var rootCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), 0);
                            panelController.setBorder(rootCell, BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                        } else {
                            final var lastTreeItem = panelController.getLastVisibleTreeItem(rootTreeItem);
                            final var lastCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), lastTreeItem);
                            // As we are dropping below the datas, the last cell is visible
                            assert lastCell != null;
                            panelController.setBorder(lastCell, BorderSide.BOTTOM);

                            // Update vertical insert line
                            startTreeItem = rootTreeItem;
                            startCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), startTreeItem);
                            stopCell = lastCell;
                            updateInsertLineIndicator(startCell, stopCell);
                            panelController.addToPanelControlSkin(insertLineIndicator);
                        }

                    } else {
                        final var item = treeItem.getValue();
                        assert item != null;

                        if (item.isPlaceholder() || item.getFxomObject() == dropTargetObject) {
                            // The place holder item is filled with a container
                            // accepting sub components
                            panelController.setBorder(HierarchyTreeCell.this, BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                        } else {
                            // REORDERING :
                            // To avoid visual movement of the horizontal border when
                            // dragging from one cell to another,
                            // we always set the border on the cell bottom location :
                            // - if we handle REORDER BELOW gesture, just set the bottom 
                            // border on the current cell
                            // - if we handle REORDER ABOVE gesture, we set the bottom 
                            // border on the previous cell
                            //
                            switch (location) {

                                // REORDER ABOVE gesture
                                case TOP:
                                    if (treeItem == rootTreeItem) {
                                        panelController.setBorder(HierarchyTreeCell.this, BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                                    } else {
                                        final var index = getIndex();
                                        // Retrieve the previous cell
                                        // Note : we set the border on the bottom of the previous cell 
                                        // instead of using the top of the current cell in order to avoid
                                        // visual gap when DND from one cell to another
                                        final var previousCell
                                                = HierarchyTreeViewUtils.getTreeCell(getTreeView(), index - 1);
                                        // The previous cell is null when the item is not visible
                                        if (previousCell != null) {
                                            panelController.setBorder(previousCell, BorderSide.BOTTOM);
                                        }

                                        // Update vertical insert line
                                        startTreeItem = panelController.lookupTreeItem(dropTarget.getTargetObject());
                                        startCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), startTreeItem);
                                        stopCell = previousCell;
                                        updateInsertLineIndicator(startCell, stopCell);
                                        panelController.addToPanelControlSkin(insertLineIndicator);
                                    }
                                    break;

                                // REPARENT gesture
                                case CENTER:
                                    if (treeItem.isLeaf() || !treeItem.isExpanded()) {
                                        panelController.setBorder(HierarchyTreeCell.this, BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                                    } else {
                                        // Reparent to the treeItem as last child
                                        final TreeItem<?> lastTreeItem = panelController.getLastVisibleTreeItem(treeItem);
                                        final var lastCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), lastTreeItem);
                                        // Last cell is null when the item is not visible
                                        if (lastCell != null) {
                                            panelController.setBorder(lastCell, BorderSide.BOTTOM);
                                        }

                                        // Update vertical insert line
                                        startTreeItem = getTreeItem();
                                        startCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), startTreeItem);
                                        stopCell = lastCell;
                                        updateInsertLineIndicator(startCell, stopCell);
                                        panelController.addToPanelControlSkin(insertLineIndicator);
                                    }
                                    break;

                                // REORDER BELOW gesture
                                case BOTTOM:
                                    if (treeItem == rootTreeItem
                                            && (treeItem.isLeaf() || !treeItem.isExpanded())) {
                                        panelController.setBorder(HierarchyTreeCell.this, BorderSide.TOP_RIGHT_BOTTOM_LEFT);
                                    } else {
                                        // Reparent to the treeItem as first child
                                        panelController.setBorder(HierarchyTreeCell.this, BorderSide.BOTTOM);

                                        // Update vertical insert line
                                        startTreeItem = panelController.lookupTreeItem(dropTarget.getTargetObject());
                                        startCell = HierarchyTreeViewUtils.getTreeCell(getTreeView(), startTreeItem);
                                        stopCell = HierarchyTreeCell.this;
                                        updateInsertLineIndicator(startCell, stopCell);
                                        panelController.addToPanelControlSkin(insertLineIndicator);
                                    }
                                    break;

                                default:
                                    assert false;
                                    break;
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void updateItem(final HierarchyItem item, final boolean empty) {
        super.updateItem(item, empty);

        // The cell is not empty (TreeItem is not null) 
        // AND the TreeItem value is not null
        if (!empty && item != null) {
            updateLayout(item);
            setGraphic(graphic);
            setText(null);
            // Update parent ring when scrolling / resizing vertically / expanding and collapsing
            panelController.updateParentRing();
        } else {
            assert item == null;
            setGraphic(null);
            setText(null);
            // Clear CSS for empty cells
            panelController.clearBorderColor(this);
        }
    }

    public final void updatePlaceHolder() {
        final var paint = panelController.getParentRingColor();
        placeHolderLabel.setTextFill(paint);
        final var bw = new BorderWidths(1);
        final var bs = new BorderStroke(paint, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, bw);
        final var b = new Border(bs);
        placeHolderLabel.setBorder(b);
    }

    private void filterKeyEvent(final KeyEvent ke) {
        // empty
    }

    private void filterMouseEvent(final MouseEvent me) {

        if (me.getEventType() == MouseEvent.MOUSE_PRESSED
                && me.getButton() == MouseButton.PRIMARY) {

            // Mouse pressed on a non empty cell :
            // => we may start inline editing
            if (!isEmpty()) { // (1)
                if (me.getClickCount() >= 2) {
                    // Start inline editing the display info on double click OVER the display info label
                    // Double click over the class name label will end up with the native expand/collapse behavior
                    final var item = getItem();
                    assert item != null; // Because of (1)
                    final var option = panelController.getDisplayOption();
                    if (item.hasDisplayInfo(option)
                        && !item.isResourceKey(option) // Do not allow inline editing of the I18N value
                        && displayInfoLabel.isHover()) {
                        startEditingDisplayInfo();
                        // Consume the event so the native expand/collapse behavior is not performed
                        me.consume();
                    }
                }
            } //
            // Mouse pressed on an empty cell
            // => we perform select none
            else {
                // We clear the TreeView selection.
                // Note that this is not the same as invoking selection.clear().
                // Indeed, when empty BorderPane place holders are selected,
                // the SB selection is empty whereas the TreeView selection is not.
                getTreeView().getSelectionModel().clearSelection();
            }
        }
        updateCursor(me);
    }

    private void updateCursor(final MouseEvent me) {
        final var scene = getScene();

        if (scene == null) {
            // scene may be null when tree view is collapsed
            return;
        }
        // When another window is focused (just like the preview window), 
        // we use default cursor
        if (!getScene().getWindow().isFocused()) {
            scene.setCursor(Cursor.DEFAULT);
            return;
        }
        if (isEmpty()) {
            scene.setCursor(Cursor.DEFAULT);
        } else {
            final var rootTreeItem = getTreeView().getRoot();
            final var item = getTreeItem().getValue();
            assert item != null;
            final var isRoot = getTreeItem() == rootTreeItem;
            final var isEmpty = item.isEmpty();

            if (me.getEventType() == MouseEvent.MOUSE_ENTERED) {
                if (!me.isPrimaryButtonDown()) {
                    // Cannot DND root or place holder items
                    if (isRoot || isEmpty) {
                        setCursor(Cursor.DEFAULT);
                    } else {
                        setCursor(Cursor.OPEN_HAND);
                    }
                }
            } else if (me.getEventType() == MouseEvent.MOUSE_PRESSED) {
                // Cannot DND root or place holder items
                if (isRoot || isEmpty) {
                    setCursor(Cursor.DEFAULT);
                } else {
                    setCursor(Cursor.CLOSED_HAND);
                }
            } else if (me.getEventType() == MouseEvent.MOUSE_RELEASED) {
                // Cannot DND root or place holder items
                if (isRoot || isEmpty) {
                    setCursor(Cursor.DEFAULT);
                } else {
                    setCursor(Cursor.OPEN_HAND);
                }
            } else if (me.getEventType() == MouseEvent.MOUSE_EXITED) {
                setCursor(Cursor.DEFAULT);
            }
        }
    }

    /**
     * *************************************************************************
     * Inline editing
     *
     * We cannot use the FX inline editing because it occurs on selection +
     * simple mouse click
     * *************************************************************************
     */
    public void startEditingDisplayInfo() {
        assert getItem().hasDisplayInfo(panelController.getDisplayOption());
        final var inlineEditController
                = panelController.getEditorController().getInlineEditController();
        final TextInputControl editor;
        final Type type;
        final String initialValue;

        // 1) Build the TextInputControl used to inline edit
        //----------------------------------------------------------------------
        // INFO display option may use either a TextField or a TextArea
        if (panelController.getDisplayOption() == DisplayOption.INFO) {
            final var info = getItem().getDescription();
            final var sceneGraphObject = getItem().getFxomObject().getSceneGraphObject();
            if (sceneGraphObject instanceof TextArea || DesignHierarchyMask.containsLineFeed(info)) {
                type = Type.TEXT_AREA;
            } else {
                type = Type.TEXT_FIELD;
            }
            // displayInfoLabel.getText() may be truncated to a single line description
            // We set the initial value with the complete description value
            initialValue = getItem().getDescription();
        } //
        // FXID and NODEID options use a TextField
        else {
            type = Type.TEXT_FIELD;
            initialValue = displayInfoLabel.getText();
        }
        editor = inlineEditController.createTextInputControl(type, displayInfoLabel, initialValue);
        // CSS
        final var styleSheets
                = panelController.getPanelRoot().getStylesheets();
        editor.getStylesheets().addAll(styleSheets);
        editor.getStyleClass().add("theme-presets"); //NOI18N
        editor.getStyleClass().add(InlineEditController.INLINE_EDITOR);

        // 2) Build the COMMIT Callback
        // This callback will be invoked to commit the new value
        // It returns true if the value is unchanged or if the commit succeeded, 
        // false otherwise
        //----------------------------------------------------------------------
        final Callback<String, Boolean> requestCommit = newValue -> {
            // 1) Check the input value is valid
            // 2) If valid, commit the new value and return true
            // 3) Otherwise, return false
            final var item = getItem();
            // Item may be null when invoking UNDO while inline editing session is on going
            if (item != null) {
                final var fxomObject = item.getFxomObject();
                final var option = panelController.getDisplayOption();
                final var editorController = panelController.getEditorController();
                switch (option) {
                    case INFO:
                    case NODEID:
                        if (fxomObject instanceof final FXOMInstance fxomInstance) {
                            final var propertyName = item.getPropertyNameForDisplayInfo(option);
                            assert propertyName != null;
                            final var vpm
                                    = Metadata.getMetadata().queryValueProperty(fxomInstance, propertyName);
                            final var job1
                                    = new ModifyObjectJob(fxomInstance, vpm, newValue, editorController);
                            if (job1.isExecutable()) {
                                editorController.getJobManager().push(job1);
                            }
                        }
                        break;
                    case FXID:
                        assert newValue != null;
                        final var fxId = newValue.isEmpty() ? null : newValue;
                        final var job2
                                = new ModifyFxIdJob(fxomObject, fxId, editorController);
                        if (job2.isExecutable()) {

                            // If a controller class has been defined, 
                            // check if the fx id is an injectable field
                            final var controllerClass
                                    = editorController.getFxomDocument().getFxomRoot().getFxController();
                            if (controllerClass != null && fxId != null) {
                                final var location = editorController.getFxmlLocation();
                                final var clazz = fxomObject.getSceneGraphObject() == null ? null
                                        : fxomObject.getSceneGraphObject().getClass();
                                final var glossary = editorController.getGlossary();
                                final var fxIds1 = glossary.queryFxIds(location, controllerClass, clazz);
                                if (!fxIds1.contains(fxId)) {
                                    editorController.getMessageLog().logWarningMessage(
                                            "log.warning.no.injectable.fxid", fxId);
                                }
                            }

                            // Check duplicared fx ids
                            final var fxomDocument = editorController.getFxomDocument();
                            final var fxIds2 = fxomDocument.collectFxIds().keySet();
                            if (fxIds2.contains(fxId)) {
                                editorController.getMessageLog().logWarningMessage(
                                        "log.warning.duplicate.fxid", fxId);
                            }
                            
                            editorController.getJobManager().push(job2);

                        } else if (fxId != null) {
                            editorController.getMessageLog().logWarningMessage(
                                    "log.warning.invalid.fxid", fxId);
                        }
                        break;
                    default:
                        assert false;
                        return false;
                }
            }
            return true;
        };
        inlineEditController.startEditingSession(editor, displayInfoLabel, requestCommit, null);
    }

    private void updateLayout(final HierarchyItem item) {
        assert item != null;
        final var fxomObject = item.getFxomObject();

        // Update styling
        this.getStyleClass().removeAll(HIERARCHY_FIRST_CELL);
        if (fxomObject != null && fxomObject.getParentObject() == null) {
            this.getStyleClass().add(HIERARCHY_FIRST_CELL);
        }

        // Update ImageViews
        final var placeHolderImage = item.getPlaceholderImage();
        placeHolderImageView.setImage(placeHolderImage);
        placeHolderImageView.setManaged(placeHolderImage != null);

        final var classNameImage = item.getClassNameIcon();
        classNameImageView.setImage(classNameImage);
        classNameImageView.setManaged(classNameImage != null);

        // Included file
        if (fxomObject instanceof FXOMIntrinsic
                && ((FXOMIntrinsic) fxomObject).getType() == FXOMIntrinsic.Type.FX_INCLUDE) {
            final var resource = ImageUtils.getNodeIconURL("Included.png"); //NOI18N
            includedFileImageView.setImage(ImageUtils.getImage(resource));
            includedFileImageView.setManaged(true);
        } else {
            includedFileImageView.setImage(null);
            includedFileImageView.setManaged(false);
        }

        final var entries = getErrorReportEntries(item);
        if (entries != null) {
            assert !entries.isEmpty();
            // Update tooltip with the first entry
            final var entry = entries.getFirst();
            warningBadgeTooltip.setText(getErrorReport(entry));
            warningBadgeImageView.setImage(ImageUtils.getWarningBadgeImage());
            warningBadgeImageView.setManaged(true);
            iconsLabel.setTooltip(warningBadgeTooltip);
        } else {
            warningBadgeTooltip.setText(null);
            warningBadgeImageView.setImage(null);
            warningBadgeImageView.setManaged(false);
            iconsLabel.setTooltip(null);
        }

        // Update Labels
        final var placeHolderInfo = item.getPlaceholderInfo();
        placeHolderLabel.setText(placeHolderInfo);
        placeHolderLabel.setManaged(item.isEmpty());
        placeHolderLabel.setVisible(item.isEmpty());

        final var classNameInfo = item.getClassNameInfo();
        classNameInfoLabel.setText(classNameInfo);
        classNameInfoLabel.setManaged(classNameInfo != null);
        classNameInfoLabel.setVisible(classNameInfo != null);

        final var option = panelController.getDisplayOption();
        final var displayInfo = item.getDisplayInfo(option);
        // Do not allow inline editing of the I18N value
        if (item.isResourceKey(option)) {
            displayInfoLabel.getStyleClass().removeAll(HIERARCHY_READWRITE_LABEL);
        } else {
            if (!displayInfoLabel.getStyleClass().contains(HIERARCHY_READWRITE_LABEL)) {
                displayInfoLabel.getStyleClass().add(HIERARCHY_READWRITE_LABEL);
            }
        }
        displayInfoLabel.setText(displayInfo);
        displayInfoLabel.setManaged(item.hasDisplayInfo(option));
        displayInfoLabel.setVisible(item.hasDisplayInfo(option));
    }

    private List<ErrorReportEntry> getErrorReportEntries(final HierarchyItem item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        final var editorController = panelController.getEditorController();
        final var errorReport = editorController.getErrorReport();
        final var fxomObject = item.getFxomObject();
        assert fxomObject != null;
        return errorReport.query(fxomObject, !getTreeItem().isExpanded());
    }
    
    public String getErrorReport(final ErrorReportEntry entry) {

        final var result = new StringBuilder();

        final var fxomNode = entry.getFxomNode();

        switch (entry.getType()) {
            case UNRESOLVED_CLASS:
                result.append(I18N.getString("hierarchy.unresolved.class"));
                break;
            case UNRESOLVED_LOCATION:
                result.append(I18N.getString("hierarchy.unresolved.location"));
                break;
            case UNRESOLVED_RESOURCE:
                result.append(I18N.getString("hierarchy.unresolved.resource"));
                break;
            case INVALID_CSS_CONTENT:
                assert entry.getCssParsingReport() != null;
                result.append(makeCssParsingErrorString(entry.getCssParsingReport()));
                break;
            case UNSUPPORTED_EXPRESSION:
                result.append(I18N.getString("hierarchy.unsupported.expression"));
                break;
        }
        result.append(" "); //NOI18N
        if (fxomNode instanceof final FXOMPropertyT fxomProperty) {
            result.append(fxomProperty.getValue());
        } else if (fxomNode instanceof final FXOMIntrinsic fxomIntrinsic) {
            result.append(fxomIntrinsic.getSource());
        } else if (fxomNode instanceof final FXOMObject fxomObject) {
            final var mask = new DesignHierarchyMask(fxomObject);
            result.append(mask.getClassNameInfo());
        }

        return result.toString();
    }
    
    private void updateInsertLineIndicator(
            final TreeCell<?> startTreeCell,
            final TreeCell<?> stopTreeCell) {

        //----------------------------------------------------------------------
        // START POINT CALCULATION
        //----------------------------------------------------------------------
        // Retrieve the disclosure node from which the vertical line will start
        final double startX;
        final double startY;
        if (startTreeCell != null) {
            final var disclosureNode = startTreeCell.getDisclosureNode();
            final var startBounds = startTreeCell.getLayoutBounds();
            final var startCellPoint = startTreeCell.localToParent(
                    startBounds.getMinX(), startBounds.getMinY());

            final var disclosureNodeBounds = disclosureNode.getLayoutBounds();
            final var disclosureNodePoint = disclosureNode.localToParent(
                    disclosureNodeBounds.getMinX(), disclosureNodeBounds.getMinY());

            // Initialize start point to the disclosure node of the start cell
            startX = startCellPoint.getX()
                    + disclosureNodePoint.getX()
                    + disclosureNodeBounds.getWidth() / 2 + 1; // +1 px tuning
            startY = startCellPoint.getY()
                    + disclosureNodePoint.getY()
                    + disclosureNodeBounds.getHeight() - 6; // -6 px tuning
        } else {
            // The start cell is not visible :
            // x is set to the current cell graphic
            // y is set to the top of the TreeView / TreeTableView
            final var graphicBounds = getGraphic().getLayoutBounds();
            final var graphicPoint = getGraphic().localToParent(
                    graphicBounds.getMinX(), graphicBounds.getMinY());

            startX = graphicPoint.getX();
            startY = panelController.getContentTopY();
        }

        //----------------------------------------------------------------------
        // END POINT CALCULATION
        //----------------------------------------------------------------------
        final double endX;
        final double endY;
        endX = startX;
        if (stopTreeCell != null) {
            final var stopBounds = stopTreeCell.getLayoutBounds();
            final var stopCellPoint = stopTreeCell.localToParent(
                    stopBounds.getMinX(), stopBounds.getMinY());

            // Initialize end point to the end cell
            endY = stopCellPoint.getY()
                    + stopBounds.getHeight() // Add the stop cell height
                    - 1; // -1 px tuning
        } else {
            // The stop cell is not visisble :
            // y is set to the bottom of the TreeView / TreeTableView
            endY = panelController.getContentBottomY();
        }

        insertLineIndicator.setStartX(startX);
        insertLineIndicator.setStartY(startY);
        insertLineIndicator.setEndX(endX);
        insertLineIndicator.setEndY(endY);
    }

    private DroppingMouseLocation getDroppingMouseLocation(final DragEvent event) {
        final DroppingMouseLocation location;
        if (this.getTreeItem() != null) {
            if ((getHeight() * 0.25) > event.getY()) {
                location = DroppingMouseLocation.TOP;
            } else if ((getHeight() * 0.75) < event.getY()) {
                location = DroppingMouseLocation.BOTTOM;
            } else {
                location = DroppingMouseLocation.CENTER;
            }
        } else {
            location = DroppingMouseLocation.BOTTOM;
        }
        return location;
    }
    
    private String makeCssParsingErrorString(final CSSParsingReport r) {
        final var result = new StringBuilder();
        
        if (r.getIOException() != null) {
            result.append(r.getIOException());
        } else {
            assert !r.getParseErrors().isEmpty();
            var errorCount = 0;
            for (final var e : r.getParseErrors()) {
                result.append(e.getMessage());
                errorCount++;
                if (errorCount < 5) {
                    result.append('\n');
                } else {
                    result.append("...");
                    break;
                }
            }
        }
        
        return result.toString();
    }
    
}
