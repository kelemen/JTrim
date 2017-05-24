package org.jtrim2.build;

import java.util.Objects;
import org.gradle.api.Project;
import org.gradle.api.artifacts.maven.MavenPom;
import org.gradle.api.artifacts.maven.PomFilterContainer;
import org.gradle.api.plugins.MavenRepositoryHandlerConvention;
import org.gradle.api.tasks.Upload;
import org.gradle.plugins.signing.SigningExtension;

import static org.jtrim2.build.ProjectUtils.*;

public final class MavenConfigurer {
    private final Project project;

    public MavenConfigurer(Project project) {
        this.project = Objects.requireNonNull(project);
    }

    public void configure() {
        ProjectUtils.applyPlugin(project, "maven");

        configureSignature();

        project.afterEvaluate((evaluatedProject) -> {
            PomFilterContainer installer = getMavenHandler("install").mavenInstaller();

            MavenPom pom = installer.getPom();
            GroovyUtils.configureMavenPom(project, pom);

            configureUploadArchives();
        });
    }

    private void configureSignature() {
        ProjectUtils.applyPlugin(project, "signing");

        if (ReleaseUtils.isRelease(project)) {
            SigningExtension signing = project.getExtensions().getByType(SigningExtension.class);
            signing.sign(project.getConfigurations().getByName("archives"));
        }
    }

    private MavenRepositoryHandlerConvention getMavenHandler(String taskName) {
        Upload upload = (Upload)project.getTasks().getByName(taskName);
        return ProjectUtils.getConvention(upload.getRepositories(), MavenRepositoryHandlerConvention.class);
    }

    private void configureUploadArchives() {
        GroovyUtils.configureMavenDeployer(project);
        configureBinTray();
    }

    private void configureBinTray() {
        String jtrimRepoUrl = getStringProperty(project,
                "publishJTrimRepoUrl",
                "https://api.bintray.com/maven/kelemen/maven/JTrim2");
        String repoUser = getStringProperty(project,
                "publishJTrimUserName",
                "kelemen");
        String repoPassword = getStringProperty(project,
                "publishJTrimPassword",
                "");

        GroovyUtils.addDeployRepository(project, jtrimRepoUrl, repoUser, repoPassword);
    }
}
