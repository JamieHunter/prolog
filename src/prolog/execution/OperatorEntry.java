// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.execution;

import prolog.bootstrap.Interned;
import prolog.constants.Atomic;
import prolog.constants.PrologAtom;
import prolog.exceptions.FutureDomainError;

/**
 * Used to describe an operator for operator parsing.
 */
public class OperatorEntry implements Comparable<OperatorEntry> {

    public static final OperatorEntry ARGUMENT = new OperatorEntry(0);
    public static final OperatorEntry TERMINAL = new OperatorEntry(Integer.MAX_VALUE);

    private static final PrologAtom NULL_ATOM = PrologAtom.internalNew("");
    public static final int COMMA = 1000; // this is a special inflection point

    private final Atomic functor;
    private int precedence = 0; // all constants/terms that are not operators have this precedence
    private Code code = Code.NONE;

    /**
     * @return current precedence of operator.
     */
    public int getPrecedence() {
        return this.precedence;
    }

    /**
     * @return current operator code.
     */
    public Code getCode() {
        return this.code;
    }

    /**
     * Change precedence
     *
     * @param precedence Operator precedence, lower precedence has a higher priority.
     */
    public void setPrecedence(int precedence) {
        this.precedence = precedence;
    }

    /**
     * Change operator code
     *
     * @param code New code
     */
    public void setCode(Code code) {
        this.code = code;
    }

    /**
     * Defines associativity for operator.
     */
    private enum Assoc {
        LEFT {
            @Override
            int precedence() {
                return -1;
            }
        },
        RIGHT {
            @Override
            int precedence() {
                return 1;
            }
        },
        UNARY {
            @Override
            int precedence() {
                return -1;
            }
        },
        NONE;

        int precedence() {
            return 0;
        }
    }

    /**
     * Maps codes to flags and associativity.
     */
    public enum Code {
        FX {
            @Override
            public boolean isPrefix() {
                return true;
            }
        },
        XF {
            @Override
            public boolean isPostfix() {
                return true;
            }
        },
        FY {
            @Override
            public boolean isPrefix() {
                return true;
            }

            @Override
            Assoc associativity() {
                return Assoc.UNARY;
            }
        },
        YF {
            @Override
            public boolean isPostfix() {
                return true;
            }

            @Override
            Assoc associativity() {
                return Assoc.UNARY;
            }
        },
        XFX {
            @Override
            public boolean isBinary() {
                return true;
            }
        },
        YFX {
            @Override
            Assoc associativity() {
                return Assoc.LEFT;
            }

            @Override
            public boolean isBinary() {
                return true;
            }
        },
        XFY {
            @Override
            Assoc associativity() {
                return Assoc.RIGHT;
            }

            @Override
            public boolean isBinary() {
                return true;
            }
        },
        NONE {
        };

        /**
         * @return true if operator is prefix.
         */
        public boolean isPrefix() {
            return false;
        }

        /**
         * @return true if operator is postfix.
         */
        public boolean isPostfix() {
            return false;
        }

        /**
         * @return true if operator is binary.
         */
        public boolean isBinary() {
            return false;
        }

        /**
         * @return associativity of operator.
         */
        Assoc associativity() {
            return Assoc.NONE;
        }
    }

    /**
     * Convert an atom to a code
     *
     * @param code Atom describing operator
     * @return Internal operator code.
     */
    public Code parseCode(Atomic code) {
        if (code instanceof PrologAtom) {
            String name = ((PrologAtom) code).name();
            switch (name) {
                case "fx":
                    return Code.FX;
                case "fy":
                    return Code.FY;
                case "xf":
                    return Code.XF;
                case "yf":
                    return Code.YF;
                case "xfx":
                    return Code.XFX;
                case "xfy":
                    return Code.XFY;
                case "yfx":
                    return Code.YFX;
            }
        }
        throw new FutureDomainError(Interned.OPERATOR_PRIORITY_DOMAIN, code);
    }

    /**
     * Special no-operator
     */
    private OperatorEntry(int precedence) {
        this.functor = NULL_ATOM;
        this.precedence = precedence;
    }

    /**
     * New operator entry.
     *
     * @param functor Operator functor atom
     */
    public OperatorEntry(Atomic functor) {
        this.functor = functor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "op(" + precedence + "," + code + ",'" + functor + "')";
    }

    /**
     * Compares operators
     * TODO: This algorithm is not quite correct.
     *
     * @param o Other operator
     * @return -1 if left associative, 1 if right associative, 0 if same precedence
     */
    @Override
    public int compareTo(OperatorEntry o) {
        //
        // Comparison doesn't need to know if it's unary or binary,
        //
        // Case 1: x o1 y o2 z
        //     -1 indicates ((x o1 y) o2 z)
        //     +1 indicates (x o1 (y o2 z)
        //      0 indicates o1 & o2 are the same, ie explicit comparison required
        //
        // Case 2: x o1 y o2 (o2 postfix)
        //     -1 indicates ((x o1 y) o2)
        //     +1 indicates (x o1 (y o2))
        //
        // Case 3: o1 y o2 z (o1 prefix)
        //     -1 indicates ((o1 y) o2 z)
        //     +1 indicates (o1 (y o2 z))
        //
        // Case 4: o1 y o2 (prefix/postfix)
        //     -1 indicates ((o1 y) o2)
        //     +1 indicates (o1 (y o2))
        //
        if (precedence > o.precedence) {
            return 1;
        } else if (precedence < o.precedence) {
            return -1;
        }
        //
        // Precedence is the same,
        // now we get into details of x and y
        //
        Assoc assoc = code.associativity();
        Assoc otherAssoc = o.code.associativity();
        if (assoc == otherAssoc) {
            return assoc.precedence();
        } else if (assoc == Assoc.UNARY || otherAssoc == Assoc.UNARY) {
            // favor prefix first, postfix last
            return -1;
        } else {
            return 0; // consider any other combination incompatible
        }
    }

}
