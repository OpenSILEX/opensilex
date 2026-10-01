//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.WorkbookFixture;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.create.rows.RowError;
import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.mapping.MappingService;
import org.opensilex.aiimport.mapping.TypeIssue;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.miappe.MiappeProfile;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Every key the report emits has to exist in both language files.
 * <p>
 * Checked this way round on purpose. A test that hunted English words in the output would fail on
 * a proper noun and pass on a missing translation; this one asks the only question that matters —
 * will the interface find something to show? A key with no entry renders as
 * {@code AiImport.report.hint.experimentMissing} in front of the user.
 *
 * @author Arnaud Charleroy
 */
public class TranslationKeyTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode french;
    private static JsonNode english;

    @BeforeClass
    public static void readTheLanguageFiles() throws Exception {
        french = MAPPER.readTree(new File("front/src/lang/ai-import-fr.json"));
        english = MAPPER.readTree(new File("front/src/lang/ai-import-en.json"));
    }

    /**
     * Runs the three profiles over their own workbooks and collects every key produced.
     */
    @Test
    public void everyKeyTheReportEmitsExistsInBothLanguages() throws Exception {
        Set<String> keys = new LinkedHashSet<>();
        collect(new VitisExplorerProfile(), WorkbookFixture.vitis(), keys);
        collect(new StarProfile(), WorkbookFixture.starStandard(), keys);
        collect(new StarProfile(), WorkbookFixture.starExample(), keys);
        collect(new MiappeProfile(), WorkbookFixture.miappe(), keys);

        assertFalse("no key was collected, so this test would pass on anything", keys.isEmpty());
        assertNoneMissing(keys);
    }

    /**
     * The English text travels with the key because it is what the prompt carries, so a message
     * without it would leave the assistant with nothing to read.
     */
    @Test
    public void everyMessageAlsoCarriesItsEnglishText() throws Exception {
        for (ReportMessage message : messagesOf(new VitisExplorerProfile(), WorkbookFixture.vitis())) {
            assertTrue("a message must always carry its English text",
                    message.getEnglish() != null && !message.getEnglish().isEmpty());
        }
    }

    /**
     * The resolution's hints and warnings need an instance to be produced, so their keys are listed
     * here instead. A key added there without a translation fails this test, which is the point.
     */
    @Test
    public void theResolutionKeysAreTranslated() {
        assertNoneMissing(new LinkedHashSet<>(List.of(
                "AiImport.report.hint.experimentMissing",
                "AiImport.report.hint.experimentAmbiguous",
                "AiImport.report.hint.projectMissing",
                "AiImport.report.hint.variableInSharedResource",
                "AiImport.report.hint.variableMissing",
                "AiImport.report.hint.variableMissingWithExternalId",
                "AiImport.report.hint.germplasmMissing",
                "AiImport.report.hint.facilityMissing",
                "AiImport.report.hint.objectsNeedAnExperiment",
                "AiImport.report.hint.objectMissing",
                "AiImport.report.hint.ambiguous",
                "AiImport.report.warning.projectLookupFailed",
                "AiImport.report.warning.variableLookupFailed",
                "AiImport.report.warning.externalIdLookupFailed",
                "AiImport.report.warning.datatypeUnreadable",
                "AiImport.report.warning.germplasmLookupFailed",
                "AiImport.report.warning.facilityLookupFailed",
                "AiImport.report.warning.objectsLookupFailed",
                "AiImport.report.hint.personMissing",
                "AiImport.report.warning.personLookupFailed",
                "AiImport.report.hint.didYouMean",
                "AiImport.report.hint.confirmedByUser",
                "AiImport.report.warning.nearMatchLookupFailed",
                "AiImport.report.warning.nearMatchLimitReached",
                "AiImport.report.hint.learnedCorrection",
                "AiImport.report.warning.correctionsUnreadable")));
    }

    /**
     * One key per family of row error the platform's validators report, and per file-level refusal.
     * Derived from the enum, so a family added there without a translation fails here. The module's
     * own refusals (UNRESOLVED) and file errors carry their key with them.
     */
    @Test
    public void everyRowErrorFamilyIsTranslated() {
        Set<String> keys = new LinkedHashSet<>();
        for (RowError.Kind kind : RowError.Kind.values()) {
            if (kind != RowError.Kind.UNRESOLVED && kind != RowError.Kind.FILE) {
                keys.add("AiImport.rows.kind." + kind.name());
            }
            keys.add("AiImport.rows.family." + kind.name());
        }
        keys.addAll(List.of("AiImport.rows.kind.tooLarge", "AiImport.rows.kind.missingHeader",
                "AiImport.rows.kind.invalidHeader", "AiImport.rows.interrupted"));
        assertNoneMissing(keys);
    }

    /**
     * Every creation target has its title, and the messages of the proposals that need an instance
     * to be produced are listed.
     */
    @Test
    public void theCreationKeysAreTranslated() {
        Set<String> keys = new LinkedHashSet<>();
        for (CreationTarget target : CreationTarget.values()) {
            keys.add("AiImport.proposal.target_" + target.name());
        }
        keys.addAll(List.of(
                "AiImport.proposal.appliedObjects",
                "AiImport.proposal.field.objectType",
                "AiImport.proposal.field.objectSummary",
                "AiImport.proposal.from.objectSheet",
                "AiImport.proposal.block.noObjectRows",
                "AiImport.proposal.warn.objectsWillBeUpdated",
                "AiImport.proposal.unresolved.germplasm",
                "AiImport.proposal.unresolved.factorLevel",
                "AiImport.proposal.unresolved.facility"));
        assertNoneMissing(keys);
    }

    //#region helpers

    private void assertNoneMissing(Set<String> keys) {
        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            if (lookup(french, key) == null) {
                missing.add(key + " (fr)");
            }
            if (lookup(english, key) == null) {
                missing.add(key + " (en)");
            }
        }
        assertTrue("keys with no translation: " + missing, missing.isEmpty());
    }

    private void collect(ImportProfile profile, WorkbookStructure workbook, Set<String> keys)
            throws Exception {
        for (ReportMessage message : messagesOf(profile, workbook)) {
            if (message.getKey() != null) {
                keys.add(message.getKey());
            }
        }
    }

    /**
     * The messages a profile produces, from its anomalies and from the column mapping.
     */
    private List<ReportMessage> messagesOf(ImportProfile profile, WorkbookStructure workbook)
            throws Exception {
        List<ReportMessage> messages =
                new ArrayList<>(profile.extract(workbook).getAnomalyMessages());

        ResolutionReport empty = new ResolutionReport();
        for (ColumnMapping mapping : new MappingService().map(workbook, profile, empty)) {
            if (mapping.getSuggestionMessage() != null) {
                messages.add(mapping.getSuggestionMessage());
            }
            for (TypeIssue issue : mapping.getIssues()) {
                if (issue.getProblemMessage() != null) {
                    messages.add(issue.getProblemMessage());
                }
                if (issue.getSuggestionMessage() != null) {
                    messages.add(issue.getSuggestionMessage());
                }
            }
        }
        return messages;
    }

    private String lookup(JsonNode root, String key) {
        JsonNode node = root;
        for (String segment : key.split("\\.")) {
            node = node.get(segment);
            if (node == null) {
                return null;
            }
        }
        return node.isTextual() ? node.asText() : null;
    }

    //#endregion
}
