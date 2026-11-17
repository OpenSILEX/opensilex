//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The observations of the sheets described as holding some, in the file's own words.
 * <p>
 * A row that cannot be placed — it names nothing it was made on, or carries no readable date — is
 * left out and counted, never filed under a guess: measurements against the wrong object are worse
 * than measurements not inserted. The report says how many rows and which, so the user can fix
 * the file.
 *
 * @author Arnaud Charleroy
 */
public final class DataReader {

    static final String NO_TARGET = DescriptionRules.PREFIX + "rowsWithoutTarget";
    static final String NO_DATE = DescriptionRules.PREFIX + "rowsWithoutDate";

    private static final int ROWS_SHOWN = 5;

    /**
     * @param points    the observations
     * @param anomalies what was left out, and why
     */
    public record Read(List<DataPoint> points, List<ReportMessage> anomalies) {
    }

    private DataReader() {
    }

    /**
     * @param sheets every sheet of the files. Each is read in the date system of its own file: a
     *               conversation can hold workbooks saved in either.
     */
    public static Read read(List<DescribedSheet> sheets) {
        List<DataPoint> points = new ArrayList<>();
        List<ReportMessage> anomalies = new ArrayList<>();
        for (DescribedSheet sheet : sheets) {
            if (sheet.isReadable() && sheet.kind().holdsData()) {
                readSheet(sheet, points, anomalies);
            }
        }
        return new Read(points, anomalies);
    }

    private static void readSheet(DescribedSheet sheet, List<DataPoint> points, List<ReportMessage> anomalies) {
        SheetStructure data = sheet.sheet();
        boolean date1904 = data.isDate1904();
        String objectColumn = sheet.column(DescribedRole.OBJECT);
        boolean onObjects = objectColumn != null;
        String targetColumn = onObjects ? objectColumn : sheet.column(DescribedRole.LOCATION);
        DataPoint.TargetKind targetKind = onObjects
                ? DataPoint.TargetKind.SCIENTIFIC_OBJECT
                : DataPoint.TargetKind.FACILITY;
        String dateColumn = sheet.column(DescribedRole.DATE);
        String observerColumn = sheet.column(DescribedRole.OBSERVER);
        List<String> variables = sheet.columns(DescribedRole.VARIABLE);

        List<Integer> withoutTarget = new ArrayList<>();
        List<Integer> withoutDate = new ArrayList<>();
        for (int i = 0; i < data.getRows().size(); i++) {
            List<String> row = data.getRows().get(i);
            int rowNumber = i + 2;
            if (variables.stream().allMatch(column -> ExcelValueParser.isMissing(data.cell(row, column)))) {
                continue;
            }
            String target = data.cell(row, targetColumn).trim();
            if (target.isEmpty()) {
                withoutTarget.add(rowNumber);
                continue;
            }
            LocalDate date = ExcelValueParser.parseDate(data.cell(row, dateColumn), date1904);
            if (date == null) {
                withoutDate.add(rowNumber);
                continue;
            }
            String observer = observerColumn == null ? null : data.cell(row, observerColumn).trim();
            for (String column : variables) {
                String value = data.cell(row, column);
                if (ExcelValueParser.isMissing(value)) {
                    continue;
                }
                points.add(new DataPoint()
                        .setSheet(sheet.name())
                        .setRowNumber(rowNumber)
                        .setObjectName(target)
                        .setTargetKind(targetKind)
                        .setDate(date)
                        .setVariableKey(column)
                        .setRawValue(value)
                        .setObserver(observer == null || observer.isEmpty() ? null : observer));
            }
        }
        say(anomalies, NO_TARGET, sheet.name(), withoutTarget, "no object or facility to file it against");
        say(anomalies, NO_DATE, sheet.name(), withoutDate, "no readable date");
    }

    static void say(List<ReportMessage> anomalies, String key, String sheet, List<Integer> rows,
                            String reason) {
        if (rows.isEmpty()) {
            return;
        }
        String shown = rows.stream().limit(ROWS_SHOWN).map(String::valueOf).collect(Collectors.joining(", "));
        anomalies.add(ReportMessage.of(key, rows.size() + " row(s) of sheet '" + sheet + "' were left out: "
                        + reason + " (rows " + shown + (rows.size() > ROWS_SHOWN ? ", …" : "") + ").")
                .with("sheet", sheet)
                .with("count", rows.size())
                .with("rows", shown + (rows.size() > ROWS_SHOWN ? ", …" : "")));
    }
}
