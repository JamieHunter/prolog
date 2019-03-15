// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.bootstrap;

import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.instructions.ExecCall;
import prolog.io.PrologReadStream;
import prolog.io.PrologReadStreamImpl;
import prolog.library.Dictionary;
import prolog.library.Io;
import prolog.predicates.OnDemand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Bootstrap read script file from a Java resource. Note that ':-' directives are allowed, however there is limited
 * error handling. Specifically if backtracking occurs, an internal error is thrown.
 */
public class LoadResourceOnDemand implements OnDemand {

    private final Class<?> cls;
    private final String resource;

    public LoadResourceOnDemand(Class<?> cls, String resource) {
        this.cls = cls;
        this.resource = resource;
    }

    /**
     * Called to demand-load resource.
     *
     * @param environment Execution environment.
     */
    @Override
    public void load(Environment environment) {
        InputStream javaStream = cls.getResourceAsStream(resource);
        PrologReadStream prologStream = new PrologReadStreamImpl(resource,
                new BufferedReader(new InputStreamReader(javaStream, StandardCharsets.UTF_8)));
        for (; ; ) {
            Term term = prologStream.read(environment, new ReadOptions(environment, null));
            if (term == Io.END_OF_FILE) {
                break;
            }
            if (CompoundTerm.termIsA(term, Interned.CLAUSE_FUNCTOR, 1)) {
                // Allow limited compiler directive support, e.g. to change permissions
                // Execution is wrapped in call, but not guarded. This should only be used
                // for a select number of directives.
                CompoundTerm clause = (CompoundTerm) term;
                final Term goalTerm = clause.get(0);
                Instruction callable = new ExecCall(
                        environment,
                        goalTerm);
                callable.invoke(environment);
                if (!environment.isForward()) {
                    throw new InternalError("Directive error in resource " + resource);
                }
            } else {
                Dictionary.addClauseZ(environment, term);
            }
        }
        try {
            javaStream.close();
        } catch (IOException ioe) {
            throw new InternalError(ioe);
        }
    }
}
