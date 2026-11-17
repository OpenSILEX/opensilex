//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A sheet as the description reads it: its kind, the role of each of its columns, and what is
 * wrong with that. Computed from what the user said and the defaults, in that order of
 * precedence, and never stored.
 *
 * @author Arnaud Charleroy
 */
public final class DescribedSheet {

    private final SheetStructure sheet;
    private final SheetDescription description;
    private final SheetKind kind;
    private final Map<String, DescribedRole> roles;
    private final List<ReportMessage> problems;

    private DescribedSheet(SheetStructure sheet, SheetDescription description, SheetKind kind,
                           Map<String, DescribedRole> roles, List<ReportMessage> problems) {
        this.sheet = sheet;
        this.description = description;
        this.kind = kind;
        this.roles = roles;
        this.problems = problems;
    }

    /**
     * @param description what the user said of the sheet, or {@code null} when nothing
     */
    public static DescribedSheet of(SheetStructure sheet, SheetDescription description) {
        Map<String, DescribedRole> chosen = description == null ? Collections.emptyMap() : description.getRoles();
        if (!sheet.isTabular()) {
            return new DescribedSheet(sheet, description, SheetKind.IGNORED, new LinkedHashMap<>(),
                    new ArrayList<>());
        }

        // The defaults first, whatever the kind will accept: the kind a default points at is
        // worked out from them. The user's choices replace them where they were made.
        Map<String, DescribedRole> asked = new LinkedHashMap<>();
        for (String header : sheet.getHeaders()) {
            if (header != null && !header.isEmpty()) {
                asked.put(header, chosen.containsKey(header) ? chosen.get(header)
                        : DescriptionDefaults.roleOf(sheet, header));
            }
        }
        SheetKind kind = description != null && description.getKind() != null
                ? description.getKind()
                : DescriptionDefaults.kindOf(asked.values());

        // A default the kind does not accept is set aside; a choice of the user's is kept and
        // reported, since replacing it would be deciding for them.
        Map<String, DescribedRole> roles = new LinkedHashMap<>();
        asked.forEach((header, role) -> roles.put(header,
                chosen.containsKey(header) || role.isAcceptedBy(kind) ? role : DescribedRole.UNREAD));

        List<ReportMessage> problems = DescriptionRules.problemsOf(sheet.getName(), kind, roles, chosen,
                description == null ? null : description.getFactor());
        return new DescribedSheet(sheet, description, kind, roles, problems);
    }

    public SheetStructure sheet() {
        return sheet;
    }

    public String name() {
        return sheet.getName();
    }

    public SheetKind kind() {
        return kind;
    }

    public SheetDescription description() {
        return description;
    }

    public List<ReportMessage> problems() {
        return problems;
    }

    /**
     * @return whether the sheet is read: described as something, and without a problem
     */
    public boolean isReadable() {
        return kind.isRead() && problems.isEmpty();
    }

    public Map<String, DescribedRole> roles() {
        return roles;
    }

    public DescribedRole roleOf(String header) {
        return roles.getOrDefault(header, DescribedRole.UNREAD);
    }

    /**
     * @return the headers described so, in the order of the sheet
     */
    public List<String> columns(DescribedRole role) {
        List<String> columns = new ArrayList<>();
        roles.forEach((header, described) -> {
            if (described == role) {
                columns.add(header);
            }
        });
        return columns;
    }

    /**
     * @return the first header described so, or {@code null}
     */
    public String column(DescribedRole role) {
        List<String> columns = columns(role);
        return columns.isEmpty() ? null : columns.get(0);
    }
}
