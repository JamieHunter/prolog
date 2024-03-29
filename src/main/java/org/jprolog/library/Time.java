// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.library;

import org.jprolog.bootstrap.Predicate;
import org.jprolog.constants.PrologFloat;
import org.jprolog.execution.Environment;
import org.jprolog.expressions.Term;
import org.jprolog.unification.Unifier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * File is referenced by {@link Library} to parse all annotations.
 * Provides time predicates and utilities.
 */
public final class Time {
    private Time() {
        // Static methods/fields only
    }

    /**
     * Convert an instant to a Prolog TimeStamp
     *
     * @param instant time represented by Instant
     * @return Prolog TimeStamp
     */
    public static PrologFloat toPrologTime(Instant instant) {
        return new PrologFloat(((double) instant.toEpochMilli()) / 1000.0D);
    }

    /**
     * Convert an epoch time (seconds) to a Prolog TimeStamp
     *
     * @param epochTime time represented in seconds
     * @return Prolog TimeStamp
     */
    public static PrologFloat toPrologTime(long epochTime) {
        return new PrologFloat((double) epochTime);
    }

    /**
     * Convert a Java LocalDateTime to Prolog TimeStamp
     *
     * @param time time represented as LocalDateTime
     * @return Prolog TimeStamp
     */
    public static PrologFloat toPrologTime(LocalDateTime time) {
        return toPrologTime(time.toInstant(ZoneOffset.UTC));
    }

    /**
     * Current time
     *
     * @return Prolog TimeStamp
     */
    public static PrologFloat now() {
        return toPrologTime(Instant.now());
    }

    /**
     * Retrieves a timestamp representing current time
     *
     * @param environment Execution environment
     * @param timeStamp   Unified with current time
     */
    @Predicate("get_time")
    public static void getTime(Environment environment, Term timeStamp) {
        PrologFloat now = now();
        Unifier.unifyFloat(environment, timeStamp, now.get());
    }
}
