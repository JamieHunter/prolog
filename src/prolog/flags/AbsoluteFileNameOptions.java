// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.flags;

import prolog.constants.PrologAtom;
import prolog.exceptions.FutureFlagError;
import prolog.exceptions.PrologDomainError;
import prolog.execution.Environment;
import prolog.expressions.Term;
import prolog.library.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static prolog.bootstrap.Interned.internAtom;
import static prolog.flags.AbsoluteFileNameOptions.FileType.ATOM_txt;

/**
 * Structured options parsed from a list of option atoms, used for absolute_file_name.
 */
public class AbsoluteFileNameOptions implements Flags {

    private static OptionParser<AbsoluteFileNameOptions> parser = new OptionParser<>();

    static {
        parser.other(internAtom("extensions"), (o, v) -> o.addExtensions(v));
        parser.enumFlag(internAtom("file_type"), AbsoluteFileNameOptions.FileType.class,
                (o, v) -> o.setType(v));
    }

    public enum FileType {
        ATOM_txt,
        ATOM_prolog,
        ATOM_directory
    }

    public ArrayList<String> extensions = new ArrayList<>();
    public FileType type = ATOM_txt;

    private void addExtensions(Term list) {
        List<Term> termList = Lists.extractList(list);
        for(Term term : termList) {
            PrologAtom extAtom = PrologAtom.from(term);
            String extString = extAtom.get();
            if (extString.length() > 0 && !extString.startsWith(".")) {
                extString = "." + extString;
            }
            extensions.add(extString);
        }
    }

    private void setType(FileType type) {
        this.type = type;
    }

    private void typeExtensions() {
        if (!extensions.isEmpty()) {
            return;
        }
        switch(type) {
            case ATOM_prolog:
                extensions.add(".pl");
                break;
        }
        extensions.add("");
    }

    /**
     * Set this object of options from a list of option terms.
     *
     * @param environment Execution environment
     * @param optionsTerm List of options
     */
    public AbsoluteFileNameOptions(Environment environment, Term optionsTerm) {
        try {
            parser.apply(environment, this, optionsTerm);
            typeExtensions();
        } catch (FutureFlagError ffe) {
            throw PrologDomainError.error(environment, environment.getAtom("absolute_file_name_option"), ffe.getTerm(), ffe);
        }
    }

}
