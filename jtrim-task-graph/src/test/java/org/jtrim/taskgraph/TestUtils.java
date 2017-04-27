package org.jtrim.taskgraph;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public final class TestUtils {
    public static void testUtilityClass(Class<?> type) {
        if (!Modifier.isFinal(type.getModifiers())) {
            throw new AssertionError("Utility class must be final: " + type.getName());
        }

        Constructor<?>[] constructors = type.getConstructors();
        if (constructors.length == 0) {
            // This shouldn't be possible but it is fine.
            return;
        }

        if (constructors.length != 1) {
            throw new AssertionError("Utility class must only have a single constructor: " + type.getName());
        }

        verifyUncallable(constructors[0]);
    }

    private static void verifyUncallable(Constructor<?> constructor) {
        if (!Modifier.isPrivate(constructor.getModifiers())) {
            throw new AssertionError("Utility class' constructor must be private: " + constructor.getName());
        }

        if (constructor.getParameterCount() == 0) {
            throw new AssertionError("Utility class' constructor must have no arguments: " + constructor.getName());
        }

        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof AssertionError) {
                return;
            }
        } catch (Exception ex) {
            throw new AssertionError(constructor.getName(), ex);
        }

        throw new AssertionError("Utility class' constructor must throw an AssertionError" + constructor.getName());
    }

    private TestUtils() {
        throw new AssertionError();
    }
}
