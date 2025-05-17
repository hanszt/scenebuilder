package com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver;

import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AbstractDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.drag.target.AccessoryDropTarget;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.curve.AbstractCurveEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles.AbstractHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.handles.WindowHandles;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring.AbstractPring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.pring.NodePring;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.resizer.AbstractResizer;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.driver.tring.AbstractTring;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

public class WindowDriver extends AbstractDriver {

    public WindowDriver(final ContentPanelController contentPanelController) {
        super(contentPanelController);
    }

    @Override
    public AbstractHandles<?> makeHandles(final FXOMObject fxomObject) {
        assert fxomObject.getSceneGraphObject() instanceof Window;
        assert fxomObject instanceof FXOMInstance;
        return new WindowHandles(contentPanelController, (FXOMInstance) fxomObject);
    }

    @Override
    public AbstractTring<?> makeTring(final AbstractDropTarget dropTarget) {
        return null;
    }

    @Override
    public AbstractPring<?> makePring(final FXOMObject fxomObject) {
        assert fxomObject.getSceneGraphObject() instanceof Window;
        final var windowDesignHierarchyMask = new DesignHierarchyMask(fxomObject);
        final var scene = windowDesignHierarchyMask.getAccessory(DesignHierarchyMask.Accessory.SCENE);
        assert scene != null : "makePring should have only been called if the Window has a scene";
        assert scene.getSceneGraphObject() instanceof Scene;
        assert scene instanceof FXOMInstance;
        final var sceneDesignHierarchyMask = new DesignHierarchyMask(scene);
        final var root = sceneDesignHierarchyMask.getAccessory(DesignHierarchyMask.Accessory.ROOT);
        assert root != null;
        assert root.getSceneGraphObject() instanceof Node;
        assert root instanceof FXOMInstance;
        return new NodePring(contentPanelController, (FXOMInstance) root);
    }

    @Override
    public AbstractResizer<?> makeResizer(final FXOMObject fxomObject) {
        // Resize gesture does not apply to Windows
        return null;
    }

    @Override
    public AbstractCurveEditor<?> makeCurveEditor(final FXOMObject fxomObject) {
        return null;
    }

    @Override
    public FXOMObject refinePick(final Node hitNode, final double sceneX, final double sceneY, final FXOMObject fxomObject) {
        return fxomObject;
    }

    @Override
    public AbstractDropTarget makeDropTarget(final FXOMObject fxomObject, final double sceneX, final double sceneY) {
        assert fxomObject instanceof FXOMInstance;
        return new AccessoryDropTarget((FXOMInstance) fxomObject, DesignHierarchyMask.Accessory.SCENE);
    }

    @Override
    public Node getInlineEditorBounds(final FXOMObject fxomObject) {
        return null;
    }

    @Override
    public boolean intersectsBounds(final FXOMObject fxomObject, final Bounds bounds) {
        assert fxomObject.getSceneGraphObject() instanceof Window;
        final var windowDesignHierarchyMask = new DesignHierarchyMask(fxomObject);
        final var scene = windowDesignHierarchyMask.getAccessory(DesignHierarchyMask.Accessory.SCENE);
        if (scene == null) {
            return false;
        }
        assert scene.getSceneGraphObject() instanceof Scene;
        assert scene instanceof FXOMInstance;
        final var sceneDesignHierarchyMask = new DesignHierarchyMask(scene);
        final var root = sceneDesignHierarchyMask.getAccessory(DesignHierarchyMask.Accessory.ROOT);
        assert root != null;
        assert root.getSceneGraphObject() instanceof Node;
        final var rootNode = (Node) root.getSceneGraphObject();
        final var rootNodeBounds = rootNode.localToScene(rootNode.getLayoutBounds(), true /* rootScene */);
        return rootNodeBounds.intersects(bounds);
    }
}
