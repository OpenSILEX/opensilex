//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.objects;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the user decided for one object sheet: the type of its objects, whether it is part of the
 * next creation, and what some of its columns become.
 * <p>
 * Only what the user changed is kept here; everything else stays what the profile suggests, so a
 * profile that learns a new column improves a stored conversation too. Kept in the session and in
 * its stored copy, like the confirmed names: resuming a conversation brings the choices back.
 *
 * @author Arnaud Charleroy
 */
public class ObjectSheetPlan {

    private final String sheet;
    private URI type;
    private boolean included = true;

    /**
     * Column to target, for the columns the user mapped — an empty target meaning "not written".
     */
    private final Map<String, String> mapping = new LinkedHashMap<>();

    public ObjectSheetPlan(String sheet) {
        this.sheet = sheet;
    }

    public String getSheet() {
        return sheet;
    }

    public URI getType() {
        return type;
    }

    public ObjectSheetPlan setType(URI type) {
        this.type = type;
        return this;
    }

    public boolean isIncluded() {
        return included;
    }

    public ObjectSheetPlan setIncluded(boolean included) {
        this.included = included;
        return this;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

    public ObjectSheetPlan map(String column, String target) {
        mapping.put(column, target == null ? "" : target);
        return this;
    }
}
