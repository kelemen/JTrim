package org.jtrim2.testutils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.Consumer;
import org.jtrim2.concurrent.AsyncTasks;

public final class TestUtils {
    public static <T> T build(T obj, Consumer<? super T> config) {
        config.accept(obj);
        return obj;
    }

    public static void testUtilityClass(Class<?> type) {
        if (!Modifier.isFinal(type.getModifiers())) {
            throw new AssertionError("Utility class must be final: " + type.getName());
        }

        Constructor<?>[] constructors = type.getDeclaredConstructors();
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

        if (constructor.getParameterCount() != 0) {
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

    public static void expectUnwrappedError(Class<? extends Throwable> errorType, UnsafeRunnable task) {
        expectError(true, errorType, task);
    }

    public static void expectError(Class<? extends Throwable> errorType, UnsafeRunnable task) {
        expectError(false, errorType, task);
    }

    private static void expectError(boolean unwrap, Class<? extends Throwable> errorType, UnsafeRunnable task) {
        Throwable received = null;

        try {
            task.run();
        } catch (Throwable ex) {
            received = ex;
            if (unwrap) {
                received = AsyncTasks.unwrap(received);
            }
            if (errorType.isInstance(received)) {
                return;
            }
        }

        throw new AssertionError("Expected error: " + errorType.getName() + " but received " + received);
    }

    public static void checkFailAfterTimeout(long waitMillis, UnsafeRunnable task) throws Exception {
        try {
            task.run();
        } catch (Throwable ex) {
            Thread.sleep(waitMillis);
            task.run();
            throw new AssertionError("Verification succeeded after second try. Probably an error in the test.", ex);
        }
    }

    public static Runnable toSafeRunnable(UnsafeRunnable task) {
        Objects.requireNonNull(task, "task");
        return () -> {
            try {
                task.run();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    private TestUtils() {
        throw new AssertionError();
    }
}
