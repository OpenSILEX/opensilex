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
 * Data, so that a sheet may hold them. None is looked up: the platform keeps data outside the
 * triplestore, and a datum has no name.
 *
 * @author Arnaud Charleroy
 */
public class DataScope implements InstanceScope {

    public static final URI DATA = URI.create("http://www.opensilex.org/vocabulary/oeso#Data");

    @Override
    public URI rootClass() {
        return DATA;
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
