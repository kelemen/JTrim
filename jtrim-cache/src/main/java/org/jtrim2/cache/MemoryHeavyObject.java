package org.jtrim2.cache;

/**
 * An interface for objects retaining considerable amount of memory.
 * This interface is used by {@link MemorySensitiveCache}. This cache
 * implementation will determine the size of objects using the
 * {@link #getApproxMemorySize() getApproxMemorySize()} method of this
 * interface. Refer to the documentation of {@code MemorySensitiveCache} for
 * further details.
 * <P>
 * The size defined by this interface is not required to be exact (since
 * determining the exact memory retention of an object is not possible), only
 * an approximation. Note that some objects may share their internal fields
 * for performance reasons (e.g.: a large array), despite this such fields
 * should be included in the size calculations even though other references to
 * these fields may exists.
 * <P>
 * For heavy objects usually most memory retentions come from large arrays or
 * collections. The memory retention of a collection is not easy to measure so
 * in this case a best effort should be made to faithfully reflect the memory
 * retention on a typical JVM implementation. Somewhat easier to calculate the
 * memory retention of large primitive arrays. The size of such arrays should be
 * calculated as follows: the length of the array multiplied by the size of
 * the primitive type. For the size of the primitive types the values from
 * the following table should be used:
 * <table border="1">
 *  <tr>
 *   <th>Type</th>
 *   <th>Size of type</th>
 *  </tr>
 *  <tr>
 *   <td>boolean</td>
 *   <td>1</td>
 *  </tr>
 *  <tr>
 *   <td>byte</td>
 *   <td>1</td>
 *  </tr>
 *  <tr>
 *   <td>char</td>
 *   <td>2</td>
 *  </tr>
 *  <tr>
 *   <td>double</td>
 *   <td>8</td>
 *  </tr>
 *  <tr>
 *   <td>float</td>
 *   <td>4</td>
 *  </tr>
 *  <tr>
 *   <td>int</td>
 *   <td>4</td>
 *  </tr>
 *  <tr>
 *   <td>short</td>
 *   <td>2</td>
 *  </tr>
 * </table>
 * <P>
 * Note that implementations of this object are recommended to be immutable or
 * effectively immutable.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be completely thread-safe,
 * so the {@code getApproxMemorySize()} method can be called from any thread.
 * <h4>Synchronization transparency</h4>
 * The {@code getApproxMemorySize()} method (the only method of this interface)
 * of this interface are required to be <I>synchronization transparent</I>,
 * so they can be called in any context (e.g.: while holding a lock).
 *
 * @see MemorySensitiveCache
 * @author Kelemen Attila
 */
public interface MemoryHeavyObject {
    /**
     * Returns the approximate size of memory in bytes retained by this
     * objects. This value is only approximate in every respect: Making this
     * object unreachable does not guarantee any more available memory at all.
     * <P>
     * The return value of this object can change in case of mutable objects,
     * note however that {@code MemoryHeavyObject}s are recommended to be
     * immutable or effectively immutable.
     *
     * @return the approximate size of this object in bytes. This method
     *   must never return a negative value (zero is permitted).
     */
    public long getApproxMemorySize();
}
