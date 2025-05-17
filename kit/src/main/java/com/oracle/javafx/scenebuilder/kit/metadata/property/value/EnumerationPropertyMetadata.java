/*
 * Copyright (c) 2016, Gluon and/or its affiliates.
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

import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyT;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PrefixedValue;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 
 */
public class EnumerationPropertyMetadata extends ValuePropertyMetadata {
    
    public static final String EQUIV_NONE = "NONE"; //NOI18N
    public static final String EQUIV_AUTOMATIC = "AUTOMATIC"; //NOI18N
    public static final String EQUIV_INHERITED = "INHERIT"; //NOI18N
    
    private final Class<?> enumClass;
    private final Enum<?> defaultValue;
    private final String nullEquivalent;
    private List<String> validValues;

    public EnumerationPropertyMetadata(final PropertyName name, final Class<?> enumClass,
                                       final boolean readWrite, final Enum<?> defaultValue, final InspectorPath inspectorPath) {
        super(name, readWrite, inspectorPath);
        assert enumClass.isEnum();
        assert (!readWrite) || (defaultValue != null);
        this.enumClass = enumClass;
        this.defaultValue = defaultValue;
        this.nullEquivalent = null;
    }
    
    public EnumerationPropertyMetadata(final PropertyName name, final Class<?> enumClass,
                                       final String nullEquivalent, final boolean readWrite, final InspectorPath inspectorPath) {
        super(name, readWrite, inspectorPath);
        assert enumClass.isEnum();
        assert nullEquivalent != null;
        this.enumClass = enumClass;
        this.defaultValue = null;
        this.nullEquivalent = nullEquivalent;
    }
    
    public String getValue(final FXOMInstance fxomInstance) {
        final String result;
        
        if (isReadWrite()) {
            final var fxomProperty = fxomInstance.getProperties().get(getName());
            if (fxomProperty == null) {
                // propertyName is not specified in the fxom instance.
                // We return the default value specified in the metadata of the
                // property
                result = getDefaultValue();
            } else {
                assert fxomProperty instanceof FXOMPropertyT;
                final var fxomPropertyT = (FXOMPropertyT) fxomProperty;
                final var pv = new PrefixedValue(fxomPropertyT.getValue());
                if (pv.isBindingExpression()) {
                    result = getDefaultValue();
                } else {
                    result = fxomPropertyT.getValue();
                }
            }
        } else {
            final var o = getName().getValue(fxomInstance.getSceneGraphObject());
            if (o == null) {
                result = getDefaultValue();
            } else {
                assert o.getClass() == enumClass;
                result = o.toString();
            }
        }
        
        return result;
    }

    public void setValue(final FXOMInstance fxomInstance, final String value) {
        assert isReadWrite();
        assert value != null;
        
        final var fxomProperty = fxomInstance.getProperties().get(getName());
        if (fxomProperty == null) {
            // propertyName is not specified in the fxom instance.
            if (!value.equals(getDefaultValue())) {
                // We insert a new fxom property
                final var newProperty
                        = new FXOMPropertyT(fxomInstance.getFxomDocument(),
                        getName(), value);
                newProperty.addToParentInstance(-1, fxomInstance);
            }
        } else {
            assert fxomProperty instanceof FXOMPropertyT;
            final var fxomPropertyT = (FXOMPropertyT) fxomProperty;
            if (value.equals(getDefaultValue())) {
                fxomPropertyT.removeFromParentInstance();
            } else {
                fxomPropertyT.setValue(value);
            }
        }
    }
    
    public String getDefaultValue() {
        final String result;
        if (isReadWrite()) {
            assert (defaultValue == null) == (nullEquivalent != null);
            result = (defaultValue == null) ? nullEquivalent : defaultValue.toString();
        } else {
            result = null;
        }
        return result;
    }
    
    public List<String> getValidValues() {
        if (validValues == null) {
            validValues = new ArrayList<>();

            for (final var e : enumClass.getEnumConstants()) {
                validValues.add(e.toString());
            }
            if (nullEquivalent != null) {
                assert defaultValue == null;
                if (!validValues.contains(nullEquivalent)) {
                    validValues.addFirst(nullEquivalent);
                }
            }
        }
        return validValues;
    }

    public int getValidValuesNumber() {
        return getValidValues().size();
    }
    
    /*
     * ValuePropertyMetadata
     */
    
    @Override
    public Class<?> getValueClass() {
        return enumClass;
    }

    @Override
    public Object getDefaultValueObject() {
        return getDefaultValue();
    }

    @Override
    public Object getValueObject(final FXOMInstance fxomInstance) {
        return getValue(fxomInstance);
    }

    @Override
    public void setValueObject(final FXOMInstance fxomInstance, final Object valueObject) {
        assert valueObject instanceof String;
        setValue(fxomInstance, (String) valueObject);
    }
}
