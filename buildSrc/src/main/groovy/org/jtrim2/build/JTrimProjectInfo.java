package org.jtrim2.build;

import java.util.Objects;
import org.gradle.api.Project;

public final class JTrimProjectInfo {
    private final Project project;
    private String displayName;

    public JTrimProjectInfo(Project project) {
        this.project = Objects.requireNonNull(project, "project");
    }

    public String getDisplayName() {
        return displayName != null
                ? displayName
                : project.getName();
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDescription(String description) {
        project.setDescription(description);
    }

    public String getDescription() {
        return project.getDescription();
    }
}
