package org.jtrim2.property;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class TypeCheckerVerifierTest {
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

    @Test
    public void testNullValue() {
        TypeCheckerVerifier<MyBaseType> verifier = new TypeCheckerVerifier<>(MyBaseType.class);
        assertNull(verifier.storeValue(null));
    }

    @Test
    public void testSameType() {
        TypeCheckerVerifier<MyBaseType> verifier = new TypeCheckerVerifier<>(MyBaseType.class);

        MyBaseType value = new MyBaseType();
        assertSame(value, verifier.storeValue(value));
    }

    @Test
    public void testExtendingType1() {
        TypeCheckerVerifier<MyRootType> verifier = new TypeCheckerVerifier<>(MyRootType.class);

        MyBaseType value = new MyBaseType();
        assertSame(value, verifier.storeValue(value));
    }

    @Test
    public void testExtendingType2() {
        TypeCheckerVerifier<MyRootType> verifier = new TypeCheckerVerifier<>(MyRootType.class);

        MyType value = new MyType();
        assertSame(value, verifier.storeValue(value));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBaseType1() {
        @SuppressWarnings("unchecked")
        TypeCheckerVerifier<Object> verifier
                = (TypeCheckerVerifier<Object>)(TypeCheckerVerifier<?>)new TypeCheckerVerifier<>(MyType.class);

        MyBaseType value = new MyBaseType();
        verifier.storeValue(value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBaseType2() {
        @SuppressWarnings("unchecked")
        TypeCheckerVerifier<Object> verifier
                = (TypeCheckerVerifier<Object>)(TypeCheckerVerifier<?>)new TypeCheckerVerifier<>(MyType.class);

        MyRootType value = new MyRootType();
        verifier.storeValue(value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnrelatedType() {
        @SuppressWarnings("unchecked")
        TypeCheckerVerifier<Object> verifier
                = (TypeCheckerVerifier<Object>)(TypeCheckerVerifier<?>)new TypeCheckerVerifier<>(MyType.class);

        String value = "MY-VALUE";
        verifier.storeValue(value);
    }

    private class MyType extends MyBaseType {
    }

    private class MyBaseType extends MyRootType {
    }

    private class MyRootType {
    }
}
