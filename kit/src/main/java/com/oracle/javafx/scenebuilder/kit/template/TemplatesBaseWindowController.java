/*
 * Copyright (c) 2017, 2022, Gluon and/or its affiliates.
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

package com.oracle.javafx.scenebuilder.kit.template;

import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlWindowController;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class TemplatesBaseWindowController extends AbstractFxmlWindowController {

    private Consumer<Template> onTemplateChosen = template -> {};

    public TemplatesBaseWindowController(final URL fxmlURL, final ResourceBundle resources, final Stage owner) {
        super(fxmlURL, resources, owner);
    }

    @Override
    public void onCloseRequest(final WindowEvent event) {
        getStage().hide();
    }

    /*
     * AbstractWindowController
     */
    @Override
    protected void controllerDidCreateStage() {
        assert getRoot() != null;
        assert getRoot().getScene() != null;
        assert getRoot().getScene().getWindow() != null;
    }

    @Override
    protected void controllerDidLoadFxml() {
        super.controllerDidLoadFxml();

        initTemplates();
    }

    /**
     * Dynamically generates templates and adds them to fxml-pre-defined [desktopPane] and [mobilePane].
     */
    private void initTemplates() {
        final FlowPane desktopPane;
        final FlowPane mobilePane;

        try {
            final var templatesRoot = (ScrollPane) getRoot().lookup("#templatesRoot");
            desktopPane = (FlowPane) templatesRoot.getContent().lookup("#desktopPane");
            mobilePane = (FlowPane) templatesRoot.getContent().lookup("#mobilePane");
        } catch (final Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to lookup() [desktopPane] and [mobilePane]:", e);
            return;
        }

        for (final var template : Template.values()) {

            // this "try" is on a per template basis so that a malformed template doesn't crash valid ones
            try {
                final VBox btnRoot = FXMLLoader.load(TemplatesBaseWindowController.class.getResource("Template.fxml"));
                final var button = (Button) btnRoot.lookup("#button");
                button.setText(template.getUiName());
                button.setOnAction(e -> onTemplateChosen.accept(template));

                final var view = (ImageView) button.getGraphic();
                view.setImage(template.getImage());

                final var label = (Label) btnRoot.lookup("#labelDescription");
                label.setText(template.getDescription());

                if (template.isDesktop()) {
                    desktopPane.getChildren().add(btnRoot);
                } else {
                    mobilePane.getChildren().add(btnRoot);
                }
            } catch (final Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to load template: " + template, e);

                // do not rethrow since SB is completely functional without templates
            }
        }
    }

    public void setOnTemplateChosen(final Consumer<Template> onTemplateChosen) {
        this.onTemplateChosen = onTemplateChosen;
    }
}

