package prolog.library;

import prolog.bootstrap.DemandLoad;
import prolog.predicates.Predication;

import static prolog.bootstrap.Builtins.predicate;

public class Apply {

    /**
     * List of predicates defined by the resource "apply.pl".
     */
    @DemandLoad("apply.pl")
    public static Predication apply[] = {
            predicate("include", 3),
            predicate("exclude", 3),
            predicate("partition", 4),
            predicate("partition", 5),
            predicate("maplist", 2),
            predicate("maplist", 3),
            predicate("maplist", 4),
            predicate("convlist", 3),
            predicate("foldl", 4),
            predicate("foldl", 5),
            predicate("foldl", 6),
            predicate("foldl", 7),
            predicate("scanl", 4),
            predicate("scanl", 5),
            predicate("scanl", 6),
            predicate("scanl", 7)
    };
}
