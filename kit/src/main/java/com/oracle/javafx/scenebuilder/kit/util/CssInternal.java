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
package com.oracle.javafx.scenebuilder.kit.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.FXCollections;
import javafx.css.CssMetaData;
import javafx.css.StyleOrigin;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.Parent;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform.Theme;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import javafx.css.Rule;
import javafx.css.Style;
import javafx.css.Stylesheet;
import javafx.css.CssParser;
import javafx.scene.control.Button;

/**
 *
 * Utility classes to extract CSS information.
 * Note: Requires JavaFX 23+
 *
 */
public class CssInternal {

    CssInternal() {
        // no-op
    }

    private final static List<String> themeUrls;
    static {
        themeUrls = new ArrayList<>(Theme.CASPIAN_EMBEDDED_HIGH_CONTRAST.getStylesheetURLs());
        themeUrls.addAll(Theme.CASPIAN_EMBEDDED_QVGA_HIGH_CONTRAST.getStylesheetURLs());
        themeUrls.addAll(Theme.CASPIAN_EMBEDDED_QVGA.getStylesheetURLs());
        themeUrls.addAll(Theme.CASPIAN_EMBEDDED.getStylesheetURLs());
        themeUrls.addAll(Theme.CASPIAN_HIGH_CONTRAST.getStylesheetURLs());
        themeUrls.addAll(Theme.CASPIAN.getStylesheetURLs());
        themeUrls.addAll(Theme.MODENA_HIGH_CONTRAST_BLACK_ON_WHITE.getStylesheetURLs());
        themeUrls.addAll(Theme.MODENA_HIGH_CONTRAST_WHITE_ON_BLACK.getStylesheetURLs());
        themeUrls.addAll(Theme.MODENA_HIGH_CONTRAST_YELLOW_ON_BLACK.getStylesheetURLs());
        themeUrls.addAll(Theme.MODENA.getStylesheetURLs());
        themeUrls.addAll(Theme.MODENA_TOUCH_HIGH_CONTRAST_BLACK_ON_WHITE.getStylesheetURLs());
        themeUrls.addAll(Theme.MODENA_TOUCH_HIGH_CONTRAST_WHITE_ON_BLACK.getStylesheetURLs());
        themeUrls.addAll(Theme.MODENA_TOUCH_HIGH_CONTRAST_YELLOW_ON_BLACK.getStylesheetURLs());
        themeUrls.addAll(Theme.MODENA_TOUCH.getStylesheetURLs());
    }

    /**
     * Check if the input style is from a theme stylesheet (caspian or modena).
     *
     * @param style style to be checked
     * @return true if the style is from a theme css.
     */
    public static boolean isThemeStyle(final Style style) {
        return isThemeRule(style.getDeclaration().getRule());
    }

    public static boolean isCaspianTheme(final Style style) {
        return style.getDeclaration().getRule().getStylesheet().getUrl()
                .endsWith(Theme.CASPIAN.getStylesheetURLs().getFirst());
    }

    public static boolean isModenaTheme(final Style style) {
        return style.getDeclaration().getRule().getStylesheet().getUrl()
                .endsWith(Theme.MODENA.getStylesheetURLs().getFirst());
    }

    public static String getThemeDisplayName(final Style style) {
        var themeName = ""; //NOI18N
        final var url = style.getDeclaration().getRule().getStylesheet().getUrl();
        if (url.contains("modena")) {//NOI18N
            themeName += "modena/"; //NOI18N
        } else if (url.contains("caspian")) {//NOI18N
            themeName += "caspian/"; //NOI18N
        }
        final var file = new File(url);
        themeName += file.getName().replace(".bss", ".css");//NOI18N
        if (themeName.endsWith("modena.css")) {//NOI18N
            themeName = "modena.css";//NOI18N
        } else if (themeName.endsWith("caspian.css")) {//NOI18N
            themeName = "caspian.css";//NOI18N
        }
        return themeName;
    }

    public static boolean isThemeRule(final Rule rule) {
        final var stylePath = rule.getStylesheet().getUrl();
        assert stylePath != null;
        for (final var themeUrl : themeUrls) {
            if (stylePath.endsWith(themeUrl)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isThemeClass(final Theme theme, final String styleClass) {
        return getThemeStyleClasses(theme).contains(styleClass);
    }

    public static List<String> getThemeStyleClasses(final Theme theme) {
        final Set<String> themeClasses = new HashSet<>();
        theme.getStylesheetURLs().stream()
            .filter(s -> !EditorPlatform.isPlatformThemeStylesheetURL(s))
            .forEach(themeStyleSheet -> {
                final var resource = Button.class.getResource("/" + themeStyleSheet);
                themeClasses.addAll(getStyleClasses(resource));
            });
        return new ArrayList<>(themeClasses);
    }

    // Return the stylesheet corresponding to a style class.
    // (input parameter: a map returned by getStyleClassesMap(), styleClass)
    public static String getStyleSheet(final Map<String, String> styleClassMap, final String styleClass) {
        return styleClassMap.get(styleClass);
    }

    public static List<String> getStyleClasses(final EditorController editorController, final Set<FXOMInstance> instances) {
        return new ArrayList<>(getStyleClassesMap(editorController, instances).keySet());
    }

    public static Map<String, String> getStyleClassesMap(final EditorController editorController, final Set<FXOMInstance> instances) {
        final Map<String, String> classesMap = new TreeMap<>();
        Object fxRoot = null;
        for (final var instance : instances) {
            if (fxRoot == null) {
                fxRoot = instance.getFxomDocument().getSceneGraphRoot();
            }
            final var fxObject = instance.getSceneGraphObject();
            classesMap.putAll(getFxObjectClassesMap(fxObject, fxRoot));
        }

        // Handle the Scene stylesheets (if any)
        final List<File> sceneStyleSheets = editorController.getSceneStyleSheets();
        if (sceneStyleSheets != null) {
            for (final var stylesheet : sceneStyleSheets) {
                try {
                    final var stylesheetUrl = stylesheet.toURI().toURL();
                    for (final var styleClass : getStyleClasses(stylesheetUrl)) {
                        classesMap.put(styleClass, stylesheetUrl.toExternalForm());
                    }
                } catch (final MalformedURLException ex) {
                    return classesMap;
                }
            }
        }
        return classesMap;
    }

    // Retrieve the styClasses in the fx object scene graph
    private static Map<String, String> getFxObjectClassesMap(final Object fxObject, final Object fxRoot) {
        final Map<String, String> classesMap = new HashMap<>();
        classesMap.putAll(getSingleFxObjectClassesMap(fxObject));
        if (!(fxObject instanceof Node)) {
            return classesMap;
        }
        var node = (Node) fxObject;
        if (node == fxRoot) {
            return classesMap;
        }
        // Loop on scene graph tree, and stop at root node (to avoid to handle SB nodes)
        while (node.getParent() != null) {
            node = node.getParent();
            classesMap.putAll(getSingleFxObjectClassesMap(node));
            if (node == fxRoot) {
                break;
            }
        }
        return classesMap;
    }

    // Retrieve the styleClasses in the fx object only (not inherited ones)
    private static Map<String, String> getSingleFxObjectClassesMap(final Object fxObject) {
        final Map<String, String> classesMap = new HashMap<>();

        if (fxObject instanceof Parent) {
            final List<String> stylesheets = ((Parent) fxObject).getStylesheets();
            for (final var stylesheet : stylesheets) {
                try {
                    for (final var styleClass : getStyleClasses(new URL(stylesheet))) {
                        classesMap.put(styleClass, stylesheet);
                    }
                } catch (final MalformedURLException ex) {
                    return classesMap;
                }
            }
        }
        return classesMap;
    }

    private static Set<String> getStyleClasses(final URL url) {
        final Set<String> styleClasses = new HashSet<>();
        final Stylesheet s;
        try {
            s = new CssParser().parse(url);
        } catch (final IOException ex) {
            System.out.println("Warning: Invalid Stylesheet " + url); //NOI18N
            return styleClasses;
        }
        if (s == null) {
            // The parsed CSS file was empty. No parsing occurred.
            return styleClasses;
        }
        for (final var r : s.getRules()) {
            for (final var ss : r.getSelectors()) {
                styleClasses.addAll(ss.getStyleClassNames());
            }
        }
        return styleClasses;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getCssProperties(final Set<Class<?>> classes) {
        final var cssProperties = new TreeSet<String>();
        for (final var clazz : classes) {
            if (Node.class.isAssignableFrom(clazz)) {
                Object metadatas = null;
                try {
                    metadatas = clazz.getMethod("getClassCssMetaData").invoke(null, (Object[]) null); //NOI18N
                } catch (final NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    assert false;
                }
                for (final var metadata : ((List<CssMetaData<? extends Styleable, ?>>) metadatas)) {
                    cssProperties.add(metadata.getProperty());
                    if (metadata.getSubProperties() != null) {
                        for (final var subMetadata : metadata.getSubProperties()) {
                            cssProperties.add(subMetadata.getProperty());
                        }
                    }
                }
            }
        }
        return new ArrayList<>(cssProperties);
    }

    // If this property is ruled by CSS, return a CssPropAuthorInfo. Otherwise returns null.
    public static CssPropAuthorInfo getCssInfo(final Object fxObject, final ValuePropertyMetadata prop) {
        CssPropAuthorInfo info = null;
        Node node = null;

        if (fxObject instanceof Node) {
            node = (Node) fxObject;
        } else {
            final var styleable = fxObject instanceof Styleable ? (Styleable) fxObject : null;
            if (styleable != null) {
                node = styleable.getStyleableNode();
            }
        }
        if (node != null) {
            info = getCssInfoForNode(node, prop);
        }
        return info;
    }

    private static CssPropAuthorInfo getCssInfoForNode(final Node node, final ValuePropertyMetadata prop) {
        @SuppressWarnings("rawtypes") final var map = collectCssState(node);
        for (@SuppressWarnings("rawtypes") final var entry : map.entrySet()) {//NOI18N
            final StyleableProperty<?> beanProp = entry.getKey();
            final List<Style> styles = new ArrayList<>(entry.getValue());
            final var name = getBeanPropertyName(beanProp);
            if (!name.equals(prop.getName().getName())) {
                continue;
            }
            if (name.equals(prop.getName().getName())) {
                // If the value has an origin of Author or Inline 
                // then we have a property ruled by CSS, otherwise return null
                // This is in sync because the map is not empty
                final var origin = beanProp.getStyleOrigin();
                if (origin == null || origin.equals(StyleOrigin.USER)
                        || origin.equals(StyleOrigin.USER_AGENT)) {
                    return null;
                }
                final var styleable = beanProp.getCssMetaData();
                // Lookup the Author style
                CssPropAuthorInfo info = null;
                for (final var style : styles) {
                    final var rule = style.getDeclaration().getRule();
                    assert rule != null;
                    // StyleOrigin can be null when the value is set to its initial value.
                    final var o = rule.getOrigin();
                    if (o == null) {
                        return null;
                    }
                    if ((o.equals(StyleOrigin.AUTHOR) && (!CssInternal.isThemeStyle(style)))
                            || o.equals(StyleOrigin.INLINE)) {
                        if (info == null) {
                            info = new CssPropAuthorInfo(prop, beanProp, styleable);
                        }
                        info.getStyles().add(style);
                    }
                }
                return info;
            }
        }
        return null;
    }

    public static boolean isCssRuled(final Object fxObject, final ValuePropertyMetadata prop) {
        return getCssInfo(fxObject, prop) != null;
    }

    /**
     * CSS information attached to a Bean Property when styled with Author or
     * Inline origin.
     *
     */
    public static class CssPropAuthorInfo {

        private final ValuePropertyMetadata prop;
        private final CssMetaData<?, ?> styleable;
        private final StyleableProperty<?> value;
        private final Object val;
        private final List<Style> styles = new ArrayList<>();

        public CssPropAuthorInfo(final ValuePropertyMetadata prop, final StyleableProperty<?> value, final CssMetaData<?, ?> styleable) {
            this(prop, value, styleable, null);
        }

        private CssPropAuthorInfo(final ValuePropertyMetadata prop, final StyleableProperty<?> value, final CssMetaData<?, ?> styleable, final Object val) {
            this.prop = prop;
            this.styleable = styleable;
            this.value = value;
            this.val = val;
        }

        public CssPropAuthorInfo(final StyleableProperty<?> val, final CssMetaData<?, ?> styleable, final Object value) {
            this(null, val, styleable, value);
        }

        public StyleOrigin getOrigin() {
            return value.getStyleOrigin();
        }

        public URL getMainUrl() {
            if (getStyles().isEmpty()) {
                return null;
            } else {
                final var rule = getStyles().getFirst().getDeclaration().getRule();
                if (rule == null) {
                    return null;
                } else {
                    try {
                        return new URL(rule.getStylesheet().getUrl());
                    } catch (final MalformedURLException ex) {
                        System.out.println(ex.getMessage() + " " + ex);
                        return null;
                    }
                }
            }
        }

        public List<Style> getStyles() {
            return styles;
        }

        public Object getFxValue() {
            return val != null ? val : value.getValue();
        }

        public boolean isInline() {
            final var o = getOrigin();
            return o != null && o.equals(StyleOrigin.INLINE);
        }

        /**
         * @return the prop
         */
        public ValuePropertyMetadata getProp() {
            return prop;
        }

        /**
         * @return the cssProp
         */
        public CssMetaData<?, ?> getCssProp() {
            return styleable;
        }

    }

    public static String getBeanPropertyName(final StyleableProperty<?> val) {
        String property = null;
        if (val instanceof ReadOnlyProperty) {
            property = ((ReadOnlyProperty<?>) val).getName();
        }
        return property;
    }

    public static void attachMapToNode(final Node node) {
        final Map<StyleableProperty<?>, List<Style>> smap = new HashMap<>();
        Deprecation.setStyleMap(node, FXCollections.observableMap(smap));
    }

    public static void detachMapToNode(final Node node) {
        Deprecation.setStyleMap(node, null);
    }

    @SuppressWarnings("rawtypes")
    public static Map<StyleableProperty, List<Style>> collectCssState(final Node node) {
        attachMapToNode(node);
        // Force CSS to apply
        node.applyCss();

        final Map<StyleableProperty, List<Style>> ret = new HashMap<>();
//        ret.putAll(Deprecation.getStyleMap(node));

        final var map = Deprecation.getStyleMap(node);
        if (map != null && !map.isEmpty()) {
            for (final var entry : map.entrySet()) {
                final var key = entry.getKey();
                final var value = entry.getValue();
                if (((javafx.beans.property.Property<?>) key).getBean() == node) {
                    ret.put(key, value);
                }
            }
        }

        // Attached map may impact css performance, so remove it.
        detachMapToNode(node);
        // DEBUG
//        System.out.println("collectCssState() for " + node);
//        for (StyleableProperty s : ret.keySet()) {
//            List<Style> styles = ret.get(s);
//            for (Style style : styles) {
//                System.out.println(style.getDeclaration().getRule().getOrigin() + " ==> STYLE " + style.getDeclaration());
//                System.out.println("--> css url = " + style.getDeclaration().getRule().getStylesheet().getUrl());
//            }
//        }
        return ret;
    }

    public static StyleOrigin getOrigin(final Style style) {
        if (style == null || style.getDeclaration() == null) {
            return null;
        }
        return style.getDeclaration().getRule().getOrigin();
    }

    // From an css url, returns the theme display name
    public static String getThemeDisplayName(final String url) {
        var themeName = ""; //NOI18N
        if (url.contains("modena")) {//NOI18N
            themeName += "modena/"; //NOI18N
        } else if (url.contains("caspian")) {//NOI18N
            themeName += "caspian/"; //NOI18N
        }
        final var file = new File(url);
        themeName += file.getName().replace(".bss", ".css");//NOI18N
        if (themeName.endsWith("modena.css")) {//NOI18N
            themeName = "modena.css";//NOI18N
        } else if (themeName.endsWith("caspian.css")) {//NOI18N
            themeName = "caspian.css";//NOI18N
        }
        return themeName;
    }

}
