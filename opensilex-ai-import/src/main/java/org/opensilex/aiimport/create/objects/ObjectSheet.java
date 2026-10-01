//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.objects;

import org.opensilex.aiimport.profile.ObjectRow;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * One object sheet as the next creation would read it: its rows, the type of its objects, and what
 * each of its columns becomes — the user's choices where there are some, the profile's suggestions
 * everywhere else.
 *
 * @author Arnaud Charleroy
 */
public class ObjectSheet {

    private final String name;
    private final String nameColumn;
    private final List<String> headers;
    private final List<ObjectRow> rows;
    private final URI type;
    private final boolean typeFromFile;
    private final boolean included;
    private final Map<String, String> mapping;

    ObjectSheet(String name, String nameColumn, List<String> headers, List<ObjectRow> rows, URI type,
                boolean typeFromFile, boolean included, Map<String, String> mapping) {
        this.name = name;
        this.nameColumn = nameColumn;
        this.headers = Collections.unmodifiableList(headers);
        this.rows = Collections.unmodifiableList(rows);
        this.type = type;
        this.typeFromFile = typeFromFile;
        this.included = included;
        this.mapping = Collections.unmodifiableMap(mapping);
    }

    public String getName() {
        return name;
    }

    /**
     * @return the column naming the objects, or {@code null} when the profile does not say
     */
    public String getNameColumn() {
        return nameColumn;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<ObjectRow> getRows() {
        return rows;
    }

    /**
     * @return the type chosen, or the one the file states; {@code null} when neither says
     */
    public URI getType() {
        return type;
    }

    /**
     * @return whether the type is the file's own, not yet confirmed by anyone
     */
    public boolean isTypeFromFile() {
        return typeFromFile;
    }

    public boolean isIncluded() {
        return included;
    }

    /**
     * @return every column, with what it becomes; an empty target for a column not written
     */
    public Map<String, String> getMapping() {
        return mapping;
    }

    /**
     * @return the columns mapped to a target, in the order of the sheet
     */
    public List<String> columnsMappedTo(String target) {
        List<String> columns = new ArrayList<>();
        mapping.forEach((column, mapped) -> {
            if (target.equals(mapped)) {
                columns.add(column);
            }
        });
        return columns;
    }

    public boolean maps(String target) {
        return mapping.containsValue(target);
    }
}
