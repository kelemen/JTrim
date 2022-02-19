package org.jtrim2.build;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

public abstract class GitRepoService implements BuildService<GitRepoService.Parameters>, AutoCloseable {
    public static final String PROJECT_SERVICE_NAME = "default-git-repo-service";
    public static final String API_DOCS_SERVICE_NAME = "api-docs-git-repo-service";

    private final File repoRoot;
    private final Repository repository;

    public GitRepoService() throws IOException {
        this.repoRoot = getParameters().getRepositoryLocation().get();
        this.repository = FileRepositoryBuilder.create(new File(repoRoot, ".git"));
    }

    public static void register(Project project, String name, Action<? super Property<File>> locationConfig) {
        register(project.getGradle(), name, locationConfig);
    }

    public static void register(Gradle gradle, String name, Action<? super Property<File>> locationConfig) {
        Objects.requireNonNull(gradle, "gradle");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(locationConfig, "locationConfig");

        gradle.getSharedServices().registerIfAbsent(
                name,
                GitRepoService.class,
                spec -> {
                    spec.getMaxParallelUsages().set(1);
                    locationConfig.execute(spec.getParameters().getRepositoryLocation());
                }
        );
    }

    public static Provider<GitRepoService> getService(Project project, String name) {
        return getService(project.getGradle(), name);
    }

    @SuppressWarnings("unchecked")
    public static Provider<GitRepoService> getService(Gradle gradle, String name) {
        return (Provider<GitRepoService>) gradle.getSharedServices()
                .getRegistrations()
                .named(name)
                .get()
                .getService();
    }

    public File getRepoRoot() {
        return repoRoot;
    }

    public final GitWrapper git(ObjectFactory objects) {
        return new GitWrapper(objects, repository);
    }

    @Override
    public void close() {
        repository.close();
    }

    public interface Parameters extends BuildServiceParameters {
        public Property<File> getRepositoryLocation();
    }
}
