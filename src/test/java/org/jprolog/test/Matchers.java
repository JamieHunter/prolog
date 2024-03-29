// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.test;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;
import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.Atomic;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologEmptyList;
import org.jprolog.constants.PrologFloat;
import org.jprolog.constants.PrologInteger;
import org.jprolog.constants.PrologString;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.test.internal.ThenScope;
import org.jprolog.variables.ActiveVariable;
import org.jprolog.variables.LabeledVariable;
import org.jprolog.variables.Variable;

import java.math.BigInteger;
import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;

public final class Matchers {

    private Matchers() {
        // Utility class
    }

    private static class IsConstant<T> extends DiagnosingMatcher<T> {

        @Override
        public void describeTo(Description description) {
            description.appendText("constant term");
        }

        @Override
        protected boolean matches(Object o, Description mismatch) {
            if (o == null) {
                mismatch.appendText("is null");
                return false;
            }
            if (o instanceof Atomic) {
                return true;
            }
            mismatch.appendText("not a constant term");
            return false;
        }

    }

    private static class IsAtom<T> extends DiagnosingMatcher<T> {

        private final String name;

        public IsAtom(String name) {
            this.name = name;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("atom '" + name + "'");
        }

        @Override
        protected boolean matches(Object o, Description mismatch) {
            if (o == null) {
                mismatch.appendText("is null");
                return false;
            }
            String atomVal;
            if (o instanceof PrologAtomLike) {
                atomVal = ((PrologAtomLike) o).name();
            } else {
                mismatch.appendText("not an atom");
                return false;
            }
            if (!atomVal.equals(name)) {
                mismatch.appendText("atom was '" + atomVal + "'");
                return false;
            }
            return true;
        }
    }

    private static class IsInteger<T> extends DiagnosingMatcher<T> {

        private final Matcher<?> m;

        public IsInteger(Matcher<BigInteger> m) {
            this.m = m;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("integer ");
            m.describeTo(description);
        }

        @Override
        protected boolean matches(Object o, Description mismatch) {
            if (o == null) {
                mismatch.appendText("is null");
                return false;
            }
            if (!(o instanceof PrologInteger)) {
                mismatch.appendText("not an integer ");
                mismatch.appendValue(o);
                return false;
            }
            PrologInteger value = (PrologInteger) o;
            BigInteger javaValue = value.get();
            if (!m.matches(javaValue)) {
                mismatch.appendText("integer ");
                m.describeMismatch(javaValue, mismatch);
                return false;
            }
            return true;
        }
    }

    private static class IsFloat<T> extends DiagnosingMatcher<T> {

        private final Matcher<?> m;

        public IsFloat(Matcher<Double> m) {
            this.m = m;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("float ");
            m.describeTo(description);
        }

        @Override
        protected boolean matches(Object o, Description mismatch) {
            if (o == null) {
                mismatch.appendText("is null");
                return false;
            }
            if (!(o instanceof PrologFloat)) {
                mismatch.appendText("not a float");
                return false;
            }
            PrologFloat value = (PrologFloat) o;
            Double javaValue = value.get();
            if (!m.matches(javaValue)) {
                mismatch.appendText("float ");
                m.describeMismatch(javaValue, mismatch);
                return false;
            }
            return true;
        }
    }

    private static class IsString<T> extends DiagnosingMatcher<T> {

        private final Matcher<?> m;

        public IsString(Matcher<? super CharSequence> m) {
            this.m = m;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("string ");
            m.describeTo(description);
        }

        @Override
        protected boolean matches(Object o, Description mismatch) {
            if (o == null) {
                mismatch.appendText("is null");
                return false;
            }
            if (!(o instanceof PrologString)) {
                mismatch.appendText("not a string");
                return false;
            }
            PrologString value = (PrologString) o;
            String javaValue = value.get();
            if (!m.matches(javaValue)) {
                mismatch.appendText("string ");
                m.describeMismatch(javaValue, mismatch);
                return false;
            }
            return true;
        }
    }

    private static class IsVariable<T> extends DiagnosingMatcher<T> {

        public IsVariable() {
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("variable");
        }

        @Override
        protected boolean matches(Object o, Description mismatch) {
            if (o == null) {
                mismatch.appendText("is null");
                return false;
            }
            if (!(o instanceof Variable)) {
                mismatch.appendText("not a variable");
                return false;
            }
            return true;
        }
    }

    private static class IsVariableByName<T> extends IsVariable<T> {

        private final String name;

        public IsVariableByName(String name) {
            this.name = name;
        }

        private Term getReferencedVariable() {
            return ThenScope.getScope().getVariableValue(name);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("unified with variable '" + name + "'");
        }

        @Override
        protected boolean matches(Object o, Description mismatch) {
            if (!super.matches(o, mismatch)) {
                return false;
            }
            Term refed = getReferencedVariable();
            if (refed.compareTo((Term) o) != 0) {
                mismatch.appendText("not unified with variable '" + name + "'");
                return false;
            }
            return true;
        }
    }

    private static class IsInactiveVariable<T> extends IsVariable<T> {

        private final String name;

        public IsInactiveVariable(String name) {
            this.name = name;
        }

        private Term getVariableValue() {
            return ThenScope.getScope().getVariableValue(name);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("inactive variable '" + name + "'");
        }

        @Override
        protected boolean matches(Object o, Description mismatch) {
            if (!super.matches(o, mismatch)) {
                return false;
            }
            if (o instanceof ActiveVariable) {
                mismatch.appendText("variable is active");
                return false;
            }
            if (!(o instanceof LabeledVariable)) {
                mismatch.appendText("not LabeledVariable");
                return false;
            }
            LabeledVariable value = (LabeledVariable) o;
            if (!name.equals(value.name())) {
                mismatch.appendText("name '" + value.name() + "'");
                return false;
            }
            return true;
        }
    }

    private static class IsUninstantiated<T> extends DiagnosingMatcher<T> {

        public IsUninstantiated() {
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("uninstantiated");
        }

        @Override
        protected boolean matches(Object o, Description mismatch) {
            if (o == null) {
                mismatch.appendText("is null");
                return false;
            }
            if (!(o instanceof Term)) {
                mismatch.appendText("not a term");
                return false;
            }
            Term t = (Term) o;
            if (t.isInstantiated()) {
                mismatch.appendText("was instantiated to: " + t.value().toString());
                return false;
            }
            return true;
        }
    }

    private static class IsCompoundTerm<T> extends DiagnosingMatcher<T> {

        private final String functor;
        private final Matcher<?>[] components;

        public IsCompoundTerm(String functor, Matcher<? super Term>... components) {
            this.functor = functor;
            this.components = components;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("compound '" + functor + "'(");
            if (components.length > 0) {
                components[0].describeTo(description);
            }
            for (int i = 1; i < components.length; i++) {
                description.appendText(",");
                components[i].describeTo(description);
            }
            description.appendText(")");
        }

        @Override
        protected boolean matches(Object o, Description mismatch) {
            if (o == null) {
                mismatch.appendText("is null");
                return false;
            }
            if (!(o instanceof CompoundTerm)) {
                mismatch.appendText("not a compound term");
                return false;
            }
            CompoundTerm term = (CompoundTerm) o;
            Term actualFunctor = term.functor();
            if (!(actualFunctor.isAtom())) {
                mismatch.appendText("functor not atom");
                return false;
            }
            if (!functor.equals(((PrologAtomLike) actualFunctor).name())) {
                mismatch.appendText("functor name mismatch, was '" + ((PrologAtomLike) actualFunctor).name() + "'");
                return false;
            }
            if (term.arity() != components.length) {
                mismatch.appendText("arity mismatch, was " + term.arity());
                return false;
            }
            boolean success = true;
            for (int i = 0; i < components.length; i++) {
                if (!components[i].matches(term.get(i))) {
                    if (!success) {
                        mismatch.appendText(", ");
                    }
                    mismatch.appendText("'" + functor + "'" + ":" + (i + 1) + " ");
                    components[i].describeMismatch(term.get(i), mismatch);
                    success = false;
                }
            }
            return success;
        }
    }

    private static class IsList<T> extends DiagnosingMatcher<T> {

        private final Matcher<?> tail;
        private final Matcher<?>[] members;

        public IsList(Matcher<? super Term> tail, Matcher<? super Term>... members) {
            this.tail = tail;
            this.members = members;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("list [");

            if (members.length > 0) {
                members[0].describeTo(description);
            }
            for (int i = 1; i < members.length; i++) {
                description.appendText(",");
                members[i].describeTo(description);
            }
            if (tail != null) {
                description.appendText("|");
                tail.describeTo(description);
            }
            description.appendText("]");
        }

        @Override
        protected boolean matches(Object o, Description mismatch) {
            if (o == null) {
                mismatch.appendText("is null");
                return false;
            }
            if (!(o instanceof Term)) {
                mismatch.appendText("not a term");
                return false;
            }
            Term term = (Term) o;
            int iter = 0;
            boolean success = true;
            for (; CompoundTerm.termIsA(term, Interned.LIST_FUNCTOR, 2); iter++) {
                CompoundTerm node = (CompoundTerm) term;
                Term item = node.get(0);
                term = node.get(1); // tail
                if (iter >= members.length) {
                    // we'll get into a size mismatch later
                    continue;
                }
                if (!members[iter].matches(item)) {
                    if (!success) {
                        mismatch.appendText(", ");
                    }
                    mismatch.appendText("list:" + (iter + 1) + " ");
                    members[iter].describeMismatch(item, mismatch);
                    success = false;
                }
            }
            // tail
            if (tail == null) {
                if (term != PrologEmptyList.EMPTY_LIST) {
                    if (!success) {
                        mismatch.appendText(", ");
                    }
                    mismatch.appendText("list:tail not []");
                    success = false;
                }
            } else {
                if (!tail.matches(term)) {
                    if (!success) {
                        mismatch.appendText(", ");
                    }
                    mismatch.appendText("list:tail ");
                    tail.describeMismatch(iter, mismatch);
                    success = false;
                }
            }
            if (iter != members.length) {
                if (!success) {
                    mismatch.appendText(", ");
                }
                mismatch.appendText("list:length <" + iter + ">");
                success = false;
            }
            return success;
        }
    }

    public static <T extends Term> Matcher<T> isConstant() {
        return new IsConstant<>();
    }

    public static <T extends Term> Matcher<T> isAtom(String name) {
        return new IsAtom<>(name);
    }

    public static <T extends Term> Matcher<T> isInteger(Matcher<BigInteger> m) {
        return new IsInteger<>(m);
    }

    public static <T extends Term> Matcher<T> isInteger(BigInteger value) {
        return isInteger(equalTo(value));
    }

    public static <T extends Term> Matcher<T> isInteger(long value) {
        return isInteger(BigInteger.valueOf(value));
    }

    public static <T extends Term> Matcher<T> isFloat(Matcher<Double> m) {
        return new IsFloat<>(m);
    }

    public static <T extends Term> Matcher<T> isFloat(double value) {
        return isFloat(equalTo(value));
    }

    public static <T extends Term> Matcher<T> isString(Matcher<? super CharSequence> m) {
        return new IsString<>(m);
    }

    public static <T extends Term> Matcher<T> isString(CharSequence value) {
        return isString(equalTo(value));
    }

    public static <T extends Term> Matcher<T> isVariable() {
        return new IsVariable<>();
    }

    public static <T extends Term> Matcher<T> isVariable(String name) {
        return new IsVariableByName<>(name);
    }

    public static <T extends Term> Matcher<T> isUninstantiated() {
        return new IsUninstantiated<>();
    }

    public static <T extends Term> Matcher<T> isUnboundVariable(String name) {
        return new IsInactiveVariable<>(name);
    }

    public static <T extends Term> Matcher<T> isCompoundTerm(String functor, Matcher<? super Term>... components) {
        return new IsCompoundTerm<>(functor, components);
    }

    public static <T extends Term> Matcher<T> isList(Matcher<? super Term>... members) {
        return new IsList<>(null, members);
    }

    public static <T extends Term> Matcher<T> isListWithTail(Matcher<? super Term>... members) {
        Matcher<? super Term> tail = members[members.length - 1];
        Matcher<? super Term>[] trunc = Arrays.copyOf(members, members.length - 1);
        return new IsList<>(tail, trunc);
    }
}
