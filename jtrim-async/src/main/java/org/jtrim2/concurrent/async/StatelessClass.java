package org.jtrim2.concurrent.async;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines that a class is stateless: That is, its method calls only rely on
 * their argument and the argument passed at construction time and its methods
 * are idempotent. This annotation is to support the AsyncHelper.isSafeListener
 * method.
 *
 * @author Kelemen Attila
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface StatelessClass {
}
