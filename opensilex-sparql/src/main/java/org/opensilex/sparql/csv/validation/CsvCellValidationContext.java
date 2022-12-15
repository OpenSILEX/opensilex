/*******************************************************************************
 *                         CsvCellValidationContext.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.csv.validation;

import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.owl.ValidationContext;

/**
 * Handle validation error related to a CSV import.
 * It uses {@link CSVCell} as validation error context.
 *
 * @author rcolin
 */
public class CsvCellValidationContext extends CSVCell implements ValidationContext {

    public CsvCellValidationContext(int rowIndex, int colIndex, String value, String header) {
        super(rowIndex, colIndex, value, header);
    }

    public CsvCellValidationContext(){

    }

    @Override
    public String getProperty() {
        return getHeader();
    }

    @Override
    public void setProperty(String property) {
        setHeader(property);
    }

}
