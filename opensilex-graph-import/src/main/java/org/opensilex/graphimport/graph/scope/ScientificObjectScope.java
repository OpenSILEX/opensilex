//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph.scope;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.graphimport.graph.InstanceScope;
import org.opensilex.graphimport.graph.LookupContext;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

/**
 * Scientific objects, looked up only in the graph of the experiment the user chose and may read
 * ({@link LookupContext#of} checks it). Without an experiment, none is looked up.
 *
 * @author Arnaud Charleroy
 */
public class ScientificObjectScope implements InstanceScope {

    @Override
    public URI rootClass() {
        return URI.create(Oeso.ScientificObject.getURI());
    }

    @Override
    public boolean canLookUp(LookupContext context) {
        return context.experiment() != null;
    }

    @Override
    public URI graph(LookupContext context) {
        return context.experiment();
    }

    @Override
    public Set<URI> visible(Collection<URI> found, LookupContext context) {
        return Set.copyOf(found);
    }
}
