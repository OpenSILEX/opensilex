//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.store;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * A file of a stored conversation: the name the user gave it, and the identifier its stored copy
 * is kept under.
 *
 * @author Arnaud Charleroy
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class SavedFile {

    private String id;
    private String name;

    public SavedFile() {
    }

    public SavedFile(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
