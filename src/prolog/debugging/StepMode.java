// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

/**
 * Execution mode to step through
 */
public enum StepMode {
    LEAP {
        @Override
        public int flags(ActiveDebugger debugger, InstructionContext context) {
            return context.spyFlags(debugger.environment.spyPoints());
        }
    },
    CREEP {
        @Override
        public int flags(ActiveDebugger debugger, InstructionContext context) {
            return context != InstructionContext.NULL ? debugger.spyPoints.leashFlags : 0;
        }
    },
    IGNORE_AND_CREEP {
        @Override
        public boolean ignore() {
            return true;
        }
    },

    ;

    public int flags(ActiveDebugger debugger, InstructionContext context) {
        return context != InstructionContext.NULL ? debugger.spyPoints.leashFlags : 0;
    }

    public boolean ignore() {
        return false;
    }
}
