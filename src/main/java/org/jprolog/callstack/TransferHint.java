// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.callstack;

/**
 * When execution transfers, the reason for the transfer is given in this hint, used by the debugger.
 */
public enum TransferHint {
    /**
     * Enter a new call block
     */
    ENTER,
    /**
     * Enter a new call block for control purposes
     */
    CONTROL,
    /**
     * Enter a new call block for catching an exception
     */
    CATCH,
    /**
     * Leave a call block, return to caller
     */
    LEAVE {
        @Override
        public boolean isCall() {
            return false;
        }
        @Override
        public boolean isReturn() {
            return true;
        }
    },
    /**
     * Jump to a previous call block entry
     */
    REDO {
        @Override
        public boolean isCall() {
            return false;
        }
    },
    ;

    /**
     * @return true if the hint can be treated as a call to new execution point
     */
    public boolean isCall() {
        return true;
    }

    /**
     * @return true if the hint can be treated as a normal return to previous execution point
     */
    public boolean isReturn() {
        return false;
    }
}
