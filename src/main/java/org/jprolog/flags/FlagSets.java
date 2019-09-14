// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.flags;

import org.jprolog.bootstrap.LibraryBase;

/**
 * Ensures all validators are referenced
 */
public class FlagSets extends LibraryBase {
    {
        consult(OpenOptions.class);
        consult(CloseOptions.class);
        consult(ReadOptions.class);
        consult(WriteOptions.class);
        consult(CreateFlagOptions.class);
        consult(AbsoluteFileNameOptions.class);
        consult(PrologFlags.class);
        consult(StreamProperties.class);
    }
}