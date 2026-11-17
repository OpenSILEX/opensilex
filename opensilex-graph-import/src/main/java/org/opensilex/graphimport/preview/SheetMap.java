//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

import java.util.List;

/**
 * The sheets of an import and how their rows hang together: rows of two sheets that are one resource,
 * links from a sheet to the rows of another, links to existing resources.
 *
 * @param sheets the sheets, in the workbook's order
 * @param links  how they hang together, each with how many rows or cells say so
 * @author Arnaud Charleroy
 */
public record SheetMap(List<Sheet> sheets, List<Link> links) {

    public SheetMap {
        sheets = List.copyOf(sheets);
        links = List.copyOf(links);
    }

    /**
     * A sheet.
     *
     * @param name       its name in the import
     * @param classLabel the class of its rows, in the user's language; null while not chosen
     * @param rowCount   how many rows of values it has
     * @param resources  how many resources its rows are
     */
    public record Sheet(String name, String classLabel, int rowCount, int resources) {
    }

    /**
     * How two sheets, or a sheet and existing resources, hang together.
     *
     * @param from     the sheet the cells or rows are in
     * @param to       the other sheet; for {@link Kind#INSTANCE}, the class of the existing resources
     * @param kind     how they hang together
     * @param property the label of the property of the links; null for {@link Kind#SAME_RESOURCE}
     * @param count    how many resources, or link cells, say so
     */
    public record Link(String from, String to, Kind kind, String property, int count) {
    }

    /** How sheets hang together. */
    public enum Kind {
        /** Rows of both sheets share identifiers: they describe the same resources. */
        SAME_RESOURCE,
        /** Cells of one sheet point to rows of the other. */
        ROW,
        /** Cells of the sheet point to resources that exist already. */
        INSTANCE
    }
}
