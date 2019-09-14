// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Builtins;
import org.jprolog.bootstrap.DemandLoad;
import org.jprolog.predicates.Predication;

public class Apply {

    /**
     * List of predicates defined by the resource "apply.pl".
     */
    @DemandLoad("apply.pl")
    public static Predication apply[] = {
            Builtins.predicate("include", 3),
            Builtins.predicate("exclude", 3),
            Builtins.predicate("partition", 4),
            Builtins.predicate("partition", 5),
            Builtins.predicate("maplist", 2),
            Builtins.predicate("maplist", 3),
            Builtins.predicate("maplist", 4),
            Builtins.predicate("convlist", 3),
            Builtins.predicate("foldl", 4),
            Builtins.predicate("foldl", 5),
            Builtins.predicate("foldl", 6),
            Builtins.predicate("foldl", 7),
            Builtins.predicate("scanl", 4),
            Builtins.predicate("scanl", 5),
            Builtins.predicate("scanl", 6),
            Builtins.predicate("scanl", 7)
    };
}
