// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.LibraryBase;

/**
 * This is used to index all classes. Alternatively we could use a reflection library, but
 * that's a hammer to hit a very small nail.
 */
public final class Library extends LibraryBase {
    {
        // List of classes to consult
        consult(Arithmetic.class);
        consult(CompareImpl.class);
        consult(Apply.class);
        consult(Time.class);
        consult(Consult.class);
        consult(Control.class);
        consult(Dictionary.class);
        consult(ParsingControl.class);
        consult(Io.class);
        consult(Lists.class);
        consult(Flags.class);
        consult(Option.class);
        consult(Terms.class);
        consult(ThrowCatch.class);
        consult(Unify.class);
    }
}
