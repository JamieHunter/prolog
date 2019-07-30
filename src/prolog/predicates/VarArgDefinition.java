package prolog.predicates;

import prolog.bootstrap.Predicate;

/**
 * Provide support for any var-arg definitions for a single functor.
 */
public class VarArgDefinition {
    private final Predication predication;
    private final PredicateDefinition definition;

    public VarArgDefinition(Predication predication, PredicateDefinition definition) {
        this.predication = predication;
        this.definition = definition;
    }

    /**
     * If there is a var-arg definition that satisfies the predication, return the definition
     * @param predication Predication to look for
     * @return definition or null
     */
    public PredicateDefinition lookup(Predication predication) {
        // assume the functor is already tested
        // defined like this to later support multiple vararg variants if needed
        if (predication.arity() <= predication.arity()) {
            return definition;
        }
        return null;
    }
}
