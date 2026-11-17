//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.opensilex.aiimport.report.ReportMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * What is wrong with the description of a sheet. A sheet with a problem is not read: reading it
 * anyway would be guessing, and a guess here files measurements against the wrong objects.
 * <p>
 * The rules come from {@link DescribedRole} — which roles a kind accepts, requires, and allows once
 * — so a role added there is checked here without a line changed.
 *
 * @author Arnaud Charleroy
 */
final class DescriptionRules {

    static final String PREFIX = "AiImport.report.anomaly.custom.";
    static final String MISSING = PREFIX + "missing_";
    static final String SEVERAL = PREFIX + "several_";
    static final String NOT_FOR_KIND = PREFIX + "roleNotForKind";
    static final String NO_TARGET = PREFIX + "noTarget";

    private DescriptionRules() {
    }

    /**
     * @param roles  the role of each column of the sheet, by header
     * @param chosen what the user chose, by header: a choice the kind does not accept is a problem,
     *               where a default it does not accept is simply set aside
     * @param factor the factor the user said a sheet of levels lists the levels of, or {@code null}: it
     *               stands for the column that would name the factor
     */
    static List<ReportMessage> problemsOf(String sheet, SheetKind kind, Map<String, DescribedRole> roles,
                                          Map<String, DescribedRole> chosen, String factor) {
        List<ReportMessage> problems = new ArrayList<>();
        if (!kind.isRead()) {
            return problems;
        }
        chosen.forEach((header, role) -> {
            if (roles.containsKey(header) && !role.isAcceptedBy(kind)) {
                problems.add(ReportMessage.of(NOT_FOR_KIND, "Column '" + header + "' of sheet '" + sheet
                                + "' is described as " + role + ", which a sheet of kind " + kind
                                + " cannot have.")
                        .with("sheet", sheet)
                        .with("column", header)
                        .with("role", role)
                        .with("kind", kind));
            }
        });

        Collection<DescribedRole> described = roles.values();
        for (DescribedRole role : DescribedRole.values()) {
            long count = described.stream().filter(candidate -> candidate == role).count();
            boolean named = role == DescribedRole.FACTOR && factor != null;
            if (role.isRequiredBy(kind) && count == 0 && !named) {
                problems.add(ReportMessage.of(MISSING + role.name(), "Sheet '" + sheet + "' is described as "
                                + kind + " but has no column described as " + role + ".")
                        .with("sheet", sheet)
                        .with("kind", kind));
            }
            if (role.isSinglePerSheet() && count > 1) {
                problems.add(ReportMessage.of(SEVERAL + role.name(), "Sheet '" + sheet + "' has " + count
                                + " columns described as " + role + ", and only one is allowed.")
                        .with("sheet", sheet)
                        .with("count", count));
            }
        }
        if ((kind == SheetKind.DATA || kind == SheetKind.EVENTS) && !described.contains(DescribedRole.OBJECT)
                && !described.contains(DescribedRole.LOCATION)) {
            problems.add(ReportMessage.of(NO_TARGET, "Sheet '" + sheet + "' holds observations or events but no column "
                            + "says what they concern.")
                    .with("sheet", sheet));
        }
        return problems;
    }
}
