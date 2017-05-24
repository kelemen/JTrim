package org.jtrim2.build;

import java.util.Collections;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.HasConvention;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaPluginConvention;

public final class ProjectUtils {
    public static void applyPlugin(Project project, String pluginName) {
        project.apply(Collections.singletonMap("plugin", pluginName));
    }

    public static void applyPlugin(Project project, Class<? extends Plugin<?>> pluginType) {
        project.apply(Collections.singletonMap("plugin", pluginType));
    }

    public static JTrimProjectInfo getProjectInfo(Project project) {
        return project.getExtensions().getByType(JTrimProjectInfo.class);
    }

    public static String getDisplayName(Project project) {
        return getProjectInfo(project).getDisplayName();
    }

    public static JavaPluginConvention java(Project project) {
        return project.getConvention().getPlugin(JavaPluginConvention.class);
    }

    public static String getStringExtensionProperty(Project project, String name, String defaultValue) {
        ExtraPropertiesExtension extensions = project.getExtensions().getExtraProperties();
        if (!extensions.has(name)) {
            return defaultValue;
        }

        Object result = extensions.get(name);
        if (result == null) {
            return defaultValue;
        }

        String resultStr = result.toString();
        return resultStr != null ? resultStr.trim() : defaultValue;
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

    public static <T> T getConvention(Object container, Class<T> conventionType) {
        HasConvention convetionContainer = (HasConvention)container;
        return convetionContainer.getConvention().getPlugin(conventionType);
    }

    public static boolean isReleasedProject(Project project) {
        return project.getPlugins().findPlugin(JTrimJavaPlugin.class) != null;
    }

    public static <E> E getExtension(Project project, Class<E> extType) {
        return project.getExtensions().getByType(extType);
    }

    private ProjectUtils() {
        throw new AssertionError();
    }
}
