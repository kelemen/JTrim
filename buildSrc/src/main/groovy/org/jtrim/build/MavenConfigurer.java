package org.jtrim.build;

import java.util.Objects;
import org.gradle.api.Project;
import org.gradle.api.artifacts.maven.MavenPom;
import org.gradle.api.artifacts.maven.PomFilterContainer;
import org.gradle.api.plugins.MavenRepositoryHandlerConvention;
import org.gradle.api.tasks.Upload;

import static org.jtrim.build.ProjectUtils.*;

public final class MavenConfigurer {
    private final Project project;

    public MavenConfigurer(Project project) {
        this.project = Objects.requireNonNull(project);
    }

    public void configure() {
        ProjectUtils.applyPlugin(project, "maven");

        PomFilterContainer installer = getMavenHandler("install").mavenInstaller();

        MavenPom pom = installer.getPom();
        pom.setGroupId(project.getGroup().toString());
        pom.setArtifactId(project.getName());
        pom.setVersion(project.getVersion().toString());

        configureUploadArchives();
    }

    private MavenRepositoryHandlerConvention getMavenHandler(String taskName) {
        Upload upload = (Upload)project.getTasks().getByName(taskName);
        return ProjectUtils.getConvention(upload.getRepositories(), MavenRepositoryHandlerConvention.class);
    }

    private void configureUploadArchives() {
        String jtrimRepoUrl = getStringProperty(project,
                "publishJTrimRepoUrl",
                "https://api.bintray.com/maven/kelemen/maven/JTrim");
        String repoUser = getStringProperty(project,
                "publishJTrimUserName",
                "kelemen");
        String repoPassword = getStringProperty(project,
                "publishJTrimPassword",
                "");

        GroovyUtils.setupMavenDeployer(project, jtrimRepoUrl, repoUser, repoPassword);
    }
}
