// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.library;

import prolog.bootstrap.DemandLoad;
import prolog.predicates.Predication;

import static prolog.bootstrap.Builtins.predicate;

public class Option {

    /**
     * List of predicates defined by the resource "option.pl".
     * This library provides option processing like the built-in option processing
     * See {@link prolog.flags.OptionParser}.
     */
    @DemandLoad("option.pl")
    public static Predication option[] = {
            predicate("option", 2), // basic
            predicate("option", 3), // with default
            predicate("select_option", 3), // remove from list
            predicate("select_option", 4), // remove from list with default
            //predicate("merge_options", 3),
    };
}
