//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.junit.Test;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.row;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.sheet;

/**
 * The events a sheet of events describes: of the type the user chose, with the properties of that
 * type, on a date, to an object or a facility.
 *
 * @author Arnaud Charleroy
 */
public class EventReaderTest {

    private static final URI SPRAYING = URI.create("http://www.opensilex.org/vocabulary/oeev#ScientificObjectManagement");
    private static final String DOSE = "http://example.org/vocabulary#dose";

    private static SheetDescription events(String... roles) {
        SheetDescription description = new SheetDescription("evenements").setKind(SheetKind.EVENTS);
        for (int i = 0; i + 1 < roles.length; i += 2) {
            description.getRoles().put(roles[i], DescribedRole.valueOf(roles[i + 1]));
        }
        return description;
    }

    private static EventReader.Read read(SheetStructure sheet, SheetDescription description) {
        return EventReader.read(List.of(DescribedSheet.of(sheet, description)));
    }

    @Test
    public void aRowIsAnEventOfTheChosenTypeWithItsPropertiesAndItsCommentsJoined() {
        SheetStructure sheet = sheet("evenements", Arrays.asList("objet", "jour", "dose", "produit", "note"),
                row("P01", "2026-05-12", "2", "cuivre", "pluie"));
        SheetDescription description = events("objet", "OBJECT", "jour", "DATE", "dose", "OBJECT_PROPERTY",
                "produit", "COMMENT", "note", "COMMENT").setType(SPRAYING);
        description.getProperties().put("dose", DOSE);

        EventCandidate event = read(sheet, description).events().get(0);

        assertEquals(SPRAYING, event.getType());
        assertEquals("ScientificObjectManagement", event.getTypeLabel());
        assertEquals(LocalDate.of(2026, 5, 12), event.getDate());
        assertEquals(Arrays.asList("P01"), event.getTargetNames());
        assertEquals(DataPoint.TargetKind.SCIENTIFIC_OBJECT, event.getTargetKind());
        assertEquals("produit: cuivre ; note: pluie", event.getDescription());
        assertEquals(Map.of(URI.create(DOSE), "2"), event.getProperties());
        assertEquals(2, event.getRowNumber());
        assertEquals("evenements", event.getSheet());
    }

    @Test
    public void aFacilityIsTheTargetWhenNoColumnNamesAnObject() {
        SheetStructure sheet = sheet("evenements", Arrays.asList("parcelle", "jour"), row("Nord", "2026-05-12"));

        EventCandidate event = read(sheet, events("parcelle", "LOCATION", "jour", "DATE")).events().get(0);

        assertEquals(DataPoint.TargetKind.FACILITY, event.getTargetKind());
        assertEquals(Arrays.asList("Nord"), event.getTargetNames());
        assertEquals("a sheet with no type gives a plain event", null, event.getType());
    }

    @Test
    public void aRowWithoutATargetOrADateIsLeftOutAndCounted() {
        SheetStructure sheet = sheet("evenements", Arrays.asList("objet", "jour"),
                row("P01", "2026-05-12"), row("", "2026-05-13"), row("P02", "bientôt"), row("P03", ""));

        EventReader.Read read = read(sheet, events("objet", "OBJECT", "jour", "DATE"));

        assertEquals(1, read.events().size());
        List<String> keys = read.anomalies().stream().map(ReportMessage::getKey).collect(Collectors.toList());
        assertTrue(keys.toString(), keys.contains(DataReader.NO_TARGET) && keys.contains(DataReader.NO_DATE));
    }

    @Test
    public void aSheetThatIsNotDescribedAsEventsOrHasAProblemIsNotRead() {
        SheetStructure sheet = sheet("evenements", Arrays.asList("objet", "jour"), row("P01", "2026-05-12"));

        assertTrue("no date column: not readable",
                read(sheet, events("objet", "OBJECT")).events().isEmpty());
        assertTrue("another kind",
                read(sheet, events("objet", "OBJECT", "jour", "DATE").setKind(SheetKind.DATA)).events().isEmpty());
    }
}
