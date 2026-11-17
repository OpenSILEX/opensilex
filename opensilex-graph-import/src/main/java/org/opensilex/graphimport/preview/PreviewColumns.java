//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import org.opensilex.graphimport.interpret.ColumnInterpretation;
import org.opensilex.graphimport.interpret.Interpretation;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.interpret.SheetInterpretation;
import org.opensilex.graphimport.ontology.PropertyKind;
import org.opensilex.graphimport.ontology.PropertyView;
import org.opensilex.graphimport.validate.NodeClasses;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What each column of each sheet gives the graph, by the same rules as the graph's builder: a column
 * gives values when its property is chosen and the class of its sheet accepts it.
 *
 * @author Arnaud Charleroy
 */
final class PreviewColumns {

    private PreviewColumns() {
    }

    static Map<String, List<PreviewColumn>> of(Interpretation interpretation, InterpretationServices services) {
        Map<String, List<PreviewColumn>> bySheet = new LinkedHashMap<>();
        for (SheetInterpretation sheet : interpretation.sheets()) {
            List<PreviewColumn> columns = new ArrayList<>();
            for (ColumnInterpretation column : sheet.columns()) {
                columns.add(column(sheet, column, services));
            }
            bySheet.put(sheet.name(), columns);
        }
        return bySheet;
    }

    private static PreviewColumn column(SheetInterpretation sheet, ColumnInterpretation column,
                                        InterpretationServices services) {
        boolean inGraph = sheet.classView() != null && sheet.sheetClass().chosen() != null;
        if (inGraph && column.typeColumn()) {
            return new PreviewColumn(column.index(), column.header(), PreviewColumn.Role.TYPE, null, null);
        }
        URI chosen = column.property().chosen();
        PropertyView property = inGraph && chosen != null ? sheet.classView().getProperty(chosen) : null;
        String label = property == null ? null : NodeClasses.label(property, services);
        URI uri = property == null ? null : property.uri();
        PreviewColumn.Role role;
        if (inGraph && column.identifier()) {
            role = PreviewColumn.Role.IDENTIFIER;
        } else if (property == null) {
            role = PreviewColumn.Role.LEFT_OUT;
        } else {
            role = property.kind() == PropertyKind.OBJECT ? PreviewColumn.Role.LINK : PreviewColumn.Role.VALUE;
        }
        return new PreviewColumn(column.index(), column.header(), role, uri, label);
    }
}
