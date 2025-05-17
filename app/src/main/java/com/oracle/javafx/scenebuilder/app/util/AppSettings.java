/*
 * Copyright (c) 2016, 2024, Gluon and/or its affiliates.
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
 *  - Neither the name of Oracle Corporation and Gluon nor the names of its
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

package com.oracle.javafx.scenebuilder.app.util;

import com.oracle.javafx.scenebuilder.app.SceneBuilderApp;
import com.oracle.javafx.scenebuilder.app.about.AboutWindowController;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import jakarta.json.Json;
import jakarta.json.JsonReaderFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppSettings {

    private static final Logger LOGGER = Logger.getLogger(AppSettings.class.getName());

    public static final String APP_ICON_16 = SceneBuilderApp.class.getResource("SceneBuilderLogo_16.png").toString();
    public static final String APP_ICON_32 = SceneBuilderApp.class.getResource("SceneBuilderLogo_32.png").toString();

    public static final String LATEST_VERSION_CHECK_URL = "http://download.gluonhq.com/scenebuilder/settings.properties";
    public static final String LATEST_VERSION_NUMBER_PROPERTY = "latestversion";

    public static final String LATEST_VERSION_INFORMATION_URL = "http://download.gluonhq.com/scenebuilder/version.json";

    public static final String DOWNLOAD_URL = "https://gluonhq.com/products/scene-builder/";

    private static String sceneBuilderVersion;
    private static String latestVersion;

    private static String latestVersionText;
    private static String latestVersionAnnouncementURL;

    private static final JsonReaderFactory readerFactory = Json.createReaderFactory(null);

    static {
        initSceneBuilderVersion();
    }

    private static void initSceneBuilderVersion() {
        try (final var in = AboutWindowController.class.getResourceAsStream("about.properties")) {
            if (in != null) {
                final var sbProps = new Properties();
                sbProps.load(in);
                sceneBuilderVersion = sbProps.getProperty("build.version", "UNSET");
            }
        } catch (final IOException e) {
            LOGGER.log(Level.WARNING, "Cannot init SB version:", e);
        }
    }

    public static void setWindowIcon(final Alert alert) {
        setWindowIcon((Stage)alert.getDialogPane().getScene().getWindow());
    }
    public static void setWindowIcon(final Stage stage) {
        final var icon16 = new Image(AppSettings.APP_ICON_16);
        final var icon32 = new Image(AppSettings.APP_ICON_32);
        stage.getIcons().addAll(icon16, icon32);
    }

    public static String getSceneBuilderVersion() {
        return sceneBuilderVersion;
    }

    public static boolean isCurrentVersionLowerThan(final String version) {
        final var versionNumbers = version.split("\\.");
        final var currentVersionNumbers = sceneBuilderVersion.split("\\.");
        for (var i = 0; i < versionNumbers.length; ++i) {
            final var number = Integer.parseInt(versionNumbers[i]);
            final var currentVersionNumber = Integer.parseInt(currentVersionNumbers[i]);
            if (number > currentVersionNumber) {
                return true;
            } else if (number < currentVersionNumber) {
                return false;
            }
        }
        return false;
    }

    public static void getLatestVersion(final Consumer<String> consumer) {
        if (latestVersion == null) {
            final var fetchTask = createFetchTask(consumer);
            new Thread(fetchTask, "GetLatestVersion").start();
        } else {
            consumer.accept(latestVersion);
        }
    }

    private static final Task<String> createFetchTask(final Consumer<String> consumer) {
        return new Task<String>() {
            @Override
            protected String call() throws Exception {
                LOGGER.log(Level.FINE, "Fetching latest Scenebuilder version from: {0}", LATEST_VERSION_CHECK_URL);
                final var prop = new Properties();
                String onlineVersionNumber = null;

                URL url = null;
                try {
                    url = new URL(LATEST_VERSION_CHECK_URL);
                } catch (final MalformedURLException e) {
                    LOGGER.log(Level.WARNING, "Failed to construct version check URL: ", e);
                }

                try (final var inputStream = url.openStream()) {
                    prop.load(inputStream);
                    onlineVersionNumber = prop.getProperty(LATEST_VERSION_NUMBER_PROPERTY);

                } catch (final IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to load latest version number property: ", e);
                }
                return onlineVersionNumber;
            }

            protected void succeeded() {
                final var fetchedVersion = getValue();
                LOGGER.log(Level.INFO, "Latest online available version is: {0}", fetchedVersion);
                consumer.accept(fetchedVersion);
                latestVersion = fetchedVersion;
            }
        };
    }

    public static String getLatestVersionText() {
        if (latestVersionText == null) {
            updateLatestVersionInfo();
        }
        return latestVersionText;
    }

    private static void updateLatestVersionInfo() {
        try {
            final var url = new URL(LATEST_VERSION_INFORMATION_URL);

            try (final var reader = readerFactory.createReader(new InputStreamReader(url.openStream()))) {
                final var object = reader.readObject();
                final var announcementObject = object.getJsonObject("announcement");
                latestVersionText = announcementObject.getString("text");
                latestVersionAnnouncementURL = announcementObject.getString("url");
            } catch (final IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read latest version json: ", e);
            }
        } catch (final MalformedURLException e) {
            LOGGER.log(Level.WARNING, "Failed to construct latest version info URL: ", e);
        }
    }

    public static String getLatestVersionAnnouncementURL() {
        if (latestVersionAnnouncementURL == null) {
            updateLatestVersionInfo();
        }
        return latestVersionAnnouncementURL;
    }

    public static String getUserM2Repository() {
        final var m2Path = System.getProperty("user.home") + File.separator +
                           ".m2" + File.separator + "repository"; //NOI18N

        // TODO: Allow custom path for .m2

        assert m2Path != null;

        return m2Path;
    }

}
