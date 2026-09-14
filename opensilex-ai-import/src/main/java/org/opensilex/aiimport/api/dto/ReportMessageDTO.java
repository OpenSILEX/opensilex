//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.report.ReportMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A sentence of the report, as a translation key with its parameters, plus the English text.
 * <p>
 * Both travel. The interface renders the key so a French user reads French; the English is what the
 * language model was given, and it is also what the interface falls back to when a profile wrote a
 * sentence no key covers.
 *
 * @author Arnaud Charleroy
 */
public class ReportMessageDTO {

    private String key;

    private Map<String, String> params = new LinkedHashMap<>();

    /**
     * The sentence in English, which is what the assistant was told.
     */
    private String text;

    public static ReportMessageDTO fromModel(ReportMessage model) {
        if (model == null) {
            return null;
        }
        ReportMessageDTO dto = new ReportMessageDTO();
        dto.key = model.getKey();
        dto.params = model.getParams();
        dto.text = model.getEnglish();
        return dto;
    }

    public static List<ReportMessageDTO> fromModels(List<ReportMessage> models) {
        List<ReportMessageDTO> dtos = new ArrayList<>();
        if (models != null) {
            models.forEach(model -> dtos.add(fromModel(model)));
        }
        return dtos;
    }

    @ApiModelProperty(value = "Translation key, absent when the sentence has none",
            example = "AiImport.report.hint.experimentMissing")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @ApiModelProperty(value = "Values to interpolate into the translated sentence")
    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    @ApiModelProperty(value = "The sentence in English, and the fallback when there is no key")
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
