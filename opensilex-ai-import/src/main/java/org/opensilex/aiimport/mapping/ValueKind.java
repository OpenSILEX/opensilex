//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.mapping;

import org.apache.jena.datatypes.xsd.XSDDatatype;

/**
 * The kind of value a column actually holds, read from the cells rather than declared.
 * <p>
 * Compared against the datatype of the matching variable, this is what turns "the import failed"
 * into "column X holds decimals but the variable expects an integer".
 *
 * @author Arnaud Charleroy
 */
public enum ValueKind {

    INTEGER(XSDDatatype.XSDinteger),
    DECIMAL(XSDDatatype.XSDdecimal),
    DATE(XSDDatatype.XSDdate),
    BOOLEAN(XSDDatatype.XSDboolean),
    TEXT(XSDDatatype.XSDstring),

    /**
     * Several kinds in one column, which no single datatype can accept.
     */
    MIXED(null),

    /**
     * Every cell is empty or marked as missing.
     */
    EMPTY(null);

    private final XSDDatatype datatype;

    ValueKind(XSDDatatype datatype) {
        this.datatype = datatype;
    }

    /**
     * @return the datatype this kind would need, or {@code null} when no single datatype fits
     */
    public String getDatatypeUri() {
        return datatype == null ? null : datatype.getURI();
    }

    public boolean isNumeric() {
        return this == INTEGER || this == DECIMAL;
    }
}
