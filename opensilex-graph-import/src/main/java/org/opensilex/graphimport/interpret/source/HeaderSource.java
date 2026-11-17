//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret.source;

import org.opensilex.graphimport.interpret.Clue;
import org.opensilex.graphimport.interpret.ColumnContext;
import org.opensilex.graphimport.interpret.EvidenceSource;
import org.opensilex.graphimport.interpret.Reasons;
import org.opensilex.graphimport.ontology.HeaderReading;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.List;

/**
 * A header written as a predicate the class accepts settles its column: the person who wrote it said
 * what it holds.
 *
 * @author Arnaud Charleroy
 */
public class HeaderSource implements EvidenceSource {

    public static final String ID = "header";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Role role() {
        return Role.DECIDES;
    }

    @Override
    public List<Clue<URI>> gather(ColumnContext column) {
        HeaderReading reading = column.reading();
        if (!reading.names() || reading.form() == HeaderReading.Form.TYPE) {
            return List.of();
        }
        PropertyView property = column.classView().getProperty(reading.property());
        if (property == null) {
            return List.of();
        }
        return List.of(Clue.of(property.uri(), ID, 1, Reasons.header(Labelling.of(property, column))));
    }
}
