//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

/**
 * What the grid of the preview shows on a cell.
 *
 * @author Arnaud Charleroy
 */
public enum CellState {
    /** A value the resource would gain. */
    NEW,
    /** A value the resource holds already. */
    PRESENT,
    /** A value that would replace one the resource holds. */
    CONFLICT,
    /** A link that may mean a row of the file or an existing resource. */
    AMBIGUOUS,
    /** A cell the file must correct: it gives no value, or a problem stops the import on it. */
    ERROR,
    /** The cell that identifies its row. */
    IDENTIFIER,
    /** The cell that gives the class of its row. */
    TYPE,
    /** A cell of a column the graph leaves out. */
    IGNORED
}
