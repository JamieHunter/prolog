// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.expressions;

import org.jprolog.bootstrap.Interned;
import org.jprolog.constants.PrologAtom;
import org.jprolog.constants.PrologAtomLike;
import org.jprolog.constants.PrologChars;
import org.jprolog.constants.PrologCodePoints;
import org.jprolog.constants.PrologEmptyList;
import org.jprolog.constants.PrologInteger;
import org.jprolog.constants.PrologString;
import org.jprolog.constants.PrologStringAsList;
import org.jprolog.exceptions.FutureInstantiationError;
import org.jprolog.exceptions.FutureRepresentationError;
import org.jprolog.exceptions.FutureTypeError;

/**
 * Utilities for various types of strings.
 */
public final class Strings {
    private Strings() {
    }


    /**
     * Utility to extract list of code-points to a string
     *
     * @param list List of code points
     * @return String
     */
    public static String stringFromCodePoints(Term list) {
        Term origList = list;
        if (list == PrologEmptyList.EMPTY_LIST) {
            return "";
        }
        if (list instanceof PrologCodePoints) {
            return ((PrologCodePoints) list).getStringValue();
        }
        StringBuilder builder = new StringBuilder();
        while (list != PrologEmptyList.EMPTY_LIST) {
            if (list instanceof PrologCodePoints) {
                builder.append(((PrologCodePoints) list).getStringValue());
                return builder.toString();
            }
            if (TermList.isList(list)) {
                CompoundTerm comp = (CompoundTerm) list;
                Term e = comp.get(0);
                list = comp.get(1);
                if (e instanceof PrologInteger) {
                    builder.append(((PrologInteger) e).toChar());
                } else if (!e.isInstantiated()) {
                    throw new FutureInstantiationError(list);
                } else {
                    throw new FutureRepresentationError(Interned.CHARACTER_CODE_REPRESENTATION);
                }
            } else if (!list.isInstantiated()) {
                throw new FutureInstantiationError(list);
            } else {
                throw new FutureTypeError(Interned.LIST_TYPE, origList);
            }
        }
        return builder.toString();
    }

    /**
     * Utility to extract list of characters to a string
     *
     * @param list List of characters
     * @return String
     */
    public static String stringFromChars(Term list) {
        Term origList = list;
        if (list == PrologEmptyList.EMPTY_LIST) {
            return "";
        }
        if (list instanceof PrologChars) {
            return ((PrologChars) list).getStringValue();
        }
        StringBuilder builder = new StringBuilder();
        while (list != PrologEmptyList.EMPTY_LIST) {
            if (list instanceof PrologChars) {
                builder.append(((PrologChars) list).getStringValue());
                return builder.toString();
            }
            if (TermList.isList(list)) {
                CompoundTerm comp = (CompoundTerm) list;
                Term e = comp.get(0);
                list = comp.get(1);
                if (e instanceof PrologAtomLike) {
                    String chr = ((PrologAtomLike) e).name();
                    if (chr.length() != 1) {
                        throw new FutureTypeError(Interned.CHARACTER_TYPE, e);
                    }
                    builder.append(chr.charAt(0));
                } else if (!e.isInstantiated()) {
                    throw new FutureInstantiationError(list);
                } else {
                    throw new FutureTypeError(Interned.CHARACTER_TYPE, e);
                }
            } else if (!list.isInstantiated()) {
                throw new FutureInstantiationError(list);
            } else {
                throw new FutureTypeError(Interned.LIST_TYPE, origList);
            }
        }
        return builder.toString();
    }

    /**
     * Accepts any string type except atoms
     *
     * @param term Term to extract string from
     * @return String
     */
    public static String stringFromAnyString(Term term) {
        if (!term.isInstantiated()) {
            throw new FutureInstantiationError(term);
        }
        if (term == PrologEmptyList.EMPTY_LIST) {
            return "";
        }
        if (term instanceof PrologString) {
            return ((PrologString) term).get();
        }
        if (term instanceof PrologStringAsList) {
            return ((PrologStringAsList) term).getStringValue();
        }
        if (TermList.isList(term)) {
            CompoundTerm comp = (CompoundTerm) term;
            Term head = comp.get(0);
            if (head instanceof PrologInteger) {
                return stringFromCodePoints(term);
            }
            if (head instanceof PrologAtom) {
                return stringFromChars(term);
            }
        }
        throw new FutureTypeError(Interned.STRING_TYPE, term);
    }

    /**
     * String from anything that can depict a string, including atoms.
     *
     * @param term Term to convert
     * @return String
     */
    public static String stringFromAtomOrAnyString(Term term) {
        if (term.isAtom()) {
            return ((PrologAtomLike) term).name();
        } else {
            return stringFromAnyString(term);
        }
    }

    /**
     * Given a string, convert to list of characters
     * @param s String
     * @return list of characters or empty list
     */
    public static Term charsFromString(String s) {
        if (s.length() == 0) {
            return PrologEmptyList.EMPTY_LIST;
        } else {
            return new PrologChars(s);
        }
    }

    /**
     * Given a string, convert to list of code points
     * @param s String
     * @return list of code points or empty list
     */
    public static Term codePointsFromString(String s) {
        if (s.length() == 0) {
            return PrologEmptyList.EMPTY_LIST;
        } else {
            return new PrologCodePoints(s);
        }
    }
}
