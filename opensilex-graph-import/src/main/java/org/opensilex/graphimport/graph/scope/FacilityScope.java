//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph.scope;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.bll.FacilityLogic;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.graphimport.graph.InstanceScope;
import org.opensilex.graphimport.graph.LookupContext;
import org.opensilex.graphimport.ontology.Uris;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Facilities, through the facility logic, which keeps only those of the user's organizations and
 * sites.
 *
 * @author Arnaud Charleroy
 */
public class FacilityScope implements InstanceScope {

    @Override
    public URI rootClass() {
        return URI.create(Oeso.Facility.getURI());
    }

    @Override
    public Set<URI> visible(Collection<URI> found, LookupContext context) throws Exception {
        if (found.isEmpty()) {
            return Set.of();
        }
        return new FacilityLogic(context.sparql(), context.nosql(), context.user(), context.fs())
                .getList(new ArrayList<>(found), context.user()).stream()
                .map(FacilityModel::getUri)
                .map(Uris::expand)
                .collect(Collectors.toSet());
    }
}
