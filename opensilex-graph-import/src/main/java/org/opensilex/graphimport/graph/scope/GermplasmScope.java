//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph.scope;

import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.graph.InstanceScope;
import org.opensilex.graphimport.graph.LookupContext;
import org.opensilex.graphimport.ontology.Uris;

import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Germplasm, without those the platform hides from the user.
 *
 * @author Arnaud Charleroy
 */
public class GermplasmScope implements InstanceScope {

    @Override
    public URI rootClass() {
        return URI.create(Oeso.Germplasm.getURI());
    }

    @Override
    public Set<URI> visible(Collection<URI> found, LookupContext context) throws Exception {
        Set<URI> hidden = new GermplasmDAO(context.sparql(), context.nosql())
                .getUnauthorizedGermplasms(found, context.user()).stream()
                .map(Uris::expand)
                .collect(Collectors.toSet());
        return found.stream().filter(uri -> !hidden.contains(uri)).collect(Collectors.toSet());
    }
}
