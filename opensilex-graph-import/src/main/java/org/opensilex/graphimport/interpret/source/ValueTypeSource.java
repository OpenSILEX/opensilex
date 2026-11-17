//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret.source;

import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.interpret.Clue;
import org.opensilex.graphimport.interpret.ColumnContext;
import org.opensilex.graphimport.interpret.EvidenceSource;
import org.opensilex.graphimport.interpret.Reasons;
import org.opensilex.graphimport.ontology.PropertyKind;
import org.opensilex.graphimport.ontology.PropertyView;
import org.opensilex.graphimport.ontology.literal.DateOrder;
import org.opensilex.graphimport.ontology.literal.LiteralReader;
import org.opensilex.graphimport.ontology.literal.LiteralReading;
import org.opensilex.graphimport.ontology.literal.ReadingContext;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Whether the values read as the datatype of a literal property. It never proposes a property, since
 * every value reads as text: it confirms or casts doubt on what another source proposed.
 * <p>
 * A date whose day and month order is still open, a time without its zone, a date without its time
 * count as readable: they say the column has choices to make, not that it holds something else.
 *
 * @author Arnaud Charleroy
 */
public class ValueTypeSource implements EvidenceSource {

    public static final String ID = "values";
    static final double MOSTLY = 0.9;
    static final double CONFIRMS = 0.1;
    static final double HALF = 0.5;
    static final double DOUBTS = -0.2;
    static final double CONTRADICTS = -0.5;

    private static final Set<String> STILL_TO_DECIDE = Set.of(
            "literal.ambiguousDate", "literal.missingZone", "literal.missingTime");

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Role role() {
        return Role.MODULATES;
    }

    @Override
    public List<Clue<URI>> gather(ColumnContext column) {
        List<String> values = column.values();
        if (values.isEmpty()) {
            return List.of();
        }
        LiteralReader reader = new LiteralReader(
                new ReadingContext(column.sheet().isDate1904(), DateOrder.detect(values), null));
        List<Clue<URI>> clues = new ArrayList<>();
        for (PropertyView property : column.classView().getProperties()) {
            if (property.kind() != PropertyKind.DATA || property.range() == null) {
                continue;
            }
            int readable = 0;
            for (String value : values) {
                LiteralReading reading = reader.read(value, property.range());
                if (!reading.isRefused() || STILL_TO_DECIDE.contains(reading.problem().getKey())) {
                    readable++;
                }
            }
            clues.add(clue(property.uri(), readable, values.size(), ClassCatalog.localName(property.range())));
        }
        return clues;
    }

    private static Clue<URI> clue(URI property, int readable, int total, String datatype) {
        double share = (double) readable / total;
        if (share >= MOSTLY) {
            return Clue.of(property, ID, CONFIRMS, Reasons.valuesRead(readable, total, datatype));
        }
        double weight = share >= HALF ? DOUBTS : CONTRADICTS;
        return Clue.of(property, ID, weight, Reasons.valuesNotRead(readable, total, datatype));
    }
}
