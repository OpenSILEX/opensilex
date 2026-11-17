//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology.literal;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The order of day and month in the dates of a column written {@code 03/04/2026}, which may be the
 * 3rd of April or the 4th of March. A column decides it, not a cell: one cell with a day above 12
 * settles the whole column.
 *
 * @author Arnaud Charleroy
 */
public enum DateOrder {
    /** {@code dd/MM/yyyy}, the French way. */
    DAY_FIRST,
    /** {@code MM/dd/yyyy}, the American way. */
    MONTH_FIRST,
    /** Nothing settles it: a date whose two numbers are both 12 or less is refused as ambiguous. */
    UNDECIDED;

    static final Pattern NUMERIC_DATE = Pattern.compile("^(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{4})(?:[ T].*)?$");

    private static final int MONTHS = 12;

    /**
     * The order the values of a column show: day first when one of them can only be read day first,
     * month first likewise, undecided when none can, or when the column shows both.
     */
    public static DateOrder detect(Collection<String> values) {
        boolean dayFirst = false;
        boolean monthFirst = false;
        for (String value : values) {
            Matcher matcher = value == null ? null : NUMERIC_DATE.matcher(value.trim());
            if (matcher != null && matcher.matches()) {
                int first = Integer.parseInt(matcher.group(1));
                int second = Integer.parseInt(matcher.group(2));
                dayFirst |= first > MONTHS && second <= MONTHS;
                monthFirst |= second > MONTHS && first <= MONTHS;
            }
        }
        if (dayFirst == monthFirst) {
            return UNDECIDED;
        }
        return dayFirst ? DAY_FIRST : MONTH_FIRST;
    }
}
