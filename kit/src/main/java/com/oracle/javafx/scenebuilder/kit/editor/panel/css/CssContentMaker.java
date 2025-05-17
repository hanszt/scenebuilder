/*
 * Copyright (c) 2017 Gluon and/or its affiliates.
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

import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.CssPropertyState.CssStyle;
import com.oracle.javafx.scenebuilder.kit.editor.panel.css.NodeCssState.CssProperty;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.PropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import javafx.css.Rule;
import javafx.css.Style;
import javafx.css.StyleOrigin;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import javafx.css.CssMetaData;
import javafx.css.ParsedValue;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.Parent;

/**
 * This class construct the model exposed by the CSS Panel.
 *
 * @treatAsPrivate
 */
public class CssContentMaker {

    private CssContentMaker() {
        assert false;
    }

    /*
     *
     * Public methods
     *
     */
    public static <N extends Node> PropertyState initialValue(final N n, final CssMetaData<N, ?> sub) {
        PropertyState val = null;

        try {
            final Object fxValue;
            final String cssValue;
            final var value = sub.getInitialValue(n);
            if (value == null) {
                cssValue = "none";//NOI18N
                fxValue = cssValue;
            } else {
                fxValue = sub.getInitialValue(n);
                cssValue = CssValueConverter.toCssString(sub.getProperty(), n);
            }
            val = newInitialPropertyState(fxValue, cssValue, n, sub);
        } catch (final RuntimeException ex) {
            System.out.println(ex.getMessage() + " " + ex);
            // Ok no initial value or InitialValue bug.
        }
        return val;
    }

    @SuppressWarnings("unchecked")
    public static <N extends Node> PropertyState initialValue(final N n, final CssProperty complex,
                                                              final CssMetaData<N, ?> sub) {
        PropertyState val = null;

        try {
            final Object fxValue;
            final String cssValue;
            final var complexInitial = complex.getStyleable().getInitialValue(complex.getTarget());
            if (complexInitial == null) {
                cssValue = "none";//NOI18N
                fxValue = cssValue;
            } else {
                fxValue = sub.getInitialValue(n);
                cssValue = CssValueConverter.toCssString(sub.getProperty(), complexInitial);
            }
            val = newInitialPropertyState(fxValue, cssValue, n, sub);
        } catch (final RuntimeException ex) {
            System.out.println(ex.getMessage() + " " + ex);
            // Ok no initial value or InitialValue bug.
        }
        return val;
    }

    @SuppressWarnings("rawtypes")
    public static <N extends Node> PropertyState modelValue(final N node, final CssMetaData<?, ?> cssMeta, final FXOMObject fxomObject) {
        PropertyState val = null;

        if (fxomObject == null) {
            // In this case, we are handling a sub-component, no model value then.
            return null;
        }
        // First retrieve the java bean property and check if it is overriden by the inspector.
        final var beanPropName = CssUtils.getBeanPropertyName(node, cssMeta);
        if (beanPropName == null) {
            // No corresponding java bean property
            return null;
        }
        final var beanPropertyName = new PropertyName(beanPropName);
        assert fxomObject instanceof FXOMInstance;
        final var fxomInstance = (FXOMInstance) fxomObject;
        final var propMeta
                = Metadata.getMetadata().queryValueProperty(fxomInstance, beanPropertyName);
        if (propMeta == null) {
            // No corresponding metadata
            return null;
        }
        if (!propMeta.isReadWrite()) {
            // R/O : no overridden
            return null;
        }
        var overriden = false;
        final var defaultValue = propMeta.getDefaultValueObject();
        final var propertyValue = propMeta.getValueObject(fxomInstance);
        if ((propertyValue == null) || (defaultValue == null)) {
            if (propertyValue != defaultValue) {
                overriden = true;
            }
        } else if (!propertyValue.equals(defaultValue)) {
            overriden = true;
        }

        if (overriden) {
            // We have an override.
            val = new BeanPropertyState(propMeta, cssMeta.getProperty(), propertyValue,
                    CssValueConverter.toCssString(cssMeta.getProperty(), propertyValue));
            // An overriden can have sub properties
            if (cssMeta.getSubProperties() != null && !cssMeta.getSubProperties().isEmpty()) {
                for (final CssMetaData sub : cssMeta.getSubProperties()) {
                    // Create a virtual sub property
                    final PropertyState subProp = new BeanPropertyState(propMeta, sub.getProperty(),
                            propertyValue, CssValueConverter.toCssString(sub.getProperty(), propertyValue));
                    val.getSubProperties().add(subProp);
                }
            }
        }
        return val;
    }

    public static Node getSourceNodeForStyle(final Object component, final String property) {
        Node ret = null;
        final var n = CssUtils.getNode(component);
        if (n != null) {
            if (n.getStyle() != null && n.getStyle().contains(property)) {
                ret = n;
            } else {
                var p = n.getParent();
                while (p != null) {
                    final var s = p.getStyle();
                    if (s != null && s.contains(property)) {
                        ret = p;
                        break;
                    }
                    p = p.getParent();
                }
            }
        }
        return ret;
    }

    public static boolean isInlineInherited(final Object component, final CssPropertyState cssProperty) {
        var isInherited = false;
        final var node = CssUtils.getNode(component);

        if (node == null) {
            return false;
        }

        // Not located on this node, must be inherited then
        if (node.getStyle() == null) {
            return true;
        }

        if (!containsInStyle(cssProperty, node.getStyle())) {
            isInherited = true;
        }

        return isInherited;
    }

    public static boolean containsPseudoState(final String selector) {
        return selector.contains(":");//NOI18N
    }


    /*
     *
     * Private methods
     *
     */
    private static boolean containsInStyle(final CssPropertyState prop, final String style) {
        return style.contains(prop.getCssProperty());
    }

    public static NodeCssState getCssState(final Object selectedObject) {
        final var node = CssUtils.getSelectedNode(selectedObject);
        if (node == null) {
            return null;
        }
        Parent p = null;
        double current = 1;
        try {
            if (node.getScene() == null) {
                // The node is not visible (ContextMenu, Tooltip, ...)
                // A node MUST be in the scene to allow for CSS content collect,
                // so we add it (temporarily) to the scene. 
                final var inScene = CssUtils.getFirstAncestorWithNonNullScene(node);
                if (inScene == null) {
                    // May happen if the Content Panel is not present
                    return null;
                }
                p = inScene.getParent();
                current = node.getOpacity();
                node.setOpacity(0);
                CssUtils.addToParent(p, node);
            }
            final var state = new NodeCssState(CssInternal.collectCssState(node), node, getFXOMObject(selectedObject));
            return state;
        } finally {
            if (p != null) {
                CssUtils.removeFromParent(p, node);
                node.setOpacity(current);
            }
        }
    }

    private static FXOMObject getFXOMObject(final Object selectedObject) {
        if (selectedObject instanceof FXOMObject) {
            return (FXOMObject) selectedObject;
        } else {
            return null;
        }
    }

    @SuppressWarnings("rawtypes")
    private static <N extends Node> InitialPropertyState newInitialPropertyState(
            final Object fxValue, final String cssValue, final N n, final CssMetaData<?, ?> cssMeta) {
        final var val
                = new InitialPropertyState(cssMeta.getProperty(), fxValue, cssValue);
        if (cssMeta.getSubProperties() != null && !cssMeta.getSubProperties().isEmpty()) {
            for (final CssMetaData sub : cssMeta.getSubProperties()) {
                final var subValue
                        = CssValueConverter.getSubPropertyValue(sub.getProperty(), fxValue);
                final var subCssValue = CssValueConverter.toCssString(subValue);
                final PropertyState subProp
                        = new InitialPropertyState(sub.getProperty(), subValue, subCssValue);
                val.getSubProperties().add(subProp);
            }
        }
        return val;
    }

    // Retrieve the styles associated to the value. This is the case of lookup (or variable)
    @SuppressWarnings("rawtypes")
    protected static CssStyle retrieveStyle(final List<Style> styles, final Style style) {
        final var st = new CssStyle(style);
        final var parsedValue = style.getDeclaration().getParsedValue();
        if (parsedValue.isContainsLookups() || parsedValue.isLookup()) {
            retrieveStylesFromParsedValue(styles, st, style.getDeclaration().getParsedValue());
        }
        return st;
    }

    @SuppressWarnings("rawtypes")
    private static void retrieveStylesFromParsedValue(
            final List<Style> lst, final CssStyle current, final ParsedValue<?, ?> parsedValue) {
        final var val = parsedValue.getValue();
        if (val instanceof final ParsedValue[][] layers2) {
            // If ParsedValue is a layered sequence of values, resolve the lookups for each.

            for (final var layers : layers2) {
                for (final var layer : layers) {
                    if (layer == null) {
                        continue;
                    }
                    retrieveStylesFromParsedValue(lst, current, layer);
                }
            }
        } else if (val instanceof final ParsedValue[] layers) {
            // If ParsedValue is a sequence of values, resolve the lookups for each.
            for (final var layer : layers) {
                if (layer == null) {
                    continue;
                }
                retrieveStylesFromParsedValue(lst, current, layer);
            }
        } else {
            if (val instanceof final String value) {
                for (final var info : lst) {
                    if (value.equals(info.getDeclaration().getProperty())) {
                        // Ok matching Style
                        final var cssStyle = retrieveStyle(lst, info);
                        current.getLookupChain().add(cssStyle);
                    }
                }
            }
        }
    }

    protected static List<CssStyle> getNotAppliedStyles(
            final List<Style> appliedStyles, final Node node, final CssMetaData<?, ?> cssMeta) {
        final List<CssStyle> ret = new ArrayList<>();

        final var allStyles = Deprecation.getMatchingStyles(cssMeta, node);
//        System.out.println("===========================");
//        System.out.println("getNotAppliedStyles() called!");
//        System.out.println("===========================");
//        System.out.println("\n\n");
//        printStyles(allStyles);
        final var matchingStyles = removeUserAgentStyles(allStyles);
        final List<Style> notApplied = new ArrayList<>();
        for (final var style : matchingStyles) {
            if (!appliedStyles.contains(style)) {
                notApplied.add(style);
            }
        }
        for (final var style : notApplied) {
            if (style.getDeclaration().getProperty().equals(cssMeta.getProperty())) {
                // We need to retrieve from allStyles, in case a lookup is shared by appliedStyles and notApplied
                final var cssStyle = retrieveStyle(matchingStyles, style);
                ret.add(cssStyle);
            }
        }
        return ret;
    }

    protected static List<Style> removeUserAgentStyles(final List<Style> allStyles) {
        // With SB 2, we apply explicitly Modena/Caspian theme css on user scene graph.
        // The rules that appear with an AUTHOR origin has already been considered as USER_AGENT.
        // So when an internal css method (such as getMatchingStyles()) is called,
        // we need here to remove all USER_AGENT styles, to avoid doublons.
        final List<Style> matchingStyles = new ArrayList<>();
        for (final var style : allStyles) {
            if (!(style.getDeclaration().getRule().getOrigin() == StyleOrigin.USER_AGENT)) {
                matchingStyles.add(style);
            }
        }
        return matchingStyles;
    }
    
//    protected static void printStyles(List<Style> styles) {
//        for (Style style : styles) {
//            printStyle(style);
//        }
//
//    }
//    private static void printStyle(Style style) {
//        System.out.println(style.getDeclaration().getRule().getOrigin() + " ==> STYLE " + style.getDeclaration());
//        System.out.println("--> css url = " + style.getDeclaration().getRule().getStylesheet().getUrl());
//    }

    /**
     *
     * Public classes.
     *
     * @treatAsPrivate
     */
    public static abstract class PropertyState implements Comparable<PropertyState> {

        protected PropertyState(final String cssValue) {
            this.cssValue = cssValue;
        }
        private final List<CssStyle> notAppliedStyles = new ArrayList<>();
        private final List<PropertyState> lst = new ArrayList<>();
        private final String cssValue;

        public abstract String getCssProperty();

        public abstract Object getFxValue();

        public String getCssValue() {
            return cssValue;
        }

        public List<PropertyState> getSubProperties() {
            return lst;
        }

        public List<CssStyle> getNotAppliedStyles() {
            return notAppliedStyles;
        }

        @Override
        public int compareTo(final PropertyState t) {
            final var ps = t;
            return getCssProperty().compareTo(ps.getCssProperty());
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final var ps = (PropertyState) obj;
            return getCssProperty().compareTo(ps.getCssProperty()) == 0;
        }

        @Override
        public int hashCode() {
            var hash = 7;
            hash = 53 * hash + Objects.hashCode(this.notAppliedStyles);
            hash = 53 * hash + Objects.hashCode(this.lst);
            hash = 53 * hash + Objects.hashCode(this.cssValue);
            return hash;
        }
    }

/**
 *
 * @treatAsPrivate
 */
    public static class BeanPropertyState extends PropertyState {

        PropertyMetadata propMeta;
        private final String cssPropName;
        private final Object fxValue;

        BeanPropertyState(final PropertyMetadata propMeta, final String cssPropName, final Object fxValue, final String cssValue) {
            super(cssValue);
            this.propMeta = propMeta;
            this.cssPropName = cssPropName;
            this.fxValue = fxValue;
        }

        @Override
        public String getCssProperty() {
            return cssPropName;
        }

        public PropertyMetadata getPropertyMeta() {
            return propMeta;
        }

        @Override
        public Object getFxValue() {
            return fxValue;
        }
    }

/**
 *
 * @treatAsPrivate
 */
    public static class CssPropertyState extends PropertyState {

        protected final StyleableProperty<?> value;
        protected final CssMetaData<?, ?> cssMeta;
        private CssStyle style;

        CssPropertyState(final StyleableProperty<?> value, final CssMetaData<?, ?> cssMeta, final String cssValue) {
            super(cssValue);
            this.value = value;
            this.cssMeta = cssMeta;
        }

        @Override
        public String getCssProperty() {
            return cssMeta.getProperty();
        }

        public CssStyle getStyle() {
            return style;
        }

        void setStyle(final CssStyle style) {
            this.style = style;
        }

        @Override
        public Object getFxValue() {
            return value.getValue();
        }

        /**
         *
         * @treatAsPrivate
         */
        public static class CssStyle {

            private final Style style;
            private boolean used = true;
            private final List<CssStyle> lookupSet = new ArrayList<>();

            public CssStyle(final Style style) {
                this.style = style;
            }

            protected void setUnused() {
                used = false;
            }

            public boolean isUsed() {
                return used;
            }

            public Style getStyle() {
                return style;
            }

            public String getCssProperty() {
                return style.getDeclaration().getProperty();
            }

            @SuppressWarnings("rawtypes")
            public ParsedValue getParsedValue() {
                return style.getDeclaration().getParsedValue();
            }

            public StyleOrigin getOrigin() {
                return CssInternal.getOrigin(style);
            }

            public String getSelector() {
                var sel = style.getSelector().toString();
                if (sel.startsWith("*")) {//NOI18N
                    sel = sel.substring(1);
                }
                return sel;
            }

            public Rule getCssRule() {
                return style.getDeclaration().getRule();
            }

            public URL getUrl() {
                // Workaround!
                final var rule = getCssRule();
                if (rule == null) {
                    return null;
                } else {
                    try {
                        return URI.create(rule.getStylesheet().getUrl()).toURL();
                    } catch (final MalformedURLException ex) {
                        System.out.println(ex.getMessage() + " " + ex);
                        return null;
                    }
                }
            }

            @Override
            public String toString() {
                return style.toString();
            }

            public List<CssStyle> getLookupChain() {
                return lookupSet;
            }

            @Override
            public int hashCode() {
                var hash = 7;
                hash = 47 * hash + (this.style != null ? this.style.hashCode() : 0);
                return hash;
            }

            @Override
            public boolean equals(final Object obj) {
                if (!(obj instanceof final CssStyle cssStyle)) {
                    return false;
                }
                return style.equals(cssStyle.style);
            }
        }
    }

    /**
     *
     * Private classes.
     *
     * @treatAsPrivate
     */
    protected static class CssSubPropertyState extends CssPropertyState {

        CssSubPropertyState(final StyleableProperty<?> value, final CssMetaData<?, ?> cssMeta, final String cssValue) {
            super(value, cssMeta, cssValue);
        }

        @Override
        public Object getFxValue() {
            return CssValueConverter.getSubPropertyValue(cssMeta.getProperty(), value.getValue());
        }
    }

    private static class InitialPropertyState extends PropertyState {

        private final String name;
        private final Object fxValue;

        InitialPropertyState(final String name, final Object fxValue, final String cssValue) {
            super(cssValue);
            this.name = name;
            this.fxValue = fxValue;
        }

        @Override
        public String getCssProperty() {
            return name;
        }

        @Override
        public Object getFxValue() {
            return fxValue;
        }
    }

}
