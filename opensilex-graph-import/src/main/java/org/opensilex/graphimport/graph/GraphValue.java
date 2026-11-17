//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.opensilex.graphimport.ontology.literal.LiteralReading;

/**
 * One value a node holds for a property, with the cell it comes from: a literal, read for the
 * property's datatype, or a link.
 *
 * @param cell     where it comes from
 * @param raw      the cell as written
 * @param literal  the cell read as a literal; null for a link
 * @param language the language of the column, from a {@code @fr} header; null without one
 * @param link     what the cell points to; null for a literal
 * @author Arnaud Charleroy
 */
public record GraphValue(CellRef cell, String raw, LiteralReading literal, String language, LinkTarget link) {

    public static GraphValue literal(CellRef cell, String raw, LiteralReading reading, String language) {
        return new GraphValue(cell, raw, reading, language, null);
    }

    public static GraphValue link(CellRef cell, String raw, LinkTarget target) {
        return new GraphValue(cell, raw, null, null, target);
    }

    public boolean isLink() {
        return link != null;
    }

    /** True when the cell gives a value: read as a literal, or pointing to something known. */
    public boolean isUsable() {
        return isLink() ? link.isResolved() : !literal.isRefused();
    }

    /**
     * What two cells giving the same value share, compared once read ({@code 1,5} and {@code 1.5} are
     * one value), so that a value repeated by two sheets counts once. Null for a cell that gives no
     * value.
     */
    public String identity() {
        if (!isUsable()) {
            return null;
        }
        if (isLink()) {
            return "link:" + link.identity();
        }
        return "literal:" + literal.lexical() + "^^" + literal.datatype() + (language == null ? "" : "@" + language);
    }
}
