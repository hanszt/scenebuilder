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
package com.oracle.javafx.scenebuilder.kit.library.user;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtils;
import com.oracle.javafx.scenebuilder.kit.editor.panel.library.LibraryUtil;
import com.oracle.javafx.scenebuilder.kit.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.library.BuiltinLibrary;
import com.oracle.javafx.scenebuilder.kit.library.LibraryItem;
import com.oracle.javafx.scenebuilder.kit.library.util.FolderExplorer;
import com.oracle.javafx.scenebuilder.kit.library.util.JarExplorer;
import com.oracle.javafx.scenebuilder.kit.library.util.JarReport;
import com.oracle.javafx.scenebuilder.kit.library.util.JarReportEntry;
import com.oracle.javafx.scenebuilder.kit.library.util.ModuleExplorer;

/**
 *
 * 
 */
class LibraryFolderWatcher implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(LibraryFolderWatcher.class.getSimpleName());

    private final UserLibrary library;

    private enum FILE_TYPE {FXML, JAR, FOLDER_MARKER}

    private static final List<String> JAVAFX_MODULES = Arrays.asList(
            "javafx-base", "javafx-graphics", "javafx-controls",
            "javafx-fxml", "javafx-media", "javafx-web", "javafx-swing");

    public LibraryFolderWatcher(final UserLibrary library) {
        this.library = library;
    }

    /*
     * Runnable
     */
    
    @Override
    public void run() {
        
        try {
            library.updateExplorationCount(0);
            library.updateExplorationDate(new Date());
            runDiscovery();
            runWatching();
        } catch(final InterruptedException x) {
            // Let's stop: Typically, when UserLibrary::stopWatching is invoked, an InterruptedException is triggered to stop this watch service
        }
    }
    
    
    /*
     * Private
     */
    private void runDiscovery() throws InterruptedException {
        // First put the builtin items in the library
        library.setItems(BuiltinLibrary.getLibrary().getItems());

        // Attempts to add the maven jars, including dependencies
        final var additionalJars = library.getAdditionalJarPaths().get();
        
        final Set<Path> currentJarsOrFolders = new HashSet<>(additionalJars);
        final Set<Path> currentFxmls = new HashSet<>();
                
        // Now attempts to discover the user library folder
        final var folder = Paths.get(library.getPath());
        if (folder != null && folder.toFile().exists()) {
            boolean retry;
            do {
                try (final var stream = Files.newDirectoryStream(folder)) {
                    for (final var entry: stream) {
                        if (LibraryUtil.isJarPath(entry)) {
                            currentJarsOrFolders.add(entry);
                        } else if (LibraryUtil.isFxmlPath(entry)) {
                            currentFxmls.add(entry);
                        } else if (LibraryUtil.isFolderMarkerPath(entry)) {
                            // open folders marker file: every line should be a single folder entry
                            // we scan the file and add the path to currentJarsOrFolders
                            final var folderPaths = LibraryUtil.getFolderPaths(entry);
                            for (final var f : folderPaths) {
                                currentJarsOrFolders.add(f);
                            }
                        }
                    }
                    retry = false;
                } catch(final IOException x) {
                    Thread.sleep(2000 /* ms */);
                    retry = true;
                } finally {
                    library.updateExplorationCount(library.getExplorationCount()+1);
                }
            }
            while (retry && library.getExplorationCount() < 10);
        }
        try {
            library.setExploring(true);
            try {
                updateLibrary(currentFxmls);
                exploreAndUpdateLibrary(currentJarsOrFolders);
            }
            finally {
                library.setExploring(false);
            }
        } catch(final IOException x) { }
    }
    
    private void runWatching() throws InterruptedException {
        WatchService watchService = null;
        try {
            while (true) {
                final var folder = Paths.get(library.getPath());

                while (watchService == null) {
                    try {
                        watchService = folder.getFileSystem().newWatchService();
                    } catch(final IOException x) {
                        System.out.println("FileSystem.newWatchService() failed"); //NOI18N
                        System.out.println("Sleeping..."); //NOI18N
                        Thread.sleep(1000 /* ms */);
                    }
                }

                WatchKey watchKey = null;
                while ((watchKey == null) || (!watchKey.isValid())) {
                    try {
                        watchKey = folder.register(watchService, 
                                StandardWatchEventKinds.ENTRY_CREATE, 
                                StandardWatchEventKinds.ENTRY_DELETE, 
                                StandardWatchEventKinds.ENTRY_MODIFY);

                        WatchKey wk;
                        do {
                            wk = watchService.take();
                            assert wk == watchKey;

                            var isDirty = false;
                            for (final var e: wk.pollEvents()) {
                                final var kind = e.kind();
                                final var context = e.context();

                                if (kind == StandardWatchEventKinds.ENTRY_CREATE
                                        || kind == StandardWatchEventKinds.ENTRY_DELETE
                                        || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                    assert context instanceof Path;
                                    if (LibraryUtil.isJarPath((Path) context)) {
                                        if (!hasJarBeenAdded((Path) context)) {
                                            isDirty = true;
                                        }
                                    } else if (LibraryUtil.isFxmlPath((Path)context)){
                                        isDirty = true;
                                    } else if (LibraryUtil.isFolderMarkerPath((Path)context)) {
                                        isDirty = true;
                                    }
                                } else {
                                    assert kind == StandardWatchEventKinds.OVERFLOW;
                                }
                            }

                            // We reconstruct a full set from scratch as soon as the
                            // dirty flag is set.
                            if (isDirty) {
                                // First put the builtin items in the library
                                library.setExploring(true);
                                try {
                                    library.setItems(BuiltinLibrary.getLibrary().getItems());

                                    // Now attempts to add the maven jars
                                    final var currentMavenJars = library.getAdditionalJarPaths().get();

                                    final Set<Path> fxmls = new HashSet<>();
                                    fxmls.addAll(getAllFiles(FILE_TYPE.FXML));
                            	    updateLibrary(fxmls);

                                    final Set<Path> jarsAndFolders = new HashSet<>(currentMavenJars);
                                    jarsAndFolders.addAll(getAllFiles(FILE_TYPE.JAR));

                                    final var foldersMarkers = getAllFiles(FILE_TYPE.FOLDER_MARKER);
                                    for (final var path : foldersMarkers) {
                                        // open folders marker file: every line should be a single folder entry
                                        // we scan the file and add the path to currentJarsOrFolders
                                        final var folderPaths = LibraryUtil.getFolderPaths(path);
                                        for (final var f : folderPaths) {
                                            jarsAndFolders.add(f);
                                        }
                                    }

                            	    exploreAndUpdateLibrary(jarsAndFolders);

                                    library.updateExplorationCount(library.getExplorationCount()+1);
                                }
                                finally {
                                    library.setExploring(false);
                                }
                            }
                        } while (wk.reset());
                    } catch(final IOException x) {
                        Thread.sleep(1000 /* ms */);
                    }
                }

            }
        }
        finally {
        	// Typically, when UserLibrary::stopWatching is invoked, an InterruptedException is triggered to stop this watch service.
        	// The InterruptedException is handled outside; here we just make sure to close() the watcher service that polls the filesystem.
            if (watchService != null) {
                // we need to close the filesystem watcher here, otherwise it remains active and locking jars on library folder
                try {
                    watchService.close();
                } catch (final IOException e) {
                	LOGGER.severe("Error closing FileSystemWatchService: " + e.getMessage());
                }
            }
        }
    }

    private Set<Path> getAllFiles(final FILE_TYPE fileType) throws IOException {
        final Set<Path> res = new HashSet<>();
        final var folder = Paths.get(library.getPath());

        try (final var ds = Files.newDirectoryStream(folder)) {
            for (final var p : ds) {
                switch (fileType) {
                    case FXML:
                        if (LibraryUtil.isFxmlPath(p)) {
                            res.add(p);
                        }
                        break;
                    case JAR:
                        if (LibraryUtil.isJarPath(p)) {
                            res.add(p);
                        }
                        break;
                    case FOLDER_MARKER:
                        if (LibraryUtil.isFolderMarkerPath(p)) {
                            res.add(p);
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        return res;
    }
    
    
    private boolean hasJarBeenAdded(final Path context) {
        var hasJarBeenAdded = false;
        for (final var report : library.getJarReports()) {
            if (report.getJar().getFileName().equals(context)) {
                hasJarBeenAdded = true;
                break;
            }
        }
        return hasJarBeenAdded;
    }


    private void updateLibrary(final Collection<Path> paths) throws IOException {
        final List<LibraryItem> newItems = new ArrayList<>();
        
        for (final var path : paths) {
            newItems.add(makeLibraryItem(path));
        }

        library.addItems(newItems);
        library.updateFxmlFileReports(paths);
        library.updateExplorationDate(new Date());
    }
    
    
    private LibraryItem makeLibraryItem(final Path path) throws IOException {
        final var iconURL = ImageUtils.getNodeIconURL(null);
        final var fileName = path.getFileName().toString();
        final var itemName = fileName.substring(0, fileName.indexOf(".fxml")); //NOI18N
        var fxmlText = ""; //NOI18N
        final var buf = new StringBuilder();

        try (final var reader = new LineNumberReader(new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8))) { //NOI18N
            String line;
            while ((line = reader.readLine()) != null) {
                buf.append(line).append("\n"); //NOI18N
            }
            
            fxmlText = buf.toString();
        }

        final var res = new LibraryItem(itemName, UserLibrary.TAG_USER_DEFINED, fxmlText, iconURL, library);
        return res;
    }
    
    
    private void exploreAndUpdateLibrary(final Collection<Path> modulesOrJarsOrFolders) throws IOException {

        //  1) we create a classloader
        //  2) we explore all the modules, jars, and folders
        //  3) we construct a list of library items
        //  4) we update the user library with the class loader and items
        //  5) on startup only, we allow opening files that may/may not rely on the user library

        // 1)
        final ClassLoader classLoader;
        if (modulesOrJarsOrFolders.isEmpty()) {
            classLoader = null;
        } else {
            classLoader = new URLClassLoader(makeURLArrayFromPaths(modulesOrJarsOrFolders));
        }

        // 2)
        final List<JarReport> moduleOrJarOrFolderReports = new ArrayList<>();
        for (final var currentModuleOrJarOrFolder : modulesOrJarsOrFolders) {
            final var jarName = currentModuleOrJarOrFolder.getName(currentModuleOrJarOrFolder.getNameCount() - 1).toString();
            if (JAVAFX_MODULES.stream().anyMatch(jarName::startsWith)) {
                continue;
            }

            final JarReport jarReport;
            var resultText = "";
            final var moduleReference = LibraryUtil.getModuleReference(currentModuleOrJarOrFolder);
            if (moduleReference.isPresent()) {
                LOGGER.info(I18N.getString("log.info.explore.module", moduleReference.get().descriptor()));
                final var explorer = new ModuleExplorer(moduleReference.get());
                jarReport = explorer.explore();
                resultText = I18N.getString("log.info.explore.module.results", jarName);
            }
            else if (LibraryUtil.isJarPath(currentModuleOrJarOrFolder)) {
                LOGGER.info(I18N.getString("log.info.explore.jar", currentModuleOrJarOrFolder));
                final var explorer = new JarExplorer(currentModuleOrJarOrFolder);
                jarReport = explorer.explore(classLoader);
                resultText = I18N.getString("log.info.explore.jar.results", jarName);
            }
            else if (Files.isDirectory(currentModuleOrJarOrFolder)) {
                LOGGER.info(I18N.getString("log.info.explore.folder", currentModuleOrJarOrFolder));
                final var explorer = new FolderExplorer(currentModuleOrJarOrFolder);
                jarReport = explorer.explore(classLoader);
                resultText = I18N.getString("log.info.explore.folder.results", jarName);
            } else {
                continue;
            }

            moduleOrJarOrFolderReports.add(jarReport);

            final var sb = new StringBuilder(resultText).append("\n");
            if (jarReport.getEntries().isEmpty()) {
                sb.append("> ").append(I18N.getString("log.info.explore.no.results"));
            } else {
                jarReport.getEntries().forEach(entry -> sb.append("> ").append(entry.toString()).append("\n"));
            }
            LOGGER.info(sb.toString());

            LOGGER.info(I18N.getString("log.info.explore.end", currentModuleOrJarOrFolder));
        }

        // 3)
        final List<LibraryItem> newItems = new ArrayList<>();
        for (final var moduleOrJarOrFolderReport : moduleOrJarOrFolderReports) {
            newItems.addAll(makeLibraryItems(moduleOrJarOrFolderReport));
        }

        // 4)
        library.updateClassLoader(classLoader);
        // Remove duplicated items
        library.addItems(newItems
                .stream()
                .distinct()
                .collect(Collectors.toList()));
        library.updateJarReports(new ArrayList<>(moduleOrJarOrFolderReports));
        library.getOnFinishedUpdatingJarReports().accept(moduleOrJarOrFolderReports);
        library.updateExplorationDate(new Date());
        
        // 5
        // Fix for #45: mark end of first exploration
        library.updateFirstExplorationCompleted();
    }
    
    
    private Collection<LibraryItem> makeLibraryItems(final JarReport jarOrFolderReport) throws IOException {
        final List<LibraryItem> result = new ArrayList<>();
        final var iconURL = ImageUtils.getNodeIconURL(null);
        final var excludedItems = library.getFilter();
        final var artifactsFilter = library.getAdditionalFilter().get();
                
        for (final var e : jarOrFolderReport.getEntries()) {
            if ((e.getStatus() == JarReportEntry.Status.OK) && e.isNode()) {
                // We filter out items listed in the excluded list, based on canonical name of the class.
                final var canonicalName = e.getKlass().getCanonicalName();
                if (!excludedItems.contains(canonicalName) && 
                    !artifactsFilter.contains(canonicalName)) {
                    final var name = e.getKlass().getSimpleName();
                    final var fxmlText = BuiltinLibrary.makeFxmlText(e.getKlass());
                    result.add(new LibraryItem(name, UserLibrary.TAG_USER_DEFINED, fxmlText, iconURL, library));
                }
            }
        }
        
        return result;
    }
    
    
    private URL[] makeURLArrayFromPaths(final Collection<Path> paths) {
        final var result = new URL[paths.size()];
        var i = 0;
        for (final var p : paths) {
            try {
                final var url = p.toUri().toURL();
                if (url.toString().endsWith(".jar")) {
                    result[i++] = new URL("jar", "", url + "!/"); // <-- jar:file/path/to/jar!/
                } else {
                    result[i++] = url; // <-- file:/path/to/folder/ or jrt:/module.name
                }
            } catch (final MalformedURLException x) {
                throw new RuntimeException("Bug in " + getClass().getSimpleName(), x); //NOI18N
            }
        }
        
        return result;
    }
}
