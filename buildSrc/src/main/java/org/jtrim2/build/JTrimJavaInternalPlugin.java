package org.jtrim2.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.javadoc.Javadoc;

public final class JTrimJavaInternalPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        ProjectUtils.applyPlugin(project, JTrimJavaBasePlugin.class);
        new CheckStyleConfigurer(project, "internal").configure();

        project.getTasks().withType(Javadoc.class).configureEach(task -> {
            task.setEnabled(false);
        });
    }
}
