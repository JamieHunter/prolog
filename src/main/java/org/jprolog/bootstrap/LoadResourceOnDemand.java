// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package org.jprolog.bootstrap;

import org.jprolog.execution.Environment;
import org.jprolog.execution.Instruction;
import org.jprolog.expressions.CompoundTerm;
import org.jprolog.expressions.Term;
import org.jprolog.instructions.DeferredCallInstruction;
import org.jprolog.instructions.ExecOnce;
import org.jprolog.predicates.OnDemand;
import org.jprolog.flags.ReadOptions;
import org.jprolog.flags.StreamProperties;
import org.jprolog.io.InputBuffered;
import org.jprolog.io.InputLineHandler;
import org.jprolog.io.PrologInputStream;
import org.jprolog.io.SequentialInputStream;
import org.jprolog.library.Dictionary;
import org.jprolog.library.Io;
import org.jprolog.parser.ExpressionReader;
import org.jprolog.parser.Tokenizer;

import java.io.IOException;
import java.io.InputStream;

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
        try (InputStream javaStream = cls.getResourceAsStream(resource)) {
            if (javaStream == null) {
                throw new IOException("Unable to resolve resource " + resource);
            }
            PrologInputStream baseStream = new SequentialInputStream(javaStream);
            PrologInputStream bufferedStream = new InputBuffered(new InputLineHandler(baseStream, StreamProperties.NewLineMode.ATOM_detect), -1);
            Tokenizer tokenizer = new Tokenizer(environment,
                    new ReadOptions(environment, null),
                    bufferedStream
            );
            ExpressionReader reader = new ExpressionReader(tokenizer);
            for (; ; ) {
                Term term = reader.read();
                if (term == Io.END_OF_FILE) {
                    break;
                }
                if (CompoundTerm.termIsA(term, Interned.CLAUSE_FUNCTOR, 1)) {
                    // Allow limited compiler directive support, e.g. to change permissions
                    // Execution is wrapped in call, but not guarded. This should only be used
                    // for a select number of directives.
                    CompoundTerm clause = (CompoundTerm) term;
                    final Term goalTerm = clause.get(0);
                    Instruction callable = new ExecOnce(
                            new DeferredCallInstruction(goalTerm));
                    callable.invoke(environment);
                    if (!environment.isForward()) {
                        throw new InternalError("Directive error in resource " + resource);
                    }
                } else {
                    Dictionary.addClauseZ(environment, term);
                }
            }
            javaStream.close();
        } catch (IOException ioe) {
            throw new InternalError(ioe);
        }
    }
}
