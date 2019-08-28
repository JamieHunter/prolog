// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.execution.Environment;
import prolog.predicates.Predication;

import java.util.HashSet;

/**
 * Track Spy Points, which exist if debugger is enabled or disabled.
 */
public class SpyPoints {
    /*package*/ final Environment environment;
    /*package*/ int generation = 0; // at the time nothing is spied
    /*package*/ int leashFlags = -1;
    private final HashSet<SpySpec> spying = new HashSet<>();

    public SpyPoints(Environment environment) {
        this.environment = environment;
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
}
