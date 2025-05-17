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

import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.EditorUtils;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ColorEncoder;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import javafx.css.Rule;
import javafx.css.Size;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.PaintConverter.LinearGradientConverter;
import javafx.css.converter.DeriveColorConverter;
import javafx.css.converter.DeriveSizeConverter;
import javafx.css.converter.LadderConverter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.css.ParsedValue;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.effect.Effect;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderImage;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

/**
 *
 * @treatAsPrivate
 */
public class CssValueConverter {

    private static final List<String> STRING_VALUE = new ArrayList<>();
    private static final List<String> SINGLE_WHEN_EQUALITY = new ArrayList<>();

    private CssValueConverter() {
        assert false;
    }

    @SuppressWarnings("rawtypes")
    public static Object convert(final ParsedValue pv) {
        Object value = null;
        if (pv == null) {
            return null;
        }

        if (pv.getConverter() != null) {
            try {
                @SuppressWarnings("unchecked") final var converted = pv.getConverter().convert(pv, null);
                value = converted;
            } catch (final RuntimeException ex) {
                // OK, can't be resolved, a lookup possibly
            }
        } else {
            value = pv.getValue();
        }
        if (value instanceof ParsedValue) {
            value = convert((ParsedValue) value);
        }
        return value;
    }

    // Retrieve a CSS String from a value set thanks to CSS
    public static String toCssString(final String property, final Rule rule, final Object fxValue) {
        try {
            return getValue(property, rule, fxValue);
        } catch (final IllegalArgumentException ex) {
            return getValue(property, null, fxValue);
        }
    }

    public static String toCssString(final String property, final Object fxValue) {
        return getValue(property, null, fxValue);
    }

    public static String toCssString(final Object fxValue) {
        return getValue(null, null, fxValue);
    }

    // Retrieve the value for a sub property.
    public static Object getSubPropertyValue(final String property, final Object value) {
        if (value instanceof final Collection<?> values) {
            final List<Object> subValues = new ArrayList<>();
            for (final var bf : values) {
                subValues.add(getSubPropertyValue(property, bf));
            }
            return subValues;
        } else if (value != null && value.getClass().isArray()) {
            final var newArray = Array.newInstance(value.getClass().getComponentType(), Array.getLength(value));
            for (var i = 0; i < Array.getLength(value); i++) {
                Array.set(newArray, i, getSubPropertyValue(property, Array.get(value, i)));
            }
            return newArray;

            //
            // Background
            //
        } else if (value instanceof final Background background) {
            if (background.getFills() != null) {
                return getSubPropertyValue(property, background.getFills());
            } else if (background.getImages() != null) {
                return getSubPropertyValue(property, background.getImages());
            }
        } else if (value instanceof BackgroundFill) {
            return subBackgroundFill(property, (BackgroundFill) value);
        } else if (value instanceof BackgroundImage) {
            return subBackgroundImage(property, (BackgroundImage) value);

            //
            // Border
            //
        } else if (value instanceof final Border border) {
            if (border.getStrokes() != null) {
                return getSubPropertyValue(property, border.getStrokes());
            } else if (border.getImages() != null) {
                return getSubPropertyValue(property, border.getImages());
            }
        } else if (value instanceof BorderStroke) {
            return subBorderStroke(property, (BorderStroke) value);
        } else if (value instanceof BorderImage) {
            return subBorderImage(property, (BorderImage) value);

            //
            // Font
            //
        } else if (value instanceof Font) {
            return subFont(property, (Font) value);
        }
        return getValue(property, null, value);
    }

    static {
        STRING_VALUE.add("-fx-skin");//NOI18N
        STRING_VALUE.add("-fx-shape");//NOI18N
    }

    private static String format(final String property, final String value) {
        if (STRING_VALUE.contains(property)) {
            return "\"" + value + "\"";//NOI18N
        } else {
            return value;
        }
    }

    // FX to String value transformation entry point.
    private static String getValue(final String property, final Rule r, final Object eventValue) throws IllegalArgumentException {
        if (r == null) {
            return format(property, retrieveValue(property, eventValue));
        }

        for (final var d : r.getDeclarations()) {
            if (d.getProperty().equals(property)) {
                if (property.equals("-fx-background-radius") || property.equals("-fx-border-radius")) { //NOI18N
                    return format(property, getRadiusCssString(property, d.getParsedValue()));
                } else {
                    return format(property, getCssString(property, d.getParsedValue()));
                }
            }
        }
        throw new IllegalArgumentException("Can't compute a value");//NOI18N
    }

    static {
        SINGLE_WHEN_EQUALITY.add("-fx-padding"); //NOI18N
        SINGLE_WHEN_EQUALITY.add("-fx-background-radius"); //NOI18N
        SINGLE_WHEN_EQUALITY.add("-fx-background-insets"); //NOI18N
        SINGLE_WHEN_EQUALITY.add("-fx-border-color"); //NOI18N
        SINGLE_WHEN_EQUALITY.add("-fx-border-radius"); //NOI18N
        SINGLE_WHEN_EQUALITY.add("-fx-border-insets"); //NOI18N
        SINGLE_WHEN_EQUALITY.add("-fx-border-image-insets"); //NOI18N
        SINGLE_WHEN_EQUALITY.add("-fx-border-image-slice"); //NOI18N
        SINGLE_WHEN_EQUALITY.add("-fx-border-image-width"); //NOI18N

    }

    private static boolean singleForEquality(final String prop) {
        return SINGLE_WHEN_EQUALITY.contains(prop);
    }

    // The difference between retrieveValue and getCssStringValue
    // is that the ParsedValue is converted in the retrieveValue case, and is not converted in the 
    // getCssStringValue
    // When converting, we loose the CSS textual format present in the CSS source,
    // for instance the lookup information, or 'em' unit.
    @SuppressWarnings("rawtypes")
    private static String getCssString(final String property, final ParsedValue value) {

        // TODO : this method should be rewritten in a cleaner way...
        if (value == null) {
            return "null"; //NOI18N
        }

        // I don't like that but this is needed to only have a conversion for gradient.
        // The gradient.toString is much better than the ParsedValue.toString.
        if (value.getConverter() instanceof LinearGradientConverter
                || value.getConverter() instanceof PaintConverter.RadialGradientConverter) {

            try {
                @SuppressWarnings("unchecked") final//NOI18N
                var converted = value.getConverter().convert(value, null);

                return toCssString(converted);
            } catch (final RuntimeException ex) {
            }
        }

        final var obj = value.getValue();
        if (obj instanceof ParsedValue) {
            return getCssString(property, (ParsedValue) obj);
        }
        final var builder = new StringBuilder();
        final var isDerive = value.getConverter() instanceof DeriveColorConverter || value.getConverter() instanceof DeriveSizeConverter;
        final var isLadder = value.getConverter() instanceof LadderConverter;
        if (isDerive) {
            builder.append("derive("); //NOI18N
        }
        if (isLadder) {
            builder.append("ladder("); //NOI18N
        }
        if (obj instanceof final ParsedValue[] array) {
            var isArrayValue = false;
            if (array.length >= 1) {
                final var pval = array[0];
                Object val = null;
                if (pval != null) {
                    if (pval.getConverter() instanceof LinearGradientConverter
                            || pval.getConverter() instanceof PaintConverter.RadialGradientConverter) {
                        val = null;
                    } else {
                        val = pval.getValue();
                    }
                }
                isArrayValue = val != null && val.getClass().isArray();
            }
            final var singleForEquality = singleForEquality(property) && !isArrayValue;
            final var b = new StringBuilder();
            if (singleForEquality) {
                String latest = null;
                var areEquals = true;
                final List<String> values = new ArrayList<>(array.length);
                for (final var v : array) {
                    final var current = getCssString(property, v);
                    values.add(current);
                    areEquals &= (latest == null || current.equals(latest));
                    latest = current;
                }
                if (areEquals) {
                    var val = values.getFirst();
                    val = removeDotZeroPxPercent(val);
                    b.append(val);
                } else {
                    for (var i = 0; i < values.size(); i++) {
                        b.append(values.get(i));
                        if (i < array.length - 1) {
                            b.append(" "); //NOI18N
                        }
                    }
                }
            } else {
                for (var i = 0; i < array.length; i++) {
                    final var v = array[i];
                    var val = getCssString(property, v);
                    val = removeDotZeroPxPercent(val);
                    b.append(val);
                    if ((i < array.length - 1) && val.length() > 0) {
                        b.append(", "); //NOI18N
                    }
                }
            }

            builder.append(b.toString());
        } else {
            if (obj instanceof final ParsedValue[][] arr) {
                for (var i = 0; i < arr.length; i++) {
                    final var val = retrieveValue(property, arr[i]);
                    builder.append(val);
                    if ((i < arr.length - 1) && val.length() > 0) {
                        builder.append(", "); //NOI18N
                    }
                }
            } else {
                builder.append(retrieveValue(property, obj));
            }
        }
        if (isDerive || isLadder) {
            builder.append(")"); //NOI18N
        }
        return builder.toString();
    }

    @SuppressWarnings("rawtypes")
    private static String getRadiusCssString(final String property, final ParsedValue value) {
        // TODO : Ideally should be included in the generic getCssString() method
        
        // See  http://www.w3.org/TR/css3-background/#the-border-radius 
        
        assert property.equals("-fx-background-radius") || property.equals("-fx-border-radius"); //NOI18N
        final var sbAll = new StringBuilder();
        var obj = value.getValue();
        if (!(obj instanceof final ParsedValue[] pvArray)) {
            return null;
        }
        var index = 0;
        for (final var pvItem : pvArray) {
            // We have a CornerRadii representation here:
            // double dimension array:
            // 1- horizontal/vertical radii
            // 2- value per corner
            // If all the values are identical, only a single value is used.
            obj = pvItem.getValue();
            if (!(obj instanceof final ParsedValue[][] pvArray2)) {
                return null;
            }
            final var sbCornerRadii = new StringBuilder();
            Size initSize = null;
            var areEquals = true;
            var index2 = 0;
            for (final var pvArray1 : pvArray2) {
                // horizontal or vertical list 
                for (final var pvItem2 : pvArray1) {
                    obj = pvItem2.getValue();
                    if (!(obj instanceof final Size size)) {
                        return null;
                    }
                    sbCornerRadii.append(size).append(" "); //NOI18N
                    if (initSize == null) {
                        initSize = size;
                    } else if (!initSize.equals(size)) {
                        areEquals = false;
                    }
                }
                if (index2 != pvArray2.length - 1) {
                    // Separator between the horizontal / vertical lists
                    sbCornerRadii.append(" / "); //NOI18N
                }
                index2++;
            }
            if (areEquals) {
                sbAll.append(initSize);
            } else {
                sbAll.append(sbCornerRadii.toString().trim());
            }
            if (index != pvArray.length - 1) {
                sbAll.append(", "); //NOI18N
            }
            index++;
        }
        return removeDotZeroPxPercent(sbAll.toString());
    }

    private static String retrieveValue(final String property, Object eventValue) {
        if (eventValue instanceof ParsedValue) {
            eventValue = convert((ParsedValue<?, ?>) eventValue);
        }

        if (eventValue == null) {
            return "null"; //NOI18N
        }
        final var builder = new StringBuilder();
        if (eventValue instanceof final List<?> values) {
            final var length = values.size();
            for (var i = 0; i < length; i++) {
                final var val = retrieveValue(property, values.get(i));
                builder.append(val);
                if ((i < length - 1) && val.length() > 0) {
                    builder.append(", "); //NOI18N
                }
            }
        } else if (eventValue.getClass().isArray()) {
            final var length = Array.getLength(eventValue);
            for (var i = 0; i < length; i++) {
                final var val = retrieveValue(property, Array.get(eventValue, i));
                builder.append(val);
                if ((i < length - 1) && val.length() > 0) {
                    builder.append(", "); //NOI18N
                }
            }
        } else if (eventValue instanceof final Background background) {
            if (background.getFills() != null) {
                return retrieveValue(property, background.getFills());
            } else if (background.getImages() != null) {
                return retrieveValue(property, background.getImages());
            }
        } else if (eventValue instanceof final Border border) {
            if (border.getStrokes() != null) {
                return retrieveValue(property, border.getStrokes());
            } else if (border.getImages() != null) {
                return retrieveValue(property, border.getImages());
            }
        } else if (eventValue instanceof BackgroundFill) {
            builder.append(backgroundFillToString(property, (BackgroundFill) eventValue));
        } else if (eventValue instanceof CornerRadii) {
            builder.append(cornerRadiiToString(property, (CornerRadii) eventValue));
        } else if (eventValue instanceof BackgroundImage) {
            builder.append(backgroundImageToString(property, (BackgroundImage) eventValue));
        } else if (eventValue instanceof BorderStroke) {
            builder.append(borderStrokeToString(property, (BorderStroke) eventValue));
        } else if (eventValue instanceof BorderImage) {
            builder.append(borderImageToString(property, (BorderImage) eventValue));
        } else if (eventValue instanceof Font) {
            builder.append(fontToString(property, (Font) eventValue));
        } else if (eventValue instanceof Paint) {
            builder.append(paintToString((Paint) eventValue).toLowerCase(Locale.ROOT));
        } else if (eventValue instanceof Insets) {
            builder.append(insetsValue((Insets) eventValue));
        } else if (eventValue instanceof Effect) {
            builder.append(effectValue((Effect) eventValue));
        } else {
            var str = EditorUtils.valAsStr(eventValue);
            if (str == null) {
                str = "null"; //NOI18N
            } else {
                str = str.replaceAll("\n", " ");//NOI18N
                // Remove memory address if any
                str = str.split("@")[0]; //NOI18N
                str = removeDotZeroPxPercent(str);
            }
            builder.append(str);
        }
        return builder.toString();
    }

    private static String getColorAsWebString(final Color c) {
        final var red = (int) Math.round(c.getRed() * 255.0);
        final var green = (int) Math.round(c.getGreen() * 255.0);
        final var blue = (int) Math.round(c.getBlue() * 255.0);
        final var alpha = (int) Math.round(c.getOpacity() * 255.0);
        if (alpha == 255) {
            return String.format("#%02x%02x%02x", red, green, blue); //NOI18N
        } else {
            return String.format("#%02x%02x%02x%02x", red, green, blue, alpha); //NOI18N
        }
    }

    private static String getColorAsString(final Color color) {
        if (isStandardColor(color)) {
            return getStandardColorAsString(color);
        } else {
            return getColorAsWebString(color);
        }
    }

    private static boolean isStandardColor(final Color c) {
        return standardColors.containsKey(c);
    }
    static Map<Color, String> standardColors = ColorEncoder.getStandardColorNames();

    private static String getStandardColorAsString(final Color c) {
        return standardColors.get(c);
    }

    private static String backgroundFillToString(final String property, final BackgroundFill bf) {
        if (property == null) {
            return bf.toString();
        }
        final var builder = new StringBuilder();
        if (property.equals("-fx-background-color")) { //NOI18N
            final var p = bf.getFill();
            builder.append(paintToString(p));
        } else {
            if (property.equals("-fx-background-insets")) { //NOI18N
                builder.append(insetsValue(bf.getInsets()));
            } else {
                //the top right, bottom right, bottom left, and top left
                if (property.equals("-fx-background-radius")) { //NOI18N
                    handleCornerRadii(bf.getRadii(), builder);
                }
            }
        }
        return builder.toString();
    }

    private static String cornerRadiiToString(final String property, final CornerRadii cr) {
        if (property == null) {
            return cr.toString();
        }
        final var builder = new StringBuilder();
        handleCornerRadii(cr, builder);
        return builder.toString();
    }

    private static String backgroundImageToString(final String property, final BackgroundImage bi) {
        if (property == null) {
            return bi.toString();
        }
        final var builder = new StringBuilder();
        if (property.equals("-fx-background-image")) { //NOI18N
            final var p = bi.getImage();
            builder.append(p.getUrl());
        } else {
            if (property.equals("-fx-background-position")) {             //NOI18N
                double left = 0, right = 0, top = 0, bottom = 0;
                if (bi.getPosition().getHorizontalSide() == Side.LEFT) {
                    left = bi.getPosition().getHorizontalPosition();
                } else {
                    right = bi.getPosition().getHorizontalPosition();
                }
                if (bi.getPosition().getVerticalSide() == Side.TOP) {
                    top = bi.getPosition().getVerticalPosition();
                } else {
                    bottom = bi.getPosition().getVerticalPosition();
                }
                builder.append("left:"); //NOI18N
                builder.append(EditorUtils.valAsStr(left));
                builder.append(" right:"); //NOI18N
                builder.append(EditorUtils.valAsStr(right));
                builder.append(" top:"); //NOI18N
                builder.append(EditorUtils.valAsStr(top));
                builder.append(" bottom:"); //NOI18N
                builder.append(EditorUtils.valAsStr(bottom));
            } else {
                if (property.equals("-fx-background-repeat")) {          //NOI18N
                    if (bi.getRepeatX() != null) {
                        builder.append(bi.getRepeatX().toString());
                    } else {
                        if (bi.getRepeatY() != null) {
                            builder.append(bi.getRepeatY().toString());
                        } else {
                            builder.append("unknown repeat"); //NOI18N
                        }
                    }
                } else {
                    if (property.equals("-fx-background-size")) { //NOI18N
                        final var bs = bi.getSize();
                        if (bs.isContain()) {
                            builder.append("contain"); //NOI18N
                        } else {
                            if (bs.isCover()) {
                                builder.append("cover"); //NOI18N
                            } else {
                                if (bs.getWidth() == BackgroundSize.AUTO) {
                                    builder.append("width: auto"); //NOI18N
                                } else {
                                    builder.append("width: ").append(EditorUtils.valAsStr(bs.getWidth())); //NOI18N
                                }
                                if (bs.getHeight() == BackgroundSize.AUTO) {
                                    builder.append("height: auto"); //NOI18N
                                } else {
                                    builder.append("height: ").append(EditorUtils.valAsStr(bs.getHeight())); //NOI18N
                                }
                            }
                        }
                    }
                }
            }
        }
        return builder.toString();
    }

    private static String borderImageToString(final String property, final BorderImage bi) {
        if (property == null) {
            return bi.toString();
        }
        final var builder = new StringBuilder();
        if (property.equals("-fx-border-image")) { //NOI18N
            final var p = bi.getImage();
            builder.append(p.getUrl());
        } else {
            if (property.equals("-fx-background-position")) {             //NOI18N

            } else {
                if (property.equals("-fx-border-image-repeat")) {          //NOI18N
                    if (bi.getRepeatX() != null) {
                        builder.append(bi.getRepeatX().toString());
                    } else {
                        if (bi.getRepeatY() != null) {
                            builder.append(bi.getRepeatY().toString());
                        } else {
                            builder.append("unknown repeat"); //NOI18N
                        }
                    }
                } else {
                    if (property.equals("-fx-border-image-insets")) { //NOI18N
                        builder.append(insetsValue(bi.getInsets()));
                    } else {
                        if (property.equals("-fx-border-image-width")) { //NOI18N
                            final var bw = bi.getWidths();
                            if (MathUtils.equals(bw.getTop(), bw.getBottom())
                                    && MathUtils.equals(bw.getLeft(), bw.getRight())) {
                                builder.append(EditorUtils.valAsStr(bw.getTop()));
                            } else {
                                builder.append(EditorUtils.valAsStr(bw.getTop())).append(" "). //NOI18N
                                        append(EditorUtils.valAsStr(bw.getRight())).append(" "). //NOI18N
                                        append(EditorUtils.valAsStr(bw.getBottom())).append(" "). //NOI18N
                                        append(EditorUtils.valAsStr(bw.getLeft()));
                            }
                        } else {
                            if (property.equals("-fx-border-image-slice")) { //NOI18N
                                final var bw = bi.getSlices();
                                if (MathUtils.equals(bw.getTop(), bw.getBottom())
                                        && MathUtils.equals(bw.getLeft(), bw.getRight())) {
                                    builder.append(EditorUtils.valAsStr(bw.getTop()));
                                } else {
                                    builder.append(EditorUtils.valAsStr(bw.getTop())).append(" "). //NOI18N
                                            append(EditorUtils.valAsStr(bw.getRight())).append(" "). //NOI18N
                                            append(EditorUtils.valAsStr(bw.getBottom())).append(" "). //NOI18N
                                            append(EditorUtils.valAsStr(bw.getLeft()));
                                }
                            }
                        }
                    }
                }
            }
        }
        return builder.toString();
    }

    private static String borderStrokeToString(final String property, final BorderStroke bs) {
        if (property == null) {
            return bs.toString();
        }
        final var builder = new StringBuilder();
        //top, right, bottom, and left 
        if (property.equals("-fx-border-color")) { //NOI18N
            if (bs.getTopStroke().equals(bs.getBottomStroke())
                    && bs.getRightStroke().equals(bs.getBottomStroke())
                    && bs.getLeftStroke().equals(bs.getBottomStroke())) {
                builder.append(paintToString(bs.getBottomStroke()));
            } else {
                builder.append(paintToString(bs.getTopStroke())).append(" "); //NOI18N
                builder.append(paintToString(bs.getRightStroke())).append(" "); //NOI18N
                builder.append(paintToString(bs.getBottomStroke())).append(" "); //NOI18N
                builder.append(paintToString(bs.getLeftStroke()));
            }
        } else {
            if (property.equals("-fx-border-insets")) { //NOI18N
                builder.append(insetsValue(bs.getInsets()));
            } else {
                //the top right, bottom right, bottom left, and top left
                if (property.equals("-fx-border-radius")) { //NOI18N
                    handleCornerRadii(bs.getRadii(), builder);
                } else {
                    if (property.equals("-fx-border-style")) { //NOI18N
                        builder.append(bs.getTopStyle().toString()).append(", "); //NOI18N
                        builder.append(bs.getRightStyle().toString()).append(", "); //NOI18N
                        builder.append(bs.getBottomStyle().toString()).append(", "); //NOI18N
                        builder.append(bs.getLeftStyle().toString());
                    } else {
                        if (property.equals("-fx-border-width")) { //NOI18N
                            final var bw = bs.getWidths();
                            if (MathUtils.equals(bw.getTop(), bw.getBottom())
                                    && MathUtils.equals(bw.getRight(), bw.getBottom())
                                    && MathUtils.equals(bw.getLeft(), bw.getBottom())) {
                                builder.append(EditorUtils.valAsStr(bw.getBottom()));
                            } else {
                                builder.append(EditorUtils.valAsStr(bw.getTop())).append(" "). //NOI18N
                                        append(EditorUtils.valAsStr(bw.getRight())).append(" "). //NOI18N
                                        append(EditorUtils.valAsStr(bw.getBottom())).append(" "). //NOI18N
                                        append(EditorUtils.valAsStr(bw.getLeft()));
                            }
                        }
                    }
                }
            }
        }
        return builder.toString();
    }

    private static String paintToString(final Paint p) {
        if (p instanceof Color) {
            return getColorAsString((Color) p).toLowerCase(Locale.ROOT);
        } else {
            var gradient = p.toString();
            // Workaround for RT-22910
            gradient = gradient.replaceAll("0x", "#");//NOI18N
            gradient = removeDotZeroPxPercent(gradient);
            return gradient;
        }
    }

    private static String fontToString(final String property, final Font font) {
        if (property == null) {
            return removeAllDotZero(font.toString());
        }
        final var builder = new StringBuilder();
        if (property.equals("-fx-font")) { //NOI18N
            final var size = EditorUtils.valAsStr(font.getSize()); //NOI18N
            final var previewStr = font.getFamily() + " " + size + "px" //NOI18N
                                   + (!font.getName().equals(font.getFamily())
                    && !"Regular".equals(font.getStyle()) //NOI18N
                    ? " (" + font.getStyle() + ")" : ""); //NOI18N
            builder.append(previewStr);
        } else {
            if (property.equals("-fx-font-size")) { //NOI18N
                final var p = font.getSize();
                builder.append(EditorUtils.valAsStr(p)).append("px"); //NOI18N

            } else {
                if (property.equals("-fx-font-family")) { //NOI18N
                    builder.append(font.getFamily());
                } else {
                    if (property.equals("-fx-font-weight")) { //NOI18N
                        // There is no such property.
                        builder.append(removeAllDotZero(font.toString()));
                    } else {
                        if (property.equals("-fx-font-style")) { //NOI18N
                            builder.append(font.getStyle());
                        }
                    }
                }
            }
        }
        return builder.toString();
    }

    private static String insetsValue(final Insets insets) {
        if (MathUtils.equals(insets.getBottom(), insets.getLeft())
                && MathUtils.equals(insets.getRight(), insets.getLeft())
                && MathUtils.equals(insets.getTop(), insets.getLeft())) {
            return EditorUtils.valAsStr(insets.getLeft());
        } else {
            return EditorUtils.valAsStr(insets.getTop()) + " " + EditorUtils.valAsStr(insets.getRight()) //NOI18N
                    + " " + EditorUtils.valAsStr(insets.getBottom()) + " " + EditorUtils.valAsStr(insets.getLeft()); //NOI18N
        }
    }

    private static String effectValue(final Effect effect) {
        final var strBuild = new StringBuilder();
        var adding = effect;
        while (adding != null) {
            strBuild.append(adding.getClass().getSimpleName());
            adding = getEffectInput(adding);
            if (adding != null) {
                strBuild.append(", "); //NOI18N
            }
        }
        return strBuild.toString();
    }

    private static Object subBackgroundFill(final String property, final BackgroundFill bf) {
        if (property == null) {
            return bf;
        }
        if (property.equals("-fx-background-color")) { //NOI18N
            return bf.getFill();
        } else {
            if (property.equals("-fx-background-insets")) { //NOI18N
                return bf.getInsets();
            } else {
                return backgroundFillToString(property, bf);
            }
        }
    }

    private static Object subBackgroundImage(final String property, final BackgroundImage bi) {
        if (property == null) {
            return bi;
        }
        if (property.equals("-fx-background-image")) { //NOI18N
            return bi.getImage();
        } else {
            return backgroundImageToString(property, bi);
        }
    }

    private static Object subBorderImage(final String property, final BorderImage bi) {
        if (property == null) {
            return bi;
        }
        if (property.equals("-fx-border-image")) { //NOI18N
            return bi.getImage();
        } else {
            return borderImageToString(property, bi);
        }
    }

    private static Object subBorderStroke(final String property, final BorderStroke bs) {
        if (property == null) {
            return bs;
        }
        //top, right, bottom, and left 
        if (property.equals("-fx-border-color")) { //NOI18N
            if (bs.getTopStroke().equals(bs.getBottomStroke())
                    && bs.getRightStroke().equals(bs.getBottomStroke())
                    && bs.getLeftStroke().equals(bs.getBottomStroke())) {
                return bs.getBottomStroke();
            } else {
                final var p = new Paint[4];
                p[0] = bs.getTopStroke();
                p[1] = bs.getRightStroke();
                p[2] = bs.getBottomStroke();
                p[3] = bs.getLeftStroke();
                return p;
            }
        } else {
            if (property.equals("-fx-border-insets")) { //NOI18N
                return bs.getInsets();
            } else {
                return borderStrokeToString(property, bs);
            }
        }
    }

    private static Object subFont(final String property, final Font font) {
        if (property == null) {
            return font;
        }
        if (property.equals("-fx-font-size")) { //NOI18N
            return EditorUtils.valAsStr(font.getSize());
        } else {
            if (property.equals("-fx-font-style")) { //NOI18N
                return font.getStyle();
            } else {
                if (property.equals("-fx-font-family")) { //NOI18N
                    return font.getFamily();
                } else {
                    if (property.equals("-fx-font-weight")) { //NOI18N
                        // No font weight
                        return font.getFamily() + " " + font.getStyle(); //NOI18N
                    } else {
                        return font;
                    }
                }
            }
        }
    }

    private static String removeDotZeroPxPercent(String str) {
        // Remove ".0" in strings, for "px" and "%" notations
        str = str.replaceAll("\\.0px", "px"); //NOI18N
        str = str.replaceAll("\\.0em", "em"); //NOI18N
        str = str.replaceAll("\\.0\\%", "%"); //NOI18N
        return str;
    }

    private static String removeAllDotZero(String str) {
        // Remove all ".0" in string
        str = str.replaceAll("\\.0", ""); //NOI18N
        return str;
    }

    private static void handleCornerRadii(final CornerRadii cr, final StringBuilder builder) {
        // Each radius has a vertical and horizontal radius
        // See  http://www.w3.org/TR/css3-background/#the-border-radius 

        final var topLeftH = cr.getTopLeftHorizontalRadius();
        final var topLeftV = cr.getTopLeftVerticalRadius();
        final var topRightH = cr.getTopRightHorizontalRadius();
        final var topRightV = cr.getTopRightVerticalRadius();
        final var bottomLeftH = cr.getBottomLeftHorizontalRadius();
        final var bottomLeftV = cr.getBottomLeftVerticalRadius();
        final var bottomRightH = cr.getBottomRightHorizontalRadius();
        final var bottomRightV = cr.getBottomRightVerticalRadius();

        if (MathUtils.equals(topLeftH, topLeftV) && MathUtils.equals(topRightH, topRightV)
                && MathUtils.equals(bottomLeftH, bottomLeftV) && MathUtils.equals(bottomRightH, bottomRightV)) {
            if (MathUtils.equals(topLeftH, topRightH) && MathUtils.equals(topRightH, bottomLeftH)
                    && MathUtils.equals(bottomLeftH, bottomRightH)) {
                // Same radius for all => single value
                builder.append(EditorUtils.valAsStr(topLeftH));
            } else {
                // Same value for vertical and horizontal radii 
                // => 4 values for topLeft, topRight, bottomLeft, bottomRight
                builder.append(EditorUtils.valAsStr(topLeftH)).append(" "). //NOI18N
                        append(EditorUtils.valAsStr(topRightH)).append(" "). //NOI18N
                        append(EditorUtils.valAsStr(bottomRightH)).append(" "). //NOI18N
                        append(EditorUtils.valAsStr(bottomLeftH));
            }
        } else {
            // Separate value for each.
            // Syntax: "horizontal values / vertical values"
            builder.append(EditorUtils.valAsStr(topLeftH)).append(" "). //NOI18N
                    append(EditorUtils.valAsStr(topRightH)).append(" "). //NOI18N
                    append(EditorUtils.valAsStr(bottomRightH)).append(" "). //NOI18N
                    append(EditorUtils.valAsStr(bottomLeftH)).
                    append(" / ").//NOI18N
                    append(EditorUtils.valAsStr(topLeftV)).append(" "). //NOI18N
                    append(EditorUtils.valAsStr(topRightV)).append(" "). //NOI18N
                    append(EditorUtils.valAsStr(bottomRightV)).append(" "). //NOI18N
                    append(EditorUtils.valAsStr(bottomLeftV));
        }
    }

//    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"}) //NOI18N
    private static Effect getEffectInput(final Effect effect) {
        Effect found = null;
        try {
            found = (Effect) effect.getClass().getMethod("getInput").invoke(effect); //NOI18N
        } catch (final Throwable e) {
            // DO NOT use multi-catch syntax here, this generates a FindBugs Warning (because of SecurityException catching)
//                e.printStackTrace();
            try {
                found = (Effect) effect.getClass().getMethod("getContentInput").invoke(effect); //NOI18N
            } catch (final Throwable ee) {
                // DO NOT use multi-catch syntax here, this generates a FindBugs Warning (because of SecurityException catching)
//                    ee.printStackTrace();
            }
        }
        return found;
    }
}
