// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.flags;

import prolog.bootstrap.Interned;
import prolog.constants.Atomic;
import prolog.constants.PrologEmptyList;
import prolog.exceptions.FutureInstantiationError;
import prolog.exceptions.FutureTypeError;
import prolog.execution.Environment;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.expressions.TermList;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * Class that when executed, modifies a flags object from a list of flags. These structures are expected to be created
 * as part of the bootstrap.
 */
class OptionParser<T extends Flags> extends ParserBase<T, Void> {
    private final Map<Atomic, BiConsumer<T, Term>> consumers = new TreeMap<>();

    /**
     * Applies list of flags to a flag structure.
     *
     * @param environment Execution environment
     * @param obj         Flags object
     * @param listTerm    A term providing a list of terms.
     */
    public T apply(Environment environment, T obj, Term listTerm) {
        if (Optional.ofNullable(listTerm).orElse(PrologEmptyList.EMPTY_LIST) == PrologEmptyList.EMPTY_LIST) {
            return obj;
        }
        if (!listTerm.isInstantiated()) {
            throw new FutureInstantiationError(listTerm);
        }
        if (!CompoundTerm.termIsA(listTerm, Interned.LIST_FUNCTOR, 2)) {
            throw new FutureTypeError(Interned.LIST_TYPE, listTerm);
        }
        for (Term flagStruct : TermList.extractList(listTerm)) {
            setFlagFromStruct(obj, flagStruct);
        }
        return obj;
    }

    /**
     * Retrieve consumer from map. A consumer is used to update the target object
     *
     * @param key Option key
     * @return consumer
     */
    @Override
    protected BiConsumer<T, Term> getConsumer(Atomic key) {
        return consumers.get(key);
    }

    /**
     * Create a new entry in map.
     *
     * @param key      Option key
     * @param consumer Lambda to update object
     * @return not used (null)
     */
    @Override
    protected Void createKey(Atomic key, BiConsumer<T, Term> consumer) {
        consumers.put(key, consumer);
        return null;
    }
}
