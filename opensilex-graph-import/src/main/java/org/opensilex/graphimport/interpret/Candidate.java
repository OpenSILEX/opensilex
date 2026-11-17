//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import java.util.List;

/**
 * A possible answer with its score and every piece of evidence behind it.
 *
 * @param target   the answer
 * @param score    the sum of the weights, between 0 and 1
 * @param evidence what was found for and against it
 * @param <T>      the kind of answer
 * @author Arnaud Charleroy
 */
public record Candidate<T>(T target, double score, List<Evidence> evidence) {

    public Candidate {
        evidence = List.copyOf(evidence);
    }
}
