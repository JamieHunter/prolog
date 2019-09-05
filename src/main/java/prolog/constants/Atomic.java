// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.constants;

/**
 * This interface represents an Atomic Grounded entity.
 */
public interface Atomic extends Grounded {

    /**
     * Return a Java equivalent object
     * @return object
     */
    Object get();

    /**
     * Calling isAtomic is faster than instanceof Atomic(). This indicates that this term is atomic.
     * @return true indicating this term is atomic.
     */
    @Override
    default boolean isAtomic() {
        return true;
    }
}
