//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.junit.Test;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.flatSheet;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.row;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.sheet;

/**
 * A sheet as the description reads it, and the rules that decide whether it can be read.
 *
 * @author Arnaud Charleroy
 */
public class DescribedSheetTest {

    @Test
    public void aSheetNobodyDescribedIsReadFromItsDefaults() {
        DescribedSheet flat = DescribedSheet.of(flatSheet(), null);

        assertEquals(SheetKind.OBJECTS_AND_DATA, flat.kind());
        assertEquals(DescribedRole.OBJECT, flat.roleOf("plot_id"));
        assertEquals(DescribedRole.VARIABLE, flat.roleOf("hauteur_cm"));
        assertEquals("the date column is the one a person would call so", "date", flat.column(DescribedRole.DATE));
        assertEquals(Arrays.asList("hauteur_cm", "nb_grappes"), flat.columns(DescribedRole.VARIABLE));
        assertTrue(flat.problems().isEmpty());
        assertTrue(flat.isReadable());
    }

    @Test
    public void theKindTheUserChoseWinsOverTheOneTheRolesPointAt() {
        SheetDescription said = new SheetDescription("essai").setKind(SheetKind.OBJECTS);

        DescribedSheet described = DescribedSheet.of(flatSheet(), said);

        assertEquals(SheetKind.OBJECTS, described.kind());
        assertEquals("a default the kind does not accept is set aside, not reported",
                DescribedRole.UNREAD, described.roleOf("hauteur_cm"));
        assertTrue(described.problems().isEmpty());
    }

    @Test
    public void theRoleTheUserChoseWinsOverTheDefault() {
        SheetDescription said = new SheetDescription("essai");
        said.getRoles().put("remarque", DescribedRole.OBJECT_PROPERTY);
        said.getRoles().put("nb_grappes", DescribedRole.UNREAD);

        DescribedSheet described = DescribedSheet.of(flatSheet(), said);

        assertEquals(DescribedRole.OBJECT_PROPERTY, described.roleOf("remarque"));
        assertEquals(DescribedRole.UNREAD, described.roleOf("nb_grappes"));
        assertEquals(Arrays.asList("hauteur_cm"), described.columns(DescribedRole.VARIABLE));
    }

    @Test
    public void aRoleTheUserChoseThatTheKindDoesNotAcceptIsKeptAndReportedNotReplaced() {
        SheetDescription said = new SheetDescription("essai").setKind(SheetKind.OBJECTS);
        said.getRoles().put("hauteur_cm", DescribedRole.VARIABLE);

        DescribedSheet described = DescribedSheet.of(flatSheet(), said);

        assertEquals(DescribedRole.VARIABLE, described.roleOf("hauteur_cm"));
        assertEquals(Arrays.asList(DescriptionRules.NOT_FOR_KIND), keys(described));
        assertFalse("a sheet with a problem is not read: reading it would be guessing", described.isReadable());
    }

    @Test
    public void aSheetOfTextWithoutTabularStructureIsIgnored() {
        SheetStructure readme = new SheetStructure().setName("lisez-moi").setTabular(false).setText("Bienvenue");

        DescribedSheet described = DescribedSheet.of(readme, null);

        assertEquals(SheetKind.IGNORED, described.kind());
        assertFalse(described.isReadable());
    }

    @Test
    public void aSheetNoRoleSaysAnythingAboutIsIgnoredAndNeedsNoDescription() {
        SheetStructure words = sheet("notes", Arrays.asList("couleur", "forme"), row("rouge", "rond"));

        DescribedSheet described = DescribedSheet.of(words, null);

        assertEquals(SheetKind.IGNORED, described.kind());
        assertTrue("nothing is asked of a sheet that is not read", described.problems().isEmpty());
        assertFalse(described.isReadable());
    }

    @Test
    public void objectsWithoutAnIdentifierColumnAreReported() {
        SheetStructure noName = sheet("s", Arrays.asList("variete", "bloc"), row("Syrah", "B1"));
        SheetDescription said = new SheetDescription("s").setKind(SheetKind.OBJECTS);

        assertEquals(Arrays.asList(DescriptionRules.MISSING + "OBJECT"), keys(DescribedSheet.of(noName, said)));
    }

    @Test
    public void twoColumnsForAnIdentifierOrADateAreReported() {
        SheetDescription said = new SheetDescription("essai");
        said.getRoles().put("variete", DescribedRole.OBJECT);
        said.getRoles().put("bloc", DescribedRole.DATE);

        List<String> keys = keys(DescribedSheet.of(flatSheet(), said));

        assertTrue(keys.toString(), keys.contains(DescriptionRules.SEVERAL + "OBJECT"));
        assertTrue(keys.toString(), keys.contains(DescriptionRules.SEVERAL + "DATE"));
    }

    @Test
    public void observationsThatSayNothingAboutWhatTheyWereMadeOnAreReported() {
        SheetStructure noTarget = sheet("mesures", Arrays.asList("date", "hauteur_cm"), row("2026-06-12", "84"));
        SheetDescription said = new SheetDescription("mesures").setKind(SheetKind.DATA);

        assertEquals(Arrays.asList(DescriptionRules.NO_TARGET), keys(DescribedSheet.of(noTarget, said)));
    }

    @Test
    public void observationsOfAFacilityNeedNoObjectColumn() {
        SheetStructure weather = sheet("meteo", Arrays.asList("station", "date", "pluie_mm"),
                row("Bellegarde", "2026-06-12", "3.2"));
        SheetDescription said = new SheetDescription("meteo").setKind(SheetKind.DATA);

        DescribedSheet described = DescribedSheet.of(weather, said);

        assertEquals(DescribedRole.LOCATION, described.roleOf("station"));
        assertTrue(keys(described).toString(), described.problems().isEmpty());
    }

    @Test
    public void aSheetOfLevelsNeedsAFactorAndALevel() {
        SheetStructure levels = sheet("modalites", Arrays.asList("facteur", "modalite"), row("Cuivre", "T1"));

        assertEquals(SheetKind.FACTORS, DescribedSheet.of(levels, null).kind());
        assertTrue(DescribedSheet.of(levels, null).isReadable());

        SheetStructure noFactor = sheet("modalites", Arrays.asList("modalite"), row("T1"));
        SheetDescription said = new SheetDescription("modalites").setKind(SheetKind.FACTORS);
        assertEquals(Arrays.asList(DescriptionRules.MISSING + "FACTOR"), keys(DescribedSheet.of(noFactor, said)));
    }

    @Test
    public void aSheetOfFacilitiesNeedsTheirName() {
        SheetStructure facilities = sheet("sites", Arrays.asList("commune", "remarque"), row("Bellegarde", "vigne"));
        SheetDescription said = new SheetDescription("sites").setKind(SheetKind.FACILITIES);
        said.getRoles().put("commune", DescribedRole.UNREAD);

        assertEquals(Arrays.asList(DescriptionRules.MISSING + "LOCATION"),
                keys(DescribedSheet.of(facilities, said)));
    }

    @Test
    public void theTypeAndThePropertiesTheUserChoseAreKeptWithTheDescription() {
        SheetDescription said = new SheetDescription("essai").setType(URI.create("http://example.org/Plot"));
        said.getProperties().put("remarque", "http://example.org/hasRemark");

        DescribedSheet described = DescribedSheet.of(flatSheet(), said);

        assertEquals(URI.create("http://example.org/Plot"), described.description().getType());
        assertEquals("http://example.org/hasRemark", described.description().getProperties().get("remarque"));
        assertNull(DescribedSheet.of(flatSheet(), null).description());
    }

    private static List<String> keys(DescribedSheet sheet) {
        return sheet.problems().stream().map(ReportMessage::getKey).collect(Collectors.toList());
    }
}
