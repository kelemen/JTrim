package org.jtrim2.build;

import java.util.Objects;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.maven.MavenPom;
import org.gradle.api.artifacts.maven.PomFilterContainer;
import org.gradle.api.plugins.MavenRepositoryHandlerConvention;
import org.gradle.api.tasks.Upload;
import org.gradle.plugins.signing.SigningExtension;

import static org.jtrim2.build.ProjectUtils.*;

public final class MavenConfigurer {
    private static final String BINTRAY_UPLOAD_NAME = "uploadArchives";
    private static final String CENTRAL_UPLOAD_NAME = "uploadArchivesCentral";

    private final Project project;

    public MavenConfigurer(Project project) {
        this.project = Objects.requireNonNull(project);
    }

    public void configure() {
        ProjectUtils.applyPlugin(project, "maven");

        configureSignature();

        project.getTasks().register("uploadAll", task -> {
            task.dependsOn(BINTRAY_UPLOAD_NAME, CENTRAL_UPLOAD_NAME);
        });

        project.afterEvaluate(evaluatedProject -> {
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
        Upload upload = (Upload) project.getTasks().getByName(taskName);
        return ProjectUtils.getConvention(upload.getRepositories(), MavenRepositoryHandlerConvention.class);
    }

    private void configureUploadArchives() {
        configureBinTray();
        configureCentral();
    }

    private void configureBinTray() {
        Task uploadTask = project.getTasks().getByName(BINTRAY_UPLOAD_NAME);

        GroovyUtils.configureMavenDeployer(uploadTask);

        String jtrimRepoUrl = getStringProperty(project,
                "publishJTrimRepoUrl",
                "https://api.bintray.com/maven/kelemen/maven/JTrim2");
        String repoUser = getStringProperty(project,
                "publishJTrimUserName",
                "kelemen");
        String repoPassword = getStringProperty(project,
                "publishJTrimPassword",
                "");

        GroovyUtils.addDeployRepository(uploadTask, jtrimRepoUrl, repoUser, repoPassword);
    }

    private void configureCentral() {
        Upload uploadTask = project.getTasks().create(CENTRAL_UPLOAD_NAME, Upload.class);
        uploadTask.setConfiguration(project.getConfigurations().getByName("archives"));

        GroovyUtils.configureMavenDeployer(uploadTask);

        String repoUrl = getStringProperty(project,
                "publishCentralRepoUrl",
                "https://oss.sonatype.org/service/local/staging/deploy/maven2");
        String repoUser = getStringProperty(project,
                "publishCentralUserName",
                "");
        String repoPassword = getStringProperty(project,
                "publishCentralPassword",
                "");

        GroovyUtils.addDeployRepository(uploadTask, repoUrl, repoUser, repoPassword);
    }
}
