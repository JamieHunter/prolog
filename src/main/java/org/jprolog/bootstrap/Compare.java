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
 * Annotation Used on fields for singleton compare functions derived from {@link StackFunction}.
 * These functions are assumed to perform a binary comparison, and is mapped to a predicate. The compare function
 * pushes an atom true or false to the data stack.
 * See also {@link Predicate} and {@link Function}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Compare {
    /**
     * @return Predicate name and aliases.
     */
    String[] value();
    // arity is 2
}
