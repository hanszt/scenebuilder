/*
 * Copyright (c) 2016, 2022, Gluon and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.app.tracking;

import com.oracle.javafx.scenebuilder.app.util.AppSettings;

import java.io.DataInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tracking {
    public static final String SCENEBUILDER_TYPE = "scenebuilder";
    public static final String SCENEBUILDER_USAGE_TYPE = "scenebuilder-usage";

    public static void sendTrackingInfo(final String type, final String hash, final String email, final boolean optIn, final boolean update) {
        new Thread(() -> {
            try {
                final var java = System.getProperty("java.version");
                final var os = System.getProperty("os.arch") + " " + System.getProperty("os.name") + " " + System.getProperty("os.version");
                final var urlParameters = "email=" + (email == null ? "" : URLEncoder.encode(email, StandardCharsets.UTF_8))
                                          + "&subscribe=" + optIn
                                          + "&os=" + URLEncoder.encode(os, StandardCharsets.UTF_8)
                                          + "&java=" + URLEncoder.encode(java, StandardCharsets.UTF_8)
                                          + "&type=" + type
                                          + "&id=" + hash
                                          + "&version=" + AppSettings.getSceneBuilderVersion()
                                          + (update ? "&update=true" : "");

                final var url = new URL("http://usage.gluonhq.com/ul/log?" + urlParameters);

                final var conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setUseCaches(false);
                conn.connect();
                try (final var in = new DataInputStream(conn.getInputStream())) {
                    while (in.read() > -1) {
                    }
                }
            } catch (final Exception e) {
                Logger.getLogger(Tracking.class.getName()).log(Level.WARNING, "Failed to send tracking info: ", e);
            }
        }, "UserRegistrationThread").start();
    }
}
