/*
 * Copyright (c) 2017, 2024, Gluon and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.css;

import com.oracle.javafx.scenebuilder.kit.editor.DocumentationUrls;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform.Theme;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.AbstractDragSource;
import com.oracle.javafx.scenebuilder.kit.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.BeanPropertyState;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.CssPropertyState;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.CssPropertyState.CssStyle;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.PropertyState;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssValuePresenterFactory.CssValuePresenter;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.NodeCssState.CssProperty;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.SelectionPath.Item;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.ErrorDialog;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.SelectionPath.Path;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.FadeTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.ParsedValue;
import javafx.css.PseudoClass;
import javafx.css.Rule;
import javafx.css.StyleOrigin;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import javafx.util.Duration;

/**
 * Controller for the CSS Panel.
 *
 */
public class CssPanelController extends AbstractFxmlPanelController {
    
    private static final Logger LOGGER = Logger.getLogger(CssPanelController.class.getName());

    @FXML
    private StackPane cssPanelHost;

    @FXML
    private StackPane cssSearchPanelHost;

    @FXML
    private TableColumn<CssProperty, CssProperty> beanApiColumn;
    @FXML
    private TableColumn<CssProperty, CssProperty> builtinColumn;
    @FXML
    private TableColumn<CssProperty, CssProperty> fxThemeColumn;
    @FXML
    private TableColumn<CssProperty, CssProperty> inlineColumn;
    @FXML
    private TableColumn<CssProperty, CssProperty> propertiesColumn;
    @FXML
    private TableColumn<CssProperty, CssProperty> defaultColumn;
    @FXML
    private ToggleButton pick;
    @FXML
    private ToggleButton edit;
    @FXML
    SelectionPath selectionPath;
    @FXML
    private VBox root;
    @FXML
    private HBox header;
    @FXML
    WebView textPane;
    @FXML
    private VBox rulesBox;
    @FXML
    private ScrollPane rulesPane;
    @FXML
    private TableColumn<CssProperty, CssProperty> stylesheetsColumn;
    @FXML
    private TableView<CssProperty> table;
    @FXML
    private StackPane messagePane;
    @FXML
    private Label messageLabel;

    private TreeView<Node> rulesTree;

    private boolean advanced = false;
    private boolean styledOnly = false;
    private boolean tableColumnsOrderingReversed = false;
    private boolean dragOnGoing = false;

    private ObservableList<CssProperty> model;

    private View currentView = View.TABLE;

    private String searchPattern;
    private static Image lookups = null;

    private static final String NO_MATCHING_RULES = I18N.getString("csspanel.no.matching.rule");

    public enum View {

        TABLE, RULES, TEXT;
    }

    private Object selectedObject; // Can be either an FXOMObject (selection mode), or a Node (pick mode)
    private Selection selection;
    private final EditorController editorController;
    private final Delegate applicationDelegate;
    private final ObjectProperty<NodeCssState> cssStateProperty = new SimpleObjectProperty<>();

    /**
     * Should be implemented by the application.
     *
     * @treatAsPrivate
     */
    public static abstract class Delegate {

        public Delegate() {
            // no-op
        }

        public abstract void revealInspectorEditor(ValuePropertyMetadata propMeta);
    }

    /*
     * AbstractPanelController
     */
    @Override
    protected void fxomDocumentDidChange(final FXOMDocument oldDocument) {
        if (isCssPanelLoaded() && hasFxomDocument()) {
            updateSelectedObject();
            refresh();
        }
    }

    @Override
    protected void sceneGraphRevisionDidChange() {
        // System.out.println("CssPanelController.sceneGraphRevisionDidChange() called!");
        if (isCssPanelLoaded() && hasFxomDocument()) {
            refresh();
        }
    }

    @Override
    protected void cssRevisionDidChange() {
        // System.out.println("CssPanelController.cssRevisionDidChange() called!");
        if (isCssPanelLoaded() && hasFxomDocument()) {
            refresh();
        }
    }

    @Override
    protected void jobManagerRevisionDidChange() {
        // FXOMDocument has been modified by a job.
        // getEditorController().getJobManager().getLastJob()
        // is the job responsible of the change.
        // Since sceneGraphRevisionDidChange() will be called in this case, nothing to do here.
    }

    @Override
    protected void editorSelectionDidChange() {
        if (isCssPanelLoaded() && hasFxomDocument() && !dragOnGoing) {
            updateSelectedObject();
            refresh();
        }
    }

    /**
     * AbstractFxmlPanelController.
     *
     */
    @Override
    protected void controllerDidLoadFxml() {
        // Remove scrollPane for rules
        root.getChildren().remove(rulesPane);
        root.getChildren().remove(textPane);
        root.getChildren().remove(table);

        pick.setOnAction(t -> editorController.setPickModeEnabled(true));
        edit.setOnAction(t -> editorController.setPickModeEnabled(false));
        editorController.pickModeEnabledProperty().addListener((ChangeListener<Boolean>) (ov, oldVal, newVal) -> setPickMode(newVal));
        // Initialize the pick mode from the editorController value
        setPickMode(editorController.isPickModeEnabled());

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        disableColumnReordering();
        final Callback<TableColumn.CellDataFeatures<CssProperty, CssProperty>, ObservableValue<CssProperty>> valueFactory
                = new ValueFactory();

        propertiesColumn.setCellValueFactory(valueFactory);
        propertiesColumn.setCellFactory(new PropertiesCellFactory());

        builtinColumn.setCellValueFactory(valueFactory);
        builtinColumn.setCellFactory(new BuiltinCellFactory());

        fxThemeColumn.setCellValueFactory(valueFactory);
        fxThemeColumn.setCellFactory(new FxThemeCellFactory());

        beanApiColumn.setCellValueFactory(valueFactory);
        beanApiColumn.setCellFactory(new ModelCellFactory());

        stylesheetsColumn.setCellValueFactory(valueFactory);
        stylesheetsColumn.setCellFactory(new AuthorCellFactory());

        inlineColumn.setCellValueFactory(valueFactory);
        inlineColumn.setCellFactory(new InlineCellFactory());

        defaultColumn.setCellValueFactory(valueFactory);
        defaultColumn.setCellFactory(new DefaultCellFactory());

        editorController.themeProperty().addListener((ChangeListener<Theme>) (ov, t, t1) -> refresh());

        cssStateProperty.addListener((ChangeListener<NodeCssState>) (arg0, oldValue, newValue) -> fillPropertiesTable());

        final ChangeListener<Item> selectionListener = (arg0, oldvalue, newValue) -> {
            if (newValue != null && newValue.getItem() != null) {
                final var selectedSubNode = CssUtils.getNode(newValue.getItem());
                selectedObject = selectedSubNode;
                refresh();
                // Switch to pick mode
                editorController.setPickModeEnabled(true);
//                    // Select the sub node
                selection = editorController.getSelection();
                selection.select(getFXOMInstance(selection), selectedSubNode);
            }
        };
        selectionPath.selected().addListener(selectionListener);

        // Listen the drag property changes
        getEditorController().getDragController().dragSourceProperty().addListener((ChangeListener<AbstractDragSource>) (ov, oldVal, newVal) -> {
            if (newVal != null) {
//                    System.out.println("Drag started !");
                dragOnGoing = true;
            } else {
//                    System.out.println("Drag finished.");
                dragOnGoing = false;
                updateSelectedObject();
                refresh();
            }
        });

        // View table by default
        changeView(CssPanelController.View.TABLE);

        editorSelectionDidChange();
    }

    private static class ValueFactory implements Callback<TableColumn.CellDataFeatures<CssProperty, CssProperty>, ObservableValue<CssProperty>> {

        @Override
        public ObservableValue<CssProperty> call(final TableColumn.CellDataFeatures<CssProperty, CssProperty> param) {
            final ObjectProperty<CssProperty> val = new SimpleObjectProperty<>();
            val.setValue(param.getValue());
            return val;
        }
    }

    private static class PropertiesCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(final TableColumn<CssProperty, CssProperty> param) {
            return new CssPropertyTableCell();
        }
    }

    private class BuiltinCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(final TableColumn<CssProperty, CssProperty> param) {
            return new BuiltinValueTableCell();
        }
    }

    private class FxThemeCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(final TableColumn<CssProperty, CssProperty> param) {
            return new FxThemeValueTableCell();
        }
    }

    private class ModelCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(final TableColumn<CssProperty, CssProperty> param) {
//            System.out.println("Creating new ModelValueTableCell...");
            return new ModelValueTableCell();
        }
    }

    private class AuthorCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(final TableColumn<CssProperty, CssProperty> param) {
            return new AuthorValueTableCell();
        }
    }

    private class InlineCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(final TableColumn<CssProperty, CssProperty> param) {
            return new InlineValueTableCell();
        }
    }

    private class DefaultCellFactory implements Callback<TableColumn<CssProperty, CssProperty>, TableCell<CssProperty, CssProperty>> {

        @Override
        public TableCell<CssProperty, CssProperty> call(final TableColumn<CssProperty, CssProperty> param) {
            return new DefaultValueTableCell();
        }
    }

    /*
     *
     * Public
     *
     */
    public CssPanelController(final EditorController c, final Delegate delegate) {
        super(CssPanelController.class.getResource("CssPanel.fxml"), I18N.getBundle(), c);
        this.editorController = c;
        this.applicationDelegate = delegate;
    }

    public String getSearchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(final String searchPattern) {
        this.searchPattern = searchPattern;
        searchPatternDidChange();
    }

    /**
     * 
     * @param selectionListener selection listener.
     * @treatAsPrivate
     */
    public void addSelectionListener(final ChangeListener<Item> selectionListener) {
        selectionPath.selected().addListener(selectionListener);
    }

    /**
     * 
     * @param path path.
     * @treatAsPrivate
     */
    public void setSelectionPath(final Path path) {
        selectionPath.setSelectionPath(path);
    }

    /**
     * @treatAsPrivate
     */
    public void resetSelectionPath() {
        selectionPath.setSelectionPath(new Path(new ArrayList<>()));
    }

    /**
     * 
     * @param mess message.
     * @treatAsPrivate
     */
    public void viewMessage(final String mess) {
        root.getChildren().removeAll(messagePane, header, table, rulesPane, textPane);
//        mainMenu.setDisable(true);
//        searchBox.setDisable(true);
        messageLabel.setText(mess);
        root.getChildren().add(messagePane);
    }

    /**
     *
     * @return node.
     * @treatAsPrivate
     */
    public final Node getRulesPane() {
        return rulesPane;
    }

    /**
     *
     * @return node.
     * @treatAsPrivate
     */
    public final Node getTextPane() {
        return textPane;
    }

    /**
     *
     * @param model model.
     * @param state state.
     * @treatAsPrivate
     */
    public void setContent(final ObservableList<CssProperty> model, final NodeCssState state) {
        changeView(currentView);
        initializeRulesTextPanes(state);
        this.model = FXCollections.observableArrayList(model);
    }

    /**
     *
     * @treatAsPrivate
     */
    public void clearContent() {
        table.getItems().clear();
        resetSelectionPath();
    }

    public void filter(final String pattern) {
        if (model == null) {
            return;
        }
        final ObservableList<CssProperty> filtered;
        if (pattern == null || pattern.trim().length() == 0) {
            filtered = model;
        } else {
            filtered = FXCollections.observableArrayList();
            for (final var p : model) {
                if (p.propertyName().get().contains(pattern.trim())) {
                    filtered.add(p);
                }
            }
        }
        updateTable(filtered);
    }

    /**
     *
     * @param parent parent.
     * @param cssProp css property.
     * @param style css style.
     * @param applied applied.
     * @param isLookup lookup.
     * @treatAsPrivate
     */
    public static void attachStyleProperty(final TreeItem<Node> parent, final CssPropertyState cssProp, final CssStyle style,
                                           final boolean applied, final boolean isLookup) {
        if (isLookup) {
            final var cssValue = CssValueConverter.toCssString(style.getCssProperty(), style.getCssRule(), style.getParsedValue());
            final var item = new TreeItem<Node>(getContent(style.getCssProperty(), cssValue, style.getParsedValue(), applied));
            parent.getChildren().add(item);
        } else {
            attachStylePropertyNoLookup(parent, cssProp, style, applied);
        }
    }

    public void changeView(final View view) {
        switch (view) {
            case TABLE: {
                root.getChildren().removeAll(messagePane, header, table, rulesPane, textPane);
                root.getChildren().addAll(header, table);
                break;
            }
            case RULES: {
                root.getChildren().removeAll(messagePane, header, rulesPane, table, textPane);
                root.getChildren().addAll(header, rulesPane);
                break;
            }
            case TEXT: {
                root.getChildren().removeAll(messagePane, header, textPane, table, rulesPane);
                root.getChildren().addAll(header, textPane);
                break;
            }
        }
        currentView = view;
    }

    /**
     *
     * @treatAsPrivate
     */
    public void copyStyleablePath() {
        final var content = new ClipboardContent();
        content.putString(selectionPath.toString());
        Clipboard.getSystemClipboard().setContent(content);
    }

    public void splitDefaultsAction() {
        advanced = !advanced;
        unmerge();
    }

    public void showStyledOnly() {
        styledOnly = !styledOnly;
        if (model == null) {
            return;
        }
        showStyled(model);
    }

    public void toggleTableColumnsOrdering() {
        // switch the table columns:
        // Default to Inline ==> Inline to Defaults
        // (and vice-versa)
        final var columns = table.getColumns();
        FXCollections.reverse(columns);
        // Property column is always first
        final var propertyColumn = columns.getLast();
        columns.remove(propertyColumn);
        columns.addFirst(propertyColumn);
        tableColumnsOrderingReversed = !tableColumnsOrderingReversed;
    }

    public boolean isTableColumnsOrderingReversed() {
        return tableColumnsOrderingReversed;
    }

    public void setTableColumnsOrderingReversed(final boolean value) {
        if (table != null && tableColumnsOrderingReversed != value) {
            toggleTableColumnsOrdering();
        }
    }

    /*
     *
     * FXML methods.
     *
     * @treatAsPrivate
     */
    public void initialize() {

    }

    /*
     *
     * Private
     *
     */
    private void refresh() {
        setCSSContent();
    }

    private void updateSelectedObject() {
        selection = editorController.getSelection();
        if (!isMultipleSelection()) {
            if (isPickMode()) {
                // In pick mode:
                // If the selected node is the "root" node ==> we get its FXOMInstance
                // Else, we don't have an FXOMInstance
                final Object pickObject = selection.getCheckedHitNode();
                final var fxomInstance = getFXOMInstance(selection);
                if (fxomInstance != null && fxomInstance.getSceneGraphObject() == pickObject) {
                    selectedObject = fxomInstance;
                } else {
                    selectedObject = pickObject;
                }
//                System.out.println("(pick mode) selectedObject = " + selectedObject);
            } else {
                selectedObject = getFXOMInstance(selection);
//                System.out.println("selectedObject = " + selectedObject);
            }
        }
    }

    private static String getFirstStandardClassName(final Class<?> type) {
        var clazz = type;
        while (clazz != null) {
            if (clazz.getName().startsWith("javafx")) {//NOI18N
                return clazz.getSimpleName();
            }
            clazz = clazz.getSuperclass();
        }
        return type.getName();
    }

    private void collectCss() {
        if (selectedObject != null) {
            final var state = CssContentMaker.getCssState(selectedObject);
            if (state == null) {
                return;
            }
            cssStateProperty.setValue(state);
        }
    }

    private void setCSSContent() {
        if (selectedObject instanceof Skinnable) {
            if (((Skinnable) selectedObject).getSkin() == null) {
                return;
            }
        }

        clearContent();
        if (isMultipleSelection()) {
            viewMessage(I18N.getString("csspanel.multiselection"));
            return;
        }
        
        if (selectedObject != null) { // Update content.
            fillSelectionContent();
            collectCss();
        }
    }

    private boolean isMultipleSelection() {
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            return ((ObjectSelectionGroup) selection.getGroup()).getItems().size() > 1;
        } else {
            // GridSelectionGroup: consider the GridPane only
            return false;
        }
    }

    private boolean isCssPanelLoaded() {
        return root != null;
    }

    private boolean hasFxomDocument() {
        return getEditorController().getFxomDocument() != null;
    }

    private boolean isPickMode() {
        return editorController.isPickModeEnabled();
    }

    private void setPickMode(final boolean pickMode) {
        pick.setSelected(pickMode);
        edit.setSelected(!pickMode);
    }

    private void fillSelectionContent() {
        assert selectedObject != null;
        // Start from the Component;
        final var selectedRootNode = CssUtils.getSelectedNode(getFXOMInstance(selection));
        if (selectedRootNode == null) {
            return;
        }
        final var rootItem = new Item(selectedRootNode, createItemName(selectedRootNode), createOptional(selectedRootNode));//NOI18N

        // Seems we can skip the skin now, which is not in the scene graph anymore.
//        if (componentRootNode instanceof Skinnable) {
//            assert ((Skinnable) componentRootNode).getSkin() != null;
//            Node skinNode = ((Skinnable) componentRootNode).getSkin().getNode();
//            if (skinNode instanceof Parent) {
//                addSubStructure(componentRootNode, rootItem, (Parent) skinNode);
//            }
//        } else {
        if (selectedRootNode instanceof Parent) {
            addSubStructure(selectedRootNode, rootItem, (Parent) selectedRootNode);
        }
//        }
        final Object selectedNode = CssUtils.getSelectedNode(selectedObject);
        final var items = SelectionPath.lookupPath(rootItem, selectedNode);
        setSelectionPath(new Path(items));
    }

    private void addSubStructure(final Node componentRootNode, Item parentItem, final Node node) {
        final var fxomDoc = getEditorController().getFxomDocument();
        assert fxomDoc != null;
        final var enclosingNode = getEnclosingNode(fxomDoc, node);
        // The componentRootNode can be a skin structure (Tab, Column), in this case the enclosingNode 
        // is not == to the componentRootNode. That is why we need to compare the enclosingNode of both
        // n and componentRootNode nodes.
        final var componentRootNodeEnclosingNode = getEnclosingNode(fxomDoc, componentRootNode);
        // this is a skin's node and not a node from a component located inside 
        // the skin (eg: SplitPane content being a Button is not part of the SplitPane Skin.
        final var isOtherComponentNode = enclosingNode != componentRootNodeEnclosingNode;
        if (componentRootNode != node && !node.getStyleClass().isEmpty() && !isOtherComponentNode && !(node instanceof Skin)) {
            final var ni = new Item(node, createItemName(node), createOptional(node));//NOI18N
            parentItem.getChildren().add(ni);
            parentItem = ni;
        }
        if (node instanceof Parent && !isOtherComponentNode) {
            final var parentNode = (Parent) node;
            for (final var child : parentNode.getChildrenUnmodifiable()) {
                addSubStructure(componentRootNode, parentItem, child);
            }
        }
    }

    private Node getEnclosingNode(final FXOMDocument fxomDoc, final Node n) {
        var node = n;
        var enclosingFXOMObj = fxomDoc.searchWithSceneGraphObject(node);
        while (enclosingFXOMObj == null) {
            node = node.getParent();
            if (node == null) {
                return null;
            }
            enclosingFXOMObj = fxomDoc.searchWithSceneGraphObject(node);
        }
        final var enclosingObj = enclosingFXOMObj.getSceneGraphObject();
        assert enclosingObj instanceof Node;
        return (Node) enclosingObj;
    }

    private void fillPropertiesTable() {
        final var state = cssStateProperty.getValue();
        if (state == null) {
            return;
        }
        fillContent(state);
        filter(searchPattern);
    }

    private void fillContent(final NodeCssState state) {
        final ObservableList<CssProperty> cssModel = FXCollections.observableArrayList();
        final var styleables = state.getAllStyleables();
        for (final var sp : styleables) {
            cssModel.add(sp);
            for (final var sub : sp.getSubProperties()) {
                cssModel.add(sub);
            }
        }
        setContent(cssModel, state);
    }

    private void attachNotAppliedStyles(final TreeItem<Node> ti, final PropertyState css) {
        for (final var style : css.getNotAppliedStyles()) {
            attachStyle(css, style, ti, false);
        }
    }

    private void attachSubProperties(final TreeItem<Node> parent, final PropertyState ss) {
        for (final var sub : ss.getSubProperties()) {
            attachProperty(parent, sub);
        }
        attachNotAppliedStyles(parent, ss);
    }

    private void attachProperty(final TreeItem<Node> parent, final PropertyState ss) {
        final var hasSubs = !ss.getSubProperties().isEmpty();
        if (hasSubs) {
            if (ss instanceof CssPropertyState) {
                final var cssProp = (CssPropertyState) ss;
                if (cssProp.getStyle() != null) {
                    // Need to add the container, not the sub properties
                    final var content = getContent(ss.getCssProperty(), ss.getCssValue(), ss.getFxValue(), true);
                    final var ti = newTreeItem(content, ss);
                    parent.getChildren().add(ti);
                    attachStyles(cssProp, ti);
                    attachNotAppliedStyles(parent, ss);
                } else {
                    attachSubProperties(parent, ss);
                }
            } else {
                attachSubProperties(parent, ss);
            }
        } else {
            final var content = getContent(ss.getCssProperty(), ss.getCssValue(), ss.getFxValue(), true);
            final var ti = newTreeItem(content, ss);
            parent.getChildren().add(ti);
            if (ss instanceof CssPropertyState) {
                final var css = (CssPropertyState) ss;
                attachStyles(css, ti);
            } else {
                if (ss instanceof BeanPropertyState) {
                    final var beanProp = (BeanPropertyState) ss;
                    final var source = beanProp.getPropertyMeta().getName().toString();
                    final var contentBuilder = new StringBuilder();
                    contentBuilder.append(source);
                    ti.getChildren().add(newTreeItem(new Label(contentBuilder.toString()), ss));
                }
            }
            attachNotAppliedStyles(ti, ss);
        }
    }

    private void searchPatternDidChange() {
        filter(searchPattern);
    }

    private void updateTable(final ObservableList<CssProperty> currentModel) {
        showStyled(currentModel);
        unmerge();
    }

    private void disableColumnReordering() {
        for (final var column : table.getColumns()) {
            column.setReorderable(false);
        }
    }

    private void initializeRulesTextPanes(final NodeCssState state) {
        rulesBox.getChildren().clear();
        final var rulesList = state.getMatchingRules();
        final var htmlStyler = new HtmlStyler();
        rulesTree = new TreeView<>();
        CopyHandler.attachContextMenu(rulesTree);
        rulesTree.setShowRoot(false);
        final var ruleRoot = new TreeItem<Node>(new Text(""));//NOI18N
        rulesTree.setRoot(ruleRoot);
        for (final var rule : rulesList) {
            final var lst = rule.getDeclarations();

            final var selector = rule.getSelector();

            final var txt = selector + " { ";//NOI18N
            final var source = nonNull(getSource(rule.getRule()));
            final var text = CopyHandler.makeCopyableNode(new Text(txt + source), txt);
            final var start = new TreeItem<Node>(text);
            ruleRoot.getChildren().add(start);
            htmlStyler.cssRuleStart(selector, source);

            for (final var p : lst) {
                final var prop = p.getProp();
                attachStyleProperty(ruleRoot, prop, p.getStyle(), p.isApplied(), p.isLookup());
                final var style = p.getStyle();
                final var cssValue = CssValueConverter.toCssString(style.getCssProperty(), style.getCssRule(), style.getParsedValue());
                htmlStyler.addProperty(p.getStyle().getCssProperty(), cssValue, p.isApplied());
            }
            final var end = new TreeItem<Node>(CopyHandler.createCopyableText("}"));//NOI18N
            ruleRoot.getChildren().add(end);
            setTreeHeight(rulesTree);
            htmlStyler.cssRuleEnd();
        }
        if (ruleRoot.getChildren().isEmpty()) {
            rulesBox.getChildren().add(new Label(NO_MATCHING_RULES));
        } else {
            rulesBox.getChildren().add(rulesTree);
        }
        if (htmlStyler.isEmpty()) {
            htmlStyler.addMessage(NO_MATCHING_RULES);
        }
        textPane.getEngine().loadContent(htmlStyler.getHtmlString());
    }

    void close(final ChangeListener<Item> selectionListener) {
        selectionPath.selected().removeListener(selectionListener);
    }

    private void unmerge() {
        defaultColumn.setVisible(!advanced);
        fxThemeColumn.setVisible(advanced);
        builtinColumn.setVisible(advanced);
    }

    private void showStyled(ObservableList<CssProperty> currentModel) {
        if (styledOnly) {
            currentModel = extractStyled(currentModel);
        }
        table.getItems().setAll(currentModel);
    }

    private static ObservableList<CssProperty> extractStyled(final ObservableList<CssProperty> currentModel) {
        final ObservableList<CssProperty> ret = FXCollections.observableArrayList();
        for (final var prop : currentModel) {
            if (prop.isAuthorSource() || prop.isInlineSource() || prop.isModelSource()) {
                ret.add(prop);
            }
        }
        return ret;
    }

    private static String nodeIdentifier(final Node n) {
        if (n.getId() != null && !n.getId().equals("")) {//NOI18N
            return n.getId();
        } else {
            return n.getClass().getSimpleName();
        }
    }

    /*
     *
     * TABLE CELLS CONTENT
     *
     */
    // "Properties" column
    private static class CssPropertyTableCell extends TableCell<CssProperty, CssProperty> {

        CssPropertyTableCell() {
            getStyleClass().add("property-background");//NOI18N
        }

        @Override
        public void updateItem(final CssProperty item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                final var hl = new Hyperlink();
                hl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                hl.setOnAction(new LinkActionListener(item));
                hl.setAlignment(Pos.CENTER_LEFT);
                if (item.getMainProperty() != null) {
                    hl.setText("     " + item.propertyName().get());//NOI18N
                } else {
                    hl.setText(item.propertyName().get());
                }
                setGraphic(hl);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        }
    }

    private static class LinkActionListener implements EventHandler<ActionEvent> {

        final CssProperty item;

        public LinkActionListener(final CssProperty item) {
            this.item = item;
        }

        @Override
        public void handle(final ActionEvent event) {
            try {
                // XXX jfdenise, for now can't do better than opening the file, no Anchor per property...
                // Retrieve defining class
                EditorPlatform.open(DocumentationUrls.JAVADOC_HOME.toString()
                        + "javafx.graphics/javafx/scene/doc-files/cssref.html#" + //NOI18N
                        item.getTarget().getClass().getSimpleName().toLowerCase(Locale.ROOT));
            } catch (final IOException e) {
                Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to open JAVADOC_HOME: ", e);
            }
        }
    }

    // Css value cell content
    private abstract class CssValueTableCell extends TableCell<CssProperty, CssProperty> {

        private final List<Value> values = new ArrayList<>();
        private VBox valueBox;
        private MenuButton navigationMenuButton;
        private FadeTransition fadeTransition;
        private final MenuItem revealInInspectorMenuItem = new MenuItem(I18N.getString("csspanel.reveal.inspector"));
        private final MenuItem revealInFileBrowserMenuItem = new MenuItem();
        private MenuItem openStylesheetMenuItem = new MenuItem();

        // TODO : UI should be in an fxml file
        private class Value extends AnchorPane {

            private final VBox vbox;
            private Label sourceLabel;
            private MenuButton navigationMenuButton;

            private Value(final Node value) {
                vbox = new VBox(2);
                setAlignment(Pos.CENTER_LEFT);
                vbox.getChildren().add(value);
                getChildren().add(vbox);
                AnchorPane.setTopAnchor(vbox, 4.0);
                AnchorPane.setLeftAnchor(vbox, 4.0);
                AnchorPane.setRightAnchor(vbox, 4.0);
                AnchorPane.setBottomAnchor(vbox, 4.0);
                setOnMouseEntered(arg0 -> {
                    if ((navigationMenuButton != null) && !navigationMenuButton.isShowing()) {
                        fadeMenuButtonTo(1);
                    }
                });
                setOnMouseExited(arg0 -> {
                    if ((navigationMenuButton != null) && !navigationMenuButton.isShowing()) {
                        fadeMenuButtonTo(0);
                    }
                });
            }

            private void setSource(final Label sourceLabel) {
                this.sourceLabel = sourceLabel;
                vbox.getChildren().add(sourceLabel);
            }

            private void setNavigation(final Label navigationLabel, final MenuButton navigationMenuButton) {
                this.navigationMenuButton = navigationMenuButton;
                vbox.getChildren().add(navigationLabel);
                if (navigationMenuButton != null) {
                    getChildren().add(navigationMenuButton);
                    AnchorPane.setTopAnchor(navigationMenuButton, 4.0);
                    AnchorPane.setRightAnchor(navigationMenuButton, 4.0);
                }
            }

            private void showSource(final boolean show) {
                if (sourceLabel != null) {
                    sourceLabel.setVisible(show);
                }
                if (navigationMenuButton != null) {
                    navigationMenuButton.setVisible(show);
                }
            }

            private void fadeMenuButtonTo(final double toValue) {
                fadeTransition.stop();
                fadeTransition.setFromValue(navigationMenuButton.getOpacity());
                fadeTransition.setToValue(toValue);
                fadeTransition.play();
            }
        }

        private CssValueTableCell() {
            //  showSources.selectedProperty().addListener(new WeakChangeListener<>(sourceListener));
        }

        /**
         * WARNING: TableCell instances are reused by TableView. It must be
         * stateless. In our case, value, sourceLabel and navigationLabel MUST
         * be cleared each time updateItem is called.
         *
         * @param item
         * @param empty
         */
        @Override
        public void updateItem(final CssProperty item, final boolean empty) {
            super.updateItem(item, empty);
//            System.out.println("CssValueTableCell.updateItem() called for property: " + item.getStyleable().getProperty() );
            values.clear();
            setGraphic(null);
            if (!empty) {
                if (getStyle(item) != null && !getStyle(item).isUsed()) {//eg: -fx-backgroundfills
                    return;
                }
                // A new node MUST be constructed on each call, otherwise TableView looses the UI<->model relationship
                valueBox = new VBox(2);
                final Value currentValue;
                valueBox.setAlignment(Pos.CENTER);
                final var cssState = getPropertyState(item);
                if (cssState != null) {
                    Node n;
                    n = createValueUI(item, cssState, cssState.getFxValue(), getStyle(item));
                    if (n == null) {
                        final var label = new Label(cssState.getCssValue());
                        n = label;
                    }
                    currentValue = new Value(n);
                    if (isWinner(item)) {
                        currentValue.getStyleClass().add("winner-background");//NOI18N
                    }
                    values.add(currentValue);
                    handleSource(currentValue, item, getStyle(item));
                }

                handleNotApplied(item);

                displayValues();
            }
        }

        private void displayValues() {
            for (final var v : values) {
                VBox.setVgrow(v, Priority.ALWAYS);
                valueBox.getChildren().add(v);
            }
            setGraphic(valueBox);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        protected abstract PropertyState getPropertyState(CssProperty item);

        protected abstract CssStyle getStyle(CssProperty item);

        protected abstract boolean isWinner(CssProperty item);

        protected abstract StyleOrigin getOrigin(CssProperty item);

        protected String getNavigation(final CssProperty item, final CssStyle style) {
            return getNavigationInfo(item, style, getOrigin(item));
        }

        private void handleNotApplied(final CssProperty item) {
            final var ps = item.getWinner();
            if (ps != null) {
                for (final var style : ps.getNotAppliedStyles()) {
                    if (style.getOrigin() == getOrigin(item) && !CssContentMaker.containsPseudoState(style.getSelector())) {
                        var n = createValueUI(item, style);
                        if (n == null) {
                            n = getLabel(style);
                        }
                        final var currentValue = new Value(n);
                        values.add(currentValue);
                        handleSource(currentValue, item, style);
                    }
                }
            }
            // Case where an API call has been made, not applied fxTheme (if any) are hidden
            if (getOrigin(item) == StyleOrigin.USER_AGENT) {
                final var styles = item.getFxThemeHiddenByModel();
                for (final var style : styles) {
                    var n = createValueUI(item, style);
                    if (n == null) {
                        final var l = CssValueConverter.toCssString(style.getCssProperty(), style.getCssRule(), style.getParsedValue());
                        final var label = new Label(l);
                        n = label;
                    }
                    final var currentValue = new Value(n);
                    values.add(currentValue);
                    handleSource(currentValue, item, style);
                }
            }
        }

        private void handleSource(final Value currentValue, final CssProperty item, final CssStyle style) {
            if (style != null && !style.isUsed()) {//eg: -fx-background-fills;
                return;
            }
            final var origin = getOrigin(item);
            final var source = getSourceInfo(item, style, origin);
            if (source != null) {
                final var sourceLabel = new Label(source);
                sourceLabel.getStyleClass().add("note-label");//NOI18N
                currentValue.setSource(sourceLabel);
            }
            final var nav = getNavigation(item, style);
            if (nav != null) {
                final var navigationLabel = new Label(nav);
                navigationLabel.getStyleClass().add("note-label");//NOI18N
                if (origin != null && origin != StyleOrigin.USER_AGENT) {// No arrow for builtin and fxTheme
                    createNavigationMenuButton();
                    openStylesheetMenuItem
                            = new MenuItem(MessageFormat.format(I18N.getString("csspanel.open.stylesheet"), nav));
                    if ((origin == StyleOrigin.USER) || (origin == StyleOrigin.INLINE)) {
                        // Inspector or Inline columns
                        navigationMenuButton.getItems().add(revealInInspectorMenuItem);
                        revealInInspectorMenuItem.setOnAction(event -> navigate(item, getPropertyState(item), style, origin));
                        if (CssPanelController.this.applicationDelegate == null) {
                            // disable the menu item in this case
                            revealInInspectorMenuItem.setDisable(true);
                        }
                    } else if (origin == StyleOrigin.AUTHOR) {
                        // Stylesheets column
                        navigationMenuButton.getItems().add(openStylesheetMenuItem);
                        navigationMenuButton.getItems().add(revealInFileBrowserMenuItem);
                        revealInFileBrowserMenuItem.setText(EditorPlatform.IS_MAC
                                ? MessageFormat.format(I18N.getString("csspanel.reveal.finder"), nav)
                                : MessageFormat.format(I18N.getString("csspanel.reveal.explorer"), nav));
                        revealInFileBrowserMenuItem.setOnAction(event -> navigate(item, getPropertyState(item), style, origin));
                        openStylesheetMenuItem.setOnAction(event -> open(item, getPropertyState(item), style, origin));
                    }
                }
                currentValue.setNavigation(navigationLabel, navigationMenuButton);
            }
            currentValue.showSource(true);//showSources.isSelected()
        }

        private void createNavigationMenuButton() {
            navigationMenuButton = new MenuButton();

            final var region = new Region();
            navigationMenuButton.setGraphic(region);
            region.getStyleClass().add("cog-shape"); //NOI18N

            navigationMenuButton.setOpacity(0);
            navigationMenuButton.getStyleClass().addAll("css-panel-cog-menubutton"); //NOI18N
            fadeTransition = new FadeTransition(Duration.millis(500), navigationMenuButton);
        }
    }

    // "API defaults" column
    private class BuiltinValueTableCell extends CssValueTableCell {

        @Override
        protected PropertyState getPropertyState(final CssProperty item) {
            return item.builtinState().get();
        }

        @Override
        protected boolean isWinner(final CssProperty item) {
            return item.isBuiltinSource();
        }

        @Override
        protected StyleOrigin getOrigin(final CssProperty item) {
            return null;
        }

        @Override
        protected CssStyle getStyle(final CssProperty item) {
            return null;
        }
    }

    // "FX Theme defaults" column
    private class FxThemeValueTableCell extends CssValueTableCell {

        @Override
        protected PropertyState getPropertyState(final CssProperty item) {
            return item.fxThemeState().get();
        }

        @Override
        protected boolean isWinner(final CssProperty item) {
            return item.isFxThemeSource();
        }

        @Override
        protected StyleOrigin getOrigin(final CssProperty item) {
            return StyleOrigin.USER_AGENT;
        }

        @Override
        protected CssStyle getStyle(final CssProperty item) {
            final var ps = item.fxThemeState().get();
            return ps == null ? null : ps.getStyle();
        }
    }

    // "Inspector" column
    private class ModelValueTableCell extends CssValueTableCell {

        @Override
        protected PropertyState getPropertyState(final CssProperty item) {
            return item.modelState().get();
        }

        @Override
        protected boolean isWinner(final CssProperty item) {
            return item.isModelSource();
        }

        @Override
        protected StyleOrigin getOrigin(final CssProperty item) {
            return StyleOrigin.USER;
        }

        @Override
        protected CssStyle getStyle(final CssProperty item) {
            return null;
        }
    }

    // "Stylesheets" column
    private class AuthorValueTableCell extends CssValueTableCell {

        @Override
        protected PropertyState getPropertyState(final CssProperty item) {
            return item.authorState().get();
        }

        @Override
        protected boolean isWinner(final CssProperty item) {
            return item.isAuthorSource();
        }

        @Override
        protected StyleOrigin getOrigin(final CssProperty item) {
            return StyleOrigin.AUTHOR;
        }

        @Override
        protected CssStyle getStyle(final CssProperty item) {
            final var ps = item.authorState().get();
            return ps == null ? null : ps.getStyle();
        }
    }

    // "Inline Styles" column
    private class InlineValueTableCell extends CssValueTableCell {

        @Override
        protected PropertyState getPropertyState(final CssProperty item) {
            return item.inlineState().get();
        }

        @Override
        protected boolean isWinner(final CssProperty item) {
            return item.isInlineSource();
        }

        @Override
        protected StyleOrigin getOrigin(final CssProperty item) {
            return StyleOrigin.INLINE;
        }

        @Override
        protected CssStyle getStyle(final CssProperty item) {
            final var ps = item.inlineState().get();
            return ps == null ? null : ps.getStyle();
        }
    }

    /**
     * XXX jfdenise, handle case where the Fx Theme is not the winning style.
     * The complex case is that we need to return a propertyState BUT we don't
     * know if Fx Theme has been overriden, then we do return null.
     */
    // "Defaults" column
    private class DefaultValueTableCell extends CssValueTableCell {

        @Override
        protected PropertyState getPropertyState(final CssProperty item) {
            PropertyState ret = item.fxThemeState().get();
            if (ret == null) {
                // Do we have an override
                var foundNotApplied = false;
                final PropertyState winner = item.getWinner();
                if (winner != null) {
                    for (final var np : winner.getNotAppliedStyles()) {
                        // Not applied handling will had the value.
                        if (np.getOrigin() == StyleOrigin.USER_AGENT) {
                            foundNotApplied = true;
                            break;
                        }
                    }
                }
                if (!foundNotApplied) {
                    ret = item.builtinState().get();
                }
            }
            return ret;
        }

        @Override
        protected CssStyle getStyle(final CssProperty item) {
            CssStyle style = null;
            final var fxTheme = item.fxThemeState().get();
            if (fxTheme == null) {
                // Do we have an override
                final PropertyState winner = item.getWinner();
                if (winner != null) {
                    for (final var np : winner.getNotAppliedStyles()) {
                        // Not applied handling will had the value.
                        if (np.getOrigin() == StyleOrigin.USER_AGENT) {
                            style = np;
                            break;
                        }
                    }
                }

            } else {
                style = fxTheme.getStyle();
            }
            return style;
        }

        @Override
        protected boolean isWinner(final CssProperty item) {
            return item.isFxThemeSource() || item.isBuiltinSource();
        }

        @Override
        protected StyleOrigin getOrigin(final CssProperty item) {
            final var state = getPropertyState(item);
            // If null is returned, means that there is a not applied for fxTheme
            if (state instanceof CssPropertyState || state == null) {
                return StyleOrigin.USER_AGENT;
            } else {
                return null;
            }
        }

        // Special case, display some info when merged.
        @Override
        protected String getNavigation(final CssProperty item, final CssStyle style) {
            final var ps = getPropertyState(item);
            if (ps == null || ps instanceof CssPropertyState) {
                return I18N.getString("csspanel.fxtheme.defaults.navigation")
                        + " (" + CssInternal.getThemeDisplayName(style.getStyle()) + ")";//NOI18N
            } else {
                return I18N.getString("csspanel.api.defaults.navigation");
            }
        }
    }

    /*
     *
     *
     * NAVIGATION (to inspector, file explorer, css editor, ...
     *
     *
     */
    private void open(final CssProperty item, final PropertyState state, final CssStyle style, final StyleOrigin origin) {
        navigate(item, state, style, origin, true);
    }

    private void navigate(final CssProperty item, final PropertyState state, final CssStyle style, final StyleOrigin origin) {
        navigate(item, state, style, origin, false);
    }

    private void navigate(final CssProperty item, final PropertyState state, final CssStyle style, final StyleOrigin origin, final boolean open) {

        if (origin == StyleOrigin.USER) {// Navigate to property
            final var propName = ((BeanPropertyState) state).getPropertyMeta().getName();
            // Navigate to inspector property
            if (applicationDelegate != null) {
                applicationDelegate.revealInspectorEditor(getValuePropertyMeta(propName));
            }
        }
        if (style != null) {
            if (style.getOrigin() == StyleOrigin.AUTHOR) {// Navigate to file
                final var url = style.getUrl();
                final var path = url.toExternalForm();
                if (path.toLowerCase(Locale.ROOT).startsWith("file:/")) { //NOI18N
                    try {
                        if (open) {
                            EditorPlatform.open(path);
                        } else {
                            final var f = new File(url.toURI());
                            EditorPlatform.revealInFileBrowser(f);
                        }
                    } catch (final URISyntaxException | IOException ex) {
                        LOGGER.log(Level.SEVERE, "An unexpected error occured!", ex);
                        final var errorDialog = new ErrorDialog(editorController.getOwnerWindow());
                        errorDialog.setTitle(I18N.getString("alert.error.file.reveal.title"));
                        errorDialog.setMessage(I18N.getString("alert.error.file.reveal.message"));
                        errorDialog.setDetails(I18N.getString("alert.error.file.reveal.details", path));
                        errorDialog.setDetailsTitle(I18N.getString("alert.error.file.reveal.details.title"));
                        errorDialog.setDebugInfoWithThrowable(ex);
                        errorDialog.showAndWait();
                    }
                }
            } else {
                if (style.getOrigin() == StyleOrigin.INLINE) {
                    // Navigate to inspector style property
                    if (applicationDelegate != null) {
                        applicationDelegate.revealInspectorEditor(
                                getValuePropertyMeta(new PropertyName("style"))); //NOI18N
                    }
                }
            }
        }
    }

    private static String getNavigationInfo(
        final CssProperty item, final CssStyle cssStyle, final StyleOrigin origin) {
        if (origin == StyleOrigin.USER_AGENT) {
            return CssInternal.getThemeDisplayName(cssStyle.getStyle());
        }
        if (origin == StyleOrigin.USER) {
            final var state = (BeanPropertyState) item.modelState().get();
            return item.getTarget().getClass().getSimpleName() + "."
                    + state.getPropertyMeta().getName().getName();//NOI18N
        }
        if (origin == StyleOrigin.AUTHOR) {
            if (cssStyle != null) {
                final var url = cssStyle.getUrl();
                String name = null;
                if (url != null) {
                    name = url.toExternalForm();
                    if (name.toLowerCase(Locale.ROOT).startsWith("file:/")) { //NOI18N
                        try {
                            final var f = new File(url.toURI());
                            name = f.getName();
                        } catch (final URISyntaxException ex) {
                            System.out.println(ex.getMessage() + ": " + ex);
                        }
                    }
                }
                return name;
            }
        }
        if (origin == StyleOrigin.INLINE) {
            final var n = item.getSourceNodeForInline();
            if (n != null) {
                return nodeIdentifier(n);
            }
            return null;
        }

        return null;
    }

    /*
     *
     * Private static
     *
     */
    private static FXOMInstance getFXOMInstance(final Selection selection) {
        FXOMInstance fxomInstance = null;
        if (selection == null) {
            return null;
        }
        if (selection.getGroup() instanceof ObjectSelectionGroup) {
            final var osg = (ObjectSelectionGroup) selection.getGroup();
            for (final var item : osg.getItems()) {
                if (item instanceof FXOMInstance) {
                    fxomInstance = (FXOMInstance) item;
                }
            }
        }

        // In case of GridSelectionGroup, nothing to show (?)
        return fxomInstance;
    }

    private ValuePropertyMetadata getValuePropertyMeta(final PropertyName propName) {
        ValuePropertyMetadata valuePropMeta = null;
        if (selectedObject instanceof FXOMInstance) {
            valuePropMeta = Metadata.getMetadata().queryValueProperty(
                    (FXOMInstance) selectedObject, propName);
        }
        return valuePropMeta;
    }

    private static String getPseudoStates(final Node node) {
        final var pseudoClasses = new StringBuilder();
        final Set<PseudoClass> pseudoClassSet = node.getPseudoClassStates();
        for (final var pc : pseudoClassSet) {
            pseudoClasses.append(":").append(pc.getPseudoClassName()); //NOI18N
        }
        return pseudoClasses.toString();
    }

    // Best effort to express a potential selector. There is more than one...
    private static String localSelector(final Node node) {
        var ret = "";//NOI18N
        final var pseudoClasses = getPseudoStates(node);
        if (!node.getStyleClass().isEmpty()) {
            ret = "." + node.getStyleClass().getLast() + pseudoClasses;//NOI18N
        } else if (node.getId() != null && !node.getId().equals("")) {//NOI18N
            ret = "#" + node.getId() + pseudoClasses;//NOI18N
        }
        return ret;
    }

    private static String createItemName(final Node n) {
        return localSelector(n);
    }

    private static String createOptional(final Node n) {
        return "(" + getFirstStandardClassName(n.getClass()) + ")";//NOI18N
    }

    private static Node getContent(final String property, final String cssValue, final Object value, final boolean applied) {
        final var hbox = new HBox();
        final var l = createPropertyLabel(property + ": ", applied);//NOI18N
        hbox.getChildren().add(l);
        // Custom content. Mainly for paints and images
        final var n = getCustomContent(value);
        if (n != null) {
            hbox.getChildren().add(n);
        } else {
            final var cssValueNode = createLabel(cssValue + ";", applied);//NOI18N
            hbox.getChildren().add(cssValueNode);
        }
        return CopyHandler.makeCopyableNode(hbox, property + ": " + cssValue + ";");//NOI18N
    }

    private static Node getCustomContent(Object value) {
        Node ret = null;
        if (value instanceof ParsedValue) {
            final var pv = (ParsedValue<?, ?>) value;
            value = CssValueConverter.convert(pv);
        }
        if (value != null) {
            if (value.getClass().isArray()) {
                final var hbox = new HBox(5);
                final var size = Array.getLength(value);
                for (var i = 0; i < size; i++) {
                    final var n = getCustomContent(Array.get(value, i));
                    if (n != null) {
                        hbox.getChildren().add(n);
                        if (i < size - 1) {
                            hbox.getChildren().add(new Label(", "));//NOI18N
                        }
                    }
                }
                if (!hbox.getChildren().isEmpty()) {
                    ret = hbox;
                }
            } else {
                if (value instanceof Collection) {
                    final var hbox = new HBox(5);
                    final var collection = (Collection<?>) value;
                    final var it = collection.iterator();
                    while (it.hasNext()) {
                        final var obj = it.next();
                        final var n = getCustomContent(obj);
                        if (n != null) {
                            hbox.getChildren().add(n);
                            if (it.hasNext()) {
                                hbox.getChildren().add(new Label(", "));//NOI18N
                            }
                        }
                    }
                    if (!hbox.getChildren().isEmpty()) {
                        ret = hbox;
                    }
                } else {// Leaf value
                    final CssValuePresenter<?> presenter = CssValuePresenterFactory.getInstance().newValuePresenter(value);
                    final var customPresenter = presenter.getCustomPresenter();
                    ret = customPresenter;
                }
            }
        }
        return ret;
    }

    private static Node createLabel(final String text, final String styleclass, final boolean isApplied) {
        Node node = new Label(text);
        if (styleclass != null) {
            node.getStyleClass().add(styleclass);
        }
        if (!isApplied) {
            node = createLine((Label) node);
        }
        return node;
    }

    private static Node createLabel(final String text, final boolean isApplied) {
        return createLabel(text, null, isApplied);
    }

    private static Node createPropertyLabel(final String text, final boolean isApplied) {
        return createLabel(text, "css-panel-property", isApplied);//NOI18N
    }

    private static Node createLine(final Node node) {
        final var sp = new StackPane();
        sp.getChildren().add(node);
        final var s = new Separator(Orientation.HORIZONTAL);
        s.setValignment(VPos.CENTER);
        s.getStyleClass().add("notAppliedStyleLine");//NOI18N
        sp.getChildren().add(s);
        return sp;
    }

    private static TreeItem<Node> attachSource(final PropertyState css, final CssStyle cssStyle, final TreeItem<Node> parent, final boolean applied) {
        final var source = getSource(cssStyle);
        TreeItem<Node> srcItem = null;
        if (source != null) {
            final var hbox = new HBox(5);
            if (cssStyle.getOrigin() != StyleOrigin.INLINE) {
                final var selector = new Label(cssStyle.getSelector());
                // Workaround RT layout bug
                selector.setMinWidth(30);
                hbox.getChildren().add(selector);
                hbox.getChildren().add(new Label("{"));//NOI18N
            }
            hbox.getChildren().add(createLabel(cssStyle.getCssProperty() + ": ", applied));//NOI18N
            final var n = getCustomContent(cssStyle.getParsedValue());
            if (n != null) {
                hbox.getChildren().add(n);
            }
            final var label2 = createLabel(CssValueConverter.toCssString(cssStyle.getCssProperty(), cssStyle.getCssRule(), cssStyle.getParsedValue())
                                           + ";", applied);//NOI18N
            hbox.getChildren().add(label2);
            if (cssStyle.getOrigin() != StyleOrigin.INLINE) {
                hbox.getChildren().add(new Label("}"));//NOI18N
            }
            final var label = new Label(source);
            hbox.getChildren().add(label);
            srcItem = newTreeItem(hbox, css);
            parent.getChildren().add(srcItem);
        }
        return srcItem;
    }

    private static void attachStyle(final PropertyState css, final CssStyle style, final TreeItem<Node> parent, final boolean applied) {
        attachStyle(css, style, parent, applied, null);
    }

    private static void attachStyle(final PropertyState css, final CssStyle style, final TreeItem<Node> parent, final boolean applied, final ArrayList<String> cssPropertyList) {
        final var sourceItem = attachSource(css, style, parent, applied);
        if (cssPropertyList != null) {
            cssPropertyList.add(style.getCssProperty());
        }
        if (sourceItem != null) {
            // Do we have a chain of lookups?
            for (final var lookup : style.getLookupChain()) {

                if ((cssPropertyList != null) && cssPropertyList.contains(lookup.getCssProperty())) {
                    // This css property has already been attached
                    continue;
                }
                attachStyle(css, lookup, sourceItem, applied, cssPropertyList);
            }
        }
    }

    /**
     *
     * @param component component.
     * @param css css property state.
     * @param lookupRoot root css style.
     * @param parent parent.
     * @treatAsPrivate
     */
    public static void attachLookupStyles(final Object component, final CssPropertyState css, final CssStyle lookupRoot, final TreeItem<Node> parent) {
        // Some lookup that comes from the SB itself, skip them.
        // This is expected, these lookups are superceeded by the 
        // CssUtils.createCSSFrontier
        final var cssPropertyList = new ArrayList<String>();
        // cssPropertyList will allow to check that the same css property is not added multiple times
        attachStyle(css, lookupRoot, parent, true, cssPropertyList);
    }

    private static void attachStyles(final CssPropertyState css, final TreeItem<Node> parent) {
        if (css.getStyle() != null) {
            attachStyle(css, css.getStyle(), parent, true);
        }
    }

    private static class HtmlStyler {

        private final static String INIT_STRING = "<html><body>"; //NOI18N
        private final static String END_STRING = "</body></html>"; //NOI18N
        private final StringBuilder builder = new StringBuilder();
        private String html;

        HtmlStyler() {
            builder.append(INIT_STRING);//NOI18N
        }

        public void check() {
            if (html != null) {
                throw new IllegalArgumentException("Locked, html already generated");//NOI18N
            }
        }

        public void cssRuleStart(final String selector, final String source) {
            check();
            builder.append("<p>");//NOI18N
            builder.append("<b>").append(selector).append("</b>");//NOI18N
            builder.append("&nbsp;<b>{</b>&nbsp;").append("/*&nbsp;").append(source).append("&nbsp;*/");//NOI18N
        }

        public void cssRuleEnd() {
            check();
            builder.append("<br>");//NOI18N
            builder.append("<b>}</b>");//NOI18N
            builder.append("</p>");//NOI18N
        }

        public void addProperty(final String name, String content, final boolean applied) {
            check();
            final var sepName = name + ":&nbsp;";//NOI18N
            final var propName = "<b>" + (applied ? sepName : "<strike>" + sepName + "</strike>") + "</b>";//NOI18N
            builder.append("<br>");//NOI18N
            content = applied ? content : "<strike>" + content + "</strike>";//NOI18N
            builder.append("<span style=\"margin-left:10px;\">").append(propName).append(content).append(";").append("</span>");//NOI18N
        }

        public void addMessage(final String mess) {
            check();
            builder.append(mess);
        }

        public String getHtmlString() {
            if (html == null) {
                builder.append(END_STRING);//NOI18N
                html = builder.toString();
            }
            return html;
        }

        public boolean isEmpty() {
            return builder.toString().equals(INIT_STRING);
        }
    }

    /**
     *
     * @treatAsPrivate
     */
    public void copyRules() {
        CopyHandler.copy(rulesTree);
    }

    private static class CopyHandler {

        private static String getContent(final TreeView<Node> tv) {
            final var builder = new StringBuilder();
            for (final var item : tv.getSelectionModel().getSelectedItems()) {
                final var n = item.getValue();
                final var str = (String) n.getProperties().get(CSS_TEXT);
                if (str != null) {
                    builder.append(str);
                }
            }
            return builder.toString();
        }

        private static void copy(final TreeView<Node> tv) {
            final var cssContent = getContent(tv);
            final var content = new ClipboardContent();
            content.putString(cssContent);
            Clipboard.getSystemClipboard().setContent(content);
        }

        private static void attachContextMenu(final TreeView<Node> tv) {
            final var ctxMenu = new ContextMenu();
            final var cssContentAction = new MenuItem(I18N.getString("csspanel.copy"));
            ctxMenu.setOnShowing(arg0 -> {
            });
            tv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            cssContentAction.setOnAction(arg0 -> copy(tv));

            ctxMenu.getItems().add(cssContentAction);
            tv.setContextMenu(ctxMenu);
        }
        private static final String CSS_TEXT = "CSS_TEXT";//NOI18N

        private static <T extends Node> T makeCopyableNode(final T node, final String str) {
            node.getProperties().put(CSS_TEXT, str + "\n");//NOI18N
            return node;
        }

        private static Text createCopyableText(final String str) {
            final var text = new Text(str);
            text.getProperties().put(CSS_TEXT, str + "\n");//NOI18N
            return text;
        }
    }

    private static void setTreeHeight(final TreeView<?> tv) {
        // XXX jfdenise how to properly compute the height of the tree
        final var minHeight = 70;
        final var topLevelItems = tv.getRoot().getChildren().size();
        final var computed = topLevelItems == 0 ? 15 : (25 * topLevelItems);
        tv.setPrefHeight(Math.max(minHeight, computed));
    }

    private static String nonNull(final String string) {
        if (string == null) {
            return ""; //NOI18N
        } else {
            return string;
        }
    }

    private static TreeItem<Node> newTreeItem(final Node content, final PropertyState ss) {
        final var ti = new TreeItem<Node>(content);
        return ti;
    }

    private static String getSource(final CssStyle style) {
        final var url = style.getUrl();
        String source = null;
        if (url != null) {
            source = getSource(url, style.getOrigin());
        }
        return source;
    }

    private static String getSource(final Rule rule) {
        URL url = null;
        StyleOrigin origin = null;
        // Workaround!
        if (rule != null) {
            try {
                url = new URL(rule.getStylesheet().getUrl());
            } catch (final MalformedURLException ex) {
                System.out.println("Invalid URL: " + ex);
            }
            origin = rule.getOrigin();
        }
        return getSource(url, origin);
    }

    private static String getSource(final URL url, final StyleOrigin origin) {
        String source = null;
        if (url != null) {
            if (origin == StyleOrigin.USER_AGENT) {
                source = I18N.getString("csspanel.fxtheme.origin");
            } else {
                if (origin == StyleOrigin.USER) {
                    source = I18N.getString("csspanel.api.origin")
                            + " " //NOI18N
                            + I18N.getString("csspanel.node.property");
                } else {
                    if (origin == StyleOrigin.AUTHOR) {
                        source = url.toExternalForm();
                    }
                }
            }
        }
        return source;
    }

    private static String getSourceInfo(final CssProperty item, final CssStyle style, final StyleOrigin origin) {
        if (origin == StyleOrigin.USER_AGENT) {
            if (style != null) {
                return style.getSelector();
            }
            return null;
        }
        if (origin == StyleOrigin.USER) {
            final var state = (BeanPropertyState) item.modelState().get();
            return state.getPropertyMeta().getName().getName();
        }
        if (origin == StyleOrigin.AUTHOR) {
            if (style != null) {
                return style.getSelector();
            }
            return null;
        }
        if (origin == StyleOrigin.INLINE) {
            final var inherited = item.isInlineInherited();
            return "style" + (inherited ? " (" + I18N.getString("csspanel.inherited") + ")" : "");//NOI18N
        }

        return null;
    }

    private static Node createValueUI(final CssProperty item, final CssStyle style) {
        ParsedValue<?, ?> value = null;
        if (style != null && !style.getLookupChain().isEmpty()) {
            if (style.getLookupChain().size() == 1) {
                value = style.getLookupChain().getFirst().getParsedValue();
            } else {
                value = style.getParsedValue();
            }
        } else {
            if (style != null) {
                value = style.getParsedValue();
            }
        }

        return createValueUI(item, null, value, style);
    }

    private static Node createValueUI(final CssProperty item, final PropertyState ps, final Object value, final CssStyle style) {
        ParsedValue<?, ?>[] parsedValues = null;
        if (style != null) {
            final ParsedValue<?, ?> pv = style.getParsedValue();
            final var v = pv.getValue();
            if (v instanceof ParsedValue<?, ?>[]) {//Means lookups
                parsedValues = (ParsedValue<?, ?>[]) v;
            }
        }
        return createValueUI(item, ps, value, style, parsedValues);
    }

    private static Node createValueUI(final CssProperty item, final PropertyState ps, Object value, final CssStyle style, final ParsedValue<?, ?>[] parsedValues) {
        Node ret = null;
        if (value instanceof ParsedValue) {
            final var pv = (ParsedValue<?, ?>) value;
            value = CssValueConverter.convert(pv);
        }
        if (value != null) {
            if (value.getClass().isArray()) {
                final var hbox = new HBox(5);
                final var size = Array.getLength(value);
                var lookupIndex = 0;
                for (var i = 0; i < size; i++) {
                    final var v = Array.get(value, i);
                    final var n = getLeaf(v);
                    if (n == null) {
                        break;
                    }
                    var lookup = false;
                    if (parsedValues != null) {
                        final var pv = parsedValues[i];
                        lookup = pv.isContainsLookups();
                    }
                    if (lookup) {
                        assert style != null;
                        CssStyle lookupRoot = null;
                        if (style.getLookupChain().size() - 1 < lookupIndex) {
                            // We are in NOT APPLIED case, no lookup in matching Styles.
                            // This is an RT bug logged.
                            // XXX jfdenise, we can reconstruct the lookup chain based on the ParsedValue
                            // We need to access private field ParsedValue.resolved
                            // That is a null if this is the leaf of the Lookup
                            // That is a ParsedValue with a getvalue that is a ParsedValue
                            // to introspect.
//                            ParsedValue<?, ?> pv = parsedValues[i];
//                            if(pv.getValue() instanceof String){
//                                // OK, this is a lookup name.
//                                Object obj = pv.convert(null);
//                            } else {
//                                
//                            }
                        } else {
                            lookupRoot = style.getLookupChain().get(lookupIndex);
                        }

                        lookupIndex += 1;
                        final var lookupUI = createLookupUI(item, ps, style, lookupRoot, n);
                        hbox.getChildren().add(lookupUI);
                    } else {
                        hbox.getChildren().add(n);
                    }
                    if (i < size - 1) {
                        hbox.getChildren().add(new Label(","));//NOI18N
                    }
                }
                if (!hbox.getChildren().isEmpty()) {
                    ret = hbox;
                }
            } else {
                if (value instanceof Collection) {
                    final var hbox = new HBox(5);
                    var lookupIndex = 0;
                    final var collection = (Collection<?>) value;
                    final var it = collection.iterator();
                    var index = 0;
                    while (it.hasNext()) {
                        final var v = it.next();
                        final var n = getLeaf(v);
                        if (n == null) {
                            break;
                        }
                        var lookup = false;
                        if (parsedValues != null) {
                            final var pv = parsedValues[index];
                            lookup = pv.isContainsLookups();
                        }
                        if (lookup) {
                            CssStyle lookupRoot = null;
                            assert style != null;
                            if (style.getLookupChain().size() - 1 < lookupIndex) {
                                // We are in NOT APPLIED case, no lookup in matching Styles.
                                // This is an RT bug logged.
                                // XXX jfdenise, we can reconstruct the lookup chain based on the ParsedValue
                                // We need to access private field ParsedValue.resolved
                                // That is a null if this is the leaf of the Lookup
                                // That is a ParsedValue with a getvalue that is a ParsedValue
                                // to introspect.
//                            ParsedValue<?, ?> pv = parsedValues[i];
//                            if(pv.getValue() instanceof String){
//                                // OK, this is a lookup name.
//                                Object obj = pv.convert(null);
//                            } else {
//                                
//                            }
                            } else {
                                lookupRoot = style.getLookupChain().get(lookupIndex);
                            }
                            final var lookupUI = createLookupUI(item, ps, style, lookupRoot, n);
                            hbox.getChildren().add(lookupUI);
                            lookupIndex += 1;
                        } else {
                            hbox.getChildren().add(n);
                        }
                        if (it.hasNext()) {
                            hbox.getChildren().add(new Label(","));//NOI18N
                        }
                        index++;
                    }
                    if (!hbox.getChildren().isEmpty()) {
                        ret = hbox;
                    }
                } else {// Leaf value
                    var n = getLeaf(value);
                    if (n == null && style != null) {
                        n = getLabel(style);
                    }
                    if (style != null && !style.getLookupChain().isEmpty()) {
                        ret = createLookupUI(item, ps, style, style, n);
                    } else {
                        ret = n;
                    }
                }
            }
        }
        return ret;
    }

    private static Label getLabel(final CssStyle style) {
        final var l = CssValueConverter.toCssString(style.getCssProperty(), style.getCssRule(), style.getParsedValue());
        return new Label(l);
    }
    
    private static synchronized Image getLookupImage() {
        if (lookups == null) {
            lookups = new Image(
                CssPanelController.class.getResource("images/css-lookup-icon.png").toExternalForm()); //NOI18N
        }
        
        return lookups;
    }

    private static Node createLookupUI(
        final CssProperty item, final PropertyState ps, final CssStyle style,
        final CssStyle lookupRoot, final Node n) {

        // TODO: make an fxml file for this
        // MenuButton
        final var hbox = new HBox();
        hbox.setMaxWidth(Region.USE_PREF_SIZE);
        final var imgView = new ImageView();
        imgView.setImage(getLookupImage());
        hbox.getChildren().addAll(n, imgView);
        final var lookupMb = new MenuButton();
        lookupMb.setGraphic(hbox);
        lookupMb.getStyleClass().add("lookup-button");
        final var popupContentMi = new CustomMenuItem();
        popupContentMi.setHideOnClick(false);
        lookupMb.getItems().add(popupContentMi);

        // Popup content
        final var popupContent = new StackPane();
        popupContentMi.setContent(popupContent);
        final var lookupTv = new TreeView<Node>();
        lookupTv.setPrefSize(400, 100);
        Object val = null;
        if (ps instanceof CssPropertyState) {
            val = ((CssPropertyState) ps).getFxValue();
        } else {
            if (style != null) {
                val = style.getParsedValue();
            }
        }
        assert val != null;
        final var root = new TreeItem<Node>();
        lookupTv.setRoot(root);
        lookupTv.setShowRoot(false);
        if (ps != null) {
            assert ps instanceof CssPropertyState;
            attachLookupStyles(item.getTarget(), ((CssPropertyState) ps), lookupRoot, root);
        } else {
            attachLookupStyles(item.getTarget(), null, lookupRoot, root);
        }

        popupContent.getChildren().add(lookupTv);
        return lookupMb;
    }

    private static Node getLeaf(final Object value) {
        final CssValuePresenterFactory.CssValuePresenter<?> presenter = CssValuePresenterFactory.getInstance().newValuePresenter(value);
        final var customPresenter = presenter.getCustomPresenter();
        return customPresenter;
    }

    private static void attachStylePropertyNoLookup(final TreeItem<Node> parent,
                                                    final CssPropertyState ps, final CssStyle style, final boolean applied) {
        final var cssStyle = applied ? ps.getStyle() : style;
        final var value = applied ? ps.getFxValue() : style.getParsedValue();
        final var cssValue = CssValueConverter.toCssString(cssStyle.getCssProperty(), cssStyle.getCssRule(), cssStyle.getParsedValue());
        final var item = new TreeItem<Node>(getContent(ps.getCssProperty(), cssValue, value, applied));
        parent.getChildren().add(item);
    }

}
