package com.oracle.javafx.scenebuilder.kit.editor.job.wrap;

import java.util.ArrayList;
import java.util.List;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.atomic.AddPropertyJob;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMPropertyC;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;

/**
 * Main class used for the wrap jobs using the new window's SCENE property.
 */
public class AbstractWrapInWindowJob extends AbstractWrapInJob {
    public AbstractWrapInWindowJob(final EditorController editorController) {
        super(editorController);
    }

    @Override
    protected boolean canWrapIn() {
        final var selection = getEditorController().getSelection();
        if (selection.isEmpty()) {
            return false;
        }

        final var asg = selection.getGroup();
        if (!(asg instanceof final ObjectSelectionGroup osg)) {
            return false;
        }

        if (!osg.hasSingleParent()) {
            return false;
        }

        // Can wrap in SCENE property single selection only
        if (osg.getItems().size() != 1) {
            return false;
        }

        // Selected object must be an instance of javafx.scene.Scene
        for (final var fxomObject : osg.getItems()) {
            if (!(fxomObject.getSceneGraphObject() instanceof javafx.scene.Scene)) {
                return false;
            }
        }

        // Selected object must be root object
        final var parent = osg.getAncestor();
        if (parent != null) { // selection != root object
            return false;
        }

        return true;
    }

    @Override
    protected List<Job> wrapChildrenJobs(final List<FXOMObject> children) {
        final List<Job> jobs = new ArrayList<>();

        final var newContainerMask = new DesignHierarchyMask(newContainer);
        assert newContainerMask.isAcceptingAccessory(DesignHierarchyMask.Accessory.SCENE);

        // Retrieve the new container property name to be used
        final var newContainerPropertyName = new PropertyName("scene"); //NOI18N
        // Create the new container property
        final var newContainerProperty = new FXOMPropertyC(
                newContainer.getFxomDocument(), newContainerPropertyName);

        assert children.size() == 1;

        // Add the children to the new container
        jobs.addAll(addChildrenJobs(newContainerProperty, children));

        // Add the new container property to the new container instance
        assert newContainerProperty.getParentInstance() == null;
        final Job addPropertyJob = new AddPropertyJob(
                newContainerProperty,
                newContainer,
                -1, getEditorController());
        jobs.add(addPropertyJob);

        return jobs;
    }
}
