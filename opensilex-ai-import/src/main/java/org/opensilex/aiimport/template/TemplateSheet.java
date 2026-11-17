//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.SheetKind;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a template says of one sheet: its headers as the file had them, what its rows are, what each
 * column is, and — for objects and facilities — their type and the property each column is written
 * to.
 * <p>
 * Kinds and roles are kept by name, so that a template written by a later version that knows a role
 * this one does not still reads: the role is dropped rather than the template refused.
 *
 * @author Arnaud Charleroy
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class TemplateSheet {

    private String name;
    private List<String> headers = new ArrayList<>();
    private String kind;
    private Map<String, String> roles = new LinkedHashMap<>();
    private URI type;
    private Map<String, String> properties = new LinkedHashMap<>();

    public TemplateSheet() {
    }

    public TemplateSheet(String name, List<String> headers, SheetKind kind, Map<String, DescribedRole> roles,
                         URI type, Map<String, String> properties) {
        this.name = name;
        this.headers = new ArrayList<>(headers);
        this.kind = kind.name();
        roles.forEach((header, role) -> this.roles.put(header, role.name()));
        this.type = type;
        this.properties = new LinkedHashMap<>(properties);
    }

    public String getName() {
        return name;
    }

    public List<String> getHeaders() {
        return headers;
    }

    /**
     * @return what the rows are, or empty when this version does not know the kind
     */
    public Optional<SheetKind> kind() {
        return SheetKind.parse(kind);
    }

    /**
     * @return the role of each column it describes, by header; a role this version does not know is left out
     */
    public Map<String, DescribedRole> roles() {
        Map<String, DescribedRole> known = new LinkedHashMap<>();
        roles.forEach((header, role) -> DescribedRole.parse(role).ifPresent(parsed -> known.put(header, parsed)));
        return known;
    }

    public URI getType() {
        return type;
    }

    /**
     * @return the property each column is written to, by header, as the URI the ontology gives it
     */
    public Map<String, String> getProperties() {
        return properties;
    }
}
