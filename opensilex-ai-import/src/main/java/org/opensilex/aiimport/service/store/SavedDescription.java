//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.store;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.FileDescription;
import org.opensilex.aiimport.profile.custom.SheetDescription;
import org.opensilex.aiimport.profile.custom.SheetKind;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the user said of the files of a conversation, as it is kept: the experiment and, per sheet,
 * its kind, the role of each column they chose, its type and the property of each column.
 * <p>
 * Kinds and roles are kept by name. One a later version no longer knows is dropped on resumption —
 * the default then applies — rather than failing the whole conversation.
 *
 * @author Arnaud Charleroy
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class SavedDescription {

    @JsonProperty("experiment_uri")
    private String experimentUri;

    @JsonProperty("experiment_name")
    private String experimentName;

    private List<SavedSheet> sheets = new ArrayList<>();

    static SavedDescription of(FileDescription description) {
        SavedDescription saved = new SavedDescription();
        saved.experimentUri = description.getExperimentUri();
        saved.experimentName = description.getExperimentName();
        for (SheetDescription sheet : description.getSheets().values()) {
            saved.sheets.add(SavedSheet.of(sheet));
        }
        return saved;
    }

    /**
     * Puts what was kept back into the description of a session.
     */
    void restoreInto(FileDescription description) {
        description.setExperiment(experimentUri, experimentName);
        for (SavedSheet saved : sheets) {
            saved.restoreInto(description.sheet(saved.sheet));
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    static class SavedSheet {

        private String sheet;
        private String kind;
        private Map<String, String> roles = new LinkedHashMap<>();
        private String type;
        private Map<String, String> properties = new LinkedHashMap<>();
        private String factor;

        static SavedSheet of(SheetDescription description) {
            SavedSheet saved = new SavedSheet();
            saved.sheet = description.getSheet();
            saved.kind = description.getKind() == null ? null : description.getKind().name();
            description.getRoles().forEach((column, role) -> saved.roles.put(column, role.name()));
            saved.type = description.getType() == null ? null : description.getType().toString();
            saved.properties.putAll(description.getProperties());
            saved.factor = description.getFactor();
            return saved;
        }

        void restoreInto(SheetDescription description) {
            SheetKind.parse(kind).ifPresent(description::setKind);
            if (roles != null) {
                roles.forEach((column, role) ->
                        DescribedRole.parse(role).ifPresent(parsed -> description.getRoles().put(column, parsed)));
            }
            if (type != null && !type.isBlank()) {
                description.setType(URI.create(type));
            }
            if (properties != null) {
                description.getProperties().putAll(properties);
            }
            description.setFactor(factor);
        }
    }
}
