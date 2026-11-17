//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.mapping.ColumnRole;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.profile.FactorLevelCandidate;
import org.opensilex.aiimport.profile.ImportProfileRegistry;
import org.opensilex.aiimport.profile.ObjectRow;
import org.opensilex.aiimport.profile.ObjectSheetDefaults;
import org.opensilex.aiimport.profile.VariableCandidate;
import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.flat;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.planAndMaterial;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.row;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.sheet;
import static org.opensilex.aiimport.profile.custom.CustomWorkbooks.workbook;

/**
 * The profile of a file the user described: it reads what it was told, and never recognises a
 * file by itself.
 *
 * @author Arnaud Charleroy
 */
public class UserDefinedProfileTest {

    @Test
    public void itRecognisesNoFileAndTheRegistryNeverChoosesIt() throws Exception {
        UserDefinedProfile profile = new UserDefinedProfile();

        assertEquals(0, profile.match(WorkbookFixture.starStandard()));
        assertEquals(0, profile.match(flat()));
        ImportProfileRegistry registry = new ImportProfileRegistry();
        assertTrue("it can be asked for by its identifier", registry.getById(UserDefinedProfile.ID).isPresent());
        assertFalse("but no file is ever given to it", registry.select(WorkbookFixture.starStandard())
                .getId().equals(UserDefinedProfile.ID));
        assertFalse(registry.select(flat()).getId().equals(UserDefinedProfile.ID));
    }

    @Test
    public void theNamesOfAFlatFileAreExtractedFromTheColumnsDescribedSo() {
        ExtractedImportPlan plan = new UserDefinedProfile().extract(flat());

        assertEquals(UserDefinedProfile.ID, plan.getProfileId());
        assertEquals(Arrays.asList("P01", "P02"), plan.getScientificObjectNames());
        assertEquals(Arrays.asList("Syrah", "Grenache"), plan.getGermplasmNames());
        assertEquals(Arrays.asList("hauteur_cm", "nb_grappes"),
                plan.getVariables().stream().map(VariableCandidate::getColumnKey).collect(Collectors.toList()));
        assertEquals(Arrays.asList("essai"), plan.getVariables().get(0).getSheets());
    }

    @Test
    public void aCsvFileReadInWindowsAccentsSaysSoInTheReport() {
        WorkbookStructure file = flat();
        file.getSheets().get(0).setCharset("windows-1252");

        ExtractedImportPlan plan = new UserDefinedProfile().extract(file);

        assertTrue(plan.getNotes().get("charset of essai").startsWith("windows-1252"));
        assertFalse(new UserDefinedProfile().extract(flat()).getNotes().containsKey("charset of essai"));
    }

    @Test
    public void theLevelsOfASheetWithNoFactorColumnBelongToTheFactorTheUserNamed() {
        WorkbookStructure workbook = workbook(sheet("modalites", Arrays.asList("modalite", "remarque"),
                row("T1", "temoin"), row("T2", "cuivre")));
        FileDescription description = new FileDescription();
        description.sheet("modalites").setKind(SheetKind.FACTORS).setFactor("Traitement")
                .getRoles().put("modalite", DescribedRole.FACTOR_LEVEL);
        description.sheet("modalites").getRoles().put("remarque", DescribedRole.COMMENT);

        List<FactorLevelCandidate> levels = new UserDefinedProfile(description).extractFactorLevels(workbook);

        assertEquals(Arrays.asList("Traitement", "Traitement"),
                levels.stream().map(FactorLevelCandidate::factor).collect(Collectors.toList()));
        assertEquals(Arrays.asList("T1", "T2"),
                levels.stream().map(FactorLevelCandidate::code).collect(Collectors.toList()));
        assertEquals("temoin", levels.get(0).description());
        assertFalse(keys(new UserDefinedProfile(description).extract(workbook))
                .contains(DescriptionRules.PREFIX + "missing_FACTOR"));
    }

    @Test
    public void aSheetOfLevelsWithNeitherAFactorColumnNorAFactorNameIsNotRead() {
        WorkbookStructure workbook = workbook(sheet("modalites", Arrays.asList("modalite"), row("T1")));
        FileDescription description = new FileDescription();
        description.sheet("modalites").setKind(SheetKind.FACTORS)
                .getRoles().put("modalite", DescribedRole.FACTOR_LEVEL);
        UserDefinedProfile profile = new UserDefinedProfile(description);

        assertTrue(profile.extractFactorLevels(workbook).isEmpty());
        assertTrue(keys(profile.extract(workbook)).contains(DescriptionRules.PREFIX + "missing_FACTOR"));

        description.sheet("modalites").setFactor("   ");
        assertTrue("a blank name names nothing", profile.extractFactorLevels(workbook).isEmpty());
    }

    @Test
    public void theLevelsOfASheetTakeAllTheirCommentColumnsAsTheirDescription() {
        WorkbookStructure workbook = workbook(sheet("modalites", Arrays.asList("facteur", "modalite", "a", "b"),
                row("Traitement", "T1", "temoin", "sans produit")));
        FileDescription description = new FileDescription();
        description.sheet("modalites").setKind(SheetKind.FACTORS).getRoles().putAll(java.util.Map.of(
                "facteur", DescribedRole.FACTOR, "modalite", DescribedRole.FACTOR_LEVEL,
                "a", DescribedRole.COMMENT, "b", DescribedRole.COMMENT));

        List<FactorLevelCandidate> levels = new UserDefinedProfile(description).extractFactorLevels(workbook);

        assertEquals("a: temoin ; b: sans produit", levels.get(0).description());
    }

    @Test
    public void whatColumnsSayOfAnExperimentAProjectAndAFacilityReachesThePlan() {
        WorkbookStructure workbook = workbook(
                sheet("plan", Arrays.asList("parcelle", "essai", "projet", "but", "court"),
                        row("P01", "Essai A", "Projet X", "Mesurer", "PX")),
                sheet("sites", Arrays.asList("nom", "note"), row("Nord", "venté")));
        FileDescription description = new FileDescription();
        description.sheet("plan").setKind(SheetKind.OBJECTS).getRoles().putAll(java.util.Map.of(
                "parcelle", DescribedRole.OBJECT, "essai", DescribedRole.TRIAL, "projet", DescribedRole.PROJECT,
                "but", DescribedRole.EXPERIMENT_OBJECTIVE, "court", DescribedRole.PROJECT_SHORTNAME));
        description.sheet("sites").setKind(SheetKind.FACILITIES).getRoles().putAll(java.util.Map.of(
                "nom", DescribedRole.LOCATION, "note", DescribedRole.COMMENT));

        ExtractedImportPlan plan = new UserDefinedProfile(description).extract(workbook);

        assertEquals("Mesurer", plan.getExperimentDetails().get("Essai A").get("objective"));
        assertEquals("PX", plan.getProjectDetails().get("Projet X").get("shortname"));
        assertEquals("venté", plan.getFacilityDetails().get("Nord").get("description"));
    }

    @Test
    public void theSheetsOfEventsGiveTheEventsAndTheirTargetsAreNamedForTheResolution() {
        WorkbookStructure workbook = workbook(sheet("evenements", Arrays.asList("objet", "jour"),
                row("P01", "2026-05-12")));
        FileDescription description = new FileDescription();
        description.sheet("evenements").setKind(SheetKind.EVENTS).getRoles().putAll(java.util.Map.of(
                "objet", DescribedRole.OBJECT, "jour", DescribedRole.DATE));
        UserDefinedProfile profile = new UserDefinedProfile(description);

        assertEquals(1, profile.extractEvents(workbook).size());
        assertEquals(Arrays.asList("P01"), profile.extract(workbook).getScientificObjectNames());
        assertTrue("events belong to an experiment too",
                keys(profile.extract(workbook)).contains(DescriptionRules.PREFIX + "noExperiment"));
    }

    @Test
    public void aSheetOfEventsNeedsADateAndSomethingTheyConcern() {
        WorkbookStructure workbook = workbook(sheet("evenements", Arrays.asList("note", "jour"), row("x", "2026-05-12")));
        FileDescription description = new FileDescription();
        description.sheet("evenements").setKind(SheetKind.EVENTS).getRoles().putAll(java.util.Map.of(
                "note", DescribedRole.COMMENT, "jour", DescribedRole.DATE));

        List<String> keys = keys(new UserDefinedProfile(description).extract(workbook));

        assertTrue(keys.toString(), keys.contains(DescriptionRules.PREFIX + "noTarget"));
    }

    @Test
    public void aSheetOfExperimentsNamesTheExperimentAndFeedsItsForm() {
        WorkbookStructure workbook = workbook(sheet("Expe", Arrays.asList("essai", "nom2", "but", "projet", "site"),
                row("Essai A", "EA", "Mesurer", "Projet X", "Nord")));
        FileDescription description = new FileDescription();
        description.sheet("Expe").setKind(SheetKind.EXPERIMENTS).getRoles().putAll(java.util.Map.of(
                "essai", DescribedRole.TRIAL, "nom2", DescribedRole.EXPERIMENT_ALTERNATIVE_NAME,
                "but", DescribedRole.EXPERIMENT_OBJECTIVE, "projet", DescribedRole.PROJECT,
                "site", DescribedRole.LOCATION));

        ExtractedImportPlan plan = new UserDefinedProfile(description).extract(workbook);

        assertEquals(Arrays.asList("Essai A"), plan.getExperimentNames());
        java.util.Map<String, String> details = plan.getExperimentDetails().get("Essai A");
        assertEquals("EA", details.get("alternative_name"));
        assertEquals("Mesurer", details.get("objective"));
        assertEquals("Projet X", details.get(FieldValues.PROJECTS));
        assertEquals("Nord", details.get(FieldValues.FACILITIES));
        assertEquals(Arrays.asList("Projet X"), plan.getProjectNames());
        assertFalse("a sheet of experiments needs none", keys(plan).contains(DescriptionRules.PREFIX + "noExperiment"));
    }

    @Test
    public void aSheetOfExperimentsWithoutAColumnThatNamesThemIsNotRead() {
        WorkbookStructure workbook = workbook(sheet("Expe", Arrays.asList("but", "x"), row("Mesurer", "1")));
        FileDescription description = new FileDescription();
        description.sheet("Expe").setKind(SheetKind.EXPERIMENTS)
                .getRoles().put("but", DescribedRole.EXPERIMENT_OBJECTIVE);

        assertTrue(keys(new UserDefinedProfile(description).extract(workbook))
                .contains(DescriptionRules.PREFIX + "missing_TRIAL"));
    }

    /**
     * The sheet of levels of a STAR-like trial: the experiment, the code of each level, its name and its
     * description, and the factor named for the sheet.
     */
    @Test
    public void aSheetOfLevelsGivesEachLevelItsNameAndDescriptionAndTheFactorItsDescription() {
        WorkbookStructure workbook = workbook(sheet("modalite",
                Arrays.asList("xpe_id", "xp_trt_code", "xp_trt_name", "xp_trt_desc", "protocole"),
                row("IFV30_teisso_2024", "TNT", "Témoin non traité", "", "Essai cuivre"),
                row("IFV30_teisso_2024", "1", "Cuivre tardif", "cuivre 2 semaines après symptômes", "")));
        FileDescription description = new FileDescription();
        description.sheet("modalite").setKind(SheetKind.FACTORS).setFactor("Traitement").getRoles().putAll(
                java.util.Map.of("xpe_id", DescribedRole.TRIAL, "xp_trt_code", DescribedRole.FACTOR_LEVEL,
                        "xp_trt_name", DescribedRole.FACTOR_LEVEL_NAME,
                        "xp_trt_desc", DescribedRole.FACTOR_LEVEL_DESCRIPTION,
                        "protocole", DescribedRole.FACTOR_DESCRIPTION));

        List<FactorLevelCandidate> levels = new UserDefinedProfile(description).extractFactorLevels(workbook);

        assertEquals(2, levels.size());
        assertEquals("the code stays the level's name: it is what the objects carry", "TNT", levels.get(0).code());
        assertEquals("Témoin non traité", levels.get(0).levelDescription());
        assertEquals("Cuivre tardif — cuivre 2 semaines après symptômes", levels.get(1).levelDescription());
        assertEquals("Essai cuivre", levels.get(0).factorDescription());
        assertEquals("Traitement", levels.get(1).factor());
    }

    @Test
    public void aFactorColumnIsStillWhatNamesTheFactorWhenThereIsOne() {
        WorkbookStructure workbook = workbook(sheet("modalites", Arrays.asList("facteur", "modalite"),
                row("Bloc", "B1"), row("Traitement", "T1")));
        FileDescription description = new FileDescription();
        description.sheet("modalites").setKind(SheetKind.FACTORS).setFactor("Ignore")
                .getRoles().putAll(java.util.Map.of("facteur", DescribedRole.FACTOR,
                        "modalite", DescribedRole.FACTOR_LEVEL));

        List<FactorLevelCandidate> levels = new UserDefinedProfile(description).extractFactorLevels(workbook);

        assertEquals(Arrays.asList("Bloc", "Traitement"),
                levels.stream().map(FactorLevelCandidate::factor).collect(Collectors.toList()));
    }

    @Test
    public void aFileThatNamesNoExperimentSaysSoUntilOneIsChosen() {
        UserDefinedProfile alone = new UserDefinedProfile();

        List<String> keys = keys(alone.extract(flat()));

        assertTrue(keys.toString(), keys.contains(DescriptionRules.PREFIX + "noExperiment"));

        FileDescription description = new FileDescription().setExperiment("http://example.org/xp", "Essai 2026");
        ExtractedImportPlan chosen = new UserDefinedProfile(description).extract(flat());

        assertEquals("the profile names the experiment; the resolution looks it up",
                Arrays.asList("Essai 2026"), chosen.getExperimentNames());
        assertFalse(keys(chosen).contains(DescriptionRules.PREFIX + "noExperiment"));
    }

    @Test
    public void aColumnThatNamesTheExperimentIsEnoughAndTheChosenNameIsNotAddedToIt() {
        WorkbookStructure workbook = workbook(sheet("plan", Arrays.asList("plot_id", "essai"),
                row("P01", "Essai A"), row("P02", "Essai A")));
        FileDescription description = new FileDescription().setExperiment("http://example.org/xp", "Essai B");

        ExtractedImportPlan plan = new UserDefinedProfile(description).extract(workbook);

        assertEquals(Arrays.asList("Essai A"), plan.getExperimentNames());
    }

    @Test
    public void aSheetWhoseDescriptionIsIncompleteIsNotReadAndTheReportSaysWhy() {
        WorkbookStructure workbook = workbook(sheet("mesures", Arrays.asList("date", "hauteur_cm"),
                row("2026-06-12", "84")));
        FileDescription description = new FileDescription();
        description.sheet("mesures").setKind(SheetKind.DATA);
        UserDefinedProfile profile = new UserDefinedProfile(description);

        ExtractedImportPlan plan = profile.extract(workbook);

        assertTrue(keys(plan).toString(), keys(plan).contains(DescriptionRules.NO_TARGET));
        assertTrue("its variables are not offered", plan.getVariables().isEmpty());
        assertTrue(profile.extractDataPoints(workbook).isEmpty());
    }

    @Test
    public void theRolesAreTheOnesTheDescriptionGives() {
        UserDefinedProfile profile = new UserDefinedProfile();
        WorkbookStructure workbook = flat();

        assertEquals(ColumnRole.OBJECT, profile.roleOf(workbook, "essai", "plot_id"));
        assertEquals(ColumnRole.VARIABLE, profile.roleOf(workbook, "essai", "hauteur_cm"));
        assertEquals(ColumnRole.DATE, profile.roleOf(workbook, "essai", "date"));
        assertEquals(ColumnRole.UNKNOWN, profile.roleOf(workbook, "essai", ""));
        assertEquals(ColumnRole.UNKNOWN, profile.roleOf(workbook, "no such sheet", "plot_id"));
    }

    @Test
    public void theObjectsOfTwoSheetsAreOnePerNameAndTheirSheetKnowsItsColumns() {
        UserDefinedProfile profile = new UserDefinedProfile();
        WorkbookStructure workbook = planAndMaterial();

        List<ObjectRow> rows = profile.extractObjectRows(workbook);
        ObjectSheetDefaults defaults = profile.objectSheetDefaults(workbook, "plan");

        assertEquals(3, rows.size());
        assertEquals("plot_id", defaults.nameColumn());
        assertEquals("a type nobody chose is never suggested", null, defaults.suggestedType());
        assertTrue(defaults.targets().containsKey("materiel" + ObjectMerger.QUALIFIER + "variete"));
        assertEquals(ObjectSheetDefaults.none().targets(), profile.objectSheetDefaults(workbook, "unknown").targets());
    }

    @Test
    public void theTypeTheUserChoseIsTheOneSuggested() {
        FileDescription description = new FileDescription();
        description.sheet("plan").setType(URI.create("http://example.org/Plot"));

        ObjectSheetDefaults defaults = new UserDefinedProfile(description)
                .objectSheetDefaults(planAndMaterial(), "plan");

        assertEquals(URI.create("http://example.org/Plot"), defaults.suggestedType());
    }

    @Test
    public void theLevelsOfAFactorComeFromTheColumnsOfObjectsAndFromTheSheetsOfLevels() {
        WorkbookStructure workbook = workbook(
                sheet("plan", Arrays.asList("plot_id", "traitement", "modalite_irrigation"),
                        row("P01", "T1", "Sec"), row("P02", "T2", "Sec"), row("P03", "T1", "Arrosé")),
                sheet("modalites", Arrays.asList("facteur", "modalite", "remarque"),
                        row("Cuivre", "T1", "tard"), row("Cuivre", "T2", ""), row("Cuivre", "T1", "doublon")));

        List<FactorLevelCandidate> levels = new UserDefinedProfile().extractFactorLevels(workbook);

        assertEquals("one per factor and level, whichever sheet says it, the factor being the header",
                Arrays.asList("traitement|T1", "traitement|T2", "modalite_irrigation|Sec",
                        "modalite_irrigation|Arrosé", "Cuivre|T1", "Cuivre|T2"),
                levels.stream().map(level -> level.factor() + "|" + level.code()).collect(Collectors.toList()));
        FactorLevelCandidate tard = levels.stream().filter(level -> "Cuivre".equals(level.factor()))
                .filter(level -> "T1".equals(level.code())).findFirst().get();
        assertEquals("the comment of the sheet of levels describes the level", "tard", tard.description());
        assertEquals("the row of the sheet, as the spreadsheet numbers it", 2, tard.row());
        assertNull(levels.stream().filter(level -> "T2".equals(level.code()) && "Cuivre".equals(level.factor()))
                .findFirst().get().description());
    }

    @Test
    public void facilitiesAndObserversAndProjectsAreNamedForTheResolution() {
        WorkbookStructure workbook = workbook(sheet("mesures",
                Arrays.asList("plot_id", "site", "projet", "observateur", "date", "hauteur_cm"),
                row("P01", "Bellegarde", "Vigne", "Alice", "2026-06-12", "84")));

        ExtractedImportPlan plan = new UserDefinedProfile().extract(workbook);

        assertEquals(Arrays.asList("Bellegarde"), plan.getFacilityNames());
        assertEquals(Arrays.asList("Vigne"), plan.getProjectNames());
        assertEquals(Arrays.asList("Alice"), plan.getObserverNames());
    }

    @Test
    public void aContradictionBetweenSheetsIsAnAnomalyOfTheFile() {
        WorkbookStructure workbook = workbook(
                sheet("plan", Arrays.asList("plot_id", "variete"), row("P01", "Syrah")),
                sheet("materiel", Arrays.asList("plot_id", "variete"), row("P01", "Grenache")));

        List<String> keys = keys(new UserDefinedProfile().extract(workbook));

        assertTrue(keys.toString(), keys.contains(ObjectMerger.CONFLICT));
    }

    @Test
    public void theProfileIsAskedAboutEveryColumnButWorksTheSheetOutOnce() {
        UserDefinedProfile profile = new UserDefinedProfile();
        WorkbookStructure workbook = flat();

        for (int i = 0; i < 3; i++) {
            profile.roleOf(workbook, "essai", "plot_id");
        }
        List<ObjectRow> first = profile.extractObjectRows(workbook);

        assertTrue("the same answer, not a new one, while the workbook is the same",
                first == profile.extractObjectRows(workbook));
        assertFalse("a new workbook is worked out again", first == profile.extractObjectRows(flat()));
    }

    private static List<String> keys(ExtractedImportPlan plan) {
        return plan.getAnomalyMessages().stream().map(ReportMessage::getKey).collect(Collectors.toList());
    }
}
