// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.constants.PrologAtomInterned;
import prolog.predicates.Predication;

public class SpySpec {

    protected final PrologAtomInterned functor;

    public static SpySpec from(PrologAtomInterned functor) {
        return new AllArity(functor);
    }
    public static SpySpec from(PrologAtomInterned functor, int arity) {
        return new SingleArity(functor, arity);
    }
    public static SpySpec from(Predication.Interned predication) {
        return from(predication.functor(), predication.arity());
    }

    private SpySpec(PrologAtomInterned functor) {
        this.functor = functor;
    }

    @Override
    public int hashCode() {
        return functor.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        return this.functor == ((SpySpec)obj).functor;
    }

    public static class AllArity extends SpySpec {

        private AllArity(PrologAtomInterned functor) {
            super(functor);
        }

    }

    public static class SingleArity extends SpySpec {

        protected final int arity;

        private SingleArity(PrologAtomInterned functor, int arity) {
            super(functor);
            this.arity = arity;
        }

        @Override
        public int hashCode() {
            return super.hashCode() * 31 + arity * 7;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            return this.functor == ((SpySpec)obj).functor &&
                    this.arity == ((SingleArity)obj).arity;
        }
    }
}
