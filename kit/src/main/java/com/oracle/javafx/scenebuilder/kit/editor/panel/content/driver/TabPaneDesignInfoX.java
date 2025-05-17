/*
 * Copyright (c) 2017, 2024 Gluon and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver;

import com.oracle.javafx.scenebuilder.kit.util.Deprecation;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * @treatAsPrivate
 * A temporary class that should extend TabDesignInfo and adds
 * some additional verbs for managing TabPane at design time.
 * This could potentially move to TabDesignInfo some day.
 *
 */
public class TabPaneDesignInfoX /* extends TabDesignInfo */ {

    public TabPaneDesignInfoX() {
        // no-op
    }

    /**
     * Returns the node representing the tab header in the TabPane skin.
     * @param tabPane
     * @param tab
     * @return 
     */
    public Node getTabNode(final TabPane tabPane, final Tab tab) {
        assert tabPane != null;
        assert tabPane.getTabs().contains(tab);
        
        // Looks for the sub nodes which match the .tab CSS selector
        final var set = tabPane.lookupAll(".tab"); //NOI18N
        
        // Searches the result for the node associated to 'tab'.
        // This item has (Tab.class, tab) in its property list.
        Node result = null;
        final var it = set.iterator();
        while ((result == null) && it.hasNext()) {
            final var n = it.next();
            if (n.getProperties().get(Tab.class) == tab) {
                result = n;
            }
        }

        return result;
    }
    
    /**
     * Returns the node representing the content area in the TabPane skin.
     * @param tabPane
     * @param tab
     * @return 
     */
    public Node getContentNode(final TabPane tabPane) {
        assert tabPane != null;
        
        final Node result;
       
//        if (tabPane.getSkin() != null) {
//            assert tabPane.getSkin() instanceof TabPaneSkin;
//            final TabPaneSkin tabPaneSkin = (TabPaneSkin) tabPane.getSkin();
//            result = tabPaneSkin.getSelectedTabContentRegion();
//        } else {
//            result = null;
//        }

        final var selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            result = selectedTab.getContent();
        } else {
            result = null;
        }

        return result;
    }
    
    
    /**
     * Returns the node representing the pulldown menu in the TabPane skin.
     */
    public Node getControlMenuNode(final TabPane tabPane) {
        assert tabPane != null;
        
        // Looks for the sub node which matches the '.control-buttons-tab' selector
        return tabPane.lookup(".control-buttons-tab"); //NOI18N
    }
    
    
    /**
     * Returns the tab below (sceneX, sceneY) if any.
     * If (sceneX, sceneY) is over a tab header, returns the matching Tab.
     * If (sceneX, sceneY) is over the tab content area, returns the Tab
     * currently selected.
     * 
     * @param tabPane a tab pane
     * @param sceneX x in scene coordinate space
     * @param sceneY y in scene coordinate space
     * @return null or the tab below (sceneX, sceneY).
     */
    public Tab lookupTab(final TabPane tabPane, final double sceneX, final double sceneY) {
        Tab result = null;
        
        // The control menu may cover a tab header.
        // So we check first if (sceneX, sceneY) is in the control menu.
        // If yes, we return null because the control menu is considered
        // as a piece of the tab pane.
        final var controlMenuNode = getControlMenuNode(tabPane);
        final boolean insideControlMenu;
        if (controlMenuNode == null) {
            insideControlMenu = false;
        } else {
            final var p = controlMenuNode.sceneToLocal(sceneX, sceneY, true /* rootScene */);
            insideControlMenu = controlMenuNode.contains(p);
        }
        
        // If not inside the control menu, then checks:
        //  1) (sceneX, sceneY) is over a tab header => returns the matching tab
        //  2) (sceneX, sceneY) is over the content area => returns the selected tab
        if (!insideControlMenu) {
            
            // Checks the headers.
            final var it = tabPane.getTabs().iterator();
            while ((result == null) && it.hasNext()) {
                final var tab = it.next();
                final var tabNode = getTabNode(tabPane, tab);
                assert tabNode != null;
                final var p = tabNode.sceneToLocal(sceneX, sceneY, true /* rootScene */);
                if (tabNode.contains(p)) {
                    result = tab;
                }
            }

            // Checks the content area
            if (result == null) {
                final var contentNode = getContentNode(tabPane);
                if (contentNode != null) {
                    final var p = contentNode.sceneToLocal(sceneX, sceneY, true /* rootScene */);
                    if (contentNode.contains(p)) {
                        result = tabPane.getSelectionModel().getSelectedItem();
                    }
                }
            }
        }
        
        
        return result;
    }

    
    public Bounds computeTabBounds(final TabPane tabPane, final Tab tab) {
        final var tabNode = getTabNode(tabPane, tab);
        final var b = tabNode.getLayoutBounds();

        // Convert b from tabNode local space to tabPane local space
        final var min = Deprecation.localToLocal(tabNode, b.getMinX(), b.getMinY(), tabPane);
        final var max = Deprecation.localToLocal(tabNode, b.getMaxX(), b.getMaxY(), tabPane);
        return makeBoundingBox(min, max);
    }
    
    
    public Bounds computeContentAreaBounds(final TabPane tabPane) {
        final var contentNode = getContentNode(tabPane);
        assert contentNode != null;
        final var b = contentNode.getLayoutBounds();
        
        // Convert b from contentNode local space to tabPane local space
        final var min = Deprecation.localToLocal(contentNode, b.getMinX(), b.getMinY(), tabPane);
        final var max = Deprecation.localToLocal(contentNode, b.getMaxX(), b.getMaxY(), tabPane);
        return makeBoundingBox(min, max);
    }
    
    private static BoundingBox makeBoundingBox(final Point2D p1, final Point2D p2) {
        return new BoundingBox(
                Math.min(p1.getX(), p2.getX()),
                Math.min(p1.getY(), p2.getY()),
                Math.abs(p2.getX() - p1.getX()),
                Math.abs(p2.getY() - p1.getY()));
    }
}
