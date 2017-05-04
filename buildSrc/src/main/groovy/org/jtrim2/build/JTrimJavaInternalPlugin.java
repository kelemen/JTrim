package org.jtrim2.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class JTrimJavaInternalPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        ProjectUtils.applyPlugin(project, JTrimJavaBasePlugin.class);
        new CheckStyleConfigurer(project, "internal").configure();
    }
}
