/*
 * Copyright (c) 2017, 2024, Gluon and/or its affiliates.
 * Copyright (c) 2014, Oracle and/or its affiliates.
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

import com.oracle.javafx.scenebuilder.kit.editor.panel.css.CssContentMaker.CssPropertyState;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;
import javafx.css.Rule;
import javafx.css.Style;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.StyleOrigin;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.control.Skinnable;

/**
 *
 * @treatAsPrivate
 */
public class NodeCssState {

    private static final List<StyleOrigin> ORDERED_ORIGIN = new ArrayList<>();

    static {
        ORDERED_ORIGIN.add(StyleOrigin.USER_AGENT);// fxTheme : modena/caspian
        ORDERED_ORIGIN.add(StyleOrigin.USER);//Bean API Call
        ORDERED_ORIGIN.add(StyleOrigin.AUTHOR);//CSS files
        ORDERED_ORIGIN.add(StyleOrigin.INLINE);//Style property

    }

    @SuppressWarnings("rawtypes")
    private final Map<StyleableProperty, List<Style>> map;
    private final Node node;
    private final FXOMObject fxomObject;
    private Collection<CssContentMaker.CssPropertyState> author;
    private Collection<CssContentMaker.CssPropertyState> inline;
    private Collection<CssContentMaker.CssPropertyState> userAgent;
    private Map<MatchingRule, List<MatchingDeclaration>> matchingRules;
    private List<MatchingRule> sortedMatchingRules = new ArrayList<>();
    private Collection<CssProperty> props;

    @SuppressWarnings("rawtypes")
    protected NodeCssState(final Map<StyleableProperty, List<Style>> map, final Node node, final FXOMObject fxomObject) {
        this.map = map;
        this.node = node;
        this.fxomObject = fxomObject;
        getAuthorStyles();
        getInlineStyles();
        getUserAgentStyles();
        getMatchingRules();
        getAllStyleables();
    }

    /**
     *
     * @treatAsPrivate
     */
    @SuppressWarnings("rawtypes")
    public static class CssProperty implements Comparable<CssProperty> {

        private final CssMetaData cssMeta;
        private final CssProperty mainProperty;
        private final Node target;
        private final List<CssProperty> sub = new ArrayList<>();
        private final ObjectProperty<String> name = new SimpleObjectProperty<>();
        private final ObjectProperty<CssContentMaker.PropertyState> builtin = new SimpleObjectProperty<>();
        private final ObjectProperty<CssContentMaker.CssPropertyState> fxTheme = new SimpleObjectProperty<>();
        private final ObjectProperty<CssContentMaker.CssPropertyState> authorCss = new SimpleObjectProperty<>();
        private final ObjectProperty<CssContentMaker.CssPropertyState> inlineCss = new SimpleObjectProperty<>();
        private final ObjectProperty<CssContentMaker.PropertyState> fxmlModel = new SimpleObjectProperty<>();
        private CssContentMaker.PropertyState currentState;

        CssProperty(final NodeCssState nodeCssState, final CssMetaData cssMeta, final Node target, final FXOMObject fxomObject) {
            this(nodeCssState, null, cssMeta, target, fxomObject);
        }

        CssProperty(final NodeCssState nodeCssState, final CssProperty mainProperty,
                    final CssMetaData cssMeta, final Node target, final FXOMObject fxomObject) {
            this.mainProperty = mainProperty;
            this.cssMeta = cssMeta;
            this.target = target;
            name.setValue(cssMeta.getProperty());
            final var inlineState = nodeCssState.retrieveCssStyle(cssMeta, nodeCssState.getInlineStyles());
            if (inlineState != null) {
                inlineCss.setValue(inlineState);
            }
            final var authorState = nodeCssState.retrieveCssStyle(cssMeta, nodeCssState.getAuthorStyles());
            if (authorState != null) {
                authorCss.setValue(authorState);
            }
            final var fxThemeState = nodeCssState.retrieveCssStyle(cssMeta, nodeCssState.getUserAgentStyles());
            if (fxThemeState != null) {
                fxTheme.setValue(fxThemeState);
            }
            @SuppressWarnings("unchecked") final var builtinState = CssContentMaker.initialValue(target, mainProperty == null ? this : mainProperty, cssMeta);
            assert builtinState != null;
            builtin.setValue(builtinState);

            final var modelState = CssContentMaker.modelValue(target, cssMeta, fxomObject);
            if (modelState != null) {
                fxmlModel.setValue(modelState);
            }
        }

        public ObjectProperty<CssContentMaker.PropertyState> builtinState() {
            return builtin;
        }

        public ObjectProperty<CssContentMaker.PropertyState> modelState() {
            return fxmlModel;
        }

        public ObjectProperty<CssContentMaker.CssPropertyState> fxThemeState() {
            return fxTheme;
        }

        public ObjectProperty<CssContentMaker.CssPropertyState> authorState() {
            return authorCss;
        }

        public ObjectProperty<CssContentMaker.CssPropertyState> inlineState() {
            return inlineCss;
        }

        public ObjectProperty<String> propertyName() {
            return name;
        }

        public CssMetaData getStyleable() {
            return cssMeta;
        }

        public Node getTarget() {
            return target;
        }

        public List<CssProperty> getSubProperties() {
            return sub;
        }

        public CssProperty getMainProperty() {
            return mainProperty;
        }

        @Override
        public int compareTo(final CssProperty cssProperty) {
            return cssMeta.getProperty().compareTo(cssProperty.cssMeta.getProperty());
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final var cssProperty = (CssProperty) obj;
            return cssMeta.getProperty().compareTo(cssProperty.cssMeta.getProperty()) == 0;
        }

        @Override
        public int hashCode() {
            var hash = 3;
            hash = 31 * hash + Objects.hashCode(this.cssMeta);
            hash = 31 * hash + Objects.hashCode(this.mainProperty);
            hash = 31 * hash + Objects.hashCode(this.target);
            return hash;
        }

        public boolean isBuiltinSource() {
            return inlineState().get() == null
                    && authorState().get() == null
                    && modelState().get() == null
                    && fxThemeState().get() == null;
        }

        public boolean isFxThemeSource() {
            return fxThemeState().get() != null
                    && inlineState().get() == null
                    && authorState().get() == null
                    && modelState().get() == null;
        }

        public boolean isModelSource() {
            return modelState().get() != null
                    && inlineState().get() == null
                    && authorState().get() == null;
        }

        public boolean isAuthorSource() {
            return authorState().get() != null
                    && inlineState().get() == null;
        }

        public boolean isInlineSource() {
            return inlineState().get() != null;
        }

        // CSS only, model and builtin doesn't make sense there
        public CssContentMaker.CssPropertyState getWinner() {
            if (inlineState().get() != null) {
                return inlineState().get();
            }
            if (authorState().get() != null) {
                return authorState().get();
            }
            return fxThemeState().get();
        }

        public CssContentMaker.PropertyState getCurrentStyle() {
            if (currentState == null) {
                currentState = builtinState().get();
                final var model = modelState().get();
                final var cssState = getWinner();
                if (cssState == null) {
                    if (model != null) {
                        currentState = model;
                    }
                } else {
                    if (model != null && cssState.getStyle() != null
                            && cssState.getStyle().getOrigin() == StyleOrigin.USER_AGENT) {
                        currentState = model;
                    } else {
                        currentState = cssState;
                    }
                }
            }
            return currentState;
        }

        public StyleOrigin getCurrentStyleOrigin() {
            final var state = getCurrentStyle();
            if (state instanceof final CssPropertyState cssState) {
                return cssState.getStyle().getOrigin();
            } else {
                if (state instanceof CssContentMaker.BeanPropertyState) {
                    return StyleOrigin.USER;
                } else {
                    return null;
                }
            }
        }

        public boolean isInlineInherited() {
            var ret = false;
            final var css = inlineCss.get();
            if (css != null) {
                ret = CssContentMaker.isInlineInherited(target, css);
            }
            return ret;
        }

        public Node getSourceNodeForInline() {
            Node ret = null;
            final var css = inlineCss.get();
            if (css != null) {
                ret = CssContentMaker.getSourceNodeForStyle(target, propertyName().get());
            }
            return ret;
        }

        public List<CssContentMaker.CssPropertyState.CssStyle> getFxThemeHiddenByModel() {
            final List<CssContentMaker.CssPropertyState.CssStyle> ret = new ArrayList<>();
            final var ps = getWinner();
            final var notAppliedStyles =
                    ps == null ? 
                    Collections.<CssContentMaker.CssPropertyState.CssStyle>emptyList() : 
                    ps.getNotAppliedStyles();
            final var hasModel = modelState().get() != null;
            if (hasModel) {
                final var allStyles = Deprecation.getMatchingStyles(getStyleable(), target);
                final var matchingStyles = CssContentMaker.removeUserAgentStyles(allStyles);
                for (final var style : matchingStyles) {
                    var cssStyle = new CssContentMaker.CssPropertyState.CssStyle(style);
                    if (cssStyle.getOrigin() == StyleOrigin.USER_AGENT && !notAppliedStyles.contains(cssStyle)) {
                        if (getStyleable().getProperty().equals(cssStyle.getCssProperty())) {
                            cssStyle = CssContentMaker.retrieveStyle(matchingStyles, style);
                            ret.add(cssStyle);
                        }
                    }
                }
            }
            return ret;
        }

    }

    private CssContentMaker.CssPropertyState retrieveCssStyle(
            final CssMetaData<?, ?> cssMeta, final Collection<CssContentMaker.CssPropertyState> styles) {
        for (final var prop : styles) {
            if (prop.getCssProperty().equals(cssMeta.getProperty())) {
                return prop;
            } else {
                if (prop.getSubProperties() != null) {
                    for (final var sub : prop.getSubProperties()) {
                        if (sub.getCssProperty().equals(cssMeta.getProperty())) {
                            return (CssContentMaker.CssPropertyState) sub;
                        }
                    }
                }
            }
        }
        return null;
    }

    public Node getNode() {
        return node;
    }

    @SuppressWarnings("rawtypes")
    public final Collection<CssProperty> getAllStyleables() {
        if (props == null) {
            props = new TreeSet<>();
            final var cssMetaList = node.getCssMetaData();
            for (final var cssMeta : cssMetaList) {
                final var mainProp = new CssProperty(this, cssMeta, node, fxomObject);
                props.add(mainProp);
                if (cssMeta.getSubProperties() != null) {
                    for (final CssMetaData sub : cssMeta.getSubProperties()) {
                        final var subProp = new CssProperty(this, mainProp, sub, node, fxomObject);
                        mainProp.getSubProperties().add(subProp);
                    }
                }
            }

            if (node instanceof final Skinnable skinnable) {
                final var skinNode = skinnable.getSkin().getNode();
                final var skinList = skinNode.getCssMetaData();
                for (final var skinCssMeta : skinList) {
                    var found = false;
                    for (final CssMetaData cssMeta : cssMetaList) {
                        if (skinCssMeta.getProperty().equals(cssMeta.getProperty())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        final var mainProp = new CssProperty(this, skinCssMeta, skinNode, fxomObject);
                        props.add(mainProp);
                        if (skinCssMeta.getSubProperties() != null) {
                            for (final var sub : skinCssMeta.getSubProperties()) {
                                final var subProp = new CssProperty(this, sub, node, fxomObject);
                                mainProp.getSubProperties().add(subProp);
                            }
                        }
                    }
                }
            }
        }
        return props;
    }

    public final Collection<CssContentMaker.CssPropertyState> getAuthorStyles() {
        if (author == null) {
            author = getAppliedStyles(StyleOrigin.AUTHOR);
        }
        return author;
    }

    public final Collection<CssContentMaker.CssPropertyState> getInlineStyles() {
        if (inline == null) {
            inline = getAppliedStyles(StyleOrigin.INLINE);
        }
        return inline;
    }

    public final Collection<CssContentMaker.CssPropertyState> getUserAgentStyles() {
        if (userAgent == null) {
            userAgent = getAppliedStyles(StyleOrigin.USER_AGENT);
        }
        return userAgent;
    }

    /**
     *
     * @treatAsPrivate
     */
    public static class RuleComparator implements Comparator<MatchingRule> {

        RuleComparator() {
            // no-op
        }

        @Override
        public int compare(final MatchingRule t, final MatchingRule t1) {
            final var originComparaison = compareOrigin(
                    t.getRule().getOrigin(), t1.rule.getOrigin());
            final var tnotApplied = countNotApplied(t.declarations);
            final var t1notApplied = countNotApplied(t1.declarations);
            final var notAppliedComparaisons = tnotApplied - t1notApplied;

            if (originComparaison == 0) {// Same origin, not Applied count is what is important. The less, the stronger
                return notAppliedComparaisons;
            } else {
                return originComparaison;
            }
        }

    }

    private static int compareOrigin(final StyleOrigin toCompare, final StyleOrigin other) {
        final var index1 = ORDERED_ORIGIN.indexOf(toCompare);
        final var index2 = ORDERED_ORIGIN.indexOf(other);
        return index2 - index1;

    }

    private static int countNotApplied(final List<MatchingDeclaration> declarations) {
        var count = 0;
        for (final var decl : declarations) {
            if (!decl.isApplied()) {
                count += 1;
            }
        }
        return count;
    }

    /**
     *
     * @treatAsPrivate
     */
    public static class MatchingRule {

        private final Rule rule;
        private final String selector;
        private final List<MatchingDeclaration> declarations = new ArrayList<>();

        private MatchingRule(final Rule rule, final String selector) {
            this.rule = rule;
            this.selector = selector;
        }

        public Rule getRule() {
            return rule;
        }

        public String getSelector() {
            return selector;
        }

        @Override
        public int hashCode() {
            var hash = 7;
            hash = 59 * hash + (this.rule != null ? this.rule.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof final MatchingRule mr)) {
                return false;
            }
            return rule.equals(mr.rule);
        }

        private void addDeclarations(final List<MatchingDeclaration> values) {
            declarations.addAll(values);
        }

        public List<MatchingDeclaration> getDeclarations() {
            return Collections.unmodifiableList(declarations);
        }

        @Override
        public String toString() {
            return rule.getSelectors().toString();
        }
    }

    /**
     *
     * @treatAsPrivate
     */
    public static class MatchingDeclaration {

        private final CssContentMaker.CssPropertyState.CssStyle style;
        private final CssContentMaker.CssPropertyState prop;
        private final boolean applied;
        private final boolean lookup;

        MatchingDeclaration(final CssContentMaker.CssPropertyState.CssStyle style,
                            final CssContentMaker.CssPropertyState prop, final boolean applied, final boolean lookup) {
            this.style = style;
            this.prop = prop;
            this.applied = applied;
            this.lookup = lookup;
        }

        /**
         * @return the style
         */
        public CssContentMaker.CssPropertyState.CssStyle getStyle() {
            return style;
        }

        /**
         * @return the prop
         */
        public CssContentMaker.CssPropertyState getProp() {
            return prop;
        }

        /**
         * @return the applied
         */
        public boolean isApplied() {
            return applied;
        }

        public boolean isLookup() {
            return lookup;
        }
    }

    // This method add sub properties instead of compund property.
    private static void addSubProperties(
            final Collection<CssContentMaker.CssPropertyState> source,
            final Collection<CssContentMaker.CssPropertyState> target) {
        for (final var p : source) {
            if (p.getSubProperties().isEmpty()) {
                target.add(p);
            } else {
                for (final var sub : p.getSubProperties()) {
                    target.add((CssContentMaker.CssPropertyState) sub);
                }
            }
        }
    }

    // Sorted according to Author/User Agent and applied / not applied
    public final List<MatchingRule> getMatchingRules() {
        if (matchingRules == null) {
            final Collection<CssContentMaker.CssPropertyState> styledProperties = new TreeSet<>();
            addSubProperties(getUserAgentStyles(), styledProperties);
            addSubProperties(getAuthorStyles(), styledProperties);
                // We need them to have the exhaustive set of styled properties.
            // We compute the rules based on the set of properties.
            addSubProperties(getInlineStyles(), styledProperties);
            matchingRules = new HashMap<>();
            for (final var cssP : styledProperties) {
                final var l = cssP.getSubProperties();
                if (l.isEmpty()) {
                    addMatchingDeclaration(cssP);
                } else {
                    for (final var pp : l) {
                        final var cssSubP = (CssContentMaker.CssPropertyState) pp;
                        addMatchingDeclaration(cssSubP);
                    }
                }
            }
            for (final var entry : matchingRules.entrySet()) {
                final var rule = entry.getKey();
                // Filterout the Inline
                if (rule.getRule().getOrigin() != StyleOrigin.INLINE) {
                    rule.addDeclarations(entry.getValue());
                    sortedMatchingRules.add(rule);
                }
            }
            Collections.<MatchingRule>sort(sortedMatchingRules, new RuleComparator());
        }
        return sortedMatchingRules;
    }

    private void addMatchingDeclaration(final CssPropertyState cssP) {
        addMatchingDeclaration(cssP, cssP.getStyle(), true, false);
        for (final var s : cssP.getNotAppliedStyles()) {
            addMatchingDeclaration(cssP, s, false, false);
        }
    }

    private void addMatchingDeclaration(
            final CssPropertyState cssP, final CssPropertyState.CssStyle style, final boolean applied, final boolean isLookup) {
        final var mr = new MatchingRule(style.getCssRule(), style.getSelector());
        var lst = matchingRules.get(mr);
        if (lst == null) {
            lst = new ArrayList<>();
            matchingRules.put(mr, lst);
        }
        final var pmr = new MatchingDeclaration(style, cssP, applied, isLookup);
        var found = false;
        for (final var d : lst) {
            if (d.style.getCssProperty().equals(style.getCssProperty())) {
                found = true;
                break;
            }
        }

        if (!found) {
            lst.add(pmr);
        }
        for (final var lookup : style.getLookupChain()) {
            addMatchingDeclaration(cssP, lookup, applied, true);
        }
    }

    @SuppressWarnings("rawtypes")
    private Set<CssContentMaker.CssPropertyState> getAppliedStyles(final StyleOrigin origin) {
        final SortedSet<CssContentMaker.CssPropertyState> propertyStates = new TreeSet<>();

//            if (origin == StyleOrigin.USER_AGENT) {
//                System.out.println("===========================");
//                System.out.println("getAppliedStyles() called!");
//                System.out.println("===========================");
//                for (StyleableProperty sp : map.keySet()) {
//                    System.out.println("---------------------------");
//                    System.out.println("Styleable property: " + sp);
//                    System.out.println("---------------------------");
//                    List<Style> styles = map.get(sp);
//                    CssContentMaker.printStyles(styles);
//                }
//                System.out.println("\n\n\n");
//            }
        for (final var entry : map.entrySet()) {//NOI18N
            final StyleableProperty<?> value = entry.getKey();
//                System.out.println("\nStyleable property: " + value);
            assert entry.getValue() != null;
            assert !entry.getValue().isEmpty();
            final var st = entry.getValue().getFirst();
            final var o = CssInternal.getOrigin(st);
//                printStyle(st);
                /* If this origin is equals to the passed one, this is the nominal case.
             * If this property contains sub properties (eg:background-fills), then we need to check
             * each sub property.
             */
            final var cssMetaList = value.getCssMetaData();
            if (o == origin || cssMetaList.getSubProperties() != null) {
                    // Need the first style to compute the value
                // We have at least a style. The first one is the winner.
                final var cssValue = CssValueConverter.toCssString(cssMetaList.getProperty(),
                        st.getDeclaration().getRule(), value.getValue());

                final var pState = new CssContentMaker.CssPropertyState(value, cssMetaList, cssValue);

                /* 
                 * Each sub property can be ruled by a specific Origin, 
                 * we need to check if the sub property is in a rule of the passed origin.
                 * For example, we can have background-radius set by fxTheme 
                 * and background-color set by inline or author.
                 */
                if (cssMetaList.getSubProperties() != null) {
                    for (final CssMetaData sub : cssMetaList.getSubProperties()) {
                        final var notApplied = CssContentMaker.getNotAppliedStyles(entry.getValue(), node, sub);
                        for (final var style : entry.getValue()) {
                            final var styleOrigin = CssInternal.getOrigin(style);
                            if (style.getDeclaration().getProperty().equals(sub.getProperty())
                                    && (styleOrigin == origin)) {
                                final var cssStyle = CssContentMaker.retrieveStyle(entry.getValue(), style);
                                final var subCssValue = CssValueConverter.toCssString(sub.getProperty(), style.getDeclaration().getRule(), value.getValue());
                                final var subCss = new CssContentMaker.CssSubPropertyState(value, sub, subCssValue);
                                subCss.setStyle(cssStyle);
                                subCss.getNotAppliedStyles().addAll(notApplied);
                                pState.getSubProperties().add(subCss);
                            }
                        }
                    }
                    // eg: -fx-font set
                    final var style = CssContentMaker.retrieveStyle(entry.getValue(), st);
                    pState.setStyle(style);
                    if (!st.getDeclaration().getProperty().equals(cssMetaList.getProperty())) {
                        style.setUnused();
                    }
                } else {
                        // Single style for this single property.
                    // Transform the flat list into a chain of lookup.
                    final var style = CssContentMaker.retrieveStyle(entry.getValue(), st);
                    pState.setStyle(style);
                }
                final List<Style> applied = new ArrayList<>();
                applied.add(st);
                pState.getNotAppliedStyles().addAll(CssContentMaker.getNotAppliedStyles(applied, node, cssMetaList));
                /*
                 * In case the origin is not the same and no sub properties have been found for 
                 * the passed origin, then the property is not taken into consideration
                 */
                if (o == origin || !pState.getSubProperties().isEmpty()) {
                    propertyStates.add(pState);
                }
            }
        }
        return propertyStates;
    }
}
