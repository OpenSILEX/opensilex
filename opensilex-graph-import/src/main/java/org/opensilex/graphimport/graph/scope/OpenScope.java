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
import java.util.List;
import java.util.Set;

/**
 * A class the platform restricts no reading of: every user sees every instance. Projects, devices and
 * variables are such classes today ({@code ProjectDAO.getList}, {@code DeviceDAO.getList} and
 * {@code VariableDAO.getList} filter nothing by user).
 *
 * @author Arnaud Charleroy
 */
public class OpenScope implements InstanceScope {

    private final URI rootClass;

    public OpenScope(URI rootClass) {
        this.rootClass = rootClass;
    }

    /** The classes the platform lets everyone read. */
    public static List<InstanceScope> platformDefaults() {
        return List.of(
                new OpenScope(URI.create(Oeso.Project.getURI())),
                new OpenScope(URI.create(Oeso.Device.getURI())),
                new OpenScope(URI.create(Oeso.Variable.getURI())));
    }

    @Override
    public URI rootClass() {
        return rootClass;
    }

    @Override
    public Set<URI> visible(Collection<URI> found, LookupContext context) {
        return Set.copyOf(found);
    }
}
