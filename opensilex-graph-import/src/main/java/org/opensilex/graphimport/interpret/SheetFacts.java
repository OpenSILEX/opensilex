//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.graphimport.ontology.ClassView;

import java.util.Set;

/**
 * What the columns of every sheet may rely on about a sheet once its class and its identifier column
 * are settled.
 *
 * @param name           the sheet's name
 * @param classView      what its class accepts; null while its class is undecided
 * @param identifierKeys the identifiers of its rows, in {@link org.opensilex.graphimport.graph.InstanceIndex#key}
 *                       form; empty while its identifier column is undecided
 * @author Arnaud Charleroy
 */
public record SheetFacts(String name, ClassView classView, Set<String> identifierKeys) {

    public SheetFacts {
        identifierKeys = Set.copyOf(identifierKeys);
    }
}
