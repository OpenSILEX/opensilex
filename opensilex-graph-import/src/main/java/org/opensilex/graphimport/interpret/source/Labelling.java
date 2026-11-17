//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret.source;

import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.interpret.ColumnContext;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;

/**
 * How the sources name a property or a class in their sentences.
 *
 * @author Arnaud Charleroy
 */
final class Labelling {

    private Labelling() {
    }

    static String of(PropertyView property, ColumnContext column) {
        String label = property.labels().in(column.services().language());
        return label != null ? label : ClassCatalog.localName(property.uri());
    }

    static String ofClass(URI classUri, ColumnContext column) {
        return column.services().catalog().labelOf(classUri, column.services().language());
    }
}
