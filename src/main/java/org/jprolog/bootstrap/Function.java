// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.bootstrap;

import org.jprolog.functions.StackFunction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used on singleton fields of stack functions (derived from {@link StackFunction}.
 * See also {@link Predicate} and {@link Compare}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Function {
    /**
     * @return Function name and aliases
     */
    String[] value();

    /**
     * @return Arity of function. 1 for prefix/postfix, 2 for binary.
     */
    int arity();
}
