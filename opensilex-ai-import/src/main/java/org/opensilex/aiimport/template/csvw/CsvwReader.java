//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template.csvw;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.aiimport.template.TemplateSheet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads back a template written by {@link CsvwWriter}.
 * <p>
 * It understands this module's own documents and refuses any other, saying why: a CSVW document
 * of another tool describes columns, not what a sheet is, and guessing what it meant would be
 * worse than saying it cannot be read. Annotations it does not know are ignored, as CSVW asks.
 *
 * @author Arnaud Charleroy
 */
public final class CsvwReader {

    private final ObjectMapper mapper;

    public CsvwReader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * @param json the document, as text
     * @return the template it describes, before it belongs to anyone
     * @throws CsvwException when the document is not one this module can read
     */
    public FileTemplate read(String json) throws CsvwException {
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (IOException e) {
            throw new CsvwException("The document is not valid JSON.", e);
        }
        if (root == null || !root.isObject() || !"TableGroup".equals(root.path("@type").asText())) {
            throw new CsvwException("The document is not a CSVW group of tables.");
        }
        JsonNode tables = root.path("tables");
        if (!tables.isArray() || tables.isEmpty()) {
            throw new CsvwException("The document describes no table.");
        }

        List<TemplateSheet> sheets = new ArrayList<>();
        for (JsonNode table : tables) {
            sheets.add(sheet(table));
        }
        return FileTemplate.fromSheets(root.path(CsvwVocabulary.TITLE).asText(""), sheets);
    }

    private TemplateSheet sheet(JsonNode table) throws CsvwException {
        String url = table.path("url").asText("");
        if (url.isBlank()) {
            throw new CsvwException("A table has no url.");
        }
        SheetKind kind = SheetKind.parse(table.path(CsvwVocabulary.KIND).asText(null)).orElseThrow(() ->
                new CsvwException("The table '" + url + "' does not say what its rows are: this document was "
                        + "not written by this module."));
        JsonNode columns = table.path("tableSchema").path("columns");
        if (!columns.isArray()) {
            throw new CsvwException("The table '" + url + "' has no columns.");
        }

        List<String> headers = new ArrayList<>();
        Map<String, DescribedRole> roles = new LinkedHashMap<>();
        Map<String, String> properties = new LinkedHashMap<>();
        for (JsonNode column : columns) {
            String header = column.path("titles").asText(column.path("name").asText(""));
            if (header.isBlank()) {
                throw new CsvwException("A column of the table '" + url + "' has neither a title nor a name.");
            }
            headers.add(header);
            DescribedRole.parse(column.path(CsvwVocabulary.ROLE).asText(null))
                    .ifPresent(role -> roles.put(header, role));
            String property = column.path(CsvwVocabulary.PROPERTY).asText("");
            if (!property.isBlank()) {
                properties.put(header, property);
            }
        }
        return new TemplateSheet(url.endsWith(".csv") ? url.substring(0, url.length() - 4) : url, headers, kind,
                roles, type(table, url), properties);
    }

    private static URI type(JsonNode table, String url) throws CsvwException {
        String type = table.path(CsvwVocabulary.TYPE).asText("");
        if (type.isBlank()) {
            return null;
        }
        try {
            return new URI(type);
        } catch (URISyntaxException e) {
            throw new CsvwException("The type of the table '" + url + "' is not a URI.", e);
        }
    }
}
