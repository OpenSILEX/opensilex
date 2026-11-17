//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph.scope;

import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.graph.InstanceScope;
import org.opensilex.graphimport.graph.LookupContext;
import org.opensilex.graphimport.ontology.Uris;

import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Experiments, among those the user may read.
 *
 * @author Arnaud Charleroy
 */
public class ExperimentScope implements InstanceScope {

    @Override
    public URI rootClass() {
        return URI.create(Oeso.Experiment.getURI());
    }

    @Override
    public Set<URI> visible(Collection<URI> found, LookupContext context) throws Exception {
        Set<URI> readable = new ExperimentDAO(context.sparql(), context.nosql(), context.fs())
                .getUserExperiments(context.user()).stream()
                .map(Uris::expand)
                .collect(Collectors.toSet());
        return found.stream().filter(readable::contains).collect(Collectors.toSet());
    }
}
