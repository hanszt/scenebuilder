/*
 * Copyright (c) 2016, 2022, Gluon and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.library;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.DocumentDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AbstractModalDialog.ButtonID;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.AlertDialog;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.dialog.ErrorDialog;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMArchive;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.library.BuiltinLibrary;
import com.oracle.javafx.scenebuilder.kit.library.Library;
import com.oracle.javafx.scenebuilder.kit.library.LibraryItem;
import com.oracle.javafx.scenebuilder.kit.library.LibraryItemNameComparator;
import com.oracle.javafx.scenebuilder.kit.library.user.UserLibrary;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.preferences.MavenPreferences;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;

/**
 * This class creates and controls the <b>Library Panel</b> of Scene Builder
 * Kit.
 *
 */
public class LibraryPanelController extends AbstractFxmlPanelController {

    private String searchPattern;
    ArrayList<LibraryItem> searchData = new ArrayList<>();
    private final String TEMP_FILE_EXTENSION = ".tmp"; //NOI18N
    // The name of the library section to keep opened. This is used when e.g.
    // a user jar file is imported to the library directory.
    // If the user was doing a search then we let the library layout unchanged.
    // If the user wasn't doing a search and custom content is updated then
    // we want to get Custom section opened.
    String sectionNameToKeepOpened = null;
    boolean initiateImportDialog = false;
    final List<File> jarAndFxmlFiles = new ArrayList<>();
    private String userLibraryPathString = null;
    private final MavenPreferences mavenPreferences;
    private boolean animateAccordion;

    @FXML
    private Accordion libAccordion;
    @FXML
    Label noSearchResults;
    @FXML ListView<LibraryListItem> libSearchList;

    @FXML ListView<LibraryListItem> libList = null;

    @FXML StackPane libPane;

    /*
     * Public
     */
    
    /**
     * Creates a library panel controller for the specified editor controller.
     *
     * @param c the editor controller (never null).
     */
    public LibraryPanelController(final EditorController c, final MavenPreferences mavenPreferences) {
        super(LibraryPanelController.class.getResource("LibraryPanel.fxml"), I18N.getBundle(), c); //NOI18N
        startListeningToLibrary();
        this.mavenPreferences = mavenPreferences;
    }

    /**
     * Returns null or the search pattern applied to this library panel.
     *
     * @return null or the search pattern applied to this library panel.
     */
    public String getSearchPattern() {
        return searchPattern;
    }

    /**
     * Sets the search pattern to be applied to this library panel. When null
     * value is passed, the library panel displays all its items.
     *
     * @param searchPattern null or the search pattern to apply to this library
     * panel.
     */
    public void setSearchPattern(final String searchPattern) {
        this.searchPattern = searchPattern.toUpperCase(Locale.ENGLISH);
        searchPatternDidChange();
    }

    /**
     * @param owner
     * @treatAsPrivate Perform the import jar action.
     */
    public void performImportJarFxml(final Window owner) {
        // Open file chooser and get user selection
        final var importedFiles = performSelectJarOrFxmlFile(owner);
        processImportJarFxml(importedFiles);
    }
    
    /**
     * @param owner
     * @treatAsPrivate Perform the import jar action.
     */
    public void performImportFromFolder(final Window owner) {
        final var folder = performSelectFolder(owner);
        processImportFolder(folder);
    }
    
	/**
     * @treatAsPrivate Perform the import of the selection
     * @param objects the FXOM objects to import to customize the Library content.
     */
    public void performImportSelection(final List<FXOMObject> objects) {
        processInternalImport(objects);
    }

    public void animateAccordion(final boolean animate) {
        this.animateAccordion = animate;
        libAccordion.getPanes().forEach(tp -> tp.setAnimated(animate));
    }
    /*
     * AbstractPanelController
     */
    
    /**
     * @treatAsPrivate FXOM document did change.
     * @param oldDocument the previous fxom document or null
     */
    @Override
    protected void fxomDocumentDidChange(final FXOMDocument oldDocument) {
    }

    /**
     * @treatAsPrivate User scene graph did change.
     */
    @Override
    protected void sceneGraphRevisionDidChange() {
    }

    /**
     * @treatAsPrivate User scene graph did change.
     */
    @Override
    protected void cssRevisionDidChange() {
    }

    /**
     * @treatAsPrivate Job manager revision did change.
     */
    @Override
    protected void jobManagerRevisionDidChange() {
        // FXOMDocument has been modified by a job.
        // Library panel should probably not care for now.
    }
    
    /**
     * @treatAsPrivate Selection did change.
     */
    @Override
    protected void editorSelectionDidChange() {
    }

    /*
     * AbstractFxmlPanelController
     */
    
    /**
     * @treatAsPrivate Controller did load fxml.
     */
    @Override
    protected void controllerDidLoadFxml() {
        assert libAccordion != null;
        assert libPane != null;
        assert libList != null;
        assert noSearchResults != null;
        assert libSearchList != null;
        
        startListeningToDrop();
        setDisplayMode(DISPLAY_MODE.SECTIONS);
        populateLibraryPanel();
        setUserLibraryPathString();
    }

    private void displayModeDidChange(final DISPLAY_MODE displayMode) {
        if (libAccordion != null) {
            switch (displayMode) {
                case SECTIONS:
                    libAccordion.setVisible(true);
                    libAccordion.setManaged(true);
                    noSearchResults.setVisible(false);
                    noSearchResults.setManaged(false);
                    libSearchList.setVisible(false);
                    libSearchList.setManaged(false);
                    getLibList().setVisible(false);
                    getLibList().setManaged(false);
                    break;
                case SEARCH:
                    libAccordion.setVisible(false);
                    libAccordion.setManaged(false);
                    if (libSearchList.getItems().isEmpty()) {
                        noSearchResults.setVisible(true);
                        noSearchResults.setManaged(true);
                        libSearchList.setVisible(false);
                        libSearchList.setManaged(false);
                    } else {
                        noSearchResults.setVisible(false);
                        noSearchResults.setManaged(false);
                        libSearchList.setVisible(true);
                        libSearchList.setManaged(true);
                    }
                    getLibList().setVisible(false);
                    getLibList().setManaged(false);
                    break;
                case LIST:
                    libAccordion.setVisible(false);
                    libAccordion.setManaged(false);
                    noSearchResults.setVisible(false);
                    noSearchResults.setManaged(false);
                    libSearchList.setVisible(false);
                    libSearchList.setManaged(false);
                    getLibList().setVisible(true);
                    getLibList().setManaged(true);
                    break;
                default:
                    break;
            }
        }
    }

    /*
     * Private
     */

    /**
     * The display mode of the Library panel is used to choose how items
     * are rendered within the panel.
     */
    public enum DISPLAY_MODE {

        SECTIONS {

                    @Override
                    public String toString() {
                        return I18N.getString("library.panel.menu.view.sections");
                    }
                },
        SEARCH,
        LIST {

                    @Override
                    public String toString() {
                        return I18N.getString("library.panel.menu.view.list");
                    }
                }
    };
    
    private DISPLAY_MODE currentDisplayMode;
    private DISPLAY_MODE previousDisplayMode = DISPLAY_MODE.SECTIONS;
    
    public void setPreviousDisplayMode(final DISPLAY_MODE displayMode) {
        this.previousDisplayMode = displayMode;
    }
    
    public void setDisplayMode(final DISPLAY_MODE displayMode) {
        this.currentDisplayMode = displayMode;
        displayModeDidChange(displayMode);
    }
    
    public DISPLAY_MODE getDisplayMode() {
        return currentDisplayMode;
    }
    
    final ListChangeListener<LibraryItem> libraryItemListener = change -> libraryDidChange(null);
    
    private final ChangeListener<Library> libraryListener = (ov, t, t1) -> {
        // When a jar is imported this listener is called two times.
        // First the UserLibrary is turned into BuiltinLibrary, then it is
        // turned back into a UserLibrary with the up to date library dir
        // content.
//            System.out.println("libraryListener called - t " + t + " - t1 " + t1);
        if (t instanceof UserLibrary) {
            t.getItems().removeListener(libraryItemListener);
            t.getItems().clear();
        }
        if (t1 instanceof UserLibrary) {
            t1.getItems().addListener(libraryItemListener);
            if (sectionNameToKeepOpened != null) {
                sectionNameToKeepOpened = null;
            }
        }
        // libraryDidChange might not be called by several listeners.
        // Silencing the one below means I dunno how to get the selected index.
//            libraryDidChange(t);
    };
    
    private void startListeningToLibrary() {
        getEditorController().libraryProperty().addListener(libraryListener);
    }
    
    // For now there is no scenario where this method might be of some use.
//    private void stopListeningToLibrary() {
//        getEditorController().libraryProperty().removeListener(libraryListener);
//    }

    void libraryDidChange(final Library oldLib) {
        if (libAccordion != null) {
            // Clear the content of the panel.
            libAccordion.getPanes().clear();

            // Reconstruct the panel content based on the new Library.
            populateLibraryPanel();
        }
    }

    private String getExpandedSectionName() {
        String sectionName = null;
        
        if (libAccordion != null && libAccordion.getExpandedPane() != null) {
            sectionName = libAccordion.getExpandedPane().getText();
        }
        
//        System.out.println("getExpandedSectionName " + sectionName);
        return sectionName;
    }
    
//    private String getSelectedItemName() {
//        String selectedItemName = null;
//        
//        if (libAccordion != null && libAccordion.getExpandedPane() != null) {
//            final ListView<?> list = (ListView<?>)libAccordion.getExpandedPane().getContent();
//            Object selectedItem = list.getSelectionModel().getSelectedItem();
//            if (selectedItem instanceof LibraryListItem) {
//                selectedItemName = ((LibraryListItem)selectedItem).getLibItem().getName();
//            }
//        }
//        
////        System.out.println("getSelectedItemName " + selectedItemName);
//        return selectedItemName;
//    }
    
    // We need to discover the sections and the content of each one before being
    // able to populate the panel.
    // Each section is a TitledPane that contains a ListView of LibraryItem.
    // The order section are listed is managed by the comparator that comes with
    // the Library. In each section the LibraryItem are sorted with alphabetical
    // ordering.
    // First TitledPane is expanded.
    private void populateLibraryPanel() {
        // libData is backend structure for all that we put in the Accordion.
        final var libData = new LinkedHashMap<String, ArrayList<LibraryItem>>();
        final var sectionNames = new TreeSet<>(getEditorController().getLibrary().getSectionComparator());
        final List<TitledPane> panes = libAccordion.getPanes();
        
        searchData.clear();
        getLibList().getItems().clear();
        
        if (getEditorController().getLibrary().getItems().size() > 0) {
            // Construct a sorted set of all lib section names.
            for (final var item : getEditorController().getLibrary().getItems()) {
                sectionNames.add(item.getSection());
            }

            // Create a sorted set of lib elements for each section.
            for (final var sectionName : sectionNames) {
                libData.put(sectionName, new ArrayList<>());
            }

            // Add each LibraryItem to the appropriate set.
            for (final var item : getEditorController().getLibrary().getItems()) {
                libData.get(item.getSection()).add(item);
            }

            // Parse our lib data structure and populate the Accordion accordingly.
            for (final var sectionName : sectionNames) {
                final var itemsList = new ListView<LibraryListItem>();
                itemsList.setId(sectionName + "List"); // for QE //NOI18N
                itemsList.setCellFactory(cb);
                itemsList.addEventHandler(KeyEvent.KEY_RELEASED, keyEventHandler);
                Collections.sort(libData.get(sectionName), new LibraryItemNameComparator());
                for (final var item : libData.get(sectionName)) {
                    itemsList.getItems().add(new LibraryListItem(item));
                }
                final var sectionPane = new TitledPane(sectionName, itemsList);
                sectionPane.setId(sectionName); // for QE
                sectionPane.setAnimated(true);
                panes.add(sectionPane);

                searchData.addAll(libData.get(sectionName));

                getLibList().getItems().add(new LibraryListItem(sectionName));
                for (final var item : libData.get(sectionName)) {
                    getLibList().getItems().add(new LibraryListItem(item));
                }
            }

            if (libAccordion.getPanes().size() >= 1) {
                expandPaneWithName(sectionNameToKeepOpened);
            }

            if (libSearchList.getCellFactory() == null) {
                libSearchList.setCellFactory(cb);
            }

            if (getLibList().getCellFactory() == null) {
                getLibList().setCellFactory(cb);
            }

            libSearchList.addEventHandler(KeyEvent.KEY_RELEASED, keyEventHandler);
            getLibList().addEventHandler(KeyEvent.KEY_RELEASED, keyEventHandler);
        }
        // Update animation for all TitledPane
        libAccordion.getPanes().forEach(tp -> tp.setAnimated(animateAccordion));
    }
    
    private void expandPaneWithName(final String paneName) {
        var sectionName = paneName;
        
        if (sectionName == null) {
            sectionName = BuiltinLibrary.TAG_CONTAINERS;
        }
        
        for (final var tp : libAccordion.getPanes()) {
            if (tp.getText().equals(sectionName)) {
//                System.out.println("expandPaneWithName - Expand section " + sectionName);
                libAccordion.setExpandedPane(tp);
            }
        }
    }
    
    private void searchPatternDidChange() {
        if (searchPattern == null || searchPattern.isEmpty()) {
            currentDisplayMode = previousDisplayMode;
        } else {
            if (currentDisplayMode != DISPLAY_MODE.SEARCH) {
                previousDisplayMode = currentDisplayMode;
                currentDisplayMode = DISPLAY_MODE.SEARCH;
            }
        }
        
        // The filtering is done by ignoring case, and by retaining any item that
        // contains the given pattern. An opened question is to filter as soon as
        // the pattern is two or more characters long: for now we react from the
        // first character.
        //
        // It can occur the whole Library is changed under the foots of SceneBuilder
        // while filtering is on going. searchData is never null so if its
        // content is changing then the filtering result will be inacurrate:
        // that is acceptable.
        //
        if (currentDisplayMode.equals(DISPLAY_MODE.SEARCH)) {
            libSearchList.getItems().clear();
            final var rawFilteredItem = new ArrayList<LibraryItem>();
            for (final var item : searchData) {
                if (item.getName().toUpperCase(Locale.ROOT).contains(searchPattern)) {
                    rawFilteredItem.add(item);
                }
            }
            Collections.sort(rawFilteredItem, new LibraryItemNameComparator());
            for (final var item : rawFilteredItem) {
                libSearchList.getItems().add(new LibraryListItem(item));
            }
            rawFilteredItem.clear();
        }
        
        setDisplayMode(currentDisplayMode);
    }
    
    // Key events listened onto the ListView
    // For some reason the listener when set on the cell (see LibraryListCell)
    // is never called, probably because it is the ListView which has the focus.
    private final EventHandler<KeyEvent> keyEventHandler = e -> handleKeyEvent(e);
    
    private final Callback<ListView<LibraryListItem>, ListCell<LibraryListItem>> cb
            = param -> new LibraryListCell(getEditorController());
    
    private void handleKeyEvent(final KeyEvent e) {
        // On ENTER we try to insert the item which is selected within the Library.
        if (e.getCode() == KeyCode.ENTER) {
            // This way of doing things requires the use of an @SuppressWarnings("unchecked")
//            final LibraryItem item = ((ListView<LibraryItem>)e.getSource()).getSelectionModel().getSelectedItem();
            // hence this other way below ...
            final var source = e.getSource();
            assert source instanceof ListView;
            final var list = (ListView<?>)source;
            final var rawItem = list.getSelectionModel().getSelectedItem();
            assert rawItem instanceof LibraryListItem;
            final var listitem = (LibraryListItem)rawItem;
            final var item = listitem.getLibItem();
             
            if (getEditorController().canPerformInsert(item)) {
                getEditorController().performInsert(item);
            }
            
            e.consume();
        }
    }
    
    private void startListeningToDrop() {
        libPane.setOnDragDropped(t -> {
//                System.out.println("libPane onDragDropped");
            final var dragSource = getEditorController().getDragController().getDragSource();
            if (dragSource instanceof DocumentDragSource) {
                processInternalImport(((DocumentDragSource)dragSource).getDraggedObjects());
            } else {
                initiateImportDialog = false;
                jarAndFxmlFiles.clear();
                t.setDropCompleted(true);
                // Drop gesture is only valid when the Library is an instance of UserLibrary
                if (getEditorController().getLibrary() instanceof UserLibrary) {
                    final var db = t.getDragboard();
                    if (db.hasFiles()) {
                        final var files = db.getFiles();
                        for (final var file : files) {
                            // Keep only jar and fxml files
                            if (file.isFile() && (file.getName().endsWith(".jar") || file.getName().endsWith(".fxml"))) { //NOI18N
//                                System.out.println("libPane onDragDropped - Retaining file " + file.getName());
                                jarAndFxmlFiles.add(file);
                            }
                        }

                        // The import dialog might be kept opened by the user quite
                        // a long time.
                        // On Mac (not on Win), after around 20 seconds of opening
                        // time of the import dialog window the user sees a move from
                        // the lib panel to the Finder of the file icon, as if the drag
                        // is rejected.
                        // In order to silence (mask ?) this issue there's:
                        // - the delegation to setOnDragExited of the call of processImportJarFxml
                        // so that current handler returns fast.
                        // - the runLater in setOnDragExited, wrapped with a Timer set with a 1 second delay
                        // Is there a way to be notified when the import dialog
                        // can be run without interfering with the drag and drop sequence ?
                        initiateImportDialog = true;
                    }
                }
            }
        });
        
        libPane.setOnDragExited(t -> {
//                System.out.println("libPane onDragExited");
            if (initiateImportDialog) {
                initiateImportDialog = false;
                final var timer = new Timer(true);
                final var timerTask = new TimerTask() {

                    @Override
                    public void run() {
                        Platform.runLater(() -> processImportJarFxml(jarAndFxmlFiles));
                        // I don't need to use the timer later on so by
                        // cancelling it right here I'm sure free resources
                        // that otherwise would prevent the JVM from exiting.
                        timer.cancel();
                    }
                };
                timer.schedule(timerTask, 600); // milliseconds
            }
        });
        
        
        libPane.setOnDragOver(t -> {
//                System.out.println("libPane onDragOver");
            final var dragSource = getEditorController().getDragController().getDragSource();
            final var db = t.getDragboard();
            // db has file when dragging a file from native file manager (Mac Finder, Windows Explorer, ...).
            // dragSource is not null if the user drags something from Hierarchy or Content panel.
            if (db.hasFiles() || dragSource != null) {
                t.acceptTransferModes(TransferMode.COPY);
            }
        });
        
        // This one is called only if lib is the source of the drop.
        libPane.setOnDragDone(t -> {
            assert getEditorController().getDragController().getDragSource() != null;
            getEditorController().getDragController().end();
            t.getDragboard().clear();
            t.consume();
        });
        
    }
    
    // An internal import is an import to the Library initiated from within
    // SceneBuilder (from Content or Hierarchy).
    // We stop the watching thread to avoid potential parsing of a file that
    // would not yet be properly finalized on disk.
    private void processInternalImport(final List<FXOMObject> objects) {
        sectionNameToKeepOpened = getExpandedSectionName();
        setUserLibraryPathString();
        final var libPath = Paths.get(userLibraryPathString);
        var hasDependencies = false;
        // Selection can be multiple: as soon as one has dependencies
        // we won't import anything.
        // Copy of the dependencies remains something to do (DTL-5879).
        for (final var asset : objects) {
            if (hasDependencies(asset)) {
                hasDependencies = true;
                break;
            }
        }
        
        if (hasDependencies) {
            userLibraryUpdateRejected();
        } else {
            ((UserLibrary) getEditorController().getLibrary()).stopWatching();

            try {
                // The selection can be multiple, in which case each asset is
                // processed separately.
                for (final var asset : objects) {
                    // Create an FXML layout as a String
                    final var selection = new ArrayList<FXOMObject>();
                    selection.add(asset);
                    final var fxomArchive = new FXOMArchive(selection);
                    final var entry0 = fxomArchive.getEntries().getFirst();
                    final var fxmlText = entry0.getFxmlText();

                    // Write the FXML layout into a dedicated file stored in the user Library dir.
                    // We use the tag name of the top element and append a number:
                    // if Library dir already contains SplitPane_1.fxml and top element has
                    // tag name SplitPane then we will create SplitPane_2.fxml and so on.
                    // Note the tag name is most of the time identical to the Java class name, see DTL-6643.
                    final var prefix = asset.getGlueElement().getTagName();
                    final var fxmlFile = getUniqueFxmlFileName(prefix, userLibraryPathString);
                    writeFxmlFile(fxmlFile, fxmlText, libPath);
                }
            } finally {
                if (currentDisplayMode.equals(DISPLAY_MODE.SECTIONS)) {
                    sectionNameToKeepOpened = UserLibrary.TAG_USER_DEFINED;
                }
                    
                ((UserLibrary) getEditorController().getLibrary()).startWatching();
            }
        }
    }

    private void writeFxmlFile(final File targetFile, final String text, final Path libPath) {
        final var targetFilePath = Paths.get(targetFile.getPath());
        createUserLibraryDir(libPath);

        try {
            // Create the new file
            Files.createFile(targetFilePath);

            // Write content of file
            try (final var writer = new PrintWriter(targetFile, StandardCharsets.UTF_8)) { //NOI18N
                writer.write(text);
            }
        } catch (final IOException ioe) {
            final var errorDialog = new ErrorDialog(null);
            errorDialog.setTitle(I18N.getString("error.file.create.title"));
            errorDialog.setMessage(I18N.getString("error.file.create.message", targetFilePath.normalize().toString()));
            errorDialog.setDetails(I18N.getString("error.write.details"));
            errorDialog.setDebugInfoWithThrowable(ioe);
            errorDialog.showAndWait();
        }
    }
    
    private File getUniqueFxmlFileName(final String prefix, final String libDir) {
        var suffix = 0;
        File file = null;
        while (file == null || file.exists()) {
            suffix++;
            file = new File(libDir + File.separator + prefix + "_" + suffix + ".fxml"); //NOI18N
        }
        
        return file;
    }
    
    private void processImportJarFxml(final List<File> importedFiles) {
        if (importedFiles != null && !importedFiles.isEmpty()) {
            sectionNameToKeepOpened = getExpandedSectionName();
            final var libPath = Paths.get(((UserLibrary)getEditorController().getLibrary()).getPath());
            // Create UserLibrary dir if missing
            if (createUserLibraryDir(libPath)) {
                final var fxmlFiles = getSubsetOfFiles(".fxml", importedFiles); //NOI18N

                if (!fxmlFiles.isEmpty() && enoughFreeSpaceOnDisk(fxmlFiles) && ! hasDependencies(fxmlFiles)) {
                    copyFilesToUserLibraryDir(fxmlFiles);
                    
                    if (currentDisplayMode.equals(DISPLAY_MODE.SECTIONS)) {
                        sectionNameToKeepOpened = UserLibrary.TAG_USER_DEFINED;
                    }
                }

                final var jarFiles = getSubsetOfFiles(".jar", importedFiles); //NOI18N
                // For jar files we delegate to the import dialog.
                if (!jarFiles.isEmpty() && enoughFreeSpaceOnDisk(jarFiles)) {
                    // From here we know we will initiate the import dialog.
                    // This is why we put application window on the front.
                    // From there the import dialog window, which is application modal,
                    // should come on top of it.
                    final var window = getPanelRoot().getScene().getWindow();
                    if (window instanceof final Stage stage) {
                        stage.toFront();
                    }

                    final var iwc
                            = new ImportWindowController(this, jarFiles, mavenPreferences, (Stage) window);
                    iwc.setToolStylesheet(getEditorController().getToolStylesheet());
                    // See comment in OnDragDropped handle set in method startListeningToDrop.
                    final var userChoice = iwc.showAndWait();

                    if (userChoice.equals(ButtonID.OK) && currentDisplayMode.equals(DISPLAY_MODE.SECTIONS)) {
                        sectionNameToKeepOpened = UserLibrary.TAG_USER_DEFINED;
                    }
                }
            }
        }
    }
    
    private void processImportFolder(final File folder) {
        if (folder != null && folder.exists() && folder.isDirectory()) {
            final var libPath = Paths.get(((UserLibrary)getEditorController().getLibrary()).getPath());
            if (createUserLibraryDir(libPath)) {
                // From here we know we will initiate the import dialog.
                // This is why we put application window on the front.
                // From there the import dialog window, which is application modal,
                // should come on top of it.
                final var window = getPanelRoot().getScene().getWindow();
                if (window instanceof final Stage stage) {
                    stage.toFront();
                }

                final var iwc = new ImportWindowController(this, Arrays.asList(folder), mavenPreferences, (Stage) window);
                iwc.setToolStylesheet(getEditorController().getToolStylesheet());
                // See comment in OnDragDropped handle set in method startListeningToDrop.
                final var userChoice = iwc.showAndWait();

                if (userChoice.equals(ButtonID.OK) && currentDisplayMode.equals(DISPLAY_MODE.SECTIONS)) {
                    sectionNameToKeepOpened = UserLibrary.TAG_USER_DEFINED;
                }
            }
        }
    }
    
    private List<File> getSubsetOfFiles(final String pattern, final List<File> files) {
        final List<File> res = new ArrayList<>();
        
        for (final var file : files) {
            if (file.getName().endsWith(pattern)) {
                res.add(file);
            }
        }
        
        return res;
    }
    
    private boolean createUserLibraryDir(final Path libPath) {
        var dirCreated = false;
        try {
            // Files.createDirectories do nothing if provided Path already exists.
            Files.createDirectories(libPath, new FileAttribute<?>[]{});
            dirCreated = true;
        } catch (final IOException ioe) {
            final var errorDialog = new ErrorDialog(null);
            errorDialog.setTitle(I18N.getString("error.dir.create.title"));
            errorDialog.setMessage(I18N.getString("error.dir.create.message", libPath.normalize().toString()));
            errorDialog.setDetails(I18N.getString("error.write.details"));
            errorDialog.setDebugInfoWithThrowable(ioe);
            errorDialog.showAndWait();
        }
        
        return dirCreated;
    }
    
    private boolean enoughFreeSpaceOnDisk(final List<File> files) {
        var totalSize = Long.MAX_VALUE; // bytes
        try {
            for (final var file : files) {
                final var targetPath = Paths.get(file.getAbsolutePath());
                totalSize += Files.size(targetPath);
            }
        } catch (final IOException ioe) {
            final var errorDialog = new ErrorDialog(null);
            errorDialog.setTitle(I18N.getString("error.disk.space.title"));
            errorDialog.setMessage(I18N.getString("error.disk.space.message"));
            errorDialog.setDetails(I18N.getString("error.write.details"));
            errorDialog.setDebugInfoWithThrowable(ioe);
            errorDialog.showAndWait();
        }
        
        final var libFile = new File(((UserLibrary)getEditorController().getLibrary()).getPath());
        return totalSize < libFile.getFreeSpace();
    }
    
    // Each copy is done via an intermediate temporary file that is renamed if
    // the copy goes well (for atomicity). If a copy fails we try to erase the
    // temporary file to stick to an as clean as possible disk content.
    // TODO fix DTL-5879 [When copying FXML files in lib dir we have to copy files which are external references as well]
    void copyFilesToUserLibraryDir(final List<File> files) {
        var errorCount = 0;
        IOException savedIOE = null;
        var savedFileName = ""; //NOI18N
        Path tempTargetPath = null;
        setUserLibraryPathString();
        
        // Here we deactivate the UserLib so that it unlocks the files contained
        // in the lib dir in the file system meaning (especially on Windows).
        ((UserLibrary) getEditorController().getLibrary()).stopWatching();
        
        try {
            for (final var file : files) {
                savedFileName = file.getName();
                tempTargetPath = Paths.get(userLibraryPathString, file.getName() + TEMP_FILE_EXTENSION);
                final var ultimateTargetPath = Paths.get(userLibraryPathString, file.getName());
                Files.deleteIfExists(tempTargetPath);
                Files.copy(file.toPath(), tempTargetPath, StandardCopyOption.REPLACE_EXISTING);
                Files.move(tempTargetPath, ultimateTargetPath, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (final IOException ioe) {
            errorCount++;
            savedIOE = ioe;
        } finally {
            if (tempTargetPath != null) {
                try {
                    Files.deleteIfExists(tempTargetPath);
                } catch (final IOException ioe) {
                    errorCount++;
                    savedIOE = ioe;
                }
            }
        }

        ((UserLibrary) getEditorController().getLibrary()).startWatching();

        if (errorCount > 0) {
            final var errorDialog = new ErrorDialog(null);
            errorDialog.setTitle(I18N.getString("error.copy.title"));
            if (errorCount == 1) {
                errorDialog.setMessage(I18N.getString("error.copy.message.single", savedFileName, userLibraryPathString));
                errorDialog.setDebugInfoWithThrowable(savedIOE);
            } else {
                errorDialog.setMessage(I18N.getString("error.copy.message.multiple", errorCount, userLibraryPathString));
            }
            errorDialog.setDetails(I18N.getString("error.write.details"));
            errorDialog.showAndWait();
        }
    }
    
    /**
     * Open a file chooser that allows to select one or more FXML and JAR file.
     * @return the list of selected files
     */
    private List<File> performSelectJarOrFxmlFile(final Window owner) {
        final var fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(I18N.getString("lib.filechooser.filter.msg"),
                "*.fxml", "*.jar")); //NOI18N
        fileChooser.setInitialDirectory(EditorController.getNextInitialDirectory());
        final var selectedFiles = fileChooser.showOpenMultipleDialog(owner);
        if(selectedFiles != null && !selectedFiles.isEmpty()){
            // Keep track of the user choice for next time
            EditorController.updateNextInitialDirectory(selectedFiles.getFirst());
        }
        return selectedFiles;
    }

    /**
     * Open a file chooser that allows to select one folder
     * @return the selected folder or null
     */
    private File performSelectFolder(final Window owner) {
        final var dirChooser = new DirectoryChooser();
        dirChooser.setInitialDirectory(EditorController.getNextInitialDirectory());

        final var folder = dirChooser.showDialog(owner);
        if (folder != null) {
            // Keep track of the user choice for next time
            EditorController.updateNextInitialDirectory(folder);
        }

        return folder;
    }
    
    private void userLibraryUpdateRejected() {
        final var dialog = new AlertDialog(null);
        dialog.setTitle(I18N.getString("alert.import.reject.dependencies.title"));
        dialog.setMessage(I18N.getString("alert.import.reject.dependencies.message"));
        dialog.setDetails(I18N.getString("alert.import.reject.dependencies.details"));
        dialog.setActionButtonDisable(true);
        dialog.setActionButtonVisible(false);
        dialog.setOKButtonDisable(true);
        dialog.setOKButtonVisible(false);
        dialog.setCancelButtonTitle(I18N.getString("label.close"));
        dialog.showAndWait();
    }

    private static final PropertyName valueName = new PropertyName("value"); //NOI18N
    
    private boolean hasDependencies(final List<File> fxmlFiles) {
        var hasDependencies = false;
        var scanWentWell = true;

        for (final var fxmlFile : fxmlFiles) {
            try {
                if (hasDependencies(fxmlFile)) {
                    hasDependencies = true;
                    break;
                }
            } catch (final IOException ioe) {
                scanWentWell = false;
                hasDependencies = true; // not sure but better take no risk
                final var errorDialog = new ErrorDialog(null);
                errorDialog.setTitle(I18N.getString("error.import.reject.dependencies.scan.title"));
                errorDialog.setMessage(I18N.getString("error.import.reject.dependencies.scan.message"));
                errorDialog.setDetails(I18N.getString("error.import.reject.dependencies.scan.details"));
                errorDialog.setDebugInfoWithThrowable(ioe);
                errorDialog.showAndWait();
            }
        }

        if (hasDependencies && scanWentWell) {
            userLibraryUpdateRejected();
        }

        return hasDependencies;
    }

    private boolean hasDependencies(final File fxmlFile) throws IOException {
        var res = false;
        final URL location;
        
        location = fxmlFile.toURI().toURL();
        final var fxomDocument =
                new FXOMDocument(FXOMDocument.readContentFromURL(location), location,
                        getEditorController().getFxomDocument().getClassLoader(),
                        getEditorController().getFxomDocument().getResources(),
                        FXOMDocument.FXOMDocumentSwitch.NORMALIZED);
        res = hasDependencies(fxomDocument.getFxomRoot());
        
        return res;
    }
    
    private boolean hasDependencies(final FXOMObject rootFxomObject) {
        final var targetPaths = getDependenciesPaths(rootFxomObject);
        return targetPaths.size() > 0;
    }
    
    private List<Path> getDependenciesPaths(final FXOMObject rootFxomObject) {

        final List<Path> targetPaths = new ArrayList<>();

        for (final var p : rootFxomObject.collectPropertiesT()) {
            final var path = extractPath(p);
            if (path != null) {
                targetPaths.add(path);
            }
        }
        
        for (final var fxomObject : rootFxomObject.collectObjectWithSceneGraphObjectClass(URL.class)) {
            if (fxomObject instanceof final FXOMInstance urlInstance) {
                final var valueProperty = urlInstance.getProperties().get(valueName);
                if (valueProperty instanceof final FXOMPropertyT valuePropertyT) {
                    final var path = extractPath(valuePropertyT);
                    if (path != null) {
                        targetPaths.add(path);
                    }
                } else {
                    assert false : "valueProperty.getName() = " + valueProperty.getName();
                }
            }
        }

        return targetPaths;
    }
    
    
    private Path extractPath(final FXOMPropertyT p) {
        Path result;
        
        final var pv = new PrefixedValue(p.getValue());
        if (pv.isPlainString()) {
                final var url = URI.create(pv.getSuffix());
                result = Path.of(url);
        } else if (pv.isDocumentRelativePath()) {
            final var documentLocation = p.getFxomDocument().getLocation();
            if (documentLocation == null) {
                result = null;
            } else {
                final var url = pv.resolveDocumentRelativePath(documentLocation);
                if (url == null) {
                    result = null;
                } else {
                    try {
                        result = Paths.get(url.toURI());
                    } catch(final FileSystemNotFoundException | URISyntaxException x) {
                        result = null;
                    }
                }
            }
        } else if (pv.isClassLoaderRelativePath()) {
            final var classLoader = p.getFxomDocument().getClassLoader();
            if (classLoader == null) {
                result = null;
            } else {
                final var url = pv.resolveClassLoaderRelativePath(classLoader);
                if (url == null) {
                    result = null;
                } else {
                    try {
                        result = Paths.get(url.toURI());
                    } catch(final URISyntaxException x) {
                        result = null;
                    }
                }
                
            }
        } else {
            result = null;
        }
        
        return result;
    }
    
    private void setUserLibraryPathString() {
        if (getEditorController().getLibrary() instanceof UserLibrary
                && userLibraryPathString == null) {
            userLibraryPathString = ((UserLibrary) getEditorController().getLibrary()).getPath();
            assert userLibraryPathString != null;
        }
    }
    
    private ListView<LibraryListItem> getLibList() {
        if (libList == null) {
            libList = new ListView<>();
        }
        
        return libList;
    }
}
