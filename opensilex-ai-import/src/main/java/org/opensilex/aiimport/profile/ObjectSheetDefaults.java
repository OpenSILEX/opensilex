//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * What a profile knows of an object sheet before anyone chose anything: which column names the
 * objects, what the columns it recognises become, and the type of the objects when the file says
 * it.
 * <p>
 * Suggestions only. The user sees them as the starting position of each drop-down and changes any
 * of them; the type in particular is left empty whenever the file does not state it, since several
 * hundred objects created under a type nobody chose are not repaired by editing one of them.
 *
 * @param nameColumn    the column naming each object; always written as its name
 * @param targets       column to {@link ObjectTargets target}, for the columns the profile knows
 * @param suggestedType the type the file states for the objects of this sheet, or {@code null}
 * @author Arnaud Charleroy
 */
public record ObjectSheetDefaults(String nameColumn, Map<String, String> targets, URI suggestedType) {

    public static ObjectSheetDefaults none() {
        return new ObjectSheetDefaults(null, Collections.emptyMap(), null);
    }
}
