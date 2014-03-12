package org.jtrim.gui;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.jtrim.utils.ExceptionHelper;
import org.junit.Assume;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public final class TestUtils {
    private static void failForClass(Class<?> type, String message) {
        fail(type.getName() + ": " + message);
    }

    public static void testUtilityClass(Class<?> utilityClass) {
        if ((utilityClass.getModifiers() & Modifier.FINAL) == 0) {
            failForClass(utilityClass, "Must be declared final.");
        }

        Constructor<?>[] constructors = utilityClass.getDeclaredConstructors();
        if (constructors.length != 1) {
            failForClass(utilityClass, "Must have a single constructor");
        }

        Constructor<?> constructor = constructors[0];
        if ((constructor.getModifiers() & Modifier.PRIVATE) == 0) {
            failForClass(utilityClass, "The constructor must be declared private.");
        }

        if (constructor.getParameterTypes().length != 0) {
            failForClass(utilityClass, "The constructor must have no argument.");
        }

        try {
            constructor.setAccessible(true);
        } catch (SecurityException ex) {
            Assume.assumeTrue("The security manager prevented us from completing this test.", false);
        }

        try {
            constructor.newInstance();
        } catch (InvocationTargetException ex) {
            assertTrue("Constructor must throw an AssertionError.", ex.getCause() instanceof AssertionError);
        } catch (Throwable ex) {
            throw ExceptionHelper.throwUnchecked(ex);
        }
    }

    private TestUtils() {
        throw new AssertionError();
    }
}
