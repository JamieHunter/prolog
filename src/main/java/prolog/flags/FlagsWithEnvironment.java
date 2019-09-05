// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.flags;

import prolog.execution.Environment;

/**
 * Version of {@link Flags} that also provides the environment.
 */
public interface FlagsWithEnvironment extends Flags {
    Environment environment();
}
