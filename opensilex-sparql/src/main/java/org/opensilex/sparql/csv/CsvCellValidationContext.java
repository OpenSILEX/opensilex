/*******************************************************************************
 *                         CsvCellValidationContext.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.csv;

import org.opensilex.sparql.owl.ValidationContext;

public class CsvCellValidationContext implements ValidationContext {

    private final CSVCell csvCell;

    public CsvCellValidationContext(CSVCell csvCell) {
        this.csvCell = csvCell;
    }

    public CSVCell getCsvCell() {
        return csvCell;
    }

    @Override
    public String getProperty() {
        return csvCell.getHeader();
    }

    @Override
    public void setProperty(String property) {
        csvCell.setHeader(property);
    }

    @Override
    public String getValue() {
        return csvCell.getValue();
    }

    @Override
    public void setValue(String value) {
        csvCell.setValue(value);
    }
}
