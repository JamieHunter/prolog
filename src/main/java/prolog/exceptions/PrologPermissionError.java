// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.exceptions;

import prolog.bootstrap.Interned;
import prolog.constants.PrologAtom;
import prolog.constants.PrologAtomLike;
import prolog.execution.Environment;
import prolog.expressions.Term;

/**
 * Prolog Permission error. A permission error occurs when type and domain are valid, resource exists,
 * but the code is not permitted to use the resource (e.g. file write protected, or dictionary entry is not dynamic).
 */
public class PrologPermissionError extends PrologError {

    /**
     * Create new permission error
     *
     * @param environment Execution environment
     * @param action      Action being performed
     * @param type        Type of permission error
     * @param target      Term describing resource
     * @param description Description of permission error
     * @param cause       Java cause if any, else null
     * @return exception (not thrown)
     */
    public static PrologPermissionError error(Environment environment, PrologAtomLike action, PrologAtomLike type, Term target,
                                              String description, Throwable cause) {
        return new PrologPermissionError(
                formal(Interned.PERMISSION_ERROR_FUNCTOR, action, type, target),
                context(environment, "Permission error: " + description + " for resource: '" + target.toString() + "'"),
                cause);
    }

    /**
     * Create new permission error
     *
     * @param environment Execution environment
     * @param action      Action being performed (as an Atom)
     * @param type        Type of permission error (as an Atom)
     * @param target      Term describing resource
     * @param description Description of permission error
     * @return exception (not thrown)
     */
    public static PrologPermissionError error(Environment environment, PrologAtomLike action, PrologAtomLike type, Term target,
                                              String description) {
        return error(environment, action, type, target, description, null);
    }

    /**
     * Create new permission error
     *
     * @param environment Execution environment
     * @param action      Action being performed (as a string)
     * @param type        Type of permission error (as a string)
     * @param target      Term describing resource
     * @param description Description of permission error
     * @return exception (not thrown)
     */
    public static PrologPermissionError error(Environment environment, String action, String type, Term target,
                                              String description) {
        return error(environment, new PrologAtom(action), new PrologAtom(type), target, description);
    }

    /**
     * {@inheritDoc}
     */
    protected PrologPermissionError(Term formal, ErrorContext context, Throwable cause) {
        super(formal, context, cause);
    }

}
