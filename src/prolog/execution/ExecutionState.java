// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

/**
 * Possible execution states.
 */
public enum ExecutionState {
    FORWARD,
    BACKTRACK,
    SUCCESS {
        @Override
        public boolean isTerminal() {
            return true;
        }
    },
    FAILED {
        @Override
        public boolean isTerminal() {
            return true;
        }
    },
    ;
    boolean isTerminal() {
        return false;
    }
}
