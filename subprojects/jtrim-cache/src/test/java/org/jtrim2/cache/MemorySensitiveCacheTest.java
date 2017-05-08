package org.jtrim2.cache;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.*;

public class MemorySensitiveCacheTest {
    private static MemorySensitiveCache create(long maximumCacheSize, int maximumObjectsToCache) {
        return new MemorySensitiveCache(maximumCacheSize, maximumObjectsToCache);
    }

    private static MemorySensitiveCache create() {
        return create(Long.MAX_VALUE, 128);
    }

    /**
     * Test of clearCache method, of class MemorySensitiveCache.
     */
    @Test
    public void testClearCache() {
        MemorySensitiveCache cache = create();

        TestObj value = new TestObj(1024);
        VolatileReference<?> reference = cache.getReference(value, ReferenceType.UserRefType);
        assertSame(value, reference.get());
        cache.clearCache();
        assertNull(reference.get());
        assertEquals(0L, cache.getCurrentSize());
    }

    /**
     * Test of getCurrentSize method, of class MemorySensitiveCache.
     */
    @Test
    public void testGetCurrentSize() {
        MemorySensitiveCache cache = create();

        TestObj[] objects = new TestObj[]{
            new TestObj(548),
            new TestObj(768),
            new TestObj(357),
            new TestObj(1024),
            new TestObj(34)
        };

        long sumSize = 0;
        for (TestObj obj: objects) {
            sumSize += obj.getEffectiveSize();
            cache.getReference(obj, ReferenceType.UserRefType);
            assertEquals(sumSize, cache.getCurrentSize());
        }
        for (TestObj obj: objects) {
            cache.getReference(obj, ReferenceType.UserRefType);
            assertEquals(sumSize, cache.getCurrentSize());
        }
    }

    /**
     * Test of getMaximumCacheSize method, of class MemorySensitiveCache.
     */
    @Test
    public void testGetMaximumCacheSize() {
        long maximumCacheSize = 543689;
        assertEquals(maximumCacheSize, create(maximumCacheSize, 128).getMaximumCacheSize());
        assertEquals(1, create(1, 128).getMaximumCacheSize());
    }

    /**
     * Test of getMaximumObjectsToCache method, of class MemorySensitiveCache.
     */
    @Test
    public void testGetMaximumObjectsToCache() {
        int maximumObjectsToCache = 563;
        assertEquals(maximumObjectsToCache, create(Long.MAX_VALUE, maximumObjectsToCache).getMaximumObjectsToCache());
        assertEquals(1, create(Long.MAX_VALUE, 1).getMaximumObjectsToCache());
    }

    /**
     * Test of toString method, of class MemorySensitiveCache.
     */
    @Test
    public void testToString() {
        assertNotNull(create().toString());
    }

    @Test
    public void testToStringOfReference() {
        for (ReferenceType refType: ReferenceType.values()) {
            assertNotNull(create().getReference(new Object(), refType).toString());
        }
    }

    @Test
    public void testSingleArgConstructor() {
        long maximumCacheSize = 543689;
        MemorySensitiveCache cache = new MemorySensitiveCache(maximumCacheSize);
        assertEquals(maximumCacheSize, cache.getMaximumCacheSize());
        assertEquals(1024, cache.getMaximumObjectsToCache());
    }

    @Test
    public void testRequestForSameObjectMultipleTimes() {
        for (ReferenceType refType1: Arrays.asList(
                ReferenceType.UserRefType,
                ReferenceType.WeakRefType,
                ReferenceType.SoftRefType)) {
            for (ReferenceType refType2: Arrays.asList(
                    ReferenceType.UserRefType,
                    ReferenceType.WeakRefType,
                    ReferenceType.SoftRefType)) {
                MemorySensitiveCache cache = create();
                TestObj obj = new TestObj(128);

                cache.getReference(obj, refType1);
                assertEquals(obj.getEffectiveSize(), cache.getCurrentSize());

                cache.getReference(obj, refType2);
                assertEquals(obj.getEffectiveSize(), cache.getCurrentSize());
            }
        }
    }

    @Test
    public void testClearAllReferencesRemovesFromCache() {
        for (ReferenceType refType1: Arrays.asList(
                ReferenceType.UserRefType,
                ReferenceType.WeakRefType,
                ReferenceType.SoftRefType)) {
            for (ReferenceType refType2: Arrays.asList(
                    ReferenceType.UserRefType,
                    ReferenceType.WeakRefType,
                    ReferenceType.SoftRefType)) {
                MemorySensitiveCache cache = create();

                TestObj obj = new TestObj(128);

                VolatileReference<TestObj> ref1 = cache.getReference(obj, refType1);
                VolatileReference<TestObj> ref2 = cache.getReference(obj, refType2);
                assertEquals(obj.getEffectiveSize(), cache.getCurrentSize());

                ref1.clear();
                assertEquals(obj.getEffectiveSize(), cache.getCurrentSize());
                assertNull(ref1.get());

                ref2.clear();
                assertEquals(0L, cache.getCurrentSize());
                assertNull(ref2.get());
            }
        }
    }

    @Test
    public void testDontMaintainTooLargeObjects() {
        for (ReferenceType refType: ReferenceType.values()) {
            MemorySensitiveCache cache = create(1024, 16);
            cache.getReference(new TestObj(1025), refType);
            assertEquals(0L, cache.getCurrentSize());
        }
    }

    @Test
    public void testDontMaintainHardReferences() {
        MemorySensitiveCache cache = create();
        TestObj obj = new TestObj(1000);
        VolatileReference<TestObj> ref = cache.getReference(obj, ReferenceType.HardRefType);
        assertEquals(0L, cache.getCurrentSize());
        assertTrue(ref instanceof HardVolatileReference);
        assertSame(obj, ref.get());
    }

    @Test
    public void testDontMaintainNoReferences() {
        MemorySensitiveCache cache = create();
        TestObj obj = new TestObj(1000);
        VolatileReference<TestObj> ref = cache.getReference(obj, ReferenceType.NoRefType);
        assertEquals(0L, cache.getCurrentSize());
        assertTrue(ref instanceof NoVolatileReference);
        assertNull(ref.get());
    }

    @Test
    public void testAddArbitraryObject() {
        MemorySensitiveCache cache = create();
        cache.getReference(new Object(), ReferenceType.UserRefType);
        assertEquals(128L, cache.getCurrentSize());
    }

    @Test
    public void testAddSmallObject1() {
        MemorySensitiveCache cache = create();
        cache.getReference(new TestObj(127), ReferenceType.UserRefType);
        assertEquals(128L, cache.getCurrentSize());
    }

    @Test
    public void testAddSmallObject2() {
        MemorySensitiveCache cache = create();
        cache.getReference(new TestObj(0), ReferenceType.UserRefType);
        assertEquals(128L, cache.getCurrentSize());
    }

    @Test
    public void testAddSmallObject3() {
        MemorySensitiveCache cache = create();
        // Just don't fail for negative values even if they are not permitted.
        cache.getReference(new TestObj(-4353), ReferenceType.UserRefType);
        assertEquals(128L, cache.getCurrentSize());
    }

    @Test
    public void testSoftVolatileDoesNotDisappear() {
        MemorySensitiveCache cache = create();

        VolatileReference<TestObj> ref = cache.getReference(new TestObj(200), ReferenceType.SoftRefType);
        System.gc();
        cache.clearCache();

        assertNotNull(ref.get());
    }

    private static ReferenceType[] getVolatileRefTypes() {
        return new ReferenceType[]{
            ReferenceType.UserRefType,
            ReferenceType.WeakRefType};
    }

    @Test
    public void testExceedSizeSimple() {
        for (ReferenceType refType: getVolatileRefTypes()) {
            MemorySensitiveCache cache = create(5000, 16);

            VolatileReference<?> ref1 = cache.getReference(new TestObj(3000), refType);
            VolatileReference<?> ref2 = cache.getReference(new TestObj(1000), refType);
            VolatileReference<?> ref3 = cache.getReference(new TestObj(2000), refType);

            System.gc();

            assertNull(ref1.get());
            assertNotNull(ref2.get());
            assertNotNull(ref3.get());
        }
    }

    @Test
    public void testExceedSizeRemoveMore() {
        for (ReferenceType refType: getVolatileRefTypes()) {
            MemorySensitiveCache cache = create(5000, 16);

            VolatileReference<?> ref1 = cache.getReference(new TestObj(3000), refType);
            VolatileReference<?> ref2 = cache.getReference(new TestObj(1000), refType);
            VolatileReference<?> ref3 = cache.getReference(new TestObj(4500), refType);

            System.gc();

            assertNull(ref1.get());
            assertNull(ref2.get());
            assertNotNull(ref3.get());
        }
    }

    @Test
    public void testExceedSizeRemoveLastReferenced() {
        for (ReferenceType refType: getVolatileRefTypes()) {
            MemorySensitiveCache cache = create(5000, 16);

            VolatileReference<?> ref1 = cache.getReference(new TestObj(3000), refType);
            VolatileReference<?> ref2 = cache.getReference(new TestObj(1000), refType);

            // Reference to promote
            ref1.get();

            VolatileReference<?> ref3 = cache.getReference(new TestObj(1500), refType);

            System.gc();

            assertNotNull(ref1.get());
            assertNull(ref2.get());
            assertNotNull(ref3.get());
        }
    }

    @Test
    public void testExceedSizeRemoveLastReferencedBarelyFits() {
        for (ReferenceType refType: getVolatileRefTypes()) {
            MemorySensitiveCache cache = create(5000, 16);

            VolatileReference<?> ref1 = cache.getReference(new TestObj(3000), refType);
            VolatileReference<?> ref2 = cache.getReference(new TestObj(1000), refType);

            // Reference to promote
            ref1.get();

            VolatileReference<?> ref3 = cache.getReference(new TestObj(2000), refType);

            System.gc();

            assertNotNull(ref1.get());
            assertNull(ref2.get());
            assertNotNull(ref3.get());
        }
    }

    @Test
    public void testBarelyFitsSize() {
        for (ReferenceType refType: getVolatileRefTypes()) {
            MemorySensitiveCache cache = create(5000, 16);
            VolatileReference<?> ref1 = cache.getReference(new TestObj(3000), refType);
            VolatileReference<?> ref2 = cache.getReference(new TestObj(2000), refType);

            System.gc();

            assertNotNull(ref1.get());
            assertNotNull(ref2.get());
        }
    }

    @Test
    public void testExceedCountSimple() {
        for (ReferenceType refType: getVolatileRefTypes()) {
            MemorySensitiveCache cache = create(Long.MAX_VALUE, 2);

            VolatileReference<?> ref1 = cache.getReference(new TestObj(128), refType);
            VolatileReference<?> ref2 = cache.getReference(new TestObj(128), refType);
            VolatileReference<?> ref3 = cache.getReference(new TestObj(128), refType);

            System.gc();

            assertNull(ref1.get());
            assertNotNull(ref2.get());
            assertNotNull(ref3.get());
        }
    }

    @Test
    public void testExceedCountRemoveMore() {
        for (ReferenceType refType: getVolatileRefTypes()) {
            MemorySensitiveCache cache = create(Long.MAX_VALUE, 1);

            VolatileReference<?> ref1 = cache.getReference(new TestObj(128), refType);
            VolatileReference<?> ref2 = cache.getReference(new TestObj(128), refType);
            VolatileReference<?> ref3 = cache.getReference(new TestObj(128), refType);

            System.gc();

            assertNull(ref1.get());
            assertNull(ref2.get());
            assertNotNull(ref3.get());
        }
    }

    @Test
    public void testExceedCountRemoveLastReferenced() {
        for (ReferenceType refType: getVolatileRefTypes()) {
            MemorySensitiveCache cache = create(Long.MAX_VALUE, 2);

            VolatileReference<?> ref1 = cache.getReference(new TestObj(128), refType);
            VolatileReference<?> ref2 = cache.getReference(new TestObj(128), refType);

            // Reference to promote
            ref1.get();

            VolatileReference<?> ref3 = cache.getReference(new TestObj(128), refType);

            System.gc();

            assertNotNull(ref1.get());
            assertNull(ref2.get());
            assertNotNull(ref3.get());
        }
    }

    private static class TestObj implements MemoryHeavyObject {
        private final long size;

        public TestObj(long size) {
            this.size = size;
        }

        public long getSize() {
            return size;
        }

        public long getEffectiveSize() {
            return size > 128 ? size : 128;
        }

        @Override
        public long getApproxMemorySize() {
            return size;
        }
    }
}
