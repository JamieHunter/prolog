// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.debugging;

/**
 * Execution mode to step through
 */
public enum StepMode {
    LEAP {
        @Override
        public int flags(ActiveDebugger debugger, Scoped scope) {
            return scope.instructionContext().spyFlags(debugger.environment.spyPoints());
        }
    },
    CREEP {
    },
    IGNORE_AND_CREEP {
        @Override
        public boolean ignore() {
            return true;
        }
    },
    SKIP {
        @Override
        public int flags(ActiveDebugger debugger, Scoped scope) {
            return debugger.isSkipEnd(scope) ? ExecutionPort.EXIT_FLAG | ExecutionPort.FAIL_FLAG : 0;
        }
    },
    QSKIP {
        @Override
        public int flags(ActiveDebugger debugger, Scoped scope) {
            return debugger.isSkipEnd(scope) ? ExecutionPort.EXIT_FLAG | ExecutionPort.FAIL_FLAG :
                    scope.instructionContext().spyFlags(debugger.environment.spyPoints());
        }
    },
    NODEBUG {
        @Override
        public int flags(ActiveDebugger debugger, Scoped scope) {
            return 0;
        }
    },

    ;

    public int flags(ActiveDebugger debugger, Scoped scope) {
        return scope.traceable ? debugger.spyPoints.leashFlags : 0;
    }

    public boolean ignore() {
        return false;
    }
}
