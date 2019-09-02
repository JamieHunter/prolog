// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.bootstrap;

import prolog.execution.Environment;
import prolog.execution.Instruction;
import prolog.expressions.CompoundTerm;
import prolog.expressions.CompoundTermImpl;
import prolog.expressions.Term;
import prolog.flags.ReadOptions;
import prolog.flags.StreamProperties;
import prolog.instructions.DeferredCallInstruction;
import prolog.instructions.ExecCall;
import prolog.instructions.ExecOnce;
import prolog.io.InputBuffered;
import prolog.io.InputLineHandler;
import prolog.io.PrologInputStream;
import prolog.io.SequentialInputStream;
import prolog.library.Dictionary;
import prolog.library.Io;
import prolog.parser.ExpressionReader;
import prolog.parser.Tokenizer;
import prolog.predicates.OnDemand;

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
