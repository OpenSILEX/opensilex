//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.drafts;

import java.net.URI;
import java.util.Map;

/**
 * A facility as a sheet of facilities describes it: what the platform's facility form is opened
 * with. The type and the properties go together, since the form keeps the properties only of a
 * facility whose type it is given.
 *
 * @param type       the type the user chose for the sheet's rows, or {@code null} when none
 * @param properties the value of each property of the type, by the property's URI, for the cells
 *                   that hold one
 * @author Arnaud Charleroy
 */
public record FacilityDraft(String name, URI type, Map<String, String> properties) {
}
