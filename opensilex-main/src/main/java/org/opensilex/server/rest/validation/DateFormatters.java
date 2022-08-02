/*******************************************************************************
 *                         DateFormatters.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 12/07/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.server.rest.validation;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Collections of DateTimeFormatter objects parse and format dates based on the authorized date formats.
 *
 * See DateFormat for the authorized formats.
 *
 * @author Valentin RIGOLLE
 */
public class DateFormatters {
    private static final Map<DateFormat, DateTimeFormatter> formatters = new HashMap<>();

    /**
     * Creates (if not already existing) and returns the DateTimeFormatter corresponding to the specified DateFormat
     *
     * @param format The DateFormat to create the DateTimeFormatter with
     * @return the DateTimeFormatter corresponding to the specified DateFormat
     */
    public static DateTimeFormatter fromFormat(DateFormat format) {
        if (!formatters.containsKey(format)) {
            formatters.put(format, DateTimeFormatter.ofPattern(format.toString()));
        }
        return formatters.get(format);
    }
}
