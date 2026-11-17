//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph.scope;

import org.opensilex.core.experiment.factor.dal.FactorLevelDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.graph.InstanceScope;
import org.opensilex.graphimport.graph.LookupContext;
import org.opensilex.graphimport.ontology.Uris;

import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The levels of the factors of the experiment the user chose and may read: the only levels a
 * scientific object of that experiment may hold, by the platform's own rule. Without an experiment,
 * none is looked up.
 *
 * @author Arnaud Charleroy
 */
public class FactorLevelScope implements InstanceScope {

    @Override
    public URI rootClass() {
        return URI.create(Oeso.FactorLevel.getURI());
    }

    @Override
    public boolean canLookUp(LookupContext context) {
        return context.experiment() != null;
    }

    @Override
    public URI graph(LookupContext context) {
        return FactorScope.factorGraph(context);
    }

    @Override
    public Set<URI> visible(Collection<URI> found, LookupContext context) throws Exception {
        Set<URI> ofExperiment;
        try (var levels = new FactorLevelDAO(context.sparql()).getLevelsFromExperiment(context.experiment())) {
            ofExperiment = levels.map(Uris::expand).collect(Collectors.toSet());
        }
        return found.stream().filter(ofExperiment::contains).collect(Collectors.toSet());
    }
}
