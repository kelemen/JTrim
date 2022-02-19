package org.jtrim2.build;

import java.io.File;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

public final class JTrimRootPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        configureDefaultGitRepoService(project);
        configureApiDocsGitRepoService(project);

        ProjectUtils.applyPlugin(project, JTrimGroupPlugin.class);

        project.getExtensions().add("development", JTrimDevelopment.class);
        project.getExtensions().add("license", LicenseInfo.class);
    }

    private void configureDefaultGitRepoService(Project project) {
        File rootDir = project.getRootDir();
        GitRepoService.register(project, GitRepoService.PROJECT_SERVICE_NAME, repo -> repo.set(rootDir));
    }

    private void configureApiDocsGitRepoService(Project project) {
        Provider<String> apiDocsPathStrRef = project.getProviders().gradleProperty("releaseApiDocRepo");
        GitRepoService.register(
                project,
                GitRepoService.API_DOCS_SERVICE_NAME,
                repo -> repo.set(apiDocsPathStrRef.map(File::new))
        );
    }
}
