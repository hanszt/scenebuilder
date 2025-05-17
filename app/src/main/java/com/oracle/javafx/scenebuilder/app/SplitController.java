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
package com.oracle.javafx.scenebuilder.app;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.SplitPane.Divider;

/**
 *
 */
public class SplitController {

    public enum Target {

        FIRST, LAST
    };

    private final SplitPane splitPane;
    private final Target target;
    private final Node targetNode;
    private double dividerPosition = -1.0;

    public SplitController(final SplitPane splitPane, final Target target) {
        assert splitPane != null;
        assert splitPane.getItems().size() >= 1;

        this.splitPane = splitPane;
        this.target = target;

        final List<Node> children = splitPane.getItems();
        final var targetIndex = (target == Target.FIRST) ? 0 : children.size() - 1;
        this.targetNode = children.get(targetIndex);
    }

    public DoubleProperty position() {
        final var divider = getTargetDivider();
        return divider == null ? null : divider.positionProperty();
    }

    public double getPosition() {
        final var divider = getTargetDivider();
        return divider == null ? -1.0 : divider.getPosition();
    }

    public void setPosition(final double value) {
        final var divider = getTargetDivider();
        if (divider != null) {
            divider.setPosition(value);
        }
        dividerPosition = value;
    }

    public void showTarget() {
        if (!isTargetVisible()) {
            // Put the target node back in the split pane items
            if (target == Target.FIRST) {
                splitPane.getItems().addFirst(targetNode);
            } else {
                splitPane.getItems().add(targetNode);
            }

            // Restore the target divider position (if any)
            final List<Divider> dividers = splitPane.getDividers();
            if ((!dividers.isEmpty()) && (dividerPosition != -1)) { // (1)
                final var divider = getTargetDivider();
                assert divider != null; // Because of (1)
                divider.setPosition(dividerPosition);
            }
        }
    }

    public void hideTarget() {

        if (isTargetVisible()) {

            final List<Divider> dividers = splitPane.getDividers();
            final var positionsList = asList(splitPane.getDividerPositions());

            // Backup the target divider positions (if any)
            // so we can restore it on showing
            final var targetDivider = getTargetDivider();
            if (targetDivider != null) {
                dividerPosition = targetDivider.getPosition();
                final var targetDividerIndex = target == Target.FIRST ? 0 : dividers.size() - 1;
                positionsList.remove(targetDividerIndex);
            }

            // Removes the target node from the split pane items
            splitPane.getItems().remove(targetNode);

            // Set back remaining dividers positions if any
            if (!positionsList.isEmpty()) {
                final var positionsArray = toArray(positionsList);
                splitPane.setDividerPositions(positionsArray);
            }
        }
    }

    public void toggleTarget() {
        if (isTargetVisible()) {
            hideTarget();
        } else {
            showTarget();
        }
    }

    public void setTargetVisible(final boolean visible) {
        if (visible) {
            showTarget();
        } else {
            hideTarget();
        }
    }

    public boolean isTargetVisible() {
        return splitPane.getItems().contains(targetNode);
    }

    private Divider getTargetDivider() {
        final Divider divider;
        final List<Divider> dividers = splitPane.getDividers();
        if (!dividers.isEmpty()) {
            if (target == Target.FIRST) {
                divider = dividers.getFirst();
            } else {
                divider = dividers.getLast();
            }
        } else {
            divider = null;
        }
        return divider;
    }

    // Arrays.asList does not work with primitive types
    private static List<Double> asList(final double[] array) {
        final List<Double> list = new ArrayList<>(array.length);
        for (final var d : array) {
            list.add(d);
        }
        return list;
    }

    // List.toArray does not work with primitive types
    private static double[] toArray(final List<Double> list) {
        final var array = new double[list.size()];
        for (var i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
