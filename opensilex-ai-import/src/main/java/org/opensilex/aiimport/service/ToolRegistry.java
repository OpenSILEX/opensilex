//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensilex.aiimport.service.dto.ToolDefinition;
import org.opensilex.aiimport.service.tools.AiTool;
import org.opensilex.aiimport.service.tools.GetColumnMappingTool;
import org.opensilex.aiimport.service.tools.GetCreationFieldsTool;
import org.opensilex.aiimport.service.tools.GetReportTool;
import org.opensilex.aiimport.service.tools.GetSheetPreviewTool;
import org.opensilex.aiimport.service.tools.ProposeCreationTool;
import org.opensilex.aiimport.service.tools.SearchExperimentsTool;
import org.opensilex.aiimport.service.tools.SearchGermplasmTool;
import org.opensilex.aiimport.service.tools.SearchProjectsTool;
import org.opensilex.aiimport.service.tools.SearchSharedResourceVariablesTool;
import org.opensilex.aiimport.service.tools.SearchVariablesTool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The tools offered to the assistant, and their declarations.
 *
 * @author Arnaud Charleroy
 */
public class ToolRegistry {

    private final Map<String, AiTool> tools = new LinkedHashMap<>();
    private final List<ToolDefinition> definitions;

    public ToolRegistry(ObjectMapper mapper) {
        List<AiTool> available = Arrays.asList(
                new SearchVariablesTool(),
                new SearchSharedResourceVariablesTool(),
                new SearchExperimentsTool(),
                new SearchProjectsTool(),
                new SearchGermplasmTool(),
                new GetReportTool(),
                new GetColumnMappingTool(),
                new GetSheetPreviewTool(),
                new GetCreationFieldsTool(),
                new ProposeCreationTool());

        List<ToolDefinition> declared = new ArrayList<>(available.size());
        for (AiTool tool : available) {
            tools.put(tool.getName(), tool);
            declared.add(ToolDefinition.of(tool.getName(), tool.getDescription(),
                    tool.getParametersSchema(mapper)));
        }
        this.definitions = Collections.unmodifiableList(declared);
    }

    public List<ToolDefinition> getDefinitions() {
        return definitions;
    }

    public Optional<AiTool> get(String name) {
        return Optional.ofNullable(name == null ? null : tools.get(name));
    }
}
