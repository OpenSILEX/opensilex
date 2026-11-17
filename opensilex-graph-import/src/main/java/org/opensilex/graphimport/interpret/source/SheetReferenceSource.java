//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret.source;

import org.opensilex.graphimport.graph.InstanceIndex;
import org.opensilex.graphimport.interpret.Clue;
import org.opensilex.graphimport.interpret.ColumnContext;
import org.opensilex.graphimport.interpret.EvidenceSource;
import org.opensilex.graphimport.interpret.Reasons;
import org.opensilex.graphimport.interpret.SheetFacts;
import org.opensilex.graphimport.ontology.PropertyKind;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Values that are identifiers of the rows of a sheet, this one included: the column links to those
 * rows, through a property whose range the rows' class belongs to. A plot's {@code isPartOf} usually
 * names another plot of the same sheet.
 *
 * @author Arnaud Charleroy
 */
public class SheetReferenceSource implements EvidenceSource {

    public static final String ID = "sheets";
    static final double WEIGHT = 0.8;
    static final double MIN_SHARE = 0.5;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Role role() {
        return Role.PROPOSES;
    }

    @Override
    public List<Clue<URI>> gather(ColumnContext column) {
        if (column.identifier() || column.values().isEmpty()) {
            return List.of();
        }
        List<Clue<URI>> clues = new ArrayList<>();
        int total = column.values().size();
        for (SheetFacts sheet : column.sheets()) {
            if (sheet.classView() == null || sheet.identifierKeys().isEmpty()) {
                continue;
            }
            int referenced = 0;
            for (String value : column.values()) {
                if (sheet.identifierKeys().contains(InstanceIndex.key(value))) {
                    referenced++;
                }
            }
            double share = (double) referenced / total;
            if (share < MIN_SHARE) {
                continue;
            }
            for (PropertyView property : column.classView().getProperties()) {
                if (property.kind() == PropertyKind.OBJECT && property.range() != null
                        && sheet.classView().isA(property.range())) {
                    clues.add(Clue.of(property.uri(), ID, WEIGHT * share,
                            Reasons.sheetIdentifiers(referenced, total, sheet.name())));
                }
            }
        }
        return clues;
    }
}
