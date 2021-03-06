package org.jtrim2.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class JTrimRootPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        ProjectUtils.applyPlugin(project, JTrimGroupPlugin.class);

        project.getExtensions().add("development", JTrimDevelopment.class);
        project.getExtensions().add("license", LicenseInfo.class);
    }
}
