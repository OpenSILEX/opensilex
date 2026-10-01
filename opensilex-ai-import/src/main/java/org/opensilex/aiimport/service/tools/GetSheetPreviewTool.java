//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Returns more rows of one sheet of the uploaded file.
 * <p>
 * The prompt only carries a few rows per sheet, on purpose. This tool lets the assistant look
 * further when a question actually needs it, instead of the whole file being sent every time.
 *
 * @author Arnaud Charleroy
 */
public class GetSheetPreviewTool implements AiTool {

    public static final String NAME = "get_sheet_preview";

    private static final int DEFAULT_ROWS = 10;
    private static final int MAX_ROWS = 50;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Read more rows of one sheet of the uploaded file. Use it when answering a question "
                + "needs data beyond the sample already provided.";
    }

    @Override
    public ObjectNode getParametersSchema(ObjectMapper mapper) {
        ObjectNode schema = ToolSchemas.object(mapper);
        ToolSchemas.string(schema, "sheet", "Exact name of the sheet to read", true);
        ToolSchemas.integer(schema, "rows", "Number of rows to return, at most " + MAX_ROWS, false);
        return schema;
    }

    @Override
    public Object execute(JsonNode arguments, ToolContext context) {
        String sheetName = ToolSchemas.text(arguments, "sheet");
        if (sheetName == null) {
            return ToolSchemas.error("The 'sheet' argument is required.");
        }
        int rows = ToolSchemas.number(arguments, "rows", DEFAULT_ROWS, MAX_ROWS);

        if (context.getWorkbook() == null) {
            return ToolSchemas.error("The file has not been read yet.");
        }
        Optional<SheetStructure> found = context.getWorkbook().getSheet(sheetName);
        if (!found.isPresent()) {
            Map<String, Object> error = ToolSchemas.error(
                    "No sheet is named '" + sheetName + "' in the uploaded file.");
            error.put("available_sheets", context.getWorkbook().getSheetNames());
            return error;
        }
        SheetStructure sheet = found.get();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sheet", sheet.getName());
        response.put("total_data_rows", sheet.getDataRowCount());
        if (!sheet.isTabular()) {
            response.put("kind", "text");
            response.put("text", sheet.getText());
            return response;
        }
        response.put("kind", "table");
        response.put("headers", sheet.getHeaders());
        response.put("rows", sheet.sample(rows));
        return response;
    }
}
