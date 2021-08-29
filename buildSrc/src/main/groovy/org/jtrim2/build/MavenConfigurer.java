package org.jtrim2.build;

import java.util.Objects;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.plugins.signing.SigningExtension;

import static org.jtrim2.build.ProjectUtils.*;

public final class MavenConfigurer {
    private final Project project;

    public MavenConfigurer(Project project) {
        this.project = Objects.requireNonNull(project);
    }

    public void configure() {
        ProjectUtils.applyPlugin(project, "maven-publish");
        ProjectUtils.applyPlugin(project, "signing");

        PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
        SigningExtension signing = project.getExtensions().getByType(SigningExtension.class);

        publishing.publications(publications -> {
            MavenPublication mainPublication = publications.create("main", MavenPublication.class, publication -> {
                configureMainPublication(publication);
            });
            signing.sign(mainPublication);
        });

        publishing.repositories(repositories -> {
            repositories.mavenCentral(this::configureCentral);
        });
    }

    private void configureCentral(MavenArtifactRepository repo) {
        repo.setUrl(getStringProperty(project,
                "publishCentralRepoUrl",
                "https://oss.sonatype.org/service/local/staging/deploy/maven2")
        );
        repo.credentials(credentials -> {
            credentials.setUsername(getStringProperty(
                    project,
                    "publishCentralUserName",
                    ""));
            credentials.setPassword(getStringProperty(
                    project,
                    "publishCentralPassword",
                    ""));
        });
    }

    private void configureMainPublication(MavenPublication publication) {
        publication.from(project.getComponents().getByName("java"));

        publication.pom(pom -> {
            JTrimProjectInfo projectInfo = ProjectUtils.getProjectInfo(project);
            JTrimDevelopment jtrimDev = ProjectUtils.getDevelopmentInfo(project);

            pom.setPackaging("jar");
            pom.getName().set(projectInfo.getDisplayName());
            pom.getDescription().set(projectInfo.getDescription());

            pom.getUrl().set(jtrimDev.getUrl());
            pom.scm(scm -> {
                scm.getConnection().set(jtrimDev.getScmUrl());
                scm.getDeveloperConnection().set(jtrimDev.getScmUrl());
                scm.getUrl().set(jtrimDev.getUrl());
            });

            pom.licenses(licenses -> {
                licenses.license(license -> {
                    LicenseInfo licenseInfo = ProjectUtils.getLicenseInfo(project);
                    license.getName().set(licenseInfo.getName());
                    license.getUrl().set(licenseInfo.getUrl());
                });
            });

            jtrimDev.getDevelopers().whenObjectAdded(addedDev -> {
                pom.developers(developers -> {
                    developers.developer(developer -> {
                        developer.getId().set(addedDev.getName());
                        developer.getName().set(addedDev.getDisplayName());
                        developer.getEmail().set(addedDev.getEmail());
                    });
                });
            });
            jtrimDev.getDevelopers().whenObjectRemoved(removedDev -> {
                throw new IllegalStateException("Cannot handle removal of developer.");
            });
        });
    }
}
