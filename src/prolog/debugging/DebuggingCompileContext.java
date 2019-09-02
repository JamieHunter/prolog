// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.debugging;

import prolog.execution.CompileContext;
import prolog.execution.Environment;

public class DebuggingCompileContext extends CompileContext {
    /**
     * Create a new compile block that compiles debugger-aware instructions
     *
     * @param environmentShared Execution shared environment.
     */
    public DebuggingCompileContext(Environment.Shared environmentShared) {
        super(environmentShared);
    }
}
