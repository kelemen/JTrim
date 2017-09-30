package org.jtrim2.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.testutils.JTrimTests;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PropertyOfPropertyTest
extends
        JTrimTests<PropertyOfPropertyTest.TestFactory<MutableProperty<String>, String>> {

    public PropertyOfPropertyTest() {
        super(Arrays.asList(
                PropertyOfProperty::new,
                PropertyFactory::propertyOfProperty
        ));
    }

    private void doTest(Consumer<TestProperty> testTask) throws Exception {
        testAll(factory -> {
            testTask.accept(new TestProperty(factory));
        });
    }

    @Test
    public void testNestedValueChanges() throws Exception {
        doTest(property -> {
            PropertySource<String> propertyOfProperty = property.getPropertyOfProperty();

            Runnable listener = mock(Runnable.class);
            ListenerRef listenerRef = propertyOfProperty.addChangeListener(listener);

            property.assertValue("");

            verifyZeroInteractions(listener);

            property.setNestedValue("b");
            property.assertValue("b");
            verify(listener, only()).run();

            listenerRef.unregister();

            property.setNestedValue("c");
            property.assertValue("c");

            verify(listener, only()).run();
        });
    }

    @Test
    public void testNestedPropertyChanges() throws Exception {
        doTest(property -> {
            PropertySource<String> propertyOfProperty = property.getPropertyOfProperty();

            Runnable listener = mock(Runnable.class);
            ListenerRef listenerRef = propertyOfProperty.addChangeListener(listener);

            verifyZeroInteractions(listener);

            property.setNestedProperty("a");
            property.assertValue("a");
            verify(listener, only()).run();

            listenerRef.unregister();

            property.setNestedProperty("b");
            property.assertValue("b");

            verify(listener, only()).run();
        });
    }

    @Test
    public void testNestedPropertyChangesPreviousUntracked() throws Exception {
        doTest(property -> {
            PropertySource<String> propertyOfProperty = property.getPropertyOfProperty();

            Runnable listener = mock(Runnable.class);
            ListenerRef listenerRef = propertyOfProperty.addChangeListener(listener);

            verifyZeroInteractions(listener);

            MutableProperty<String> prevNestedProperty = property.setNestedProperty("a");
            verify(listener, only()).run();

            prevNestedProperty.setValue("x");
            property.assertValue("a");
            verify(listener, only()).run();

            listenerRef.unregister();
        });
    }

    @Test
    public void testNestedPropertyAndNestedValueChanges() throws Exception {
        doTest(property -> {
            PropertySource<String> propertyOfProperty = property.getPropertyOfProperty();

            Runnable listener = mock(Runnable.class);
            ListenerRef listenerRef = propertyOfProperty.addChangeListener(listener);

            verifyZeroInteractions(listener);

            property.setNestedProperty("a");
            property.assertValue("a");
            verify(listener, only()).run();

            property.setNestedValue("b");
            property.assertValue("b");
            verify(listener, times(2)).run();

            listenerRef.unregister();

            property.setNestedValue("c");
            property.assertValue("c");

            property.setNestedProperty("d");
            property.assertValue("d");

            verify(listener, times(2)).run();
        });
    }

    @Test
    public void testConcurrent() throws Exception {
        int threadCount = getThreadCount();
        int subPropertyCount = Math.max(2, threadCount / 2);
        int listenerCount = Math.max(2, threadCount - subPropertyCount);

        doTest(property -> {
            for (int tryIndex = 0; tryIndex < 100; tryIndex++) {
                List<String> keys = IntStream
                        .range(0, subPropertyCount)
                        .mapToObj(index -> "Key." + index)
                        .collect(Collectors.toList());

                Map<String, MutableProperty<String>> nestedProperties = new LinkedHashMap<>();
                keys.forEach(key -> {
                    MutableProperty<String> nestedProperty = PropertyFactory.memProperty(key + ".NewValue");
                    nestedProperties.put(key, nestedProperty);
                });

                List<Runnable> tasks = new ArrayList<>();
                keys.forEach(key -> {
                    MutableProperty<String> newProperty = nestedProperties.get(key);
                    tasks.add(() -> property.setNestedProperty(newProperty));
                });

                List<MockListener> listeners = new ArrayList<>();
                Collection<ListenerRef> listenerRefs = new ConcurrentLinkedQueue<>();
                for (int i = 0; i < listenerCount; i++) {
                    MockListener listener = new MockListener();
                    listeners.add(listener);
                    tasks.add(() -> {
                        ListenerRef listenerRef = property.getPropertyOfProperty().addChangeListener(listener);
                        listenerRefs.add(listenerRef);
                    });
                }

                Tasks.runConcurrently(tasks.toArray(new Runnable[tasks.size()]));

                listeners.forEach(MockListener::reset);

                // Determine which nested property won.
                String selectedValue = property.getValue();
                String selectedKey = selectedValue.substring(0, selectedValue.lastIndexOf('.'));
                MutableProperty<String> selected = nestedProperties.get(selectedKey);
                assertNotNull("One nested property must be set.", selected);

                String updatedValue = "UpdatedValue1";

                // Verify that updating the selected property notifies the listeners
                // and update the value
                listeners.forEach(MockListener::verifyNotCalled);
                selected.setValue(updatedValue);
                listeners.forEach(MockListener::verifyCalledOnce);
                property.assertValue(updatedValue);

                // Verify that setting other properties does not notify the listeners
                // nor modify the value
                listeners.forEach(MockListener::reset);
                AtomicInteger valueIndex = new AtomicInteger();
                nestedProperties.values().stream()
                        .filter(current -> current != selected)
                        .forEach(current -> current.setValue("IgnoredUpdate" + valueIndex.getAndIncrement()));
                listeners.forEach(MockListener::verifyNotCalled);
                property.assertValue(updatedValue);

                // Verify that listeners don't get called after unregistering them,
                // regardless what we modify.
                listeners.forEach(MockListener::reset);
                listenerRefs.forEach(ListenerRef::unregister);
                property.originalNested.setValue("LateOriginalNested");
                nestedProperties.values()
                        .forEach(current -> current.setValue("LateNested" + valueIndex.getAndIncrement()));
                property.setNestedProperty("LastNestedPropertyValue");
                listeners.forEach(MockListener::verifyNotCalled);
            }
        });
    }

    public interface TestFactory<S, R> {
        public PropertySource<R> create(
                PropertySource<? extends S> rootSrc,
                Function<? super S, ? extends PropertySource<? extends R>> nestedPropertyGetter);
    }

    private static final class MockListener implements Runnable {
        private volatile Runnable wrapped;

        public MockListener() {
            this.wrapped = mock(Runnable.class);
        }

        public void reset() {
            wrapped = mock(Runnable.class);
        }

        public void verifyCallCount(int callCount) {
            verify(wrapped, times(callCount)).run();
        }

        public void verifyNotCalled() {
            verifyZeroInteractions(wrapped);
        }

        public void verifyCalledOnce() {
            verifyCallCount(1);
        }

        @Override
        public void run() {
            wrapped.run();
        }
    }

    private static final class TestProperty {
        private final MutableProperty<String> originalNested;
        private final MutableProperty<MutableProperty<String>> property;
        private final PropertySource<String> wrapper;

        public TestProperty(TestFactory<MutableProperty<String>, String> factory) {
            this.originalNested = PropertyFactory.memProperty("");
            this.property = PropertyFactory.memPropertyConcurrent(originalNested, SyncTaskExecutor.getSimpleExecutor());
            this.wrapper = factory.create(property, arg -> arg);
        }

        public MutableProperty<String> getOriginalNested() {
            return originalNested;
        }

        public MutableProperty<String> setNestedProperty(String value) {
            MutableProperty<String> prevNestedProperty = property.getValue();
            property.setValue(PropertyFactory.memProperty(value));
            return prevNestedProperty;
        }

        public void setNestedProperty(MutableProperty<String> newNestedProperty) {
            property.setValue(newNestedProperty);
        }

        public void setNestedValue(String value) {
            property.getValue().setValue(value);
        }

        public PropertySource<String> getPropertyOfProperty() {
            return wrapper;
        }

        public void assertValue(String expectedValue) {
            assertEquals("value", expectedValue, getValue());
        }

        public String getValue() {
            return wrapper.getValue();
        }
    }
}
