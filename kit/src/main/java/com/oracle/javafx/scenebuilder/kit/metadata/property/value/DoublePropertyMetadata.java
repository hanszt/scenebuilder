/*
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
package com.oracle.javafx.scenebuilder.kit.metadata.property.value;

import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import javafx.scene.layout.Region;

/**
 *
 * 
 */
public class DoublePropertyMetadata extends TextEncodablePropertyMetadata<java.lang.Double> {
    
    public enum DoubleKind {
        COORDINATE,         // any double
        NULLABLE_COORDINATE,// any double or null
        SIZE,               // x >= 0
        USE_COMPUTED_SIZE,      // x >= 0 or x == Region.USE_COMPUTED_SIZE
        USE_PREF_SIZE,          // x >= 0 or x == Region.USE_COMPUTED_SIZE or x == Region.USE_PREF_SIZE
        EFFECT_SIZE,        // 0 <= x <= 255.0
        ANGLE,              // 0 <= x < 360
        OPACITY,            // 0 <= x <= 1.0
        PROGRESS,           // 0 <= x <= 1.0
        PERCENTAGE          // -1 or 0 <= x <= 100.0
    };

    private final DoubleKind kind;

    public DoublePropertyMetadata(final PropertyName name, final DoubleKind kind,
                                  final boolean readWrite, final Double defaultValue, final InspectorPath inspectorPath) {
        super(name, Double.class, readWrite, defaultValue, inspectorPath);
        assert (kind != DoubleKind.NULLABLE_COORDINATE) || (defaultValue == null);
        this.kind = kind;
    }
    
    public DoubleKind getKind() {
        return kind;
    }
    
    public boolean isValidValue(final Double value) {
        final boolean result;
        
        if (kind == DoubleKind.NULLABLE_COORDINATE) {
            result = true;
        } else if (value == null) {
            result = false;
        } else {
            result = switch (kind) {
                case COORDINATE -> true;
                case SIZE -> (0 <= value);
                case USE_COMPUTED_SIZE -> ((0 <= value) || (value == Region.USE_COMPUTED_SIZE));
                case USE_PREF_SIZE -> (0 <= value)
                                      || (value == Region.USE_COMPUTED_SIZE)
                                      || (value == Region.USE_PREF_SIZE);
                case PERCENTAGE -> (value == -1) || ((0 <= value) && (value <= 100.0));
                case EFFECT_SIZE, ANGLE, OPACITY, PROGRESS -> true;
                default -> {
                    assert false;
                    yield false;
                }
            };
        }
        
        return result;
    }
    
    public Double getCanonicalValue(final Double value) {
        final Double result;
        
        if (value == null) {
            result = null;
        } else {
            result = switch (kind) {
                case COORDINATE, NULLABLE_COORDINATE, SIZE, USE_COMPUTED_SIZE, USE_PREF_SIZE -> value;
                case EFFECT_SIZE -> Math.min(255.0, Math.max(0, value));
                case ANGLE -> Math.IEEEremainder(value, 360.0);
                case OPACITY, PROGRESS -> Math.min(1, Math.max(0, value));
                default -> {
                    assert false;
                    yield value;
                }
            };
        }
        
        return result;
    }

    /*
     * SingleValuePropertyMetadata
     */
    
    @Override
    public Double makeValueFromString(final String string) {
        return Double.valueOf(string);
    }
}
