//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.HeaderReading;

import java.util.List;

/**
 * What a source may look at to interpret one column.
 *
 * @param sheet      the sheet the column belongs to
 * @param index      the position of the column, from 0
 * @param header     the header as written
 * @param reading    what the header says by its form
 * @param values     the first distinct values of the column, in row order, missing ones left out
 * @param classView  what the class of the sheet accepts
 * @param identifier true when the column identifies the rows of its sheet
 * @param sheets     what is known of every sheet of the workbook, this one included
 * @param services   the lookups shared by the whole interpretation
 * @author Arnaud Charleroy
 */
public record ColumnContext(
        SheetStructure sheet,
        int index,
        String header,
        HeaderReading reading,
        List<String> values,
        ClassView classView,
        boolean identifier,
        List<SheetFacts> sheets,
        InterpretationServices services) {
}
