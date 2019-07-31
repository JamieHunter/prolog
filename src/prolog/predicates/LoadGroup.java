// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.predicates;

import prolog.constants.PrologFloat;
import prolog.library.Time;

/**
 * Book keeping to support re-consultation
 */
public class LoadGroup {
    private final String id;
    private final PrologFloat time;

    public LoadGroup(String id, PrologFloat time) {
        this.id = id;
        this.time = time;
    }

    /**
     * Retrieve ID associated with this prolog atom
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieve time as epoch time
     *
     * @return time
     */
    public PrologFloat getTime() {
        return time;
    }

    public static class Interactive extends LoadGroup {
        public Interactive() {
            super("", Time.now());
        }
    }
}
