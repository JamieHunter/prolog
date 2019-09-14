// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Builtins;
import org.jprolog.bootstrap.DemandLoad;
import org.jprolog.predicates.Predication;

public class Option {

    /**
     * List of predicates defined by the resource "option.pl".
     * This library provides option processing like the built-in option processing
     * See {@link prolog.flags.OptionParser}.
     */
    @DemandLoad("option.pl")
    public static Predication option[] = {
            Builtins.predicate("option", 2), // basic
            Builtins.predicate("option", 3), // with default
            Builtins.predicate("select_option", 3), // remove from list
            Builtins.predicate("select_option", 4), // remove from list with default
            //predicate("merge_options", 3),
    };
}
