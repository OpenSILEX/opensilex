//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph.scope;

import org.opensilex.core.experiment.factor.dal.FactorDAO;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.graph.InstanceScope;
import org.opensilex.graphimport.graph.LookupContext;
import org.opensilex.graphimport.ontology.Uris;

import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The factors of the experiment the user chose and may read ({@link LookupContext#of} checks it): a
 * factor belongs to one experiment, and the same name may designate another factor elsewhere. Without
 * an experiment, none is looked up.
 *
 * @author Arnaud Charleroy
 */
public class FactorScope implements InstanceScope {

    @Override
    public URI rootClass() {
        return URI.create(Oeso.Factor.getURI());
    }

    @Override
    public boolean canLookUp(LookupContext context) {
        return context.experiment() != null;
    }

    @Override
    public URI graph(LookupContext context) {
        return factorGraph(context);
    }

    @Override
    public Set<URI> visible(Collection<URI> found, LookupContext context) throws Exception {
        Set<URI> studied = new FactorDAO(context.sparql()).getByExperiment(context.experiment(), null).stream()
                .map(factor -> Uris.expand(factor.getUri()))
                .collect(Collectors.toSet());
        return found.stream().filter(studied::contains).collect(Collectors.toSet());
    }

    /** The graph the platform keeps factors and their levels in. */
    static URI factorGraph(LookupContext context) {
        try {
            return context.sparql().getDefaultGraphURI(FactorModel.class);
        } catch (Exception e) {
            throw new IllegalStateException("The graph of factors is not known to the platform", e);
        }
    }
}
