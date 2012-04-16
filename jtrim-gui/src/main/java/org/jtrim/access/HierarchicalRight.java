package org.jtrim.access;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.jtrim.collections.ArraysEx;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines hierarchically structured rights defined by list of right objects.
 * The list of objects are said to be the parts of the right and these rights
 * can be any objects (even {@code null}) but are strongly recommended to be
 * immutable. This list completely defines a hierarchical right.
 * <P>
 * A right starting with the same list (using
 * {@link java.util.Objects#equals(java.lang.Object, java.lang.Object) Objects.equals(Object, Object)}
 * for comparing the right parts) as the complete list of right parts of another
 * right is said to be a subright of the latter right. Notice that by this
 * definition a right is always a subright of itself. And every hierarchical
 * right is the subright of the right with an empty list of right parts
 * (the universal right).
 * <P>
 * Two hierarchical right are said to be conflicting if and only if one of the
 * right is subright of the other one.
 * <P>
 * This class does not have a public constructor and can only be initiated
 * by one of its factory methods:
 * <ul>
 *  <li>{@link #create(java.lang.Object[]) create(Object...)}</li>
 *  <li>{@link #createFromList(java.util.List) createFromList(List<?>)}</li>
 *  <li>{@link #createWithParent(org.jtrim.access.HierarchicalRight, java.lang.Object) createWithParent(HierarchicalRight, Object)}</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * Instances of this class are immutable and as such are thread-safe even in
 * the face of unsynchronized concurrent access. Note that although instances
 * are immutable, the right parts may not be immutable themselves but
 * <B>it is strongly recommended to use immutable right parts</B>.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @see HierarchicalAccessManager
 * @author Kelemen Attila
 */
public final class HierarchicalRight {
    private static final HierarchicalRight UNIVERSAL_RIGHT
            = new HierarchicalRight(new Object[0], 0, 0);

    private final int offset;
    private final int length;
    private final Object[] rights;

    private final List<Object> rightListView;

    /**
     * Creates a hierarchical right from an array of right parts.
     * The returned hierarchical right will contain the specified right parts
     * in the order they appear in the array.
     *
     * @param rights the right parts. This argument can be specified as a
     *   varargs argument and cannot be {@code null} (although the individual
     *   elements can be {@code null}s).
     * @return the hierarchical right defined by the specified list of
     *   right parts. This method never returns {@code null} but not
     *   necessarily returns a unique object for each call.
     *
     * @throws NullPointerException thrown if the argument is {@code null}
     */
    public static HierarchicalRight create(Object... rights) {
        return rights.length > 0
                ? new HierarchicalRight(rights)
                : UNIVERSAL_RIGHT;
    }

    /**
     * Creates a hierarchical right from an list of right parts.
     * The returned hierarchical right will contain the specified right parts
     * in the order they appear in the list.
     *
     * @param rights the list right parts. This argument cannot be {@code null}
     *   (although the individual elements can be {@code null}s).
     * @return the hierarchical right defined by the specified list of
     *   right parts. This method never returns {@code null} but not
     *   necessarily returns a unique object for each call.
     *
     * @throws NullPointerException thrown if the argument is {@code null}
     */
    public static HierarchicalRight createFromList(List<?> rights) {
        // rights.toArray() will be cloned which would not be required if
        // toArray() was properly implemented but to be on the safe side
        // we will clone the array in the constructor.
        return !rights.isEmpty()
                ? new HierarchicalRight(rights.toArray())
                : UNIVERSAL_RIGHT;
    }

    /**
     * Creates a subright of a specific right appending a single right part
     * to it. The newly created instance will have a right list which right
     * parts are the specified parent rights and (as the last element) the
     * specified right part.
     *
     * @param parentRight the parent right of the newly created right.
     *   This argument cannot be {@code null}.
     * @param childRight the last element of the right part list of the newly
     *   created hierarchical right. This argument can be {@code null}.
     * @return the hierarchical right defined by the specified parent right and
     *   child right part. This method never returns {@code null} but not
     *   necessarily returns a unique object for each call.
     *
     * @throws NullPointerException thrown if the {@code parentRight}
     *   is {@code null}
     */
    public static HierarchicalRight createWithParent(
            HierarchicalRight parentRight, Object childRight) {

        int parentNextOffset = parentRight.offset + parentRight.length;
        if (parentRight.rights.length > parentNextOffset) {
            if (parentRight.rights[parentNextOffset] == childRight) {
                // What a luck, we do not need to copy the array.
                return new HierarchicalRight(parentRight.rights,
                        parentRight.offset, parentRight.length + 1);
            }
        }

        Object[] newRights = new Object[parentRight.length + 1];
        System.arraycopy(parentRight.rights, parentRight.offset,
                newRights, 0, parentRight.length);
        newRights[newRights.length - 1] = childRight;

        return new HierarchicalRight(newRights, 0, newRights.length);
    }

    private HierarchicalRight(Object[] rights) {
        this(rights.clone(), 0, rights.length);
    }

    private HierarchicalRight(Object[] rights, int offset, int length) {
        this.offset = offset;
        this.length = length;
        this.rights = rights;

        this.rightListView = ArraysEx.viewAsList(
                rights, offset, length);
    }

    /**
     * Returns an unmodifiable list of the right parts of this hierarchical
     * right. The returned list will contain the elements in order.
     *
     * @return an unmodifiable list of the right parts of this hierarchical
     *   right. This method never returns {@code null} but may return an empty
     *   list if this right is the universal right.
     */
    public List<Object> getRights() {
        return rightListView;
    }

    /**
     * Return {@code true} if this right is the universal right. The universal
     * right conflicts with every hierarchical right (including itself) and
     * has zero right parts.
     *
     * @return {@code true} if this right is the universal right, {@code false}
     *   otherwise
     */
    public boolean isUniversal() {
        return rightListView.isEmpty();
    }

    /**
     * Returns {@code true} if this right is the subright of the specified
     * right. That is the right parts of the specified hierarchical right
     * begins with the right parts of this hierarchical right (using
     * {@link java.util.Objects#equals(java.lang.Object, java.lang.Object) Objects.equals(Object, Object)}
     * for comparing the right parts.
     *
     * @param right the specified hierarchical right which to which this right
     *   is tested to be its subright. This argument cannot be {@code null}.
     * @return {@code true} if this right is the subright of the specified
     *   right, {@code false} otherwise
     *
     * @throws NullPointerException thrown if the argument is {@code null}
     */
    public boolean isChildRight(HierarchicalRight right) {
        if (right.length > length) {
            return false;
        }

        final Object[] rights1 = rights;
        final Object[] rights2 = right.rights;
        final int offset1 = offset;
        final int offset2 = right.offset;
        int len = length;

        for (int i = 0; i < len; i++) {
            Object right1 = rights1[i + offset1];
            Object right2 = rights2[i + offset2];
            if (right1 != right2) {
                if (right1 == null || right2 == null) {
                    return false;
                }

                if (!right1.equals(right2)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns a parent right of this right. The right parts of the specified
     * will be the same as the first requested number of parts of this
     * hierarchical right.
     * <P>
     * This right is always the subright of the returned right.
     *
     * @param index {@code index + 1} number of right parts will be removed
     *   from the end of the right part list of this hierarchical right unless
     *   this number is higher (or equal) to the number of right parts this
     *   hierarchical right has: in which case the universal right is returned.
     * @return the requested parent right of this right. This method never
     *   returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code index} is a negative
     *   integer
     */
    public HierarchicalRight getParentRight(int index) {
        ExceptionHelper.checkArgumentInRange(index, 0, Integer.MAX_VALUE, "index");

        int childLength = index + 1;
        return rightListView.size() > childLength
                ? new HierarchicalRight(rights, offset, length - childLength)
                : UNIVERSAL_RIGHT;
    }

    /**
     * Returns the parent right of this hierarchical right. This method call
     * is equivalent to {@link #getParentRight(int) getParentRight(0)}.
     *
     * @return the requested parent right of this right. This method never
     *   returns {@code null}. This method returns the universal right for
     *   the universal right.
     */
    public HierarchicalRight getParentRight() {
        return getParentRight(0);
    }

    /**
     * Returns the last element of the right part list of this hierarchical
     * right. The result is the same as
     * {@code getRights().get(getRights().size - 1)} except for the
     * universal right.
     *
     * @return the last element of the right part list of this hierarchical
     *   right. This method can return {@code null} if the last element is
     *   actually {@code null}.
     *
     * @throws NoSuchElementException thrown if this right is the universal
     *   right
     */
    public Object getChildRight() {
        if (!isUniversal()) {
            return rights[rights.length - 1];
        }
        else {
            throw new NoSuchElementException("Universal right does not have an element.");
        }
    }

    /**
     * Checks whether this right is the same hierarchical right as the
     * object specified. Two hierarchical right is considered equal if
     * their right list part is the same using
     * {@link java.util.Objects#equals(java.lang.Object, java.lang.Object) Objects.equals(Object, Object)}
     * for comparing the right parts.
     * <P>
     * This method will return {@code false} for every object that is not an
     * instance of {@code HierarchicalRight}.
     *
     * @param obj the right to which this right is to be compared. This
     *   argument can be {@code null} in which case {@code false} is returned.
     * @return {@code true} if the specified hierarchical right is the same
     *   right as this right, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HierarchicalRight other = (HierarchicalRight) obj;
        final int len1 = length;
        final int len2 = other.length;
        if (len1 != len2) {
            return false;
        }

        final int offset1 = offset;
        final int offset2 = other.offset;

        final Object[] rights1 = rights;
        final Object[] rights2 = other.rights;

        for (int i = 0; i < len1; i++) {
            if (!Objects.equals(rights1[i + offset1], rights2[i + offset2])) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 7;

        final Object[] currentRights = rights;
        final int currentOffset = offset;
        final int currentLength = length;

        for (int i = 0; i < currentLength; i++) {
            Object element = currentRights[i + currentOffset];
            hash = 79 * hash + Objects.hashCode(element);
        }

        return hash;
    }

    /**
     * Returns the string representation of this hierarchical right in no
     * particular format. The string representation will contain the right parts
     * of this hierarchical right in order.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "HierarchicalRight{" + rightListView + '}';
    }
}