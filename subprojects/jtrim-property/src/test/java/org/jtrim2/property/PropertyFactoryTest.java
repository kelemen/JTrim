package org.jtrim2.property;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PropertyFactoryTest {
    // Contains simple tests. More tests can be found in the test of the
    // actual implementation

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(PropertyFactory.class);
    }

    @SuppressWarnings("unchecked")
    private static PropertyVerifier<Object> mockVerifier() {
        return mock(PropertyVerifier.class);
    }

    @SuppressWarnings("unchecked")
    private static PropertyPublisher<Object> mockPublisher() {
        return mock(PropertyPublisher.class);
    }

    @Test
    public void testMemProperty_GenericType() {
        Object value = new Object();
        MutableProperty<Object> property = PropertyFactory.memProperty(value);
        assertTrue(property instanceof MemProperty);
        assertSame(value, property.getValue());
    }

    @Test(expected = NullPointerException.class)
    public void testMemProperty_GenericTypeWithNull() {
        PropertyFactory.memProperty(null);
    }

    @Test
    public void testMemProperty_GenericType_booleanAllowNull1() {
        Object value = new Object();
        MutableProperty<Object> property = PropertyFactory.memProperty(value, true);
        assertTrue(property instanceof MemProperty);
        assertSame(value, property.getValue());
    }

    @Test
    public void testMemProperty_GenericType_booleanAllowNull2() {
        MutableProperty<Object> property = PropertyFactory.memProperty(null, true);
        assertTrue(property instanceof MemProperty);
        assertNull(property.getValue());
    }

    @Test
    public void testMemProperty_GenericType_booleanNotAllowNull1() {
        Object value = new Object();
        MutableProperty<Object> property = PropertyFactory.memProperty(value, false);
        assertTrue(property instanceof MemProperty);
        assertSame(value, property.getValue());
    }

    @Test(expected = NullPointerException.class)
    public void testMemProperty_GenericType_booleanNotAllowNull2() {
        PropertyFactory.memProperty(null, false);
    }

    /**
     * Test of memProperty method, of class PropertyFactory.
     */
    @Test
    public void testMemProperty_GenericType_PropertyVerifier() {
        Object value = new Object();
        PropertyVerifier<Object> verifier = mockVerifier();
        when(verifier.storeValue(any())).thenReturn(value);

        MutableProperty<Object> property = PropertyFactory.memProperty(value, verifier);
        verify(verifier).storeValue(same(value));

        assertTrue(property instanceof MemProperty);
        assertSame(value, property.getValue());
    }

    /**
     * Test of memProperty method, of class PropertyFactory.
     */
    @Test
    public void testMemProperty_3args() {
        Object value = new Object();
        PropertyVerifier<Object> verifier = mockVerifier();
        PropertyPublisher<Object> publisher = mockPublisher();

        when(verifier.storeValue(any())).thenReturn(value);
        when(publisher.returnValue(any())).thenReturn(value);

        MutableProperty<Object> property = PropertyFactory.memProperty(value, verifier, publisher);
        verify(verifier).storeValue(same(value));

        assertTrue(property instanceof MemProperty);
        assertSame(value, property.getValue());
        verify(publisher).returnValue(same(value));
    }

    @Test
    public void testMemPropertyConcurrent_GenericType() {
        Object value = new Object();
        MutableProperty<Object> property
                = PropertyFactory.memPropertyConcurrent(value, SyncTaskExecutor.getSimpleExecutor());
        assertTrue(property instanceof ConcurrentMemProperty);
        assertSame(value, property.getValue());
    }

    @Test(expected = NullPointerException.class)
    public void testMemPropertyConcurrent_GenericTypeWithNull() {
        PropertyFactory.memPropertyConcurrent(null, SyncTaskExecutor.getSimpleExecutor());
    }

    @Test
    public void testMemPropertyConcurrent_GenericType_booleanAllowNull1() {
        Object value = new Object();
        MutableProperty<Object> property
                = PropertyFactory.memPropertyConcurrent(value, true, SyncTaskExecutor.getSimpleExecutor());
        assertTrue(property instanceof ConcurrentMemProperty);
        assertSame(value, property.getValue());
    }

    @Test
    public void testMemPropertyConcurrent_GenericType_booleanAllowNull2() {
        MutableProperty<Object> property
                = PropertyFactory.memPropertyConcurrent(null, true, SyncTaskExecutor.getSimpleExecutor());
        assertTrue(property instanceof ConcurrentMemProperty);
        assertNull(property.getValue());
    }

    @Test
    public void testMemPropertyConcurrent_GenericType_booleanNotAllowNull1() {
        Object value = new Object();
        MutableProperty<Object> property
                = PropertyFactory.memPropertyConcurrent(value, false, SyncTaskExecutor.getSimpleExecutor());
        assertTrue(property instanceof ConcurrentMemProperty);
        assertSame(value, property.getValue());
    }

    @Test(expected = NullPointerException.class)
    public void testMemPropertyConcurrent_GenericType_booleanNotAllowNull2() {
        PropertyFactory.memPropertyConcurrent(null, false, SyncTaskExecutor.getSimpleExecutor());
    }

    /**
     * Test of memPropertyConcurrent method, of class PropertyFactory.
     */
    @Test
    public void testMemPropertyConcurrent_4args() {
        Object value = new Object();
        PropertyVerifier<Object> verifier = mockVerifier();
        PropertyPublisher<Object> publisher = mockPublisher();

        when(verifier.storeValue(any())).thenReturn(value);
        when(publisher.returnValue(any())).thenReturn(value);

        MutableProperty<Object> property = PropertyFactory.memPropertyConcurrent(
                value, verifier, publisher, SyncTaskExecutor.getSimpleExecutor());

        verify(verifier).storeValue(same(value));

        assertTrue(property instanceof ConcurrentMemProperty);
        assertSame(value, property.getValue());
        verify(publisher).returnValue(same(value));
    }

    /**
     * Test of proxyProperty method, of class PropertyFactory.
     */
    @Test
    public void testProxyProperty() {
        MutableProperty<Object> backingProperty = PropertyFactory.memProperty(new Object());
        MutablePropertyProxy<Object> proxy = PropertyFactory.proxyProperty(backingProperty);

        assertSame(backingProperty.getValue(), proxy.getValue());
        assertTrue(proxy instanceof DefaultMutablePropertyProxy);
    }

    /**
     * Test of constSource method, of class PropertyFactory.
     */
    @Test
    public void testConstSource_GenericType() {
        Object value = new Object();
        PropertySource<Object> source = PropertyFactory.constSource(value);
        assertSame(value, source.getValue());
        assertTrue(source instanceof ConstSource);
    }

    /**
     * Test of constSource method, of class PropertyFactory.
     */
    @Test
    public void testConstSource_GenericType_PropertyPublisher() {
        Object value = new Object();
        PropertyPublisher<Object> publisher = mockPublisher();
        when(publisher.returnValue(any())).thenReturn(value);

        PropertySource<Object> source = PropertyFactory.constSource(value, publisher);
        assertSame(value, source.getValue());
        verify(publisher).returnValue(same(value));
        assertTrue(source instanceof ConstSource);
    }

    /**
     * Test of protectedView method, of class PropertyFactory.
     */
    @Test
    public void testProtectedView() {
        PropertySource<?> wrapped = mock(PropertySource.class);
        PropertySource<Object> source = PropertyFactory.protectedView(wrapped);
        source.getValue();
        verify(wrapped).getValue();
    }

    /**
     * Test of proxySource method, of class PropertyFactory.
     */
    @Test
    public void testProxySource() {
        PropertySource<Object> backingProperty = PropertyFactory.memProperty(new Object());
        PropertySourceProxy<Object> proxy = PropertyFactory.proxySource(backingProperty);

        assertSame(backingProperty.getValue(), proxy.getValue());
        assertTrue(proxy instanceof DefaultPropertySourceProxy);
    }

    @Test
    public void testNoOpVerifierNotNull() {
        Object value = new Object();
        assertSame(value, PropertyFactory.noOpVerifier().storeValue(value));
    }

    @Test
    public void testNoOpVerifierNull() {
        assertNull(PropertyFactory.noOpVerifier().storeValue(null));
    }

    @Test
    public void testNotNullVerifierNotNull() {
        Object value = new Object();
        assertSame(value, PropertyFactory.notNullVerifier().storeValue(value));
    }

    @Test(expected = NullPointerException.class)
    public void testNotNullVerifierNull() {
        PropertyFactory.notNullVerifier().storeValue(null);
    }

    /**
     * Test of typeCheckerVerifier method, of class PropertyFactory.
     */
    @Test
    public void testTypeCheckerVerifier() {
        PropertyVerifier<String> verifier = PropertyFactory.typeCheckerVerifier(String.class);
        assertTrue(verifier instanceof TypeCheckerVerifier);

        String value = "MY-TEST-VALUE";
        assertEquals(value, verifier.storeValue(value));
    }

    /**
     * Test of combinedVerifier method, of class PropertyFactory.
     */
    @Test
    public void testCombinedVerifier_PropertyVerifier_PropertyVerifier() {
        Object input1 = new Object();
        Object output1 = new Object();
        Object output2 = new Object();

        PropertyVerifier<Object> wrapped1 = mockVerifier();
        PropertyVerifier<Object> wrapped2 = mockVerifier();

        when(wrapped1.storeValue(input1)).thenReturn(output1);
        when(wrapped2.storeValue(output1)).thenReturn(output2);

        PropertyVerifier<Object> verifier = PropertyFactory.combinedVerifier(wrapped1, wrapped2);
        assertTrue(verifier instanceof CombinedVerifier);

        assertSame(output2, verifier.storeValue(input1));
        verify(wrapped1).storeValue(same(input1));
        verify(wrapped2).storeValue(same(output1));
    }

    /**
     * Test of combinedVerifier method, of class PropertyFactory.
     */
    @Test
    public void testCombinedVerifier_ListZeroElements() {
        PropertyVerifier<Object> verifier = PropertyFactory.combinedVerifier(
                Collections.<PropertyVerifier<Object>>emptyList());

        Object value = new Object();
        assertSame(value, verifier.storeValue(value));
        assertNull(verifier.storeValue(null));
    }

    /**
     * Test of combinedVerifier method, of class PropertyFactory.
     */
    @Test
    public void testCombinedVerifier_ListSingleElements() {
        Object input = new Object();
        Object output = new Object();

        PropertyVerifier<Object> wrapped = mockVerifier();
        when(wrapped.storeValue(input)).thenReturn(output);

        PropertyVerifier<Object> verifier = PropertyFactory.combinedVerifier(
                Collections.singletonList(wrapped));

        assertSame(output, verifier.storeValue(input));
        verify(wrapped).storeValue(same(input));
    }

    /**
     * Test of combinedVerifier method, of class PropertyFactory.
     */
    @Test
    public void testCombinedVerifier_ListTwoElements() {
        Object input1 = new Object();
        Object output1 = new Object();
        Object output2 = new Object();

        PropertyVerifier<Object> wrapped1 = mockVerifier();
        PropertyVerifier<Object> wrapped2 = mockVerifier();

        when(wrapped1.storeValue(input1)).thenReturn(output1);
        when(wrapped2.storeValue(output1)).thenReturn(output2);

        PropertyVerifier<Object> verifier = PropertyFactory.combinedVerifier(
                Arrays.asList(wrapped1, wrapped2));
        assertTrue(verifier instanceof CombinedVerifier);

        assertSame(output2, verifier.storeValue(input1));
        verify(wrapped1).storeValue(same(input1));
        verify(wrapped2).storeValue(same(output1));
    }

    /**
     * Test of listVerifier method, of class PropertyFactory.
     */
    @Test
    public void testListVerifier() {
        PropertyVerifier<Object> elementVerifier = mockVerifier();
        PropertyVerifier<List<Object>> verifier = PropertyFactory.listVerifier(elementVerifier, true);
        assertTrue(verifier instanceof ListVerifier);

        verifier.storeValue(Arrays.asList(null, null, null));
        verify(elementVerifier, times(3)).storeValue(null);
    }

    @Test
    public void testNoOpPublisherNotNull() {
        Object value = new Object();
        assertSame(value, PropertyFactory.noOpPublisher().returnValue(value));
    }

    @Test
    public void testNoOpPublisherNull() {
        assertNull(PropertyFactory.noOpPublisher().returnValue(null));
    }

    @Test
    public void testLazilyNotifiedSource1() {
        TestObjWithEquals initialValue = new TestObjWithEquals("VALUE");
        MutableProperty<TestObjWithEquals> wrapped = PropertyFactory.memProperty(initialValue);
        PropertySource<TestObjWithEquals> property = PropertyFactory.lazilyNotifiedSource(wrapped);
        assertTrue(property instanceof LazilyNotifiedPropertySource);
        assertSame(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        wrapped.setValue(new TestObjWithEquals("VALUE"));
        wrapped.setValue(new TestObjWithEquals("VALUE"));
        verify(listener, atMost(1)).run();
    }

    @Test
    public void testLazilyNotifiedSource2() {
        TestObjWithIdentity initialValue = new TestObjWithIdentity("VALUE");
        MutableProperty<TestObjWithIdentity> wrapped = PropertyFactory.memProperty(initialValue);
        PropertySource<TestObjWithIdentity> property
                = PropertyFactory.lazilyNotifiedSource(wrapped, TestObjWithIdentity.STR_CMP);
        assertTrue(property instanceof LazilyNotifiedPropertySource);
        assertSame(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        wrapped.setValue(new TestObjWithIdentity("VALUE"));
        wrapped.setValue(new TestObjWithIdentity("VALUE"));
        verify(listener, atMost(1)).run();
    }

    @Test
    public void testLazilyNotifiedProperty1() {
        TestObjWithEquals initialValue = new TestObjWithEquals("VALUE");
        MutableProperty<TestObjWithEquals> wrapped = PropertyFactory.memProperty(initialValue);
        PropertySource<TestObjWithEquals> property = PropertyFactory.lazilyNotifiedProperty(wrapped);
        assertTrue(property instanceof LazilyNotifiedMutableProperty);
        assertSame(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        wrapped.setValue(new TestObjWithEquals("VALUE"));
        wrapped.setValue(new TestObjWithEquals("VALUE"));
        verify(listener, atMost(1)).run();
    }

    @Test
    public void testLazilyNotifiedProperty2() {
        TestObjWithIdentity initialValue = new TestObjWithIdentity("VALUE");
        MutableProperty<TestObjWithIdentity> wrapped = PropertyFactory.memProperty(initialValue);
        PropertySource<TestObjWithIdentity> property
                = PropertyFactory.lazilyNotifiedProperty(wrapped, TestObjWithIdentity.STR_CMP);
        assertTrue(property instanceof LazilyNotifiedMutableProperty);
        assertSame(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        wrapped.setValue(new TestObjWithIdentity("VALUE"));
        wrapped.setValue(new TestObjWithIdentity("VALUE"));
        verify(listener, atMost(1)).run();
    }

    @Test
    public void testLazilySetProperty1() {
        TestObjWithEquals initialValue = new TestObjWithEquals("VALUE");
        MutableProperty<TestObjWithEquals> wrapped = PropertyFactory.memProperty(initialValue);
        MutableProperty<TestObjWithEquals> property = PropertyFactory.lazilySetProperty(wrapped);
        assertTrue(property instanceof LazilySetProperty);
        assertSame(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        property.setValue(new TestObjWithEquals("VALUE"));
        verifyNoInteractions(listener);
    }

    @Test
    public void testLazilySetProperty2() {
        TestObjWithIdentity initialValue = new TestObjWithIdentity("VALUE");
        MutableProperty<TestObjWithIdentity> wrapped = PropertyFactory.memProperty(initialValue);
        MutableProperty<TestObjWithIdentity> property
                = PropertyFactory.lazilySetProperty(wrapped, TestObjWithIdentity.STR_CMP);
        assertTrue(property instanceof LazilySetProperty);
        assertSame(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        property.setValue(new TestObjWithIdentity("VALUE"));
        verifyNoInteractions(listener);
    }

    @Test
    public void testTrimmedString() {
        PropertySource<String> wrapped = PropertyFactory.constSource("test str");
        PropertySource<String> property = PropertyFactory.trimmedString(wrapped);

        assertSame(wrapped.getValue(), property.getValue());
        assertTrue(property instanceof TrimmedPropertySource);
    }

    @Test
    public void testConvert() {
        Object source = new Object();
        PropertySource<Object> sourceProperty = PropertyFactory.constSource(source);
        PropertySource<AtomicReference<?>> property = PropertyFactory.convert(sourceProperty, AtomicReference::new);

        assertTrue(property instanceof ConverterProperty);
        assertSame(source, property.getValue().get());
    }
}
