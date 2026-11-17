//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.opensilex.aiimport.mapping.HeaderRoleDictionary;
import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.HeaderMatcher;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * What a sheet is taken to be when the user has not said: the platform's dictionary of agronomic
 * headers for a column, and the roles of the columns for the sheet. A starting point the user
 * confirms, never a decision: nothing here is written down with the description.
 *
 * @author Arnaud Charleroy
 */
final class DescriptionDefaults {

    /**
     * Headers that name a factor in a sheet of levels. The dictionary does not know them: it reads
     * a treatment, never the factor it belongs to.
     */
    private static final Set<String> FACTOR_HEADERS = new HashSet<>(Arrays.asList("facteur", "factor"));

    private DescriptionDefaults() {
    }

    /**
     * @return the role the header gives away, or else {@link DescribedRole#VARIABLE} for a column
     * holding at least one number, or else {@link DescribedRole#UNREAD}
     */
    static DescribedRole roleOf(SheetStructure sheet, String header) {
        if (FACTOR_HEADERS.contains(HeaderMatcher.normalize(header))) {
            return DescribedRole.FACTOR;
        }
        Optional<DescribedRole> known = DescribedRole.from(HeaderRoleDictionary.roleOf(header));
        if (known.isPresent()) {
            return known.get();
        }
        for (String value : sheet.distinctValues(header)) {
            if (ExcelValueParser.parseNumber(value) != null) {
                return DescribedRole.VARIABLE;
            }
        }
        return DescribedRole.UNREAD;
    }

    /**
     * @param roles the role of each column, whatever the kind would accept
     * @return the kind the roles point at: observations of described objects, observations, levels
     * of a factor, objects, or else nothing to read
     */
    static SheetKind kindOf(Collection<DescribedRole> roles) {
        boolean object = roles.contains(DescribedRole.OBJECT);
        boolean date = roles.contains(DescribedRole.DATE);
        boolean variable = roles.contains(DescribedRole.VARIABLE);
        boolean describesTheObject = roles.contains(DescribedRole.GERMPLASM)
                || roles.contains(DescribedRole.FACTOR_LEVEL)
                || roles.contains(DescribedRole.PARENT_OBJECT)
                || roles.contains(DescribedRole.POSITION)
                || roles.contains(DescribedRole.OBJECT_PROPERTY);
        if (object && date && variable && describesTheObject) {
            return SheetKind.OBJECTS_AND_DATA;
        }
        if (date && variable) {
            return SheetKind.DATA;
        }
        if (roles.contains(DescribedRole.FACTOR) && roles.contains(DescribedRole.FACTOR_LEVEL)) {
            return SheetKind.FACTORS;
        }
        if (object) {
            return SheetKind.OBJECTS;
        }
        // A sheet that names experiments and nothing they hold is the description of the experiments.
        if (roles.contains(DescribedRole.TRIAL) && !date && !variable) {
            return SheetKind.EXPERIMENTS;
        }
        return SheetKind.IGNORED;
    }
}
