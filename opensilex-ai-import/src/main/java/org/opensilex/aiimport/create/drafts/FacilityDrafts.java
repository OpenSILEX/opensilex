//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.drafts;

import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.DescribedSheet;
import org.opensilex.aiimport.profile.custom.SheetDescription;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The facilities the sheets of facilities describe, row by row. Which property a column is written
 * to is what the user chose in the description, from the properties the ontology gives the type:
 * none is named here.
 *
 * @author Arnaud Charleroy
 */
public final class FacilityDrafts {

    private FacilityDrafts() {
    }

    /**
     * @return a draft per named row of every readable sheet of facilities, in the order of the files
     */
    public static List<FacilityDraft> of(AiImportSession session) {
        List<FacilityDraft> drafts = new ArrayList<>();
        if (session.getWorkbook() == null) {
            return drafts;
        }
        for (SheetStructure sheet : session.getWorkbook().getSheets()) {
            SheetDescription description = session.getDescription().find(sheet.getName()).orElse(null);
            DescribedSheet described = DescribedSheet.of(sheet, description);
            if (!sheet.isTabular() || described.kind() != SheetKind.FACILITIES || !described.isReadable()) {
                continue;
            }
            String nameColumn = described.column(DescribedRole.LOCATION);
            for (List<String> row : sheet.getRows()) {
                String name = sheet.cell(row, nameColumn).trim();
                if (!name.isEmpty()) {
                    drafts.add(new FacilityDraft(name, description == null ? null : description.getType(),
                            properties(sheet, row, described, description)));
                }
            }
        }
        return drafts;
    }

    private static Map<String, String> properties(SheetStructure sheet, List<String> row, DescribedSheet described,
                                                  SheetDescription description) {
        Map<String, String> properties = new LinkedHashMap<>();
        if (description == null) {
            return properties;
        }
        for (String column : described.columns(DescribedRole.OBJECT_PROPERTY)) {
            String property = description.getProperties().get(column);
            String value = sheet.cell(row, column).trim();
            if (property != null && !property.isBlank() && !value.isEmpty()) {
                properties.put(property, value);
            }
        }
        return properties;
    }
}
