/**
 * <p>
 * When progressing forward, BoundVariables are instantiated by setting it's value to non-null. This requires also
 * writing a backtrace entry that will set value back to null. If the context is deterministic, there is no need to
 * backtrack, so the backtrack entry can be erased. Co-referencing is done by introducing a new special
 * co-reference variable that is effectively a join of the two previous variables.
 * </p>
 * <pre>
 *             Coreference[A_B]
 *                /     \
 *               /       \
 *            Var(A)     Var(B)
 * </pre>
 * <p>
 * Assume Var(A) and Var(B) are both deterministic. In this particular case, it's ok to also erase the co-reference.
 * However, assume Var(A) is deterministic, and Var(B) is not. In this case, Var(A) will have it's entry erased, and
 * there will exist a trace of undo(Var(B)), undo(Coreference()).
 * </p>
 */
package prolog.variables;