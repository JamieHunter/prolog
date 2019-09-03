// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.instructions;

import prolog.expressions.Term;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * This is a variation of ExecBagOf that performs additional collation of the target set.
 */
public class ExecSetOf extends ExecBagOf {

    public ExecSetOf(Term template, Term callable, Term list) {
        super(template, callable, list);
    }

    /**
     * Collate the values into sorted unique instances
     *
     * @param source List of terms
     * @return collated set
     */
    @Override
    protected ArrayList<Term> collate(ArrayList<Term> source) {
        TreeSet<Term> collated = new TreeSet<>();
        collated.addAll(source);
        ArrayList<Term> copy = new ArrayList<>();
        copy.addAll(collated);
        return copy;
    }
}
