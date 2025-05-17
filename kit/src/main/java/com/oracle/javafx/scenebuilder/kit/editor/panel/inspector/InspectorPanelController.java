/*
 * Copyright (c) 2016, 2024, Gluon and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.AbstractDragSource;
import com.oracle.javafx.scenebuilder.kit.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyCacheHintJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifySelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.ModifyFxIdJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.togglegroup.ModifySelectionToggleGroupJob;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.*;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.PropertyEditor.LayoutFormat;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors.*;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.selection.AbstractSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.*;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.*;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.effect.EffectPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.keycombination.KeyCombinationPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.ListValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.StringListPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.paint.ColorPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.paint.PaintPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ValuePropertyMetadataClassComparator;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ValuePropertyMetadataNameComparator;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 *
 */
public class InspectorPanelController extends AbstractFxmlPanelController {

    @FXML
    private TitledPane propertiesTitledPane;
    @FXML
    private ScrollPane propertiesScroll;
    @FXML
    private GridPane propertiesSection;
    @FXML
    private TitledPane layoutTitledPane;
    @FXML
    private ScrollPane layoutScroll;
    @FXML
    private GridPane layoutSection;
    @FXML
    private TitledPane codeTitledPane;
    @FXML
    private ScrollPane codeScroll;
    @FXML
    private GridPane codeSection;
    @FXML
    private TitledPane allTitledPane;
    @FXML
    private ScrollPane allScroll;
    @FXML
    private GridPane allContent;
    @FXML
    private StackPane searchStackPane;
    @FXML
    private GridPane searchContent;
    @FXML
    private Accordion accordion;
    @FXML
    private SplitPane inspectorRoot;

    public enum SectionId {

        PROPERTIES,
        LAYOUT,
        CODE,
        NONE
    }

    public enum ViewMode {

        SECTION, // View properties by section (default)
        PROPERTY_NAME, // Flat view of all properties, ordered by name
        PROPERTY_TYPE  // Flat view of all properties, ordered by type
    }

    public enum ShowMode {

        ALL, // Show all the properties (default)
        EDITED // Show only the properties which have been set in the FXML
    }
    //
    private static final String fxmlFile = "Inspector.fxml"; //NOI18N
    private static final String FXID_SUBSECTION_NAME = "Identity";
    private String searchPattern;
    private SectionId previousExpandedSection;
    private PropertyEditor lastPropertyEditorValueChanged = null;
    private boolean dragOnGoing = false;
    //
    // Editor pools
    private final Stack<Editor> i18nStringEditorPool = new Stack<>();
    private final Stack<Editor> stringEditorPool = new Stack<>();
    private final Stack<Editor> doubleEditorPool = new Stack<>();
    private final Stack<Editor> integerEditorPool = new Stack<>();
    private final Stack<Editor> booleanEditorPool = new Stack<>();
    private final Stack<Editor> enumEditorPool = new Stack<>();
    private final Stack<Editor> durationEditorPool = new Stack<>();
    private final Stack<Editor> effectPopupEditorPool = new Stack<>();
    private final Stack<Editor> fontPopupEditorPool = new Stack<>();
    private final Stack<Editor> genericEditorPool = new Stack<>();
    private final Stack<Editor> insetsEditorPool = new Stack<>();
    private final Stack<Editor> boundedDoubleEditorPool = new Stack<>();
    private final Stack<Editor> rotateEditorPool = new Stack<>();
    private final Stack<Editor> anchorPaneConstraintsEditorPool = new Stack<>();
    private final Stack<Editor> styleEditorPool = new Stack<>();
    private final Stack<Editor> styleClassEditorPool = new Stack<>();
    private final Stack<Editor> stylesheetEditorPool = new Stack<>();
    private final Stack<Editor> observableListStringEditorPool = new Stack<>();
    private final Stack<Editor> fxIdEditorPool = new Stack<>();
    private final Stack<Editor> eventHandlerEditorPool = new Stack<>();
    private final Stack<Editor> functionalInterfaceEditorPool = new Stack<>();
    private final Stack<Editor> cursorEditorPool = new Stack<>();
    private final Stack<Editor> paintPopupEditorPool = new Stack<>();
    private final Stack<Editor> imageEditorPool = new Stack<>();
    private final Stack<Editor> boundsPopupEditorPool = new Stack<>();
    private final Stack<Editor> point3DEditorPool = new Stack<>();
    private final Stack<Editor> dividerPositionsEditorPool = new Stack<>();
    private final Stack<Editor> textAlignmentEditorPool = new Stack<>();
    private final Stack<Editor> keyCombinationPopupEditorPool = new Stack<>();
    private final Stack<Editor> columnResizePolicyEditorPool = new Stack<>();
    private final Stack<Editor> rectangle2DPopupEditorPool = new Stack<>();
    private final Stack<Editor> toggleGroupEditorPool = new Stack<>();
    private final Stack<Editor> buttonTypeEditorPool = new Stack<>();
    private final Stack<Editor> includeFxmlEditorPool = new Stack<>();
    private final Stack<Editor> charsetEditorPool = new Stack<>();
    private final Stack<Editor> colorEditorPool = new Stack<>();

    // ...
    //
    // Subsection title pool
    private final Stack<SubSectionTitle> subSectionTitlePool = new Stack<>();
    //
    // Map of editor pools
    private final HashMap<Class<? extends Editor>, Stack<Editor>> editorPools = new HashMap<>();
    //
    // Editors currently in use
    //   Could be a HashMap<SectionId, PropertyEditor> 
    //   if we want to optimize a bit more the property editors usage,
    //   by re-using them directly in the GridPane, instead of using the pools.
    private final List<Editor> editorsInUse = new ArrayList<>();
    //
    // SubSectionTitles currently in use
    private final List<SubSectionTitle> subSectionTitlesInUse = new ArrayList<>();
    //
    private final SectionId[] sections = {SectionId.PROPERTIES, SectionId.LAYOUT, SectionId.CODE};
    //
    // State variables
    private final ObjectProperty<ViewMode> viewModeProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<ShowMode> showModeProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<SectionId> expandedSectionProperty = new SimpleObjectProperty<>();

    // Inspector state
    private SelectionState selectionState;
    private final EditorController editorController;

    private double searchResultDividerPosition;

    // Charsets for the properties of included elements
    private Map<String, Charset> availableCharsets;

    /*
     * Public
     */
    public InspectorPanelController(final EditorController editorController) {
        super(InspectorPanelController.class.getResource(fxmlFile), I18N.getBundle(), editorController);
        this.editorController = editorController;
        this.availableCharsets = CharsetEditor.getStandardCharsets();
        viewModeProperty.setValue(ViewMode.SECTION);
        viewModeProperty.addListener((obv, previousMode, mode) -> viewModeChanged(previousMode, mode));

        showModeProperty.setValue(ShowMode.ALL);
        showModeProperty.addListener((obv, previousMode, mode) -> showModeChanged());

        expandedSectionProperty.setValue(SectionId.PROPERTIES);
        expandedSectionProperty.addListener((obv, previousSectionId, sectionId) -> expandedSectionChanged());

        // Editor pools init
        editorPools.put(I18nStringEditor.class, i18nStringEditorPool);
        editorPools.put(StringEditor.class, stringEditorPool);
        editorPools.put(DoubleEditor.class, doubleEditorPool);
        editorPools.put(IntegerEditor.class, integerEditorPool);
        editorPools.put(BooleanEditor.class, booleanEditorPool);
        editorPools.put(EnumEditor.class, enumEditorPool);
        editorPools.put(DurationEditor.class, durationEditorPool);
        editorPools.put(EffectPopupEditor.class, effectPopupEditorPool);
        editorPools.put(FontPopupEditor.class, fontPopupEditorPool);
        editorPools.put(GenericEditor.class, genericEditorPool);
        editorPools.put(InsetsEditor.class, insetsEditorPool);
        editorPools.put(BoundedDoubleEditor.class, boundedDoubleEditorPool);
        editorPools.put(RotateEditor.class, rotateEditorPool);
        editorPools.put(AnchorPaneConstraintsEditor.class, anchorPaneConstraintsEditorPool);
        editorPools.put(StyleEditor.class, styleEditorPool);
        editorPools.put(StyleClassEditor.class, styleClassEditorPool);
        editorPools.put(StylesheetEditor.class, stylesheetEditorPool);
        editorPools.put(StringListEditor.class, observableListStringEditorPool);
        editorPools.put(FxIdEditor.class, fxIdEditorPool);
        editorPools.put(EventHandlerEditor.class, eventHandlerEditorPool);
        editorPools.put(FunctionalInterfaceEditor.class, functionalInterfaceEditorPool);
        editorPools.put(CursorEditor.class, cursorEditorPool);
        editorPools.put(PaintPopupEditor.class, paintPopupEditorPool);
        editorPools.put(ImageEditor.class, imageEditorPool);
        editorPools.put(BoundsPopupEditor.class, boundsPopupEditorPool);
        editorPools.put(Point3DEditor.class, point3DEditorPool);
        editorPools.put(DividerPositionsEditor.class, dividerPositionsEditorPool);
        editorPools.put(TextAlignmentEditor.class, textAlignmentEditorPool);
        editorPools.put(KeyCombinationPopupEditor.class, keyCombinationPopupEditorPool);
        editorPools.put(ColumnResizePolicyEditor.class, columnResizePolicyEditorPool);
        editorPools.put(Rectangle2DPopupEditor.class, rectangle2DPopupEditorPool);
        editorPools.put(ToggleGroupEditor.class, toggleGroupEditorPool);
        editorPools.put(ButtonTypeEditor.class, buttonTypeEditorPool);
        editorPools.put(IncludeFxmlEditor.class, includeFxmlEditorPool);
        editorPools.put(CharsetEditor.class, charsetEditorPool);
        editorPools.put(ColorPopupEditor.class, colorEditorPool);

        // ...
    }

    public Accordion getAccordion() {
        return accordion;
    }

    public SectionId getExpandedSectionId() {
        if (!isInspectorLoaded()) {
            return null;
        }
        final var expandedSection = accordion.getExpandedPane();
        final InspectorPanelController.SectionId result;

        if (expandedSection == null) {
            // all sections are collapsed
            result = InspectorPanelController.SectionId.NONE;
        } else if (expandedSection == propertiesTitledPane) {
            result = InspectorPanelController.SectionId.PROPERTIES;
        } else if (expandedSection == layoutTitledPane) {
            result = InspectorPanelController.SectionId.LAYOUT;
        } else if (expandedSection == codeTitledPane) {
            result = InspectorPanelController.SectionId.CODE;
        } else {
            // may happen if the view mode has been changed
            return null;
        }

        return result;
    }

    public ViewMode getViewMode() {
        return viewModeProperty.getValue();
    }
    
    public void setViewMode(final ViewMode mode) {
        assert mode != null;
        viewModeProperty.setValue(mode);
    }
    
    private void viewModeChanged(final ViewMode previousMode, final ViewMode mode) {
        if (!isInspectorLoaded()) {
            return;
        }
        if (previousMode == ViewMode.SECTION) {
            previousExpandedSection = getExpandedSectionId();
        }
        accordion.getPanes().clear();
        switch (mode) {
            case SECTION:
                accordion.getPanes().addAll(propertiesTitledPane, layoutTitledPane, codeTitledPane);
                if (previousExpandedSection != null) {
                    setExpandedSection(previousExpandedSection);
                }
                break;
            case PROPERTY_NAME:
            case PROPERTY_TYPE:
                accordion.getPanes().add(allTitledPane);
                allTitledPane.setExpanded(true);
                rebuild();
                break;
            default:
                throw new IllegalStateException("Unexpected view mode " + mode); //NOI18N
        }
        updateClassNameInSectionTitles();
    }

    public ShowMode getShowMode() {
        return showModeProperty.getValue();
    }

    public void setShowMode(final ShowMode mode) {
        assert mode != null;
        showModeProperty.setValue(mode);
    }
    
    private void showModeChanged() {
        if (!isInspectorLoaded()) {
            return;
        }
        rebuild();
    }

    public SectionId getExpandedSection() {
        return expandedSectionProperty.getValue();
    }

    public void setExpandedSection(final SectionId sectionId) {
        assert sectionId != null;
        expandedSectionProperty.setValue(sectionId);
    }
    
    private void expandedSectionChanged() {
        if (!isInspectorLoaded()) {
            return;
        }
        final var expandedSection = getExpandedSection();
        if (expandedSection == null) {
            return;
        }
        final var tp = switch (expandedSection) {
            case NONE -> null;
            case PROPERTIES -> propertiesTitledPane;
            case LAYOUT -> layoutTitledPane;
            case CODE -> codeTitledPane;
        };
        accordion.setExpandedPane(tp);
    }

    public boolean isEditedMode() {
        return getShowMode() == ShowMode.EDITED;
    }

    public String getSearchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(final String searchPattern) {
        this.searchPattern = searchPattern;
        searchPatternDidChange();
    }

    public void animateAccordion(final boolean animate) {
        accordion.getPanes().forEach(tp -> tp.setAnimated(animate));
    }

    /*
     * AbstractPanelController
     */
    @Override
    protected void fxomDocumentDidChange(final FXOMDocument oldDocument) {
//        System.out.println("FXOM Document changed : " + getEditorController().getFxomDocument());
        if (isInspectorLoaded() && hasFxomDocument()) {
            selectionState.initialize();
            rebuild();
        }
    }

    @Override
    protected void sceneGraphRevisionDidChange() {
//        System.out.println("Scene graph changed.");
        if (!dragOnGoing) {
            updateInspector();
        }
    }

    @Override
    protected void cssRevisionDidChange() {
//        System.out.println("CSS changed.");
        Platform.runLater(() -> {
            if (!dragOnGoing) {
                updateInspector();
            }
        });
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
//        System.out.println("Selection changed.");
        // DTL-6570 should be resolved before this assertion is back.
//        assert !editorController.isTextEditingSessionOnGoing();
        if (!dragOnGoing) {
            updateInspector();
        }
    }

    /*
     * AbstractFxmlPanelController
     */
    @Override
    protected void controllerDidLoadFxml() {

        // Sanity checks
        assert propertiesTitledPane != null;
        assert propertiesScroll != null;
        assert propertiesSection != null;
        assert layoutTitledPane != null;
        assert layoutScroll != null;
        assert layoutSection != null;
        assert codeTitledPane != null;
        assert codeScroll != null;
        assert codeSection != null;
        assert allTitledPane != null;
        assert allScroll != null;
        assert allContent != null;
        assert searchStackPane != null;
        assert searchContent != null;
        assert accordion != null;
        assert inspectorRoot != null;

        propertiesTitledPane.expandedProperty().addListener((ChangeListener<Boolean>) (ov, wasExpanded, expanded) -> handleTitledPane(wasExpanded, expanded, SectionId.PROPERTIES));
        layoutTitledPane.expandedProperty().addListener((ChangeListener<Boolean>) (ov, wasExpanded, expanded) -> handleTitledPane(wasExpanded, expanded, SectionId.LAYOUT));
        codeTitledPane.expandedProperty().addListener((ChangeListener<Boolean>) (ov, wasExpanded, expanded) -> handleTitledPane(wasExpanded, expanded, SectionId.CODE));

        // Clean the potential nodes added for design purpose in fxml
        clearSections();

        // Listen the drag property changes
        getEditorController().getDragController().dragSourceProperty().addListener((ChangeListener<AbstractDragSource>) (ov, oldVal, newVal) -> {
            if (newVal != null) {
//                    System.out.println("Drag started !");
                dragOnGoing = true;
            } else {
//                    System.out.println("Drag finished.");
                dragOnGoing = false;
                updateInspector();
            }
        });
        
        // Listen the Scene stylesheets changes
        getEditorController().sceneStyleSheetProperty().addListener((ChangeListener<ObservableList<File>>) (ov, t, t1) -> updateInspector());
        
        selectionState = new SelectionState(editorController);
        viewModeChanged(null, getViewMode());
        expandedSectionChanged();
        
        accordion.expandedPaneProperty().addListener((ChangeListener<TitledPane>) (ov, t, t1) -> {
            expandedSectionProperty.setValue(getExpandedSectionId());
        });
        
        accordion.setPrefSize(300, 700);
        buildExpandedSection();
        updateClassNameInSectionTitles();
        searchResultDividerPosition = inspectorRoot.getDividerPositions()[0];
        searchPatternDidChange();
    }

    /*
     * Private
     */
    private void updateInspector() {
        if (isInspectorLoaded() && hasFxomDocument()) {
            final var newSelectionState = new SelectionState(editorController);
            if (isInspectorStateChanged(newSelectionState) || isEditedMode()) {
                selectionState = newSelectionState;
                rebuild();
            } else {
                // we may have a property changed here.
                selectionState = newSelectionState;
                updateClassNamesExtraForIncludes();
                reset();
            }
        }
    }

    private void updateClassNamesExtraForIncludes() {
        if(!getSelectedIntrinsics().isEmpty()) {
            updateClassNameInSectionTitles();
        }
    }

    private boolean isInspectorStateChanged(final SelectionState newSelectionState) {
        // Inspector state change if one of the following is true:
        // - selected classes change
        // - common parent change
        // - resolve state change
        return (!newSelectionState.getSelectedClasses().equals(selectionState.getSelectedClasses())
                || (newSelectionState.getCommonParentClass() != selectionState.getCommonParentClass())
                || (!newSelectionState.getUnresolvedInstances().equals(selectionState.getUnresolvedInstances())));
    }

    private void searchPatternDidChange() {
        if (isInspectorLoaded()) {
            // Collapse/Expand the search result panel
            if (hasSearchPattern()) {
                if (!inspectorRoot.getItems().contains(searchStackPane)) {
                    inspectorRoot.getItems().addFirst(searchStackPane);
                    inspectorRoot.setDividerPositions(searchResultDividerPosition);
                }
            } else {
                // Save the divider position for next search
                searchResultDividerPosition = inspectorRoot.getDividerPositions()[0];
                if (inspectorRoot.getItems().contains(searchStackPane)) {
                    inspectorRoot.getItems().remove(searchStackPane);
                }
            }

            buildFlatContent(searchContent);
        }
    }

    private void rebuild() {
//        System.out.println("Inspector rebuild() called !");
        // The inspector structure has changed :
        // - selection changed
        // - parent changed
        // - search pattern changed
        // - SceneGraphObject resolved state changed
        // ==> the current section is to be fully rebuilt
        // TBD: we could optimize this by only refreshing values if 
        //      same element class + same container class + same search pattern.
        clearSections();
        if (getViewMode() == ViewMode.SECTION) {
            buildExpandedSection();
        } else {
            buildFlatContent(allContent);
        }
        updateClassNameInSectionTitles();
        if (hasSearchPattern()) {
            buildFlatContent(searchContent);
        }
    }

    private void reset() {
//        System.out.println("Inspector reset() called !");
        // A property has changed, a reference has changed (e.g. css file). 
        // or a selection of an identical node appears
        // ==> For all the editors currently in use:
        // - reset (state, suggested list, ...)
        // - reset the value
//        System.out.println("Refresh all the editors in use...");

        for (final var editor : editorsInUse) {

            if (editor instanceof PropertyEditor) {
                if (editor == lastPropertyEditorValueChanged) {
                    // do not reset an editor that just changed its value and initiated the reset
                    lastPropertyEditorValueChanged = null;
                    continue;
                }
                resetPropertyEditor((PropertyEditor) editor);
//                System.out.println("reset " + ((PropertyEditor) editor).getPropertyNameText());
            }
            setEditorValueFromSelection(editor);
        }
    }

    private void buildExpandedSection() {
        buildSection(getExpandedSectionId());
    }

    private void buildSection(final SectionId sectionId) {
        if (sectionId == SectionId.NONE) {
            return;
        }
//        System.out.println("\nBuilding section " + sectionId + " - Selection : " + selection.getEntries());
        final var gridPane = getSectionContent(sectionId);
        gridPane.getChildren().clear();
        if (handleSelectionMessage(gridPane)) {
            return;
        }

        // Get Metadata
        final var propMetaAll = getValuePropertyMetadata();

        final SortedMap<InspectorPath, ValuePropertyMetadata> propMetaSection = new TreeMap<>(Metadata.getMetadata().INSPECTOR_PATH_COMPARATOR);
        assert propMetaAll != null;
        var i = 0;
        for (final var valuePropMeta : propMetaAll) {
            final var inspectorPath = valuePropMeta.getInspectorPath();
            // Check section
            if (!isSameSection(inspectorPath.getSectionTag(), sectionId)) {
                continue;
            }
            if (valuePropMeta.isStaticProperty() && !isStaticPropertyRelevant(valuePropMeta.getName())) {
                i++;
                continue;
            }
            if (isEditedMode()) {
                if (!isPropertyEdited(valuePropMeta, propMetaAll)) {
                    continue;
                }
            }
            propMetaSection.put(valuePropMeta.getInspectorPath(), valuePropMeta);
        }


        var currentSubSection = ""; //NOI18N
        var lineIndex = 0;
        if (sectionId == SectionId.CODE) {
            // add fx:id here, since it is not a property.
            // It has its own sub section title
            addSubSectionSeparator(gridPane, lineIndex, FXID_SUBSECTION_NAME);
            lineIndex++;
            currentSubSection = FXID_SUBSECTION_NAME;
            lineIndex = addFxIdEditor(gridPane, lineIndex);
        }

        if (propMetaSection.isEmpty()) {
            displayEmptyMessage(gridPane);
            return;
        }

        if (lineIndex == 0 && propMetaSection.isEmpty()) {
            displayEmptyMessage(gridPane);
            return;
        }

        final var iter = propMetaSection.entrySet().iterator();
        final Set<PropertyName> groupProperties = new HashSet<>();
        while (iter.hasNext()) {
            // Loop on properties
            final var entry = iter.next();
            final var inspectorPath = entry.getKey();
            final var propMeta = entry.getValue();
            final var newSubSection = inspectorPath.getSubSectionTag();
//            System.out.println(inspectorPath.getSectionTag() + " - " + newSubSection + " - " + propMeta.getName());
            if (!currentSubSection.equalsIgnoreCase(newSubSection)) {
                addSubSectionSeparator(gridPane, lineIndex, newSubSection);
                lineIndex++;
                currentSubSection = newSubSection;
            }
            if (isGroupedProperty(propMeta.getName())) {
                // Several properties are grouped in a single editor (e.g. AnchorPane constraints)
                if (groupProperties.contains(propMeta.getName())) {
                    continue;
                }
                final var propertiesEditor
                        = getInitializedPropertiesEditor(propMeta.getName(), propMetaSection.values(), groupProperties);
                if (propertiesEditor == null) {
                    continue;
                }
                lineIndex = addInGridPane(gridPane, propertiesEditor, lineIndex);
            } else {
                lineIndex = addInGridPane(gridPane, getInitializedPropertyEditor(propMeta), lineIndex);
            }
        }
    }

    private void addSubSectionSeparator(final GridPane gridPane, final int lineIndex, final String titleStr) {
        final var title = getSubSectionTitle(titleStr);
        gridPane.add(title, 0, lineIndex);
        GridPane.setColumnSpan(title, GridPane.REMAINING);
        final var rowConstraint = new RowConstraints();
        rowConstraint.setValignment(VPos.CENTER);
        gridPane.getRowConstraints().add(rowConstraint);
    }

    private PropertiesEditor getInitializedPropertiesEditor(final PropertyName groupedPropName,
                                                            final Collection<ValuePropertyMetadata> propMetas, final Set<PropertyName> groupProperties) {
        final var propMetaGroup = getGroupedPropertiesMetadata(groupedPropName, propMetas, groupProperties);
        final var propertiesEditor = getPropertiesEditor(propMetaGroup);
        if (propertiesEditor == null) {
            return null;
        }
        for (final var propertyEditor : propertiesEditor.getPropertyEditors()) {
            setEditorValueFromSelection(propertyEditor);
            handlePropertyEditorChanges(propertyEditor);
        }
        return propertiesEditor;
    }

    private PropertyEditor getInitializedPropertyEditor(final ValuePropertyMetadata propMeta) {
        final var propertyEditor = getPropertyEditor(propMeta);

        setEditorValueFromSelection(propertyEditor);
        handlePropertyEditorChanges(propertyEditor);
        return propertyEditor;
    }

    private int addFxIdEditor(final GridPane gridPane, final int lineIndex) {
        final var propertyEditor = makePropertyEditor(FxIdEditor.class, null);
        setFxIdFromSelection(propertyEditor);
        handlePropertyEditorChanges(propertyEditor);
        return addInGridPane(gridPane, propertyEditor, lineIndex);
    }

    private void handlePropertyEditorChanges(final PropertyEditor propertyEditor) {
        handleValueChange(propertyEditor);
        handleTransientValueChange(propertyEditor);
        handleEditingChange(propertyEditor);
        handleNavigateRequest(propertyEditor);
    }

    private boolean isGroupedProperty(final PropertyName propName) {
        // AnchorPane anchors only for now
        return isAnchorConstraintsProp(propName);
    }

    private boolean isGroupEdited(final Collection<ValuePropertyMetadata> propMetaAll) {
        // AnchorPane anchors only for now
        return isAnchorConstraintsEdited(propMetaAll);
    }

    private boolean isPropertyEdited(final ValuePropertyMetadata valuePropMeta, final Collection<ValuePropertyMetadata> propMetadatas) {
        final var propName = valuePropMeta.getName();
        final var groupedProperty = isGroupedProperty(propName);
        if (!groupedProperty && !isPropertyEdited(valuePropMeta)) {
            return false;
        }
        if (groupedProperty) {
            // We may have some properties edited in a group, some not.
            // In this case, we want to show all the goup properties.
            if (!isGroupEdited(new HashSet<>(propMetadatas))) {
                return false;
            }
        }
        return true;
    }

    private boolean isAnchorConstraintsProp(final PropertyName propName) {
        final var anchorPropNames = new String[]{Editor.topAnchorPropName, Editor.rightAnchorPropName,
            Editor.bottomAnchorPropName, Editor.leftAnchorPropName};
        return Arrays.asList(anchorPropNames).contains(propName.toString());
    }

    private boolean isAnchorConstraintsEdited(final Collection<ValuePropertyMetadata> propMetaAll) {
        for (final var valuePropMeta : propMetaAll) {
            if (isAnchorConstraintsProp(valuePropMeta.getName())) {
                if (isPropertyEdited(valuePropMeta)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ValuePropertyMetadata[] getGroupedPropertiesMetadata(final PropertyName groupedPropName,
                                                                 final Collection<ValuePropertyMetadata> propMetas, final Set<PropertyName> groupProperties) {
        // For now, the SB metadata does NOT include this grouping information.
        // Since we have for now only AnchorPane constraints properties in this case,
        // this is handled at the inspector level.
        // We may include this in the metadata in the future if we have a sigificant number
        // of properties in this case (i.e. if we plan to implement editors for rotateX/Y/Z,
        // min/max/prefWidth, etc...)

        //
        // AnchorPane anchors only for now
        //
        assert isAnchorConstraintsProp(groupedPropName);
        final var anchorsNb = 4;
        final var propMetaGroup = new ArrayList<ValuePropertyMetadata>();
        // Create an empty list, to be able to set the entries at the right index.
        for (var ii = 0; ii < anchorsNb; ii++) {
            propMetaGroup.add(null);
        }

        // Loop on properties to find anchors properties
        for (final var propMeta : propMetas) {
            final var propName = propMeta.getName();
            if (!isAnchorConstraintsProp(propName)) {
                continue;
            }
            groupProperties.add(propName);
            switch (propName.toString()) {
                case Editor.topAnchorPropName:
                    propMetaGroup.set(0, propMeta);
                    break;
                case Editor.rightAnchorPropName:
                    propMetaGroup.set(1, propMeta);
                    break;
                case Editor.bottomAnchorPropName:
                    propMetaGroup.set(2, propMeta);
                    break;
                case Editor.leftAnchorPropName:
                    propMetaGroup.set(3, propMeta);
                    break;
                default:
                    assert false;
            }
        }
        return propMetaGroup.toArray(new ValuePropertyMetadata[propMetaGroup.size()]);
    }

    private boolean isSameSection(final String sectionStr, final SectionId sectionId) {
        return sectionStr.equalsIgnoreCase(sectionId.toString());
    }

    private boolean hasSelectedElement() {
        return hasFxomDocument() && (!selectionState.isSelectionEmpty());
    }

    private boolean hasSelectedElementNothingForInspector() {
        return hasFxomDocument() && getSelectedInstances().isEmpty();
    }

    private boolean hasSelectedIntrinsicNothingForInspector() {
        return hasFxomDocument() && getSelectedIntrinsics().isEmpty();
    }


    private Set<FXOMIntrinsic> getSelectedIntrinsics() {
        return selectionState.selectedIntrinsics;
    }

    private boolean hasMultipleSelection() {
        return getSelectedInstances().size() > 1;
    }

    private boolean hasUnresolvedInstance() {
        return getUnresolvedInstances().size() > 0;
    }

    private void buildFlatContent(final GridPane gridPane) {
//        System.out.println("\nBuilding Flat panel" + " - Selection : " + selection.getEntries());
        gridPane.getChildren().clear();
        gridPane.getRowConstraints().clear();
        if (handleSelectionMessage(gridPane)) {
            return;
        }
        if (isSearch(gridPane) && !hasSearchPattern()) {
            addMessage(gridPane, I18N.getString("inspector.message.searchpattern.empty"));
            return;
        }
        final var isOrderdByType = getViewMode() == ViewMode.PROPERTY_TYPE;

        // Get Metadata
        final var propMetadatas = getValuePropertyMetadata();
        if (propMetadatas.isEmpty()) {
            addMessage(gridPane, I18N.getString("inspector.message.no.properties"));
            return;
        }
        final var propMetadataList = Arrays.asList(propMetadatas.toArray(new ValuePropertyMetadata[propMetadatas.size()]));
        if (isOrderdByType) {
            Collections.sort(propMetadataList, new ValuePropertyMetadataClassComparator());
        } else {
            Collections.sort(propMetadataList, new ValuePropertyMetadataNameComparator());
        }

        final List<ValuePropertyMetadata> orderedPropMetadatas = new ArrayList<>();
        for (final var valuePropMeta : propMetadataList) {
            if (isSearch(gridPane) && !isSearchPatternMatch(valuePropMeta)) {
                continue;
            }
            if (valuePropMeta.isStaticProperty() && !isStaticPropertyRelevant(valuePropMeta.getName())) {
                continue;
            }
            if (isEditedMode()) {
                if (!isPropertyEdited(valuePropMeta, propMetadataList)) {
                    continue;
                }
            }
            orderedPropMetadatas.add(valuePropMeta);
        }

        if (orderedPropMetadatas.isEmpty()) {
            displayEmptyMessage(gridPane);
            return;
        }

        var lineIndex = 0;
        final Set<PropertyName> groupProperties = new HashSet<>();
        for (final var propMeta : orderedPropMetadatas) {
            if (isGroupedProperty(propMeta.getName())) {
                if (groupProperties.contains(propMeta.getName())) {
                    continue;
                }
                // Several properties are grouped in a single editor (e.g. AnchorPane constraints)
                final var propertiesEditor
                        = getInitializedPropertiesEditor(propMeta.getName(), new HashSet<>(orderedPropMetadatas), groupProperties);
                if (propertiesEditor == null) {
                    continue;
                }
                lineIndex = addInGridPane(gridPane, propertiesEditor, lineIndex);
            } else {
                lineIndex = addInGridPane(gridPane, getInitializedPropertyEditor(propMeta), lineIndex);
            }
        }
    }

    private boolean handleSelectionMessage(final GridPane gridPane) {
        if (!hasSelectedElement()) {
            addMessage(gridPane, I18N.getString("inspector.message.no.selected"));
            return true;
        }
        if (hasSelectedElementNothingForInspector() && hasSelectedIntrinsicNothingForInspector()) {
            addMessage(gridPane, I18N.getString("inspector.message.no.thingforinspector"));
            return true;
        }
        if (hasUnresolvedInstance()) {
            addMessage(gridPane, I18N.getString("inspector.message.no.resolved"));
            return true;
        }
        return false;
    }

    private void displayEmptyMessage(final GridPane gridPane) {
        final String messKey;
        if (isSearch(gridPane)) {
            messKey = "label.search.noresults";
        } else if (isEditedMode()) {
            messKey = "inspector.message.no.propertiesedited";
        } else {
            messKey = "inspector.message.no.properties";
        }
        addMessage(gridPane, I18N.getString(messKey));
    }

    private boolean isSearchPatternMatch(final ValuePropertyMetadata propMeta) {
        final var propSimpleName = propMeta.getName().getName();
        // Check model name
        if (propSimpleName.toLowerCase(Locale.ENGLISH).contains(searchPattern.toLowerCase(Locale.ENGLISH))) {
            return true;
        }

        // Check display name
        return EditorUtils.toDisplayName(propSimpleName).toLowerCase(Locale.ENGLISH).contains(searchPattern.toLowerCase(Locale.ENGLISH));
    }

    private boolean isStaticPropertyRelevant(final PropertyName propName) {
        final boolean isRelevant;
        if(isIntrinsic()) {
            isRelevant = checkIfStaticPropertyRelevantForIntrinsic(propName);
        }
        else {
            // Check if the static property class is the common parent of the selection
            if (getCommonParent() == null) return false;
            isRelevant =  getCommonParent() == propName.getResidenceClass();
        }
        return isRelevant;
    }

    private boolean isIntrinsic() {
        var result = false;
        if(selectionState.selection.getHitItem() instanceof FXOMIntrinsic) {
            result = true;
        }
        return result;
    }

    private boolean checkIfStaticPropertyRelevantForIntrinsic(final PropertyName propName) {
        final var fxomIntrinsic = (FXOMIntrinsic) selectionState.selection.getHitItem();
        return fxomIntrinsic.getParentObject() != null && fxomIntrinsic.getParentProperty().getParentInstance().getSceneGraphObject().getClass() == propName.getResidenceClass();
    }

    private boolean hasSearchPattern() {
        return (searchPattern != null) && !searchPattern.isEmpty();
    }

    private boolean isSearch(final GridPane gridPane) {
        return gridPane == searchContent;
    }

    private int addInGridPane(final GridPane gridPane, final Editor editor, int lineIndex) {
        final var row1Constraints = new RowConstraints();
        final LayoutFormat editorLayout;
        final HBox propNameNode;
        final String propNameText;
        if (editor instanceof PropertyEditor) {
            propNameNode = ((PropertyEditor) editor).getPropNameNode();
            propNameText = ((PropertyEditor) editor).getPropertyNameText();
            editorLayout = ((PropertyEditor) editor).getLayoutFormat();
        } else {
            // PropertiesEditor
            propNameNode = ((PropertiesEditor) editor).getNameNode();
            propNameText = ((PropertiesEditor) editor).getPropertyNameText();
            if (getViewMode() == ViewMode.SECTION) {
                editorLayout = LayoutFormat.SIMPLE_LINE_NO_NAME;
            } else {
                editorLayout = LayoutFormat.DOUBLE_LINE;
            }
        }
        propNameNode.setFocusTraversable(false);
        final var menu = editor.getMenu();
        // For SQE tests
        menu.setId(propNameText + " Menu"); //NOI18N
        final var valueEditor = editor.getValueEditor();
        // For SQE tests
        valueEditor.setId(propNameText + " Value"); //NOI18N

        if (editorLayout == LayoutFormat.DOUBLE_LINE) {
            // We have to wrap the property name and the value editor in a VBox
            row1Constraints.setValignment(VPos.TOP);
            gridPane.getRowConstraints().add(row1Constraints);
            final var editorBox = new VBox();
            editorBox.getChildren().addAll(propNameNode, valueEditor);
            propNameNode.setAlignment(Pos.CENTER_LEFT);
            GridPane.setColumnSpan(editorBox, 2);
            gridPane.add(editorBox, 0, lineIndex);
        } else {
            // One row
            gridPane.getRowConstraints().add(lineIndex, row1Constraints);
            if (editorLayout != LayoutFormat.SIMPLE_LINE_NO_NAME) {
                gridPane.add(propNameNode, 0, lineIndex);
                if (editorLayout == LayoutFormat.SIMPLE_LINE_CENTERED) {
                    // Property name, valued editor and cog menu are aligned, centered.
                    propNameNode.setAlignment(Pos.CENTER_LEFT);
                } else if (editorLayout == LayoutFormat.SIMPLE_LINE_TOP) {
                    // Property name, valued editor and cog menu are aligned on top.
                    propNameNode.setAlignment(Pos.TOP_LEFT);
                    row1Constraints.setValignment(VPos.TOP);
                } else if (editorLayout == LayoutFormat.SIMPLE_LINE_BOTTOM) {
                    // Property name, valued editor and cog menu are aligned on the bottom.
                    propNameNode.setAlignment(Pos.BOTTOM_LEFT);
                    row1Constraints.setValignment(VPos.BOTTOM);
                }
                GridPane.setColumnSpan(propNameNode, 1);
                GridPane.setColumnSpan(valueEditor, 1);
                gridPane.add(valueEditor, 1, lineIndex);
            } else {
                // LayoutFormat.SIMPLE_LINE_NO_NAME
                row1Constraints.setValignment(VPos.CENTER);
                GridPane.setColumnSpan(valueEditor, 2);
                gridPane.add(valueEditor, 0, lineIndex);
            }
        }

        // Add cog menu
        gridPane.add(menu, 2, lineIndex);

        lineIndex++;
        return lineIndex;
    }


    // used to get the CssId PropertyEditor to update the value while the SceneBuilder is running
    private StringEditor getCssIdEditor(){
        final ValuePropertyMetadata metadataForCssIDEditor = new StringPropertyMetadata(new PropertyName("id"), true,
                null, new InspectorPath("Properties", "JavaFX CSS", 3));
        final var cssIdEditor = (StringEditor) getPropertyEditor(metadataForCssIDEditor);
        handlePropertyEditorChanges(cssIdEditor);
        return cssIdEditor;
    }

//    private Button createButtonForFxId(){
//        Button button = new Button("Also set CSS-Id with fx:id");
//        button.setOnAction((ActionEvent) -> {
//            String fxId = getSelectedInstance().getFxId();
//            if (fxId == null)
//                return;
//            setSelectedFXOMInstances(getCssIdEditor().getPropertyMeta(), fxId);
//        });
//        return button;
//    }

    private void handleValueChange(final PropertyEditor propertyEditor) {
        // Handle the value change
        propertyEditor.addValueListener((ov, oldValue, newValue) -> {
//                System.out.println("Value change : " + newValue);
            if (!propertyEditor.isUpdateFromModel()) {
                lastPropertyEditorValueChanged = propertyEditor;
                updateValueInModel(propertyEditor, oldValue, newValue);
            }
            if (propertyEditor.isRuledByCss()) {
                editorController.getMessageLog().logWarningMessage(
                        "inspector.css.overridden", propertyEditor.getPropertyNameText());
            }
        });
    }

    private void handleTransientValueChange(final PropertyEditor propertyEditor) {
        // Handle the transient value change (no job here, only the scene graph is updated)
        propertyEditor.addTransientValueListener((ov, oldValue, newValue) -> {
//                System.out.println("Transient value change : " + newValue);
            lastPropertyEditorValueChanged = propertyEditor;
            for (final var fxomInstance : getSelectedInstances()) {
                propertyEditor.getPropertyMeta().setValueInSceneGraphObject(fxomInstance, newValue);
            }
        });
    }

    private void updateValueInModel(final PropertyEditor propertyEditor, final Object oldValue, final Object newValue) {
        if (propertyEditor.isUpdateFromModel()) {
            return;
        }
//        System.out.println("Property " + propertyEditor.getPropertyName() + ": Value changed from \"" + oldValue + "\" to \"" + newValue + "\"");
        if (propertyEditor instanceof FxIdEditor) {
            assert (newValue instanceof String) || (newValue == null);
            setSelectedFXOMInstanceFxId(getSelectedObject(), (String) newValue);
        } else if (propertyEditor instanceof ToggleGroupEditor) {
            assert (newValue instanceof String) || (newValue == null);
            setSelectionToggleGroup((String) newValue);
        } else {
            setSelectedFXOMInstances(propertyEditor.getPropertyMeta(), newValue);
        }
    }

    private void handleEditingChange(final PropertyEditor propertyEditor) {
        // Handle the editing change
        propertyEditor.addEditingListener((ov, oldValue, newValue) -> {
            if (newValue) {
                // Editing session starting
//                    System.out.println("textEditingSessionDidBegin() called.");
                editorController.textEditingSessionDidBegin(p -> {
                    // requestSessionEnd
                    if (propertyEditor.getCommitListener() != null) {
                        propertyEditor.getCommitListener().handle(null);
                    }
                    final var hasError = propertyEditor.isInvalidValue();
                    if (!hasError) {
//                                System.out.println("textEditingSessionDidEnd() called (from callback).");
                        if (editorController.isTextEditingSessionOnGoing()) {
                            editorController.textEditingSessionDidEnd();
                        }
                    }
//                            System.out.println("textEditingSessionDidBegin callback returns : " + !hasError);
                    return !hasError;
                });
            } else {
                // Editing session completed
                if (editorController.isTextEditingSessionOnGoing()) {
//                        System.out.println("textEditingSessionDidEnd() called.");
                    editorController.textEditingSessionDidEnd();
                    if (propertyEditor.getCommitListener() != null) {
                        propertyEditor.getCommitListener().handle(null);
                    }
                }
            }
        });

    }

    private void handleNavigateRequest(final PropertyEditor propertyEditor) {
        // Handle a navigate request from an editor
        propertyEditor.addNavigateListener((ov, oldStr, newStr) -> {
            if (newStr != null) {
                setFocusToEditor(new PropertyName(newStr));
            }
        });
    }

    private void setSelectedFXOMInstances(final ValuePropertyMetadata propMeta, final Object value) {
        final var cacheHintPN = new PropertyName("cacheHint"); //NOI18N
        final ModifySelectionJob job;
        if (cacheHintPN.equals(propMeta.getName())) {
            job = new ModifyCacheHintJob(propMeta, value, getEditorController());
        } else {
            job = new ModifySelectionJob(propMeta, value, getEditorController());
        }
//        System.out.println(job.getDescription());
        pushJob(job);
    }

    private void setSelectedFXOMInstanceFxId(final FXOMObject fxomObject, final String fxId) {
        final var job = new ModifyFxIdJob(fxomObject, fxId, getEditorController());
        pushJob(job);
    }

    private void setSelectionToggleGroup(final String tgId) {
        final var job = new ModifySelectionToggleGroupJob(tgId, getEditorController());
        pushJob(job);
    }

    private void pushJob(final Job job) {
        if (job.isExecutable()) {
            getEditorController().getJobManager().push(job);
        } else {
            System.out.println("Modify job not executable (because no value change?)");
        }
    }

    // Check if a property is edited
    private boolean isPropertyEdited(final ValuePropertyMetadata propMeta) {
        for (final var instance : getSelectedInstances()) {
            if (!propMeta.isReadWrite()) {
                continue;
            }
            final var value = propMeta.getValueObject(instance);
            final var defaultValue = propMeta.getDefaultValueObject();
            if (!EditorUtils.areEqual(value, defaultValue)) {
                return true;
            }
        }
        return false;
    }

    // Set the editor value from selection
    private void setEditorValueFromSelection(final Editor editor) {
        if (editor instanceof FxIdEditor) {
            setFxIdFromSelection(editor);
        } else if (isPropertyEditor(editor)) {
            setEditorValueFromSelection((PropertyEditor) editor);
        } else if (isPropertiesEditor(editor)) {
            for (final var propertyEditor : ((PropertiesEditor) editor).getPropertyEditors()) {
                setEditorValueFromSelection(propertyEditor);
            }
        }
    }

    // Set the fx:id from selection
    private void setFxIdFromSelection(final Editor editor) {
        assert editor instanceof FxIdEditor;
        final var fxIdEditor = (FxIdEditor) editor;
        if (hasMultipleSelection()) {
            // multi-selection ==> indeterminate
            fxIdEditor.setIndeterminate(true);
            fxIdEditor.setDisable(true);
        } else {
            final var instanceFxId = getSelectedObject().getFxId();
            fxIdEditor.setDisable(false);
            fxIdEditor.setUpdateFromModel(true);
            fxIdEditor.reset(getSuggestedFxIds(getControllerClass()), getEditorController());
            fxIdEditor.setValue(instanceFxId);
            fxIdEditor.setUpdateFromModel(false);
        }
    }

    // Set the editor value from selection
    private void setEditorValueFromSelection(final PropertyEditor propertyEditor) {

        // Determine the property value
        Object val = null;
        var isIndeterminate = false;
        var isReadWrite = true;
        var isRuledByCss = false;
        CssInternal.CssPropAuthorInfo cssInfo = null;
        final var propName = propertyEditor.getPropertyName();

        // General case
        var first = true;
        for (final var instance : getSelectedInstances()) {
            final var propMeta = Metadata.getMetadata().queryValueProperty(instance, propName);
            assert propMeta != null;
            final var newVal = propMeta.getValueObject(instance);
//            System.out.println(propName + " value : " + newVal);
            if (!propMeta.isReadWrite()) {
                isReadWrite = false;
            }
            if (first) {
                val = newVal;
                first = false;
            } else if (!EditorUtils.areEqual(newVal, val)) {
                isIndeterminate = true;
            }

            cssInfo = CssInternal.getCssInfo(instance.getSceneGraphObject(), propMeta);
            if (cssInfo != null) {
                isRuledByCss = true;
            }
        }
        
        propertyEditor.setUpdateFromModel(true);
        if (isRuledByCss && cssInfo != null) {
            propertyEditor.setRuledByCss(true);
            propertyEditor.setCssInfo(cssInfo);
            if(propertyEditor.isDisablePropertyBound()){
                propertyEditor.unbindDisableProperty();
            }
            propertyEditor.setValue(cssInfo.getFxValue()); //adds CSS values to the ValueEditor
            propertyEditor.getValueEditor().setDisable(true); // disables the ValueEditor when CSS is present
        } else {
            propertyEditor.setRuledByCss(false);
            propertyEditor.setCssInfo(null);
            if (propertyEditor.getValueEditor() != null && propertyEditor.getValueEditor().isDisabled()) {
                // if ValueEditor is present and disabled it will enable it
                // it happens when another component is clicked and a ValueEditor was disabled
                if(!propertyEditor.isDisablePropertyBound()){
                    propertyEditor.getValueEditor().setDisable(false);
                }
            }
            if (isIndeterminate) {
                propertyEditor.setIndeterminate(true);
            } else {
                propertyEditor.setValue(val); //sets the default values or values from FXML tags
            }
        }
        propertyEditor.setUpdateFromModel(false);
        
        if (!(propertyEditor instanceof GenericEditor)) {
            if (!isReadWrite) {
                propertyEditor.setDisable(true);
            } else {
                propertyEditor.setDisable(false);
            }
        }
    }

    private PropertyEditor getPropertyEditor(final ValuePropertyMetadata propMeta) {
        final PropertyEditor propertyEditor;

        if (propMeta instanceof StringPropertyMetadata) {
            propertyEditor = switch (propMeta.getName().getName()) {
                case "style" -> //NOI18N
                    makePropertyEditor(StyleEditor.class, propMeta);
                case "id" -> //NOI18N
                    makePropertyEditor(StringEditor.class, propMeta);
                case "charset" -> makePropertyEditor(CharsetEditor.class, propMeta);
                default -> makePropertyEditor(I18nStringEditor.class, propMeta);
            };
        } else if (propMeta instanceof ListValuePropertyMetadata) {
            propertyEditor = switch (propMeta.getName().getName()) {
                case "styleClass" -> //NOI18N
                    makePropertyEditor(StyleClassEditor.class, propMeta);
                case "stylesheets" -> //NOI18N
                    makePropertyEditor(StylesheetEditor.class, propMeta);
                case "buttonTypes" -> //NOI18N
                    makePropertyEditor(ButtonTypeEditor.class, propMeta);
                case "dividerPositions" -> //NOI18N
                    makePropertyEditor(DividerPositionsEditor.class, propMeta);
                case "source" -> //NOI18N
                    makePropertyEditor(IncludeFxmlEditor.class, propMeta);
                default ->
                    makePropertyEditor(propMeta instanceof StringListPropertyMetadata ? StringListEditor.class : GenericEditor.class, propMeta);
            };
        } else if (propMeta instanceof final DoublePropertyMetadata doublePropMeta) {
            // Double editors
            final var kind = doublePropMeta.getKind();
            if ((kind == DoubleKind.OPACITY) || (kind == DoubleKind.PROGRESS) || isBoundedByProperties(propMeta)) {
                propertyEditor = makePropertyEditor(BoundedDoubleEditor.class, propMeta);
            } else if ((kind == DoubleKind.COORDINATE)
                    || (kind == DoubleKind.USE_COMPUTED_SIZE) || (kind == DoubleKind.USE_PREF_SIZE)
                    || (kind == DoubleKind.NULLABLE_COORDINATE)) {
                // We may have constants to add
                propertyEditor = makePropertyEditor(DoubleEditor.class, propMeta);
            } else if (kind == DoubleKind.ANGLE) {
                propertyEditor = makePropertyEditor(RotateEditor.class, propMeta);
            } else {
                // other kind to be added when editors available...
                // Use simple double editor for now
                propertyEditor = makePropertyEditor(DoubleEditor.class, propMeta);
            }
        } else if (propMeta instanceof IntegerPropertyMetadata) {
            // Integer editor
            propertyEditor = makePropertyEditor(IntegerEditor.class, propMeta);
        } else if (propMeta instanceof BooleanPropertyMetadata) {
            // Boolean editor
            propertyEditor = makePropertyEditor(BooleanEditor.class, propMeta);
        } else if (propMeta instanceof EnumerationPropertyMetadata) {
            propertyEditor = switch (propMeta.getName().getName()) {
                case "textAlignment" -> //NOI18N
                    makePropertyEditor(TextAlignmentEditor.class, propMeta);
                default -> makePropertyEditor(EnumEditor.class, propMeta);
            };
        } else if (propMeta instanceof InsetsPropertyMetadata) {
            // Insets editor
            propertyEditor = makePropertyEditor(InsetsEditor.class, propMeta);
        } else if (propMeta instanceof CursorPropertyMetadata) {
            // Cursor editor
            propertyEditor = makePropertyEditor(CursorEditor.class, propMeta);
        } else if (propMeta instanceof EventHandlerPropertyMetadata) {
            // EventHandler editor
            propertyEditor = makePropertyEditor(EventHandlerEditor.class, propMeta);
        } else if (propMeta instanceof FunctionalInterfacePropertyMetadata) {
          // Functional Interface editor
            propertyEditor = makePropertyEditor(FunctionalInterfaceEditor.class, propMeta);
        } else if (propMeta instanceof EffectPropertyMetadata) {
            // Effect editor
            propertyEditor = makePropertyEditor(EffectPopupEditor.class, propMeta);
        } else if (propMeta instanceof FontPropertyMetadata) {
            // Font editor
            propertyEditor = makePropertyEditor(FontPopupEditor.class, propMeta);
        } else if (propMeta instanceof PaintPropertyMetadata) {
            // Paint editor
            propertyEditor = makePropertyEditor(PaintPopupEditor.class, propMeta);
        } else if (propMeta instanceof ImagePropertyMetadata) {
            // Image editor
            propertyEditor = makePropertyEditor(ImageEditor.class, propMeta);
        } else if (propMeta instanceof BoundsPropertyMetadata) {
            // Bounds editor
            propertyEditor = makePropertyEditor(BoundsPopupEditor.class, propMeta);
        } else if (propMeta instanceof Point3DPropertyMetadata) {
            // Point3D editor
            propertyEditor = makePropertyEditor(Point3DEditor.class, propMeta);
        } else if (propMeta instanceof KeyCombinationPropertyMetadata) {
            // KeyCombination editor
            propertyEditor = makePropertyEditor(KeyCombinationPopupEditor.class, propMeta);
        } else if ((propMeta instanceof TableViewResizePolicyPropertyMetadata)
                || (propMeta instanceof TreeTableViewResizePolicyPropertyMetadata)) {
            // ColumnResizePolicy editor
            propertyEditor = makePropertyEditor(ColumnResizePolicyEditor.class, propMeta);
        } else if (propMeta instanceof Rectangle2DPropertyMetadata) {
            // Rectangle2D editor
            propertyEditor = makePropertyEditor(Rectangle2DPopupEditor.class, propMeta);
        } else if (propMeta instanceof ToggleGroupPropertyMetadata) {
            // ToggleGroup editor
            propertyEditor = makePropertyEditor(ToggleGroupEditor.class, propMeta);
        } else if (propMeta instanceof DurationPropertyMetadata) {
            propertyEditor = makePropertyEditor(DurationEditor.class, propMeta);
        } else if (propMeta instanceof ColorPropertyMetadata) {
            propertyEditor = makePropertyEditor(ColorPopupEditor.class, propMeta);
        } else {
            // Generic editor
            propertyEditor = makePropertyEditor(GenericEditor.class, propMeta);
        }

        // Set all the "Code" properties a double line layout
        if (isSameSection(propMeta.getInspectorPath().getSectionTag(), SectionId.CODE)) {
            propertyEditor.setLayoutFormat(LayoutFormat.DOUBLE_LINE);
        }
        return propertyEditor;
    }

    private PropertiesEditor getPropertiesEditor(final ValuePropertyMetadata[] propMetas) {
        // AnchorPane only for now
        for (final var propMeta : propMetas) {
            if (propMeta == null) {
                // may happen if search
                return null;
            }
            assert isAnchorConstraintsProp(propMeta.getName());
        }
        return makePropertiesEditor(AnchorPaneConstraintsEditor.class, propMetas);
    }

    private Map<String, Object> getConstants(final DoublePropertyMetadata doublePropMeta) {
        final Map<String, Object> constants = new TreeMap<>();
        final var propNameStr = doublePropMeta.getName().getName();
        final var kind = doublePropMeta.getKind();
        if (propNameStr.contains("maxWidth") || propNameStr.contains("maxHeight")) { //NOI18N
            constants.put("MAX_VALUE", Double.MAX_VALUE); //NOI18N
        }
        if (kind == DoubleKind.USE_COMPUTED_SIZE) {
            constants.put(DoubleKind.USE_COMPUTED_SIZE.toString(), Region.USE_COMPUTED_SIZE);
        } else if (kind == DoubleKind.USE_PREF_SIZE) {
            constants.put(DoubleKind.USE_COMPUTED_SIZE.toString(), Region.USE_COMPUTED_SIZE);
            constants.put(DoubleKind.USE_PREF_SIZE.toString(), Region.USE_PREF_SIZE);
        } else if (kind == DoubleKind.NULLABLE_COORDINATE) {
            constants.put("NULL", null); //NOI18N
        } else if (kind == DoubleKind.PROGRESS) {
            constants.put("INDETERMINATE", ProgressIndicator.INDETERMINATE_PROGRESS);
        }
        return constants;
    }

    private Map<String, Object> getConstants(final IntegerPropertyMetadata integerPropMeta) {
        final Map<String, Object> constants = new TreeMap<>();
        final var propNameStr = integerPropMeta.getName().getName();
        if (propNameStr.contains("columnSpan") || propNameStr.contains("rowSpan")) { //NOI18N
            constants.put("REMAINING", GridPane.REMAINING); //NOI18N
        } else if (propNameStr.contains("prefColumnCount")) {
            if (getSelectedClasses().size() == 1) {
                if (getSelectedClass() == TextField.class || getSelectedClass() == PasswordField.class) {
                    constants.put("DEFAULT_PREF_COLUMN_COUNT", TextField.DEFAULT_PREF_COLUMN_COUNT); //NOI18N
                } else if (getSelectedClass() == TextArea.class) {
                    constants.put("DEFAULT_PREF_COLUMN_COUNT", TextArea.DEFAULT_PREF_COLUMN_COUNT); //NOI18N
                }
            }
        } else if (propNameStr.contains("prefRowCount")) {
            assert getSelectedClass() == TextArea.class;
            constants.put("DEFAULT_PREF_ROW_COUNT", TextArea.DEFAULT_PREF_ROW_COUNT); //NOI18N
        }
        return constants;
    }

    private int getMax(final IntegerPropertyMetadata integerPropMeta) {
        final var propNameStr = integerPropMeta.getName().getName();
        if (propNameStr.contains("columnIndex") || propNameStr.contains("columnSpan")) { //NOI18N
            final var gridPane = getGridPane(propNameStr);
            if (gridPane == null) {
                // multi-selection from different GridPanes: not supported for now
                return getMin(integerPropMeta);
            }
            final var nbColumns = Deprecation.getGridPaneColumnCount(gridPane);
            if (propNameStr.contains("columnIndex")) {//NOI18N
                // index start to 0
                return nbColumns - 1;
            }
            if (propNameStr.contains("columnSpan")) {//NOI18N
                final var maxIndex = getSpanPropertyMaxIndex(propNameStr);
                return nbColumns - maxIndex;
            }
        }
        if (propNameStr.contains("rowIndex") || propNameStr.contains("rowSpan")) { //NOI18N
            final var gridPane = getGridPane(propNameStr);
            if (gridPane == null) {
                // multi-selection from different GridPanes: not supported for now
                return getMin(integerPropMeta);
            }
            final var nbRow = Deprecation.getGridPaneRowCount(gridPane);
            if (propNameStr.contains("rowIndex")) {//NOI18N
                // index start to 0
                return nbRow - 1;
            }
            if (propNameStr.contains("rowSpan")) {//NOI18N
                final var maxIndex = getSpanPropertyMaxIndex(propNameStr);
                return nbRow - maxIndex;
            }
        }
        return Integer.MAX_VALUE;
    }

    private int getMin(final IntegerPropertyMetadata integerPropMeta) {
        final var propNameStr = integerPropMeta.getName().getName();
        if (propNameStr.contains("columnSpan") || propNameStr.contains("rowSpan")) { //NOI18N
            return 1;
        }
        return 0;
    }
    
    private boolean isMultiLinesSupported(final Set<Class<?>> selectedClasses, final ValuePropertyMetadata propMeta) {
        final var propertyNameStr = propMeta.getName().getName();
        if (selectedClasses.contains(TextField.class) || selectedClasses.contains(PasswordField.class)) {
            if (propertyNameStr.equalsIgnoreCase("text")) {
                return false;
            }
        }
        if (propertyNameStr.equalsIgnoreCase("promptText")) {
            return false;
        }

        if (propertyNameStr.equalsIgnoreCase("ellipsisString")) {
            return false;
        }
        return true;
    }

    private int getSpanPropertyMaxIndex(final String propNameStr) {
        assert propNameStr.contains("columnSpan") || propNameStr.contains("rowSpan");
        var maxIndex = 0;
        for (final var instance : getSelectedInstances()) {
            assert instance.getSceneGraphObject() instanceof Node;
            Integer index;
            final var node = (Node) instance.getSceneGraphObject();
            if (propNameStr.contains("columnSpan")) {//NOI18N
                index = GridPane.getColumnIndex(node);
            } else {
                index = GridPane.getRowIndex(node);
            }
            if (index == null) {
                index = 0;
            }
            if (index > maxIndex) {
                maxIndex = index;
            }
        }
        return maxIndex;
    }
    
    private GridPane getGridPane(final String propNameStr) {
        assert propNameStr.contains("columnIndex") || propNameStr.contains("columnSpan") //NOI18N
                || propNameStr.contains("rowIndex") || propNameStr.contains("rowSpan");//NOI18N
        final var commonParent = selectionState.getCommonParentObject();
            if (commonParent == null) {
                return null;
            }
        final var parentObj = commonParent.getSceneGraphObject();
            assert parentObj instanceof GridPane;
            return (GridPane) parentObj;
    }

    private boolean isInspectorLoaded() {
        return accordion != null;
    }

    private boolean hasFxomDocument() {
        return getEditorController().getFxomDocument() != null;
    }

    private void addMessage(final GridPane gridPane, final String mess) {
        final var label = new Label(mess);
        label.getStyleClass().add("inspector-message");
        GridPane.setHalignment(label, HPos.LEFT);
        gridPane.add(label, 0, 0, 3, 1);
    }

    private Set<ValuePropertyMetadata> getValuePropertyMetadata() {
        return Metadata.getMetadata().queryValueProperties(getSelectedClasses());
    }

    private void clearSections() {
        // Put all the editors used in the editor pools
        for (final var editor : editorsInUse) {

            final var editorPool = editorPools.get(editor.getClass());
            assert editorPool != null;
            editorPool.push(editor);
            // remove all editor listeners
            editor.removeAllListeners();
        }
        editorsInUse.clear();

        // Put all the subSectionTitles used in its pool
        for (final var subSectionTitle : subSectionTitlesInUse) {
            subSectionTitlePool.push(subSectionTitle);
        }
        subSectionTitlesInUse.clear();

        // Clear section content
        for (final var section : sections) {
            final var content = getSectionContent(section);
            if (content != null) {
                getSectionContent(section).getChildren().clear();
                getSectionContent(section).getRowConstraints().clear();
            }
        }
        allContent.getChildren().clear();
        allContent.getRowConstraints().clear();
        searchContent.getChildren().clear();
        searchContent.getRowConstraints().clear();

        // Set the scrollbars in upper position
//        propertiesScroll.setVvalue(0);
//        layoutScroll.setVvalue(0);
//        codeScroll.setVvalue(0);
//        allScroll.setVvalue(0);
//        searchScrollPane.setVvalue(0);
    }

    private GridPane getSectionContent(final SectionId sectionId) {
        assert sectionId != SectionId.NONE;
        final GridPane gp = switch (sectionId) {
            case PROPERTIES -> propertiesSection;
            case LAYOUT -> layoutSection;
            case CODE -> codeSection;
            default -> throw new IllegalStateException("Unexpected section id " + sectionId); //NOI18N
        };
        return gp;
    }

    private void handleTitledPane(final boolean wasExpanded, final boolean expanded, final SectionId sectionId) {
        if (!wasExpanded && expanded) {
            // TitledPane is expanded
            if (getSectionContent(sectionId).getChildren().isEmpty()) {
                buildSection(sectionId);
            }
        }
    }

    private Node getSubSectionTitle(final String title) {
        final SubSectionTitle subSectionTitle;
        if (subSectionTitlePool.isEmpty()) {
//            System.out.println("Creating NEW subsection title...");
            subSectionTitle = new SubSectionTitle(title);
        } else {
//            System.out.println("Getting subsection title from CACHE...");
            subSectionTitle = subSectionTitlePool.pop();
            subSectionTitle.setTitle(title);
        }
        subSectionTitlesInUse.add(subSectionTitle);
        return subSectionTitle.getNode();
    }

    private PropertyEditor makePropertyEditor(final Class<? extends Editor> editorClass, final ValuePropertyMetadata propMeta) {
        final Editor editor;
        PropertyEditor propertyEditor = null;
        final var editorPool = editorPools.get(editorClass);
        if ((editorPool != null) && !editorPool.isEmpty()) {
            editor = editorPool.pop();
            assert isPropertyEditor(editor);
            propertyEditor = (PropertyEditor) editor;
        }

        propertyEditor = makeOrResetPropertyEditor(editorClass, propMeta, propertyEditor);

        editorsInUse.add(propertyEditor);
        return propertyEditor;
    }

    private void resetPropertyEditor(final PropertyEditor propertyEditor) {
        assert propertyEditor != null;
        makeOrResetPropertyEditor(propertyEditor.getClass(), propertyEditor.getPropertyMeta(), propertyEditor);
    }

    private PropertyEditor makeOrResetPropertyEditor(
        final Class<? extends Editor> editorClass, final ValuePropertyMetadata propMeta, final PropertyEditor propertyEditor) {
        var createdPropertyEditor = propertyEditor;
        if (createdPropertyEditor != null) {
            createdPropertyEditor.setUpdateFromModel(true);
        }
        final var selectedClasses = getSelectedClasses();
        if (editorClass == I18nStringEditor.class) {
            if (createdPropertyEditor != null) {
                ((I18nStringEditor) createdPropertyEditor).reset(propMeta, selectedClasses, isMultiLinesSupported(selectedClasses, propMeta));
            } else {
                createdPropertyEditor = new I18nStringEditor(propMeta, selectedClasses, isMultiLinesSupported(selectedClasses, propMeta));
            }
        } else if (editorClass == StringEditor.class) {
            if (createdPropertyEditor != null) {
                ((StringEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new StringEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == DoubleEditor.class) {
            assert propMeta instanceof DoublePropertyMetadata;
            final var doublePropMeta = (DoublePropertyMetadata) propMeta;
            if (createdPropertyEditor != null) {
                ((DoubleEditor) createdPropertyEditor).reset(propMeta, selectedClasses, getConstants(doublePropMeta));
            } else {
                createdPropertyEditor = new DoubleEditor(propMeta, selectedClasses, getConstants(doublePropMeta));
            }
        } else if (editorClass == IntegerEditor.class) {
            assert propMeta instanceof IntegerPropertyMetadata;
            final var integerPropMeta = (IntegerPropertyMetadata) propMeta;
            if (createdPropertyEditor != null) {
                ((IntegerEditor) createdPropertyEditor).reset(propMeta, selectedClasses,
                        getConstants(integerPropMeta), getMin(integerPropMeta), getMax(integerPropMeta));
            } else {
                createdPropertyEditor = new IntegerEditor(propMeta, selectedClasses,
                        getConstants(integerPropMeta), getMin(integerPropMeta), getMax(integerPropMeta));
            }
        } else if (editorClass == BooleanEditor.class) {
            if (createdPropertyEditor != null) {
                ((BooleanEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new BooleanEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == EnumEditor.class) {
            if (createdPropertyEditor != null) {
                ((EnumEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new EnumEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == InsetsEditor.class) {
            if (createdPropertyEditor != null) {
                ((InsetsEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new InsetsEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == BoundedDoubleEditor.class) {
            assert propMeta instanceof DoublePropertyMetadata;
            final var doublePropMeta = (DoublePropertyMetadata) propMeta;
            if (createdPropertyEditor != null) {
                ((BoundedDoubleEditor) createdPropertyEditor).reset(propMeta, selectedClasses, getSelectedInstances(), getConstants(doublePropMeta));
            } else {
                createdPropertyEditor = new BoundedDoubleEditor(propMeta, selectedClasses, getSelectedInstances(), getConstants(doublePropMeta));
            }
        } else if (editorClass == RotateEditor.class) {
            if (createdPropertyEditor != null) {
                ((RotateEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new RotateEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == StyleEditor.class) {
            if (createdPropertyEditor != null) {
                ((StyleEditor) createdPropertyEditor).reset(propMeta, selectedClasses, getEditorController());
            } else {
                createdPropertyEditor = new StyleEditor(propMeta, selectedClasses, getEditorController());
            }
        } else if (editorClass == StyleClassEditor.class) {
            if (createdPropertyEditor != null) {
                ((StyleClassEditor) createdPropertyEditor).reset(propMeta, selectedClasses, getSelectedInstances(), getEditorController());
            } else {
                createdPropertyEditor = new StyleClassEditor(propMeta, selectedClasses, getSelectedInstances(), getEditorController());
            }
        } else if (editorClass == StylesheetEditor.class) {
            if (createdPropertyEditor != null) {
                ((StylesheetEditor) createdPropertyEditor).reset(propMeta, selectedClasses, getEditorController().getFxmlLocation());
            } else {
                createdPropertyEditor = new StylesheetEditor(propMeta, selectedClasses, getEditorController().getFxmlLocation());
            }
        } else if (editorClass == StringListEditor.class) {
            if (createdPropertyEditor != null) {
                ((StringListEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new StringListEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == FxIdEditor.class) {
            final var controllerClass = getControllerClass();
            if (createdPropertyEditor != null) {
                ((FxIdEditor) createdPropertyEditor).reset(getSuggestedFxIds(controllerClass), getEditorController());
            } else {
                createdPropertyEditor = new FxIdEditor(getSuggestedFxIds(controllerClass), getEditorController());
            }
        } else if (editorClass == CursorEditor.class) {
            if (createdPropertyEditor != null) {
                ((CursorEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new CursorEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == EventHandlerEditor.class) {
            if (createdPropertyEditor != null) {
                ((EventHandlerEditor) createdPropertyEditor).reset(propMeta, selectedClasses, getSuggestedEventHandlers(getControllerClass()));
            } else {
                createdPropertyEditor = new EventHandlerEditor(propMeta, selectedClasses, getSuggestedEventHandlers(getControllerClass()));
            }
        } else if (editorClass == FunctionalInterfaceEditor.class) {
            if (createdPropertyEditor != null) {
                // "getSuggestedEventHandlers" (a method that already existed in SB code) isn't working right. It simply
                // returns all the methods in the Controller class regardless of if they are good candidates for
                // EventHandlers. We use if because at least this way we'll present all the methods available as
                // auto-suggestions.
                ((FunctionalInterfaceEditor) createdPropertyEditor).reset(propMeta, selectedClasses, getSuggestedEventHandlers(getControllerClass()));
            } else {
                createdPropertyEditor = new FunctionalInterfaceEditor(propMeta, selectedClasses, getSuggestedEventHandlers(getControllerClass()));
            }
        } else if (editorClass == EffectPopupEditor.class) {
            if (createdPropertyEditor != null) {
                ((EffectPopupEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new EffectPopupEditor(propMeta, selectedClasses, getEditorController());
            }
        } else if (editorClass == FontPopupEditor.class) {
            if (createdPropertyEditor != null) {
                ((FontPopupEditor) createdPropertyEditor).reset(propMeta, selectedClasses, getEditorController());
            } else {
                createdPropertyEditor = new FontPopupEditor(propMeta, selectedClasses, getEditorController());
            }
        } else if (editorClass == PaintPopupEditor.class) {
            if (createdPropertyEditor != null) {
                ((PaintPopupEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new PaintPopupEditor(propMeta, selectedClasses, getEditorController());
            }
        } else if (editorClass == ImageEditor.class) {
            if (createdPropertyEditor != null) {
                ((ImageEditor) createdPropertyEditor).reset(propMeta, selectedClasses, getEditorController().getFxmlLocation());
            } else {
                createdPropertyEditor = new ImageEditor(propMeta, selectedClasses, getEditorController().getFxmlLocation());
            }
        } else if (editorClass == BoundsPopupEditor.class) {
            if (createdPropertyEditor != null) {
                ((BoundsPopupEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new BoundsPopupEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == Point3DEditor.class) {
            if (createdPropertyEditor != null) {
                ((Point3DEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new Point3DEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == DividerPositionsEditor.class) {
            if (createdPropertyEditor != null) {
                ((DividerPositionsEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new DividerPositionsEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == TextAlignmentEditor.class) {
            if (createdPropertyEditor != null) {
                ((TextAlignmentEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new TextAlignmentEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == KeyCombinationPopupEditor.class) {
            if (createdPropertyEditor != null) {
                ((KeyCombinationPopupEditor) createdPropertyEditor).reset(propMeta, selectedClasses, getEditorController());
            } else {
                createdPropertyEditor = new KeyCombinationPopupEditor(propMeta, selectedClasses, getEditorController());
            }
        } else if (editorClass == ColumnResizePolicyEditor.class) {
            if (createdPropertyEditor != null) {
                ((ColumnResizePolicyEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new ColumnResizePolicyEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == Rectangle2DPopupEditor.class) {
            if (createdPropertyEditor != null) {
                ((Rectangle2DPopupEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new Rectangle2DPopupEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == ToggleGroupEditor.class) {
            if (createdPropertyEditor != null) {
                ((ToggleGroupEditor) createdPropertyEditor).reset(propMeta, selectedClasses, getSuggestedToggleGroups());
            } else {
                createdPropertyEditor = new ToggleGroupEditor(propMeta, selectedClasses, getSuggestedToggleGroups());
            }
        } else if (editorClass == ButtonTypeEditor.class) {
            if (createdPropertyEditor != null) {
                ((ButtonTypeEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new ButtonTypeEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == DurationEditor.class) {
            if (createdPropertyEditor != null) {
                ((DurationEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new DurationEditor(propMeta, selectedClasses);
            }
        }
        else if(editorClass == IncludeFxmlEditor.class) {
            createdPropertyEditor = createOrResetIncludeFxmlEditor(createdPropertyEditor, selectedClasses, propMeta);
        }
        else if(editorClass == CharsetEditor.class) {
            createdPropertyEditor = createOrResetCharsetEditor(createdPropertyEditor, selectedClasses, propMeta);
        } else if (editorClass == ColorPopupEditor.class) {
            if (createdPropertyEditor != null) {
                createdPropertyEditor.reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new ColorPopupEditor(propMeta, selectedClasses, getEditorController());
            }
        }
        else {
            if (createdPropertyEditor != null) {
                ((GenericEditor) createdPropertyEditor).reset(propMeta, selectedClasses);
            } else {
                createdPropertyEditor = new GenericEditor(propMeta, selectedClasses);
            }
        }
        if(createdPropertyEditor != null)
            createdPropertyEditor.setUpdateFromModel(false);

        return createdPropertyEditor;
    }

    private PropertyEditor createOrResetIncludeFxmlEditor(final PropertyEditor propertyEditor, final Set<Class<?>> selectedClasses, final ValuePropertyMetadata propMeta) {
        final PropertyEditor newPropertyEditor;
        if (propertyEditor != null) {
            newPropertyEditor = propertyEditor;
            propertyEditor.reset(propMeta, selectedClasses);
        }
        else {
            newPropertyEditor = new IncludeFxmlEditor(propMeta, selectedClasses, getEditorController());
        }
        return newPropertyEditor;
    }

    private PropertyEditor createOrResetCharsetEditor(final PropertyEditor propertyEditor, final Set<Class<?>> selectedClasses, final ValuePropertyMetadata propMeta) {
        PropertyEditor newPropertyEditor = null;
        if (propMeta instanceof StringPropertyMetadata) {
            if (propertyEditor != null) {
                newPropertyEditor = propertyEditor;
                ((CharsetEditor) propertyEditor).reset(propMeta, selectedClasses, this.availableCharsets);
            } else {
                newPropertyEditor = new CharsetEditor(propMeta, selectedClasses, this.availableCharsets);
            }
        }
        return newPropertyEditor;
    }

    private PropertiesEditor makePropertiesEditor(final Class<? extends Editor> editorClass, final ValuePropertyMetadata[] propMetas) {
        Editor editor = null;
        final PropertiesEditor propertiesEditor;
        final var editorPool = editorPools.get(editorClass);
        if ((editorPool != null) && !editorPool.isEmpty()) {
            editor = editorPool.pop();
            assert isPropertiesEditor(editor);
        }

        // Only AnchorPane for now
        assert editorClass == AnchorPaneConstraintsEditor.class;

        if (editor != null) {
            assert editor instanceof AnchorPaneConstraintsEditor;
            ((AnchorPaneConstraintsEditor) editor).reset(
                    propMetas[0], propMetas[1], propMetas[2], propMetas[3], getSelectedInstances());
        } else {
            editor = new AnchorPaneConstraintsEditor("AnchorPane Constraints", propMetas[0], propMetas[1], propMetas[2], propMetas[3], getSelectedInstances());
        }
        propertiesEditor = (AnchorPaneConstraintsEditor) editor;

        editorsInUse.add(editor);
        return propertiesEditor;
    }

    private static class SubSectionTitle {

        @FXML
        private Label titleLb;

        private Parent root;

        public SubSectionTitle(final String title) {
            initialize(title);
        }

        // Separate method to avoid FindBugs warning
        private void initialize(final String title) {
//          System.out.println("Loading new SubSection.fxml...");
            final var fxmlURL = SubSectionTitle.class.getResource("SubSection.fxml");
          root = EditorUtils.loadFxml(fxmlURL, this);
          titleLb.setText(title);
        }

        public void setTitle(final String title) {
            titleLb.setText(title);
        }

        public Node getNode() {
            return root;
        }
    }

    private void updateClassNameInSectionTitles() {
        final var intrinsicClassName = "FXOMIntrinsic";
        var selClass = ""; //NOI18N
        if (getSelectedClasses().size() > 1) {
            selClass = I18N.getString("inspector.sectiontitle.multiple");
        } else if (getSelectedClasses().size() == 1) {
            selClass = getSelectedClass().getSimpleName();
            if(intrinsicClassName.equals(selClass)) {
                selClass =  retrieveNameForIntrinsic();
            }

        }

        for (final var titledPane : accordion.getPanes()) {
            final var graphic = titledPane.getGraphic();
            assert graphic instanceof Label;
            if (titledPane == allTitledPane) {
                allTitledPane.setText(null);
            } else {
                if (!selClass.isEmpty() && !selClass.startsWith(" :")) { //NOI18N
                    selClass = " : " + selClass; //NOI18N
                }
            }
            ((Label) graphic).setText(selClass);
        }
    }

    private String retrieveNameForIntrinsic() {
        final var includeTagBinder = "fx:include - ";
        var source = "";
        if(getSelectedIntrinsics().iterator().hasNext()) {
            final var fxomIntrinsic = getSelectedIntrinsics().iterator().next();
            final var p = Paths.get(fxomIntrinsic.getSource());
            source = includeTagBinder.concat(p.getFileName().toString());
        }
        return source;
    }

    private boolean isPropertyEditor(final Editor editor) {
        return editor instanceof PropertyEditor;
    }

    private boolean isPropertiesEditor(final Editor editor) {
        return editor instanceof PropertiesEditor;
    }

    private List<String> getSuggestedFxIds(final String controllerClass) {
        // Is not needed if multiple selection.
        if (controllerClass == null || hasMultipleSelection()) {
            return Collections.emptyList();
        }
        final var glossary = getEditorController().getGlossary();
        URL location = null;
        if (getEditorController().getFxomDocument() != null) {
            location = getEditorController().getFxomDocument().getLocation();
        }
        final var fxIds = glossary.queryFxIds(location, controllerClass, getSelectedClass());
        // Remove the already used FxIds
        fxIds.removeAll(getFxIdsInUse());
        return fxIds;
    }

    private List<String> getFxIdsInUse() {
        final var fxomIndex = new FXOMFxIdIndex(getEditorController().getFxomDocument());
        return new ArrayList<>(fxomIndex.getFxIds().keySet());
    }

    private List<String> getSuggestedEventHandlers(final String controllerClass) {
        if (controllerClass == null) {
            return Collections.emptyList();
        }
        final var glossary = getEditorController().getGlossary();
        URL location = null;
        if (getEditorController().getFxomDocument() != null) {
            location = getEditorController().getFxomDocument().getLocation();
        }
        return glossary.queryEventHandlers(location, controllerClass);
    }

    private List<String> getSuggestedToggleGroups() {
        final var fxomIndex = new FXOMFxIdIndex(getEditorController().getFxomDocument());
        final var tgs = fxomIndex.collectToggleGroups();
        final var tgNames = new ArrayList<String>();
        for (final var tg : tgs) {
            tgNames.add(tg.getFxId());
        }
        return tgNames;
    }

    private String getControllerClass() {
        return getEditorController().getFxomDocument().getFxomRoot().getFxController();
    }

    // 
    // Helper methods for SelectionState class
    //
    private Set<FXOMInstance> getSelectedInstances() {
        return selectionState.getSelectedInstances();
    }

    private FXOMObject getSelectedObject() {
        if(getSelectedInstances().size() == 1) {
            return (FXOMInstance) getSelectedInstances().toArray()[0];
        }
        else if(getSelectedIntrinsics().size() == 1) {
            return (FXOMIntrinsic) getSelectedIntrinsics().toArray()[0];
        }
        return null;
    }

    private Set<FXOMInstance> getUnresolvedInstances() {
        return selectionState.getUnresolvedInstances();
    }

    private Set<Class<?>> getSelectedClasses() {
        return selectionState.getSelectedClasses();
    }

    private Class<?> getSelectedClass() {
        assert getSelectedClasses().size() == 1;
        return (Class<?>) getSelectedClasses().toArray()[0];
    }

    private Class<?> getCommonParent() {
        return selectionState.getCommonParentClass();
    }

    private boolean isBoundedByProperties(final ValuePropertyMetadata propMeta) {
        // Only ScrollPane.hValue and ScrollPane.vValue for now
        if (propMeta.getName().toString().equals(Editor.hValuePropName)
                || propMeta.getName().toString().equals(Editor.vValuePropName)) {
            return true;
        }
        return false;
    }

    /*
     *   This class represents the selection state: 
     *   - the selected instances, 
     *   - the selected classes,
     *   - the common parent for the selected instances (if any),
     *   - the unresolved selected instances (if any), 
     *      in case of an instance is missing its corresponding object (for instance a png file)
     */
    private final class SelectionState {

        private final Selection selection;
        private final Set<FXOMInstance> selectedInstances = new HashSet<>();
        private final Set<FXOMIntrinsic> selectedIntrinsics = new HashSet<>();
        private final Set<Class<?>> selectedClasses = new HashSet<>();
        private Class<?> commonParentClass;
        private FXOMObject commonParentObject;
        private final Set<FXOMInstance> unresolvedInstances = new HashSet<>();

        public SelectionState(final EditorController editorController) {
            this.selection = editorController.getSelection();
            initialize();
        }

        protected void initialize() {
            // New selection: initializePopupContent all the selection variables
            selectedInstances.clear();
            selectedIntrinsics.clear();
            if (selection.getGroup() instanceof ObjectSelectionGroup) {
                handleObjectSelectionGroup(selection.getGroup());
            } else if (selection.getGroup() instanceof GridSelectionGroup) {
                handleGridSelectionGroup(selection.getGroup());
            }

            selectedClasses.clear();
            for (final var instance : selectedInstances) {
                if (instance.getDeclaredClass() != null) { // null means unresolved instance
                    selectedClasses.add(instance.getDeclaredClass());
                }
            }

            commonParentClass = null;
            for (final var instance : selectedInstances) {
                if (commonParentClass == null) {
                    // first instance
                    commonParentClass = getParentClass(instance);
                } else {
                    if (getParentClass(instance) != commonParentClass) {
                        commonParentClass = null;
                        break;
                    }
                }
            }

            for (final var intrinsic : selectedIntrinsics) {
                if (commonParentClass == null) {
                    // first instance
                    commonParentClass = getParentClass(intrinsic);
                } else {
                    if (getParentClass(intrinsic) != commonParentClass) {
                        commonParentClass = null;
                        break;
                    }
                }
            }

            commonParentObject = null;
            for (final var instance : selectedInstances) {
                if (commonParentObject == null) {
                    // first instance
                    commonParentObject = instance.getParentObject();
                } else {
                    if (instance.getParentObject() != commonParentObject) {
                        commonParentObject = null;
                        break;
                    }
                }
            }

            for (final var intrinsic : selectedIntrinsics) {
                if (commonParentObject == null) {
                    // first instance
                    commonParentObject = intrinsic.getParentObject();
                } else {
                    if (intrinsic.getParentObject() != commonParentObject) {
                        commonParentObject = null;
                        break;
                    }
                }
            }

            unresolvedInstances.clear();
            for (final var instance : selectedInstances) {
                if (instance.getSceneGraphObject() == null) {
                    unresolvedInstances.add(instance);
                }
            }
        }

        private void handleGridSelectionGroup(final AbstractSelectionGroup group) {
            final var gsg = (GridSelectionGroup) group;
            for (final var inst : gsg.collectConstraintInstances()) {
                selectedInstances.add(inst);
                // Open the Layout section, since all the row/columns properties are there.
                if (getExpandedSectionId() != SectionId.LAYOUT) {
                    setExpandedSection(SectionId.LAYOUT);
                }
            }
        }

        private void handleObjectSelectionGroup(final AbstractSelectionGroup group) {
            final var osg = (ObjectSelectionGroup) group;
            for (final var obj : osg.getItems()) {
                handleFxomInstance(obj);
                handleFxomIntrincis(obj);
            }
        }

        private void handleFxomInstance(final FXOMObject obj) {
            if (obj instanceof FXOMInstance) {
                selectedInstances.add((FXOMInstance) obj);
            }
        }

        private void handleFxomIntrincis(final FXOMObject obj) {
            if(obj instanceof final FXOMIntrinsic intrinsic) {
                selectedIntrinsics.add(intrinsic);
                final var fxomInstance = intrinsic.createFxomInstanceFromIntrinsic();
                selectedInstances.add(fxomInstance);
            }
        }

        private boolean isSelectionEmpty() {
            return selection.isEmpty();
        }

        private Set<Class<?>> getSelectedClasses() {
            return selectedClasses;
        }

        private Class<?> getCommonParentClass() {
            return commonParentClass;
        }

        private FXOMObject getCommonParentObject() {
            return commonParentObject;
        }

        private Set<FXOMInstance> getSelectedInstances() {
            return selectedInstances;
        }

        private Set<FXOMInstance> getUnresolvedInstances() {
            return unresolvedInstances;
        }

        private Class<?> getParentClass(final FXOMObject instance) {
            final var parent = instance.getParentObject();
            if (parent == null) {
                // root
                return null;
            }
            // A parent is always a FXOMInstance
            assert parent instanceof FXOMInstance;
            return ((FXOMInstance) parent).getDeclaredClass();
        }

    }

    /*
     * Set the focus to a given property value editor,
     * and move the scrolllbar so that it is visible.
     * Typically used by CSS analyzer.
     */
    public void setFocusToEditor(final PropertyName propName) {
        // Retrieve the editor
        PropertyEditor editor = null;
        for (final var ed : editorsInUse) {
            if (ed instanceof PropertyEditor) {
                if (propName.equals(((PropertyEditor) ed).getPropertyName())) {
                    editor = (PropertyEditor) ed;
                }
            }
        }
        if (editor == null) {
            // editor not found
            return;
        }

        final var editorToFocus = editor;

        final var valueEditorNode = editorToFocus.getValueEditor();
        // Search the ScrollPane
        ScrollPane sp = null;
        Node node = valueEditorNode.getParent();
        while (node != null) {
            if (node instanceof ScrollPane) {
                sp = (ScrollPane) node;
                break;
            }
            node = node.getParent();
        }
        if (sp == null) {
            return;
        }

        // Position the scrollBar such as the editor is centered in the TitledPane (when possible)
        final var scrollPane = sp;
        final var editorHeight = valueEditorNode.getLayoutBounds().getHeight();
        final var pt = Deprecation.localToLocal(valueEditorNode, 0, 0, scrollPane.getContent());
        // viewport height
        final var vpHeight = scrollPane.getViewportBounds().getHeight();
        // Position of the editor in the scrollPane content
        var selY = pt.getY();
        // Height of the scrollPane content
        final var contentHeight = scrollPane.getContent().getLayoutBounds().getHeight();
        // Position of the middle point of the scrollPane content
        final var contentMiddle = contentHeight / 2;
        // Manage the editor height depending on its position
        if (selY > contentMiddle) {
            selY += editorHeight;
        } else {
            selY -= editorHeight;
        }
        // Compute the move to apply to position the editor on the middle of the scrollPane content
        final var moveContent = selY - contentMiddle;
        // Size ratio between scrollPane content and viewport
        final var vpRatio = contentHeight / vpHeight;
        // Move to apply to the editor to position it in the middle of the viewport
        final var moveVp = moveContent / vpRatio;
        // Position of the editor in the viewport
        final var selYVp = (vpHeight / 2) + moveVp;
        // Position in percent
        final var scrollPos = selYVp / vpHeight;
        // Finally, set the scrollBar position
        scrollPane.setVvalue(scrollPos);

        // Set the focus to the editor
        editorToFocus.requestFocus();
    }

}
