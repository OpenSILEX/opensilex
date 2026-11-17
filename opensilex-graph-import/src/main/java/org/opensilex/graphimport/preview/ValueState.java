//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

/**
 * What an import would do with a value, once compared with what the resource already holds.
 *
 * @author Arnaud Charleroy
 */
public enum ValueState {
    /** The resource does not hold it yet: it would be added. */
    NEW,
    /** The resource already holds it. */
    PRESENT,
    /** The property takes fewer values than the resource and the file give together: one would be replaced. */
    CONFLICT,
    /**
     * A link whose name is both a row of the file and an existing resource that row is not: which one is
     * meant is unclear.
     */
    AMBIGUOUS,
    /** The cell gives no value: refused, or a link nothing answers to. */
    UNUSABLE
}
