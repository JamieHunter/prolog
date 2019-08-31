// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.constants.PrologAtom;
import prolog.constants.PrologEmptyList;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.expressions.TermListImpl;
import prolog.predicates.Predication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Track Spy Points, which exist if debugger is enabled or disabled.
 */
public class SpyPoints {
    /*package*/ int generation = 0; // at the time nothing is spied
    /*package*/ int leashFlags = -1;
    private final HashSet<SpySpec> spying = new HashSet<>();

    public SpyPoints() {
    }

    public void addSpy(SpySpec spySpec) {
        spying.add(spySpec);
        generation++;
    }

    public void removeSpy(SpySpec spySpec) {
        spying.remove(spySpec);
        generation++;
    }

    public void removeAll() {
        spying.clear();
        generation++;
    }

    public int computeSpyFlags(Predication.Interned predication) {
        if (predication == null) {
            return 0;
        }
        SpySpec spec = SpySpec.from(predication.functor(), predication.arity());
        if (!spying.contains(spec)) {
            spec = SpySpec.from(predication.functor());
            if (!spying.contains(spec)) {
                return 0;
            }
        }
        return -1;
    }

    /**
     * Iterator of spy spec
     * @return specs
     */
    public Collection<SpySpec> enumerate() {
        return Collections.unmodifiableSet(spying);
    }

    /**
     * Current leash flags as a list.
     * @return leash flags
     * @param environment Environment context
     */
    public Term getLeashFlags(Environment environment) {
        if (leashFlags == 0) {
            return PrologEmptyList.EMPTY_LIST;
        }
        ArrayList<Term> flags = new ArrayList<>();
        if ((leashFlags & ExecutionPort.CALL_FLAG) != 0) {
            flags.add(new PrologAtom("call"));
        }
        if ((leashFlags & ExecutionPort.EXIT_FLAG) != 0) {
            flags.add(new PrologAtom("exit"));
        }
        if ((leashFlags & ExecutionPort.REDO_FLAG) != 0) {
            flags.add(new PrologAtom("redo"));
        }
        if ((leashFlags & ExecutionPort.FAIL_FLAG) != 0) {
            flags.add(new PrologAtom("fail"));
        }
        if ((leashFlags & ExecutionPort.EXCEPTION_FLAG) != 0) {
            flags.add(new PrologAtom("exception"));
        }
        return new TermListImpl(flags, PrologEmptyList.EMPTY_LIST);
    }

    /**
     * Retrieve flags as a bitmap
     * @return flags
     */
    public int getLeashFlags() {
        return leashFlags;
    }

    /**
     * Set the leash flags
     * @param leashFlags New leash flags
     */
    public void setLeashFlags(int leashFlags) {
        this.leashFlags = leashFlags;
    }

    /**
     * Parse the leash flag
     */
    public int parseLeashFlag(String flag) {
        for(ExecutionPort port : ExecutionPort.values()) {
            if (port.atomName() != null && port.atomName().equals(flag)) {
                return port.flag();
            }
        }
        return 0;
    }
}
