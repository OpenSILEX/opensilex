//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Small workbooks built by hand, shaped like the files a user brings: no template, headers a
 * person would write. Nothing is read from disk, so no binary is added to the repository.
 *
 * @author Arnaud Charleroy
 */
public final class CustomWorkbooks {

    private CustomWorkbooks() {
    }

    public static SheetStructure sheet(String name, List<String> headers, List<String>... rows) {
        return new SheetStructure().setName(name).setHeaders(headers).setRows(new ArrayList<>(Arrays.asList(rows)));
    }

    public static List<String> row(String... cells) {
        return Arrays.asList(cells);
    }

    public static WorkbookStructure workbook(SheetStructure... sheets) {
        return new WorkbookStructure().setFileName("file.xlsx").setSheets(new ArrayList<>(Arrays.asList(sheets)));
    }

    /**
     * One sheet that is at once the plan, the plant material and the observations: a row per
     * observation, the objects being the distinct values of the first column.
     */
    @SuppressWarnings("unchecked")
    public static SheetStructure flatSheet() {
        return sheet("essai",
                Arrays.asList("plot_id", "variete", "traitement", "bloc", "date", "hauteur_cm", "nb_grappes", "remarque"),
                row("P01", "Syrah", "T1", "B1", "2026-06-12", "84", "12", ""),
                row("P02", "Grenache", "T2", "B1", "2026-06-12", "79", "9", "bord"),
                row("P01", "Syrah", "T1", "B1", "2026-07-03", "131", "14", ""),
                row("P02", "Grenache", "T2", "B1", "2026-07-03", "120", "NA", ""));
    }

    public static WorkbookStructure flat() {
        return workbook(flatSheet());
    }

    /**
     * The plan of a trial in one sheet and the plant material of the same plots in another.
     */
    @SuppressWarnings("unchecked")
    public static WorkbookStructure planAndMaterial() {
        return workbook(
                sheet("plan", Arrays.asList("plot_id", "bloc", "traitement"),
                        row("P01", "B1", "T1"), row("P02", "B1", "T2")),
                sheet("materiel", Arrays.asList("plot_id", "variete"),
                        row("P01", "Syrah"), row("P02", "Grenache"), row("P03", "Merlot")));
    }
}
