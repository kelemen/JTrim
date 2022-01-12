package org.jtrim2.build;

import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

public final class JTrimProjectInfo {
    private final Property<String> displayName;
    private final Property<String> description;

    public JTrimProjectInfo(Project project) {
        ObjectFactory objects = project.getObjects();

        this.displayName = objects.property(String.class);
        this.displayName.set(project.getName());

        this.description = objects.property(String.class);
        this.description.set(this.displayName.map(value -> {
            String projectDescription = project.getDescription();
            return projectDescription != null ? projectDescription : value;
        }));
    }

    public Property<String> getDisplayName() {
        return displayName;
    }

    public Property<String> getDescription() {
        return description;
    }
}
