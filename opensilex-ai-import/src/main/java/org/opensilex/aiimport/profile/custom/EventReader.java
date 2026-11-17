//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the events the sheets of events describe: a row is an event that happened to an object, or
 * at a facility, on a date, of the type the user chose for the sheet, with the properties that type
 * has.
 * <p>
 * As for the observations, a row that cannot be placed — no object or facility, no readable date —
 * is left out and counted, never filed under a guess.
 *
 * @author Arnaud Charleroy
 */
public final class EventReader {

    /**
     * @param events    the events
     * @param anomalies what was left out, and why
     */
    public record Read(List<EventCandidate> events, List<ReportMessage> anomalies) {
    }

    private EventReader() {
    }

    public static Read read(List<DescribedSheet> sheets) {
        List<EventCandidate> events = new ArrayList<>();
        List<ReportMessage> anomalies = new ArrayList<>();
        for (DescribedSheet sheet : sheets) {
            if (sheet.isReadable() && sheet.kind() == SheetKind.EVENTS) {
                readSheet(sheet, events, anomalies);
            }
        }
        return new Read(events, anomalies);
    }

    private static void readSheet(DescribedSheet sheet, List<EventCandidate> events, List<ReportMessage> anomalies) {
        SheetStructure data = sheet.sheet();
        boolean onObjects = sheet.column(DescribedRole.OBJECT) != null;
        String targetColumn = onObjects ? sheet.column(DescribedRole.OBJECT) : sheet.column(DescribedRole.LOCATION);
        DataPoint.TargetKind targetKind = onObjects
                ? DataPoint.TargetKind.SCIENTIFIC_OBJECT
                : DataPoint.TargetKind.FACILITY;
        String dateColumn = sheet.column(DescribedRole.DATE);
        List<String> comments = sheet.columns(DescribedRole.COMMENT);
        SheetDescription said = sheet.description();
        URI type = said == null ? null : said.getType();

        List<Integer> withoutTarget = new ArrayList<>();
        List<Integer> withoutDate = new ArrayList<>();
        for (int i = 0; i < data.getRows().size(); i++) {
            List<String> row = data.getRows().get(i);
            int rowNumber = i + 2;
            String target = data.cell(row, targetColumn).trim();
            if (target.isEmpty()) {
                withoutTarget.add(rowNumber);
                continue;
            }
            LocalDate date = ExcelValueParser.parseDate(data.cell(row, dateColumn), data.isDate1904());
            if (date == null) {
                withoutDate.add(rowNumber);
                continue;
            }
            EventCandidate event = new EventCandidate()
                    .setSheet(sheet.name())
                    .setRowNumber(rowNumber)
                    .setDate(date)
                    .setTargetKind(targetKind)
                    .setDescription(FieldValues.joined(data, row, comments))
                    .setType(type)
                    .setTypeLabel(type == null ? null : localName(type));
            event.getTargetNames().add(target);
            if (said != null) {
                for (String column : sheet.columns(DescribedRole.OBJECT_PROPERTY)) {
                    String property = said.getProperties().get(column);
                    String value = data.cell(row, column).trim();
                    if (property != null && !property.isBlank() && !value.isEmpty()) {
                        event.getProperties().put(URI.create(property), value);
                    }
                }
            }
            events.add(event);
        }
        DataReader.say(anomalies, DataReader.NO_TARGET, sheet.name(), withoutTarget,
                "no object or facility the event concerns");
        DataReader.say(anomalies, DataReader.NO_DATE, sheet.name(), withoutDate, "no readable date");
    }

    /**
     * The name a type is read by when the ontology's label is not at hand: what follows the last
     * {@code #} or {@code /} of its URI.
     */
    private static String localName(URI type) {
        String text = type.toString();
        return text.substring(Math.max(text.lastIndexOf('#'), text.lastIndexOf('/')) + 1);
    }
}
