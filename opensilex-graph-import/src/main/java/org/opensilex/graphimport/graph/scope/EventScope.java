//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph.scope;

import org.opensilex.graphimport.graph.InstanceScope;
import org.opensilex.graphimport.graph.LookupContext;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

/**
 * Events, so that a sheet may hold them. None is looked up by name: an event has none. The event writer
 * finds those the instance holds by their type, what they concern and their moments.
 *
 * @author Arnaud Charleroy
 */
public class EventScope implements InstanceScope {

    @Override
    public URI rootClass() {
        return URI.create("http://www.opensilex.org/vocabulary/oeev#Event");
    }

    @Override
    public boolean canLookUp(LookupContext context) {
        return false;
    }

    @Override
    public Set<URI> visible(Collection<URI> found, LookupContext context) {
        return Set.of();
    }
}
