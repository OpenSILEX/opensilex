//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template.csvw;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.RoleOntology;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.aiimport.template.TemplateSheet;

import java.util.Map;
import java.util.Optional;

/**
 * Writes a template as CSVW, the W3C's way of saying what the columns of a CSV file are: one table
 * per sheet, each column with its name and the ontology property its role stands for, the column
 * that names the objects as the table's primary key.
 * <p>
 * What CSVW has no word for — the kind of a sheet, the role of a column, the type of the rows and
 * the property a column is written to — is carried as annotations under IRIs of the module, which
 * a reader of another tool ignores and this module's {@link CsvwReader} reads back.
 *
 * @author Arnaud Charleroy
 */
public final class CsvwWriter {

    private final ObjectMapper mapper;

    public CsvwWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ObjectNode write(FileTemplate template) {
        ObjectNode root = mapper.createObjectNode();
        root.put("@context", CsvwVocabulary.CONTEXT);
        root.put("@type", "TableGroup");
        root.put(CsvwVocabulary.TITLE, template.getName());
        ArrayNode tables = root.putArray("tables");
        for (TemplateSheet sheet : template.getSheets()) {
            tables.add(table(sheet));
        }
        return root;
    }

    private ObjectNode table(TemplateSheet sheet) {
        ObjectNode table = mapper.createObjectNode();
        // The file the sheet came from is not kept: the url is the name a CSV of this sheet would have.
        table.put("url", sheet.getName() + ".csv");
        sheet.kind().ifPresent(kind -> table.put(CsvwVocabulary.KIND, kind.name()));
        if (sheet.getType() != null) {
            table.put(CsvwVocabulary.TYPE, sheet.getType().toString());
        }
        ObjectNode schema = table.putObject("tableSchema");
        ArrayNode columns = schema.putArray("columns");
        Map<String, DescribedRole> roles = sheet.roles();
        for (String header : sheet.getHeaders()) {
            ObjectNode column = columns.addObject();
            column.put("name", name(header));
            column.put("titles", header);
            DescribedRole role = roles.get(header);
            if (role != null) {
                column.put(CsvwVocabulary.ROLE, role.name());
                Optional<String> predicate = RoleOntology.predicate(role);
                predicate.ifPresent(uri -> column.put("propertyUrl", uri));
            }
            String property = sheet.getProperties().get(header);
            if (property != null && !property.isBlank()) {
                column.put(CsvwVocabulary.PROPERTY, property);
                // A property the user matched a column to is what the column means.
                column.put("propertyUrl", property);
            }
        }
        roles.entrySet().stream().filter(entry -> entry.getValue() == DescribedRole.OBJECT
                && sheet.kind().map(kind -> kind != SheetKind.IGNORED).orElse(false))
                .findFirst().ifPresent(entry -> schema.put("primaryKey", name(entry.getKey())));
        return table;
    }

    /**
     * A column's name is a token CSVW can use as an identifier; its title keeps the header as it was.
     */
    static String name(String header) {
        String token = header.trim().replaceAll("[^\\p{L}\\p{N}_]+", "_");
        return token.isEmpty() ? "column" : token;
    }
}
