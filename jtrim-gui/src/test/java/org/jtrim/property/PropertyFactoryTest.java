package org.jtrim.property;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class PropertyFactoryTest {
    // Contains simple tests. More tests can be found in the test of the
    // actual implementation

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
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
        stub(verifier.storeValue(any())).toReturn(value);

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

        stub(verifier.storeValue(any())).toReturn(value);
        stub(publisher.returnValue(any())).toReturn(value);

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

        stub(verifier.storeValue(any())).toReturn(value);
        stub(publisher.returnValue(any())).toReturn(value);

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
        stub(publisher.returnValue(any())).toReturn(value);

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

    /**
     * Test of noOpVerifier method, of class PropertyFactory.
     */
    @Test
    public void testNoOpVerifier() {
        assertSame(NoOpVerifier.getInstance(), PropertyFactory.noOpVerifier());
    }

    /**
     * Test of notNullVerifier method, of class PropertyFactory.
     */
    @Test
    public void testNotNullVerifier() {
        assertSame(NotNullVerifier.getInstance(), PropertyFactory.notNullVerifier());
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

        stub(wrapped1.storeValue(input1)).toReturn(output1);
        stub(wrapped2.storeValue(output1)).toReturn(output2);

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
        stub(wrapped.storeValue(input)).toReturn(output);

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

        stub(wrapped1.storeValue(input1)).toReturn(output1);
        stub(wrapped2.storeValue(output1)).toReturn(output2);

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

    /**
     * Test of noOpPublisher method, of class PropertyFactory.
     */
    @Test
    public void testNoOpPublisher() {
        assertSame(NoOpPublisher.getInstance(), PropertyFactory.noOpPublisher());
    }
}
