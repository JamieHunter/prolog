// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.constants;

import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * A self describing self referencing entity. In other languages, it would be considered a symbol or an enum value.
 * Equality is determined almost entirely by reference.
 */
public final class PrologAtomInterned extends PrologAtomLike {
    private final String name;
    protected final Holder holder;

    private PrologAtomInterned(Holder holder) {
        this.name = holder.name;
        this.holder = holder; // maintain a reference - holder exists for as long as the interned atom exists
    }

    /**
     * Name of the atom.
     *
     * @return Name
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrologAtomInterned intern(Environment environment) {
        return this;
    }

    /**
     * Intern an atom, error if not an atom
     *
     * @param environment Execution environment
     * @param term        Term to convert
     * @return Interned atom
     */
    public static PrologAtomInterned from(Environment environment, Term term) {
        return PrologAtomLike.from(term).intern(environment);
    }

    /**
     * Intern an atom, error if not an atom
     *
     * @param environmentShared Shared execution environment
     * @param term              Term to convert
     * @return Interned atom
     */
    public static PrologAtomInterned from(Environment.Shared environmentShared, Term term) {
        return PrologAtomLike.from(term).intern(environmentShared);
    }

    /**
     * Retrieve interned atom via intern cache
     *
     * @param name  Name of atom to retrieve
     * @param cache Interned cache
     * @return Interned atom
     */
    public static PrologAtomInterned get(String name, Map<Holder, WeakReference<Holder>> cache) {
        // The hoops are to utilize cache as a weak set, but ensuring we resolve to the same holder
        synchronized (cache) {
            Holder key = new Holder(name);
            WeakReference<Holder> weakHolder = cache.get(key);
            if (weakHolder != null) {
                Holder ref = weakHolder.get();
                if (ref != null) {
                    // happy path
                    return ref.get();
                }
            }
            // Create or Refresh interned atom
            cache.put(key, new WeakReference<>(key));
            return key.get();
        }
    }

    /**
     * This is to permit weak hash map, synchronized in Environment
     */
    public static class Holder {
        private final String name;
        private WeakReference<PrologAtomInterned> interned;

        private Holder(PrologAtomInterned interned) {
            this.name = interned.name;
            this.interned = new WeakReference<>(interned);
        }

        public Holder(String name) {
            this.name = name;
            this.interned = null;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (obj.getClass() != this.getClass()) {
                return false;
            }
            Holder other = (Holder) obj;
            return name.equals(other.name);
        }

        @Override
        public String toString() {
            return name;
        }

        public PrologAtomInterned get() {
            PrologAtomInterned i = interned != null ? interned.get() : null;
            if (i == null) {
                i = new PrologAtomInterned(this);
                interned = new WeakReference<>(i);
            }
            return i;
        }
    }
}
