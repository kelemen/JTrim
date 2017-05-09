/**
 * Contains convenience classes for defining properties able to notify clients
 * of changes.
 * <P>
 * The two main interfaces of this package are
 * {@link org.jtrim2.property.PropertySource} and {@link org.jtrim2.property.MutableProperty}.
 * {@code PropertySource} might be used when there is a property which might
 * change independently of client code and {@code MutableProperty} is a property
 * which might be adjusted by client code.
 * <P>
 * The factory class {@link org.jtrim2.property.PropertyFactory} contains lots
 * of helper methods to create various kinds of properties. Often however, you
 * might need boolean properties. For various factory methods for boolean
 * properties, see the {@link org.jtrim2.property.BoolProperties} class.
 * <P>
 * Note that usually you need to use properties in Swing components and so in
 * this case properties are expected to be adjusted only from the
 * <I>AWT Event dispatch thread</I>. Unlike in Swing however, reading properties
 * are defined to be thread-safe by this package.
 *
 * @see org.jtrim2.property.PropertyFactory
 * @see org.jtrim2.property.BoolProperties
 */
package org.jtrim2.property;
