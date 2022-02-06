package org.jtrim2.build;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.VersionCatalog;
import org.gradle.api.artifacts.VersionCatalogsExtension;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavadocTool;

public final class ProjectUtils {
    public static JavaLanguageVersion getCompileJavaVersion(Project project) {
        return JavaLanguageVersion.of(getVersion(project, "java"));
    }

    public static Provider<JavadocTool> javadocTool(Project project, JavaToolchainService toolchainService) {
        return toolchainService.javadocToolFor(spec -> {
            spec.getLanguageVersion().set(JavaLanguageVersion.of(getVersion(project, "javadocVersion")));
        });
    }

    public static Path scriptFile(Project project, String... subPaths) {
        String[] allPaths = new String[subPaths.length + 1];
        allPaths[0] = "gradle";
        System.arraycopy(subPaths, 0, allPaths, 1, subPaths.length);
        return BuildFileUtils.rootPath(project, allPaths);
    }

    public static void applyScript(Project project, String name) {
        project.apply(Collections.singletonMap("from", scriptFile(project, name)));
    }

    private static VersionCatalog libs(Project project) {
        return project.getExtensions().getByType(VersionCatalogsExtension.class).named("libs");
    }

    public static String getVersion(Project project, String name) {
        return libs(project)
                .findVersion(name)
                .orElseThrow(() -> new NoSuchElementException("Missing version for " + name))
                .getRequiredVersion();
    }

    public static Provider<ExternalModuleDependencyBundle> getBundle(Project project, String name) {
        return libs(project)
                .findBundle(name)
                .orElseThrow(() -> new NoSuchElementException("Missing bundle for " + name));
    }

    public static void applyPlugin(Project project, String pluginName) {
        project.apply(Collections.singletonMap("plugin", pluginName));
    }

    public static void applyPlugin(Project project, Class<? extends Plugin<?>> pluginType) {
        project.apply(Collections.singletonMap("plugin", pluginType));
    }

    public static JTrimProjectInfo getProjectInfo(Project project) {
        return project.getExtensions().getByType(JTrimProjectInfo.class);
    }

    private static <T> T getRootExtension(Project project, Class<? extends T> type) {
        return project.getRootProject().getExtensions().getByType(type);
    }

    public static LicenseInfo getLicenseInfo(Project project) {
        return getRootExtension(project, LicenseInfo.class);
    }

    public static JTrimDevelopment getDevelopmentInfo(Project project) {
        return getRootExtension(project, JTrimDevelopment.class);
    }

    public static JavaPluginExtension java(Project project) {
        JavaPluginExtension java = tryGetJava(project);
        if (java == null) {
            throw new IllegalArgumentException(project.getPath() + " does not have the java plugin applied.");
        }
        return java;
    }

    public static String getModuleName(Project project) {
        return project.getGroup() + "." + project.getName().replace("jtrim-", "");
    }

    public static JavaPluginExtension tryGetJava(Project project) {
        return project.getExtensions().findByType(JavaPluginExtension.class);
    }

    public static String getStringProperty(Project project, String name, String defaultValue) {
        if (!project.hasProperty(name)) {
            return defaultValue;
        }

        Object result = project.property(name);
        if (result == null) {
            return defaultValue;
        }

        String resultStr = result.toString();
        return resultStr != null ? resultStr.trim() : defaultValue;
    }

    public static boolean isReleasedProject(Project project) {
        return project.getPlugins().findPlugin(JTrimJavaPlugin.class) != null;
    }

    public static Provider<List<Project>> releasedSubprojects(Project parent) {
        Objects.requireNonNull(parent, "parent");
        return releasedProjects(parent.getObjects(), parent.getProviders(), () -> parent.getSubprojects().stream());
    }

    public static Provider<List<Project>> releasedProjects(
            ObjectFactory objects,
            ProviderFactory providers,
            Supplier<? extends Stream<? extends Project>> projectsProviders) {

        Objects.requireNonNull(objects, "objects");
        Objects.requireNonNull(providers, "providers");
        Objects.requireNonNull(projectsProviders, "projectsProviders");

        ListProperty<Project> result = objects.listProperty(Project.class);
        result.set(providers.provider(() -> {
            return projectsProviders
                    .get()
                    .filter(ProjectUtils::isReleasedProject)
                    .collect(Collectors.toList());
        }));
        return result;
    }

    public static <E> E getExtension(Project project, Class<E> extType) {
        E result = tryGetExtension(project, extType);
        if (result == null) {
            throw new IllegalArgumentException("Missing extension: "
                    + extType.getSimpleName()
                    + " for project " + project.getPath());
        }
        return result;
    }

    public static <E> E tryGetExtension(Project project, Class<E> extType) {
        return project.getExtensions().findByType(extType);
    }

    private ProjectUtils() {
        throw new AssertionError();
    }
}
