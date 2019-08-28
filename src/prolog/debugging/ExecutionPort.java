// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

/**
 * When paused, port at time of pause.
 */
public enum ExecutionPort {
    CALL {
        @Override
        public String display() {
            return "Call";
        }
        @Override
        public int flag() {
            return CALL_FLAG;
        }
    },
    EXIT {
        @Override
        public String display() {
            return "Exit";
        }
        @Override
        public int flag() {
            return EXIT_FLAG;
        }
    },
    REDO {
        @Override
        public String display() {
            return "Redo";
        }
        @Override
        public int flag() {
            return REDO_FLAG;
        }
        @Override
        public boolean canIgnore() {
            return false;
        }
    },
    FAIL {
        @Override
        public String display() {
            return "Fail";
        }
        @Override
        public int flag() {
            return FAIL_FLAG;
        }
    },
    EXCEPTION {
        @Override
        public String display() {
            return "Exception";
        }
        @Override
        public int flag() {
            return EXCEPTION_FLAG;
        }
        @Override
        public boolean canIgnore() {
            return false;
        }
    },
    DEFERRED {
        // Not really a port
        @Override
        public String display() {
            return null;
        }

        @Override
        public int flag() {
            return -1;
        }
    },
    RETURN {
        // Not really a port
        @Override
        public String display() {
            return null;
        }

        @Override
        public int flag() {
            return -1;
        }
    };
    public static int CALL_FLAG = 0x01;
    public static int EXIT_FLAG = 0x02;
    public static int FAIL_FLAG = 0x04;
    public static int REDO_FLAG = 0x08;
    public static int EXCEPTION_FLAG = 0x10;
    public static int ANY_FLAG = -1;

    public abstract String display();
    public abstract int flag();

    public boolean canIgnore() {
        return true;
    }
}
