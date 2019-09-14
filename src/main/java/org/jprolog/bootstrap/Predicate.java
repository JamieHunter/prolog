// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.bootstrap;

import org.jprolog.execution.CompileContext;
import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.library.Library;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation can be used on static Methods or static Fields,
 * <p>
 * Case 1: Static method with first parameter is {@link Environment},
 * Subsequent parameters are of type {@link Term}.
 * This is treated as a lambda function executed during a forward visit.
 * One or more names are specified.
 * </p>
 * <p>
 * Case 2: Static method with first parameter is {@link CompileContext},
 * second parameter is a {@link CompoundTerm}. A list of names and arity must
 * be specified. Method is called to compile one or more {@link Instruction}'s.
 * </p>
 * <p>
 * Case 3: Static field of type {@link Instruction}. This is assumed to be a singleton
 * instruction, and will execute slightly faster than the lambda approach.
 * </p>
 * Note that the libraries are chained back to {@link Builtins} for these predicates to be interpreted. See
 * {@link Builtins} and {@link Library}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Predicate {
    /**
     * @return Predicate name and aliases.
     */
    String[] value();

    /**
     * @return Arity if specified (-1 to infer).
     */
    int arity() default -1;

    /**
     * @return Vararg if specified (true if enabled).
     */
    boolean vararg() default false;

    /**
     * @return true if predicate is invisible from trace
     */
    boolean notrace() default false;
}
