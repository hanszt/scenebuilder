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
package com.oracle.javafx.scenebuilder.kit.library;

import com.oracle.javafx.scenebuilder.kit.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtils;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ServiceLoader;

import javafx.scene.layout.Region;

/**
 *
 * @treatAsPrivate
 */
public class BuiltinLibrary extends Library {

    // In SB 1.1 the section names of the Library have been localized. We assume
    // for now we stick to this approach, but fact is the support of custom
    // sections could change the rules of the game.
    public static final String TAG_CONTAINERS     = "Containers"; //NOI18N
    public static final String TAG_CONTROLS       = "Controls"; //NOI18N
    public static final String TAG_MENU           = "Menu"; //NOI18N
    public static final String TAG_MISCELLANEOUS  = "Miscellaneous"; //NOI18N
    public static final String TAG_SHAPES         = "Shapes"; //NOI18N
    public static final String TAG_CHARTS         = "Charts"; //NOI18N
    public static final String TAG_3D             = "3D"; //NOI18N

    
    private static BuiltinLibrary library = null;
    
    private final BuiltinSectionComparator sectionComparator
            = new BuiltinSectionComparator();
    
    // This qualifier is for use to provide a flavor of a component that has no
    // children, in addition to the one having children. Typical use is for
    // Accordion, ScrollPane, SplitPane, TabPane, TitledPane (see DTL-6274).
    private static final String EMPTY_QUALIFIER = " " //NOI18N
            + I18N.getString("label.qualifier.empty");
    
    private static final String HORIZONTAL_QUALIFIER = " " //NOI18N
            + I18N.getString("label.qualifier.horizontal");
    private static final String VERTICAL_QUALIFIER = " " //NOI18N
            + I18N.getString("label.qualifier.vertical");
    
    /*
     * Public
     */
    
    public static synchronized BuiltinLibrary getLibrary() {
        if (library == null) {
            library = new BuiltinLibrary();
        }
        return library;
    }

    public static String getEmptyQualifier() {
        return EMPTY_QUALIFIER;
    }

    /**
     * Creates the FXML content to import a given class as follows:
     * <pre>{@code
     *     <?xml version="1.0" encoding="UTF-8"?> //NOI18N
     *     <?import a.b.C?>
     *     <C/>
     * }</pre>
     *
     * @param componentClass the class
     * @return a String with the FXML content
     */
    public static String makeFxmlText(final Class<?> componentClass) {
        final var sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //NOI18N
        sb.append("<?import "); //NOI18N
        sb.append(componentClass.getCanonicalName());
        sb.append("?>"); //NOI18N
        sb.append("<"); //NOI18N
        sb.append(componentClass.getSimpleName());
        sb.append("/>\n"); //NOI18N

        return sb.toString();
    }

    /*
     * Library
     */
    
    @Override
    public Comparator<String> getSectionComparator() {
        return sectionComparator;
    }
    
    /*
     * Debug
     */
    
    public static void main(final String[] args) {
        getLibrary();
    }
    
    /*
     * Private
     */
    
    private BuiltinLibrary() {
        // Containers
        addCustomizedItem(javafx.scene.control.Accordion.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.Accordion.class, TAG_CONTAINERS,
                "AccordionEmpty", "Accordion", EMPTY_QUALIFIER); //NOI18N
        addRegionItem200x200(javafx.scene.layout.AnchorPane.class, TAG_CONTAINERS);
        addRegionItem200x200(javafx.scene.layout.BorderPane.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.ButtonBar.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.DialogPane.class, TAG_CONTAINERS);
        addDefaultItem(javafx.scene.control.DialogPane.class, TAG_CONTAINERS, EMPTY_QUALIFIER);
        addRegionItem200x200(javafx.scene.layout.FlowPane.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.layout.GridPane.class, TAG_CONTAINERS);
        addRegionItem200x100(javafx.scene.layout.HBox.class, TAG_CONTAINERS);
        addRegionItem200x200(javafx.scene.layout.Pane.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.ScrollPane.class, TAG_CONTAINERS);
        addRegionItem200x200(javafx.scene.control.ScrollPane.class, TAG_CONTAINERS, EMPTY_QUALIFIER);
        addCustomizedItem(javafx.scene.control.SplitPane.class, TAG_CONTAINERS,
                "SplitPaneH", "SplitPane-h", HORIZONTAL_QUALIFIER); //NOI18N
        addCustomizedItem(javafx.scene.control.SplitPane.class, TAG_CONTAINERS,
                "SplitPaneV", "SplitPane-v", VERTICAL_QUALIFIER); //NOI18N
        addRegionItem200x200(javafx.scene.control.SplitPane.class, TAG_CONTAINERS, EMPTY_QUALIFIER,
                "SplitPane-h"); //NOI18N
        addRegionItem200x150(javafx.scene.layout.StackPane.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.Tab.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.TabPane.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.TabPane.class, TAG_CONTAINERS,
                "TabPaneEmpty", "TabPane", EMPTY_QUALIFIER); //NOI18N
        addRegionItem200x200(javafx.scene.text.TextFlow.class, TAG_CONTAINERS);
        addRegionItem200x200(javafx.scene.layout.TilePane.class, TAG_CONTAINERS);
        addCustomizedItem(javafx.scene.control.TitledPane.class, TAG_CONTAINERS);
        addRegionItem200x200(javafx.scene.control.TitledPane.class, TAG_CONTAINERS, EMPTY_QUALIFIER);
        addCustomizedItem(javafx.scene.control.ToolBar.class, TAG_CONTAINERS);
        addRegionItem100x200(javafx.scene.layout.VBox.class, TAG_CONTAINERS);
        
        // Controls
        addCustomizedItem(javafx.scene.control.Button.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.CheckBox.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.ChoiceBox.class, TAG_CONTROLS);
        addDefaultItem(javafx.scene.control.ColorPicker.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.ComboBox.class, TAG_CONTROLS);
        addDefaultItem(javafx.scene.control.DatePicker.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.web.HTMLEditor.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.Hyperlink.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.image.ImageView.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.Label.class, TAG_CONTROLS);
        addRegionItem200x200(javafx.scene.control.ListView.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.media.MediaView.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.MenuBar.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.MenuButton.class, TAG_CONTROLS);
        addRegionItem200x200(javafx.scene.control.Pagination.class, TAG_CONTROLS);
        addDefaultItem(javafx.scene.control.PasswordField.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.ProgressBar.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.ProgressIndicator.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.RadioButton.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.ScrollBar.class, TAG_CONTROLS,
                "ScrollBarH", "ScrollBar-h", HORIZONTAL_QUALIFIER); //NOI18N
        addCustomizedItem(javafx.scene.control.ScrollBar.class, TAG_CONTROLS,
                "ScrollBarV", "ScrollBar-v", VERTICAL_QUALIFIER); //NOI18N
        addCustomizedItem(javafx.scene.control.Separator.class, TAG_CONTROLS,
                "SeparatorH", "Separator-h", HORIZONTAL_QUALIFIER); //NOI18N
        addCustomizedItem(javafx.scene.control.Separator.class, TAG_CONTROLS,
                "SeparatorV", "Separator-v", VERTICAL_QUALIFIER); //NOI18N
        addCustomizedItem(javafx.scene.control.Slider.class, TAG_CONTROLS,
                "SliderH", "Slider-h", HORIZONTAL_QUALIFIER); //NOI18N
        addCustomizedItem(javafx.scene.control.Slider.class, TAG_CONTROLS,
                "SliderV", "Slider-v", VERTICAL_QUALIFIER); //NOI18N
        addDefaultItem(javafx.scene.control.Spinner.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.SplitMenuButton.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.TableColumn.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.TableView.class, TAG_CONTROLS);
        addRegionItem200x200(javafx.scene.control.TextArea.class, TAG_CONTROLS);
        addDefaultItem(javafx.scene.control.TextField.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.ToggleButton.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.TreeTableColumn.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.control.TreeTableView.class, TAG_CONTROLS);
        addRegionItem200x200(javafx.scene.control.TreeView.class, TAG_CONTROLS);
        addCustomizedItem(javafx.scene.web.WebView.class, TAG_CONTROLS);
        
        // Menu
        addCustomizedItem(javafx.scene.control.CheckMenuItem.class, TAG_MENU);
        addCustomizedItem(javafx.scene.control.ContextMenu.class, TAG_MENU);
        addCustomizedItem(javafx.scene.control.CustomMenuItem.class, TAG_MENU);
        addCustomizedItem(javafx.scene.control.Menu.class, TAG_MENU);
        addCustomizedItem(javafx.scene.control.MenuItem.class, TAG_MENU);
        addCustomizedItem(javafx.scene.control.RadioMenuItem.class, TAG_MENU);
        addCustomizedItem(javafx.scene.control.SeparatorMenuItem.class, TAG_MENU);
        
        // Miscellaneous
        addCustomizedItem(javafx.scene.canvas.Canvas.class, TAG_MISCELLANEOUS);
        addDefaultItem(javafx.scene.Group.class, TAG_MISCELLANEOUS);
        addRegionItem200x200(javafx.scene.layout.Region.class, TAG_MISCELLANEOUS);
        addCustomizedItem(javafx.scene.Scene.class, TAG_MISCELLANEOUS);
        addCustomizedItem(javafx.stage.Stage.class, TAG_MISCELLANEOUS);
        addCustomizedItem(javafx.scene.SubScene.class, TAG_MISCELLANEOUS);
        addDefaultItem(javafx.embed.swing.SwingNode.class, TAG_MISCELLANEOUS);
        addCustomizedItem(javafx.scene.control.Tooltip.class, TAG_MISCELLANEOUS);

        // Shapes
        addCustomizedItem(javafx.scene.shape.Arc.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.ArcTo.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Box.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Circle.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.ClosePath.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.CubicCurve.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.CubicCurveTo.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Cylinder.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Ellipse.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.HLineTo.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Line.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.LineTo.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.MeshView.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.MoveTo.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Path.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Polygon.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Polyline.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.QuadCurve.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.QuadCurveTo.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Rectangle.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.Sphere.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.shape.SVGPath.class, TAG_SHAPES);
        addCustomizedItem(javafx.scene.text.Text.class, TAG_SHAPES);
        addDefaultItem(javafx.scene.shape.VLineTo.class, TAG_SHAPES);
        
        // Charts
        addCustomizedItem(javafx.scene.chart.AreaChart.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.chart.BarChart.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.chart.BubbleChart.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.chart.LineChart.class, TAG_CHARTS);
        addDefaultItem(javafx.scene.chart.PieChart.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.chart.ScatterChart.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.chart.StackedAreaChart.class, TAG_CHARTS);
        addCustomizedItem(javafx.scene.chart.StackedBarChart.class, TAG_CHARTS);
        
        // 3D
        addCustomizedItem(javafx.scene.AmbientLight.class, TAG_3D);
        addDefaultItem(javafx.scene.ParallelCamera.class, TAG_3D);
        addDefaultItem(javafx.scene.PerspectiveCamera.class, TAG_3D);
        addCustomizedItem(javafx.scene.PointLight.class, TAG_3D);

        // External items
        addExternalItems();
    }
    
    
    private void addDefaultItem(final Class<?> componentClass, final String section, final String... qualifiers) {
        final var name = componentClass.getSimpleName();
        final var nameWithQualifier = new StringBuilder(name);
        for (final var qualifier : qualifiers) {
            nameWithQualifier.append(qualifier);
        }
        final var fxmlText = makeFxmlText(componentClass);
        addItem(nameWithQualifier.toString(), fxmlText, section, name);
    }

    private void addRegionItem200x200(final Class<? extends Region> componentClass, final String section) {
        addRegionItem200x200(componentClass, section, null);
    }
    
    private void addRegionItem200x200(final Class<? extends Region> componentClass, final String section, final String qualifier) {
        addRegionItem200x200(componentClass, section, qualifier, null);
    }
    
    private void addRegionItem200x200(final Class<? extends Region> componentClass, final String section, final String qualifier, final String iconName) {
        final var name = componentClass.getSimpleName();
        final var nameWithQualifier = new StringBuilder(name);
        if (qualifier != null) {
            nameWithQualifier.append(qualifier);
        }
        final var fxmlText = makeRegionFxmlText(componentClass, 200.0, 200.0);
        var theIconName = name;
        if (iconName != null) {
            theIconName = iconName;
        }
        addItem(nameWithQualifier.toString(), fxmlText, section, theIconName);
    }
    
    
    private void addRegionItem200x100(final Class<? extends Region> componentClass, final String section) {
        final var name = componentClass.getSimpleName();
        final var fxmlText = makeRegionFxmlText(componentClass, 200.0, 100.0);
        addItem(name, fxmlText, section, name);
    }
    
    
    private void addRegionItem200x150(final Class<? extends Region> componentClass, final String section) {
        final var name = componentClass.getSimpleName();
        final var fxmlText = makeRegionFxmlText(componentClass, 200.0, 150.0);
        addItem(name, fxmlText, section, name);
    }
    
    
    private void addRegionItem100x200(final Class<? extends Region> componentClass, final String section) {
        final var name = componentClass.getSimpleName();
        final var fxmlText = makeRegionFxmlText(componentClass, 100.0, 200.0);
        addItem(name, fxmlText, section, name);
    }
    
    
    private void addCustomizedItem(final Class<?> componentClass, final String section) {
        addCustomizedItem(componentClass, section, null);
    }
    
    private void addCustomizedItem(final Class<?> componentClass, final String section, final String qualifier) {
        final var name = componentClass.getSimpleName();
        addCustomizedItem(componentClass, section, name, name, qualifier);
    }
    
    private void addCustomizedItem(final Class<?> componentClass, final String section,
                                   final String fxmlBaseName, final String iconName, final String qualifier) {
        var nameWithQualifier = componentClass.getSimpleName();
        if (qualifier != null) {
            nameWithQualifier += qualifier;
        }
        final var fxmlText = readCustomizedFxmlText(fxmlBaseName, componentClass);
        assert fxmlText != null;
        addItem(nameWithQualifier, fxmlText, section, iconName);
    }
    
    
    private void addItem(final String name, final String fxmlText, final String section, final String iconName) {
        final var iconURL = ImageUtils.getNodeIconURL(iconName + ".png"); //NOI18N
        final var item = new LibraryItem(name, section, fxmlText, iconURL, this);
        getItems().add(item);
    }

    private void addExternalItems() {
        final var providers = getExternalItemProviders();
        final var orderedSections = BuiltinSectionComparator.getOrderedSections();
        for (final var provider : providers) {
            for (final var item : provider.getExternalSectionItems()) {
                final var nameWithQualifier = item.getSimpleName();
                final var resourceUrl = provider.getClass().getResource(provider.getItemsFXMLPath() + "/" + nameWithQualifier + ".fxml");
                assert resourceUrl != null;
                final var fxmlText = readFxmlURL(resourceUrl);
                assert fxmlText != null;
                final var iconURL = provider.getClass().getResource(provider.getItemsIconPath() + "/" + nameWithQualifier + ".png");
                assert iconURL != null;
                final var libraryItem = new LibraryItem(nameWithQualifier, provider.getExternalSectionName(), fxmlText, iconURL, this);
                getItems().add(libraryItem);
            }
            final var position = provider.getExternalSectionPosition();
            if (position < 0 || position >= orderedSections.size()) {
                orderedSections.add(provider.getExternalSectionName());
            } else {
                orderedSections.add(provider.getExternalSectionPosition(), provider.getExternalSectionName());
            }
        }
    }

    private Collection<ExternalSectionProvider> getExternalItemProviders() {
        final var loader = ServiceLoader.load(ExternalSectionProvider.class);
        final Collection<ExternalSectionProvider> providers = new ArrayList<>();
        loader.iterator().forEachRemaining(providers::add);
        return providers;
    }

    private static String makeRegionFxmlText(final Class<? extends Region> componentClass,
                                             final double pw, final double ph) {
        final var sb = new StringBuilder();
        
        /*
         * <?xml version="1.0" encoding="UTF-8"?> //NOI18N
         * 
         * <?import a.b.C?>
         * 
         * <C prefWidth="pw" prefHeight="ph"/> //NOI18N
         */
        
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //NOI18N
        
        sb.append("<?import "); //NOI18N
        sb.append(componentClass.getCanonicalName());
        sb.append("?>"); //NOI18N
        sb.append("<"); //NOI18N
        sb.append(componentClass.getSimpleName());
        if (pw != Region.USE_COMPUTED_SIZE) {
            sb.append(" prefWidth=\""); //NOI18N
            sb.append(pw);
            sb.append("\""); //NOI18N
        }
        if (ph != Region.USE_COMPUTED_SIZE) {
            sb.append(" prefHeight=\""); //NOI18N
            sb.append(ph);
            sb.append("\""); //NOI18N
        }
        sb.append(" />\n"); //NOI18N
        
        return sb.toString();
    }

    private String readCustomizedFxmlText(final String fxmlBaseName, final Class<?> componentClass) {
        final var fxmlPath = "builtin/" + fxmlBaseName + ".fxml"; //NOI18N
        final var fxmlURL = BuiltinLibrary.class.getResource(fxmlPath);
        assert fxmlURL != null : "fxmlBaseName=" + fxmlBaseName; //NOI18N
        return readFxmlURL(fxmlURL);
    }

    private String readFxmlURL(final URL fxmlURL) {
        final String result;

        try {
            result = FXOMDocument.readContentFromURL(fxmlURL);
        } catch (final IOException x) {
            throw new IllegalStateException("Bug in " + getClass().getSimpleName(), x); //NOI18N
        } catch (final NullPointerException ex) {
            System.out.println("fxmlURL =  " + fxmlURL);
            throw ex;
        }
        return result;
    }
}
