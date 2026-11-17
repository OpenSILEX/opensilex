//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.DescribedSheet;
import org.opensilex.aiimport.profile.custom.FileDescription;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.aiimport.template.FileTemplateMatcher;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * What the user said of their files and what is made of it: the experiment, each sheet as it is
 * read, and the kinds and roles they can choose from — served here so the screen lists whatever
 * the module offers.
 *
 * @author Arnaud Charleroy
 */
public class FileDescriptionDTO {

    private String experiment;

    @JsonProperty("experiment_name")
    private String experimentName;

    @JsonProperty("available_kinds")
    private List<ChoiceDTO> availableKinds = new ArrayList<>();

    @JsonProperty("available_roles")
    private List<ChoiceDTO> availableRoles = new ArrayList<>();

    private List<SheetDescriptionDTO> sheets = new ArrayList<>();

    public static FileDescriptionDTO fromModel(AiImportSession session) {
        FileDescription description = session.getDescription();
        FileDescriptionDTO dto = new FileDescriptionDTO();
        dto.experiment = description.getExperimentUri();
        dto.experimentName = description.getExperimentName();
        for (SheetKind kind : SheetKind.values()) {
            dto.availableKinds.add(ChoiceDTO.of(kind.name(), kind.labelKey()));
        }
        for (DescribedRole role : DescribedRole.values()) {
            dto.availableRoles.add(ChoiceDTO.of(role.name(), role.labelKey())
                    .setSinglePerSheet(role.isSinglePerSheet())
                    .setAcceptedBy(Arrays.stream(SheetKind.values())
                            .filter(kind -> role.isAcceptedBy(kind))
                            .map(SheetKind::name)
                            .collect(Collectors.toList())));
        }
        if (session.getWorkbook() != null) {
            for (SheetStructure sheet : session.getWorkbook().getSheets()) {
                if (sheet.isTabular()) {
                    dto.sheets.add(SheetDescriptionDTO.fromModel(
                            DescribedSheet.of(sheet, description.find(sheet.getName()).orElse(null))));
                }
            }
        }
        return dto;
    }

    /**
     * Proposes to each sheet the user has not described what the best template for it says.
     * Nothing is applied: a proposal is only shown.
     *
     * @param templates the templates the user may use; the one the conversation keeps up to date is
     *                  left out, since it would only propose back what the user said
     */
    public FileDescriptionDTO propose(AiImportSession session, List<FileTemplate> templates) {
        List<FileTemplate> others = templates.stream()
                .filter(template -> !template.getId().equals(session.getTemplateId())).collect(Collectors.toList());
        for (SheetDescriptionDTO sheet : sheets) {
            if (sheet.isKindChosen() || !sheet.getRolesChosen().isEmpty() || sheet.getRdfType() != null) {
                continue;
            }
            session.getWorkbook().getSheet(sheet.getSheet()).flatMap(data -> FileTemplateMatcher.best(data, others))
                    .ifPresent(match -> sheet.setSuggestion(SheetSuggestionDTO.fromModel(match)));
        }
        return this;
    }

    @ApiModelProperty(value = "The experiment the user chose for the file, when no column names it")
    public String getExperiment() {
        return experiment;
    }

    @ApiModelProperty(value = "Its name, as the file is matched against")
    public String getExperimentName() {
        return experimentName;
    }

    @ApiModelProperty(value = "The kinds of sheet the user can choose from")
    public List<ChoiceDTO> getAvailableKinds() {
        return availableKinds;
    }

    @ApiModelProperty(value = "The roles a column can be described as, with the kinds of sheet that accept each")
    public List<ChoiceDTO> getAvailableRoles() {
        return availableRoles;
    }

    public List<SheetDescriptionDTO> getSheets() {
        return sheets;
    }
}
