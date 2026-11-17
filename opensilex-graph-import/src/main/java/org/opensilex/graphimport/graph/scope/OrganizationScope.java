//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph.scope;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.OrganizationSearchFilter;
import org.opensilex.graphimport.graph.InstanceScope;
import org.opensilex.graphimport.graph.LookupContext;
import org.opensilex.graphimport.ontology.Uris;

import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Organizations, among those the platform's search shows the user.
 *
 * @author Arnaud Charleroy
 */
public class OrganizationScope implements InstanceScope {

    @Override
    public URI rootClass() {
        return URI.create(FOAF.Organization.getURI());
    }

    @Override
    public Set<URI> visible(Collection<URI> found, LookupContext context) throws Exception {
        if (found.isEmpty()) {
            return Set.of();
        }
        // Not restricted to the found URIs in the filter: it compares URI objects, and a prefixed and a
        // full form of one organization would not match.
        OrganizationSearchFilter filter = new OrganizationSearchFilter().setUser(context.user());
        Set<URI> readable = new OrganizationDAO(context.sparql()).search(filter).stream()
                .map(OrganizationModel::getUri)
                .map(Uris::expand)
                .collect(Collectors.toSet());
        return found.stream().filter(readable::contains).collect(Collectors.toSet());
    }
}
