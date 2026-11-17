//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.graphimport.graph.InstanceIndex;
import org.opensilex.graphimport.interpret.alias.LearnedAliases;
import org.opensilex.graphimport.ontology.HeaderResolver;

import java.net.URI;

/**
 * The lookups one interpretation shares between its sheets and columns, each answering once.
 *
 * @param catalog   the classes a sheet may hold
 * @param views     the views of those classes
 * @param instances the existing instances the user may see
 * @param aliases   the aliases the user taught
 * @param headers   recognises headers written as predicates
 * @param owner     the user the interpretation is made for
 * @param language  the language labels are given in
 * @author Arnaud Charleroy
 */
public record InterpretationServices(
        ClassCatalog catalog,
        ClassViews views,
        InstanceIndex instances,
        LearnedAliases aliases,
        HeaderResolver headers,
        URI owner,
        String language) {
}
