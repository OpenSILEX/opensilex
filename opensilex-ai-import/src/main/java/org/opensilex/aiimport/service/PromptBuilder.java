//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensilex.aiimport.create.CreationRequirements;
import org.opensilex.aiimport.create.RequiredField;
import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.ResolutionStatus;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles the system prompt: the rules, the profile's domain knowledge, a bounded view of the
 * file, and what the instance already has.
 * <p>
 * The rules matter more than the rest. The assistant is told, in the strongest terms the format
 * allows, that it must not invent identifiers and must ask rather than guess. Everything factual it
 * is allowed to state comes either from the report below or from a tool call.
 *
 * @author Arnaud Charleroy
 */
public class PromptBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptBuilder.class);

    /**
     * How much of a prose sheet, such as a ReadMe, is inserted. These sheets are short by nature,
     * but a truncation guard keeps a pathological file from filling the context.
     */
    private static final int MAX_SHEET_TEXT_CHARS = 4000;

    /**
     * How many sheets get a sample of rows. The rest are reachable through `get_sheet_preview`, and
     * in a template every stage sheet looks like the one before it.
     */
    private static final int SAMPLED_SHEETS = 3;

    /**
     * How many example values are quoted per resolution status.
     */
    private static final int MAX_EXAMPLES_PER_STATUS = 8;

    /**
     * How many columns needing attention are listed before deferring to `get_mapping`.
     */
    private static final int MAX_MAPPING_LINES = 12;

    /**
     * Below this many tabular sheets, declaring shared columns separately saves nothing.
     */
    private static final int MIN_SHEETS_TO_SHARE_HEADERS = 3;

    private final ObjectMapper mapper;
    private final int sampleRowsPerSheet;

    public PromptBuilder(ObjectMapper mapper, int sampleRowsPerSheet) {
        this.mapper = mapper;
        this.sampleRowsPerSheet = Math.max(1, sampleRowsPerSheet);
    }

    public String build(WorkbookStructure workbook, ImportProfile profile, ResolutionReport report,
                        List<ColumnMapping> mappings, List<CreationRequirements> creationRequirements,
                        String language) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(rules(language)).append("\n\n");
        prompt.append("## What this file family is\n\n")
                .append(profile.getPromptContext(workbook)).append("\n\n");
        prompt.append("## The uploaded file\n\n")
                .append(asJson(structureView(workbook))).append("\n\n");
        prompt.append(sheetTexts(workbook));
        prompt.append("## What this OpenSILEX instance already has\n\n")
                .append(asJson(reportSummary(report))).append("\n\n");
        prompt.append("## How each column was mapped, and where the values disagree with it\n\n")
                .append("This mapping was computed from the template and from the instance, not by\n")
                .append("you. Explain it, question it if something looks wrong, and offer to help\n")
                .append("the user fix what does not line up. Do not restate it column by column.\n\n")
                .append(asJson(mappingSummary(mappings))).append("\n\n");
        prompt.append(creationSection(creationRequirements)).append("\n\n");
        prompt.append(howToSeeMore());

        return prompt.toString();
    }

    /**
     * @return the fields each creation accepts, read from the same computation that validates a
     * draft.
     * <p>
     * Derived rather than described in prose: a required field that changes in the code would
     * otherwise leave the assistant confidently proposing a field that no longer exists, and having
     * its draft refused for a reason it could not see.
     */
    private String creationSection(List<CreationRequirements> requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return "";
        }
        StringBuilder section = new StringBuilder("## What you can draft with propose_creation\n\n");

        List<Map<String, Object>> view = new ArrayList<>();
        for (CreationRequirements target : requirements) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("creates", target.getTarget().name());
            entry.put("available_now", target.isAvailable());

            List<String> required = new ArrayList<>();
            List<String> optional = new ArrayList<>();
            for (RequiredField field : target.getFields()) {
                String described = field.getName()
                        + (field.getSuggestedValue() == null
                                ? ""
                                : " (the file suggests: " + field.getSuggestedValue() + ")");
                if (field.isRequired()) {
                    required.add(described);
                } else {
                    optional.add(field.getName());
                }
            }
            entry.put("required_fields", required);
            entry.put("optional_fields", optional);
            if (!target.getBlockers().isEmpty()) {
                entry.put("blocked_because", target.getBlockers());
            }
            view.add(entry);
        }
        return section.append(asJson(view)).toString();
    }

    /**
     * Tells the model that the summaries above are summaries, and which tool returns the rest.
     * <p>
     * This is what keeps the prompt small. Sending the whole report and the whole mapping every
     * turn cost about four times as much, and most of it was never read: a question about one
     * column does not need the other sixty.
     */
    private String howToSeeMore() {
        return String.join("\n", Arrays.asList(
                "## Getting the detail",
                "",
                "The two sections above are summaries. When a question needs more than they hold,",
                "call a tool rather than guessing or asking the user to repeat themselves:",
                "",
                "- `get_report` returns the resolved items of one category, filtered by status.",
                "- `get_mapping` returns the full mapping of one column, with every offending cell.",
                "- `get_sheet_preview` returns more rows of one sheet.",
                "- `get_creation_fields` returns the exact fields a creation accepts, which changes",
                "  once something has been created.",
                "",
                "Do not apologise for looking something up; it is what these are for."
        ));
    }

    private String rules(String language) {
        List<String> lines = new ArrayList<>();
        lines.add("You help a scientist prepare a spreadsheet for import into OpenSILEX, a research");
        lines.add("data management system for plant phenotyping. You are talking to the person who");
        lines.add("owns the data.");
        lines.add("");
        lines.add("Your job, in this order:");
        lines.add("1. Say what the file appears to contain, in a few sentences: what was observed,");
        lines.add("   on what, over what period, and how the sheets are organised.");
        lines.add("2. State plainly what is already in the instance and what is not, using the");
        lines.add("   report below.");
        lines.add("3. Ask about what is genuinely missing or contradictory. Ask a few precise");
        lines.add("   questions at a time, not a long list.");
        lines.add("");
        lines.add("Rules you must not break:");
        lines.add("- Never invent a URI, an identifier or a resource. Every URI you write must come");
        lines.add("  from the report below or from a tool result. If you do not have one, say so.");
        lines.add("- Never claim something exists in the instance without having seen it in the");
        lines.add("  report or in a tool result. Call a tool instead of guessing.");
        lines.add("- You can draft a creation, here in the conversation. When the user wants a");
        lines.add("  project, an experiment, or the observation data inserted:");
        lines.add("    1. describe what it would contain, in prose — which name, which dates, which");
        lines.add("       objective, and where each value comes from;");
        lines.add("    2. then call propose_creation with those values.");
        lines.add("  A card appears under your message with the values, each one editable, and a");
        lines.add("  confirm button. The user decides.");
        lines.add("- Describe first, propose second. A draft that appears with no explanation asks");
        lines.add("  the user to approve something nobody accounted for.");
        lines.add("- Never invent a value for a required field. If propose_creation reports one as");
        lines.add("  still missing, ask the user for it and propose again with their answer. The");
        lines.add("  objective of an experiment is the usual case: no data file states it.");
        lines.add("- One draft at a time. Settle the current one before proposing another.");
        lines.add("- You never confirm a draft yourself, and nothing is written until the user does.");
        lines.add("  Say what the draft will create; never say you have created it.");
        lines.add("- Never tell the user to go to the general OpenSILEX screens to create a project,");
        lines.add("  an experiment, or to import the data. You can draft all three here, filled in");
        lines.add("  from their file. Sending them elsewhere throws away the work this page did.");
        lines.add("- Variables are handled from this page, in this order of preference:");
        lines.add("    1. if the report says a variable is FOUND_IN_SHARED_RESOURCE, propose the");
        lines.add("       VARIABLE creation: it copies the variable AND its entity, characteristic,");
        lines.add("       method and unit from that instance, in one step. Always prefer this — it");
        lines.add("       adds nothing new to this instance's referential;");
        lines.add("    2. if it exists nowhere, tell the user to use the Create button beside that");
        lines.add("       variable in the report. It opens the platform's own variable form with");
        lines.add("       the name and the ontology identifier already filled in, and its selectors");
        lines.add("       let them pick an existing entity, characteristic, method and unit, or");
        lines.add("       create one there. Say which of the four the file does supply, so they");
        lines.add("       know what is left to decide.");
        lines.add("  No template names all four components, so never state one you have not read.");
        lines.add("  A component created by mistake is a permanent entry in this instance.");
        lines.add("- Germplasm and scientific objects cannot be created from this page. For those,");
        lines.add("  the general OpenSILEX screens are the right answer.");
        lines.add("- Never silently correct a value. When a cell disagrees with the type its");
        lines.add("  variable expects, say which cell, why, and what would fix it, then offer to");
        lines.add("  walk the user through it. A measurement converted behind their back is a");
        lines.add("  measurement nobody can trace.");
        lines.add("- Do not invent units, methods or measurement scales. If a variable's unit is not");
        lines.add("  in the file, ask.");
        lines.add("- When the report flags an inconsistency, raise it explicitly rather than working");
        lines.add("  around it.");
        lines.add("");
        lines.add("Information typically missing from files like this, worth asking about:");
        lines.add("- which project and which experiment the data belongs to, and the experiment's");
        lines.add("  start and end dates;");
        lines.add("- the entity, characteristic, method and unit of each new variable — a file");
        lines.add("  rarely names more than the unit;");
        lines.add("- what the observer codes stand for, since they become the data provenance;");
        lines.add("- how coded values should be read, when the file uses a scale or an abbreviation.");
        lines.add("");
        lines.add("Style: be brief and concrete. Use short paragraphs and lists. Markdown is");
        lines.add("rendered, so use it, but keep tables small. Do not repeat the whole report back");
        lines.add("to the user: they can see it beside the conversation.");
        lines.add("");
        lines.add("Write in " + describeLanguage(language) + ".");
        return String.join("\n", lines);
    }

    private String describeLanguage(String language) {
        if (language == null || language.isEmpty()) {
            return "English";
        }
        return language.toLowerCase().startsWith("fr") ? "French" : "English";
    }

    /**
     * @return the file reduced to what the model needs.
     * <p>
     * Columns shared by every tabular sheet are declared once instead of being repeated: in the
     * reference template the nine cartouche columns accounted for 153 of the 264 headers sent.
     * Sample rows are kept for the first few sheets only, since `get_sheet_preview` returns the
     * rest of any sheet on demand.
     */
    private Map<String, Object> structureView(WorkbookStructure workbook) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("file_name", workbook.getFileName());
        if (workbook.isDate1904()) {
            view.put("date_system", "1904");
        }

        List<String> shared = sharedHeaders(workbook);
        if (!shared.isEmpty()) {
            view.put("columns_present_in_every_table_sheet", shared);
        }

        int sampled = 0;
        List<Map<String, Object>> sheets = new ArrayList<>();
        for (SheetStructure sheet : workbook.getSheets()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", sheet.getName());
            if (!sheet.isTabular()) {
                entry.put("kind", "text");
                sheets.add(entry);
                continue;
            }
            entry.put("kind", "table");
            entry.put("data_rows", sheet.getDataRowCount());

            List<String> own = new ArrayList<>();
            for (String header : sheet.getHeaders()) {
                if (!header.isEmpty() && !shared.contains(header)) {
                    own.add(header);
                }
            }
            entry.put("columns_beyond_the_shared_ones", own);

            if (sampled < SAMPLED_SHEETS) {
                entry.put("sample", sheet.sample(sampleRowsPerSheet));
                sampled++;
            }
            sheets.add(entry);
        }
        view.put("sheets", sheets);
        return view;
    }

    /**
     * @return the headers most tabular sheets have, in the order they first appear
     * <p>
     * A majority rather than all of them: a workbook usually carries a catalogue or a lookup sheet
     * whose headers have nothing in common with the data sheets, and requiring unanimity lets that
     * one sheet defeat the whole saving.
     */
    private List<String> sharedHeaders(WorkbookStructure workbook) {
        List<SheetStructure> tabular = new ArrayList<>();
        workbook.getSheets().forEach(sheet -> {
            if (sheet.isTabular()) {
                tabular.add(sheet);
            }
        });
        if (tabular.size() < MIN_SHEETS_TO_SHARE_HEADERS) {
            return new ArrayList<>();
        }
        int threshold = (tabular.size() + 1) / 2;

        List<String> shared = new ArrayList<>();
        for (SheetStructure sheet : tabular) {
            for (String header : sheet.getHeaders()) {
                if (header.isEmpty() || shared.contains(header)) {
                    continue;
                }
                int occurrences = 0;
                for (SheetStructure other : tabular) {
                    if (other.hasHeader(header)) {
                        occurrences++;
                    }
                }
                if (occurrences >= threshold) {
                    shared.add(header);
                }
            }
        }
        return shared;
    }

    private String sheetTexts(WorkbookStructure workbook) {
        StringBuilder builder = new StringBuilder();
        for (SheetStructure sheet : workbook.getSheets()) {
            if (sheet.isTabular() || sheet.getText() == null || sheet.getText().isEmpty()) {
                continue;
            }
            String text = sheet.getText();
            if (text.length() > MAX_SHEET_TEXT_CHARS) {
                text = text.substring(0, MAX_SHEET_TEXT_CHARS) + "\n[truncated]";
            }
            builder.append("## Text of the '").append(sheet.getName()).append("' sheet\n\n")
                    .append("These are the instructions the template author wrote for whoever fills\n")
                    .append("the file. Treat them as the authoritative description of the format.\n\n")
                    .append(text).append("\n\n");
        }
        return builder.toString();
    }

    /**
     * @return the report as counts, anomalies, and a few examples per status.
     * <p>
     * The full report repeated the same status and the same hint on 137 of its 175 entries in the
     * reference case, which is a third of the prompt spent saying one thing many times.
     * `get_report` returns any category in full.
     */
    private Map<String, Object> reportSummary(ResolutionReport report) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("experiments", categorySummary(report.getExperiments()));
        view.put("projects", categorySummary(report.getProjects()));
        view.put("variables", categorySummary(report.getVariables()));
        view.put("germplasm", categorySummary(report.getGermplasm()));
        view.put("scientific_objects", categorySummary(report.getScientificObjects()));
        view.put("facilities", categorySummary(report.getFacilities()));

        if (!report.getAnomalies().isEmpty()) {
            view.put("inconsistencies_found_in_the_file", report.getAnomalies());
        }
        if (!report.getWarnings().isEmpty()) {
            view.put("lookups_that_could_not_be_completed", report.getWarnings());
        }
        if (!report.getNotes().isEmpty()) {
            view.put("notes", report.getNotes());
        }
        view.put("status_meaning", statusMeanings());
        return view;
    }

    /**
     * @return per status: how many, a few example values, and the hint they share
     */
    private Map<String, Object> categorySummary(List<ResolvedItem> items) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", items.size());
        if (items.isEmpty()) {
            return summary;
        }

        Map<ResolutionStatus, List<ResolvedItem>> byStatus = new LinkedHashMap<>();
        for (ResolvedItem item : items) {
            byStatus.computeIfAbsent(item.getStatus(), ignored -> new ArrayList<>()).add(item);
        }

        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<ResolutionStatus, List<ResolvedItem>> entry : byStatus.entrySet()) {
            List<ResolvedItem> group = entry.getValue();
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("status", entry.getKey().name());
            line.put("count", group.size());
            line.put("examples", examples(group));
            if (group.size() > MAX_EXAMPLES_PER_STATUS) {
                line.put("more", group.size() - MAX_EXAMPLES_PER_STATUS
                        + " further values, from get_report");
            }
            // The hint is the same across a status group, so it is stated once.
            group.stream()
                    .map(ResolvedItem::getHint)
                    .filter(hint -> hint != null && !hint.isEmpty())
                    .findFirst()
                    .ifPresent(hint -> line.put("hint", hint));
            groups.add(line);
        }
        summary.put("by_status", groups);
        return summary;
    }

    /**
     * @return the first few source values, each with its matched name when it has one
     */
    private List<String> examples(List<ResolvedItem> items) {
        List<String> examples = new ArrayList<>();
        for (ResolvedItem item : items) {
            if (examples.size() == MAX_EXAMPLES_PER_STATUS) {
                break;
            }
            StringBuilder example = new StringBuilder(item.getSourceValue());
            if (item.getExternalId() != null) {
                example.append(" (").append(item.getExternalId()).append(')');
            }
            if (item.getMatches().size() == 1) {
                example.append(" -> ").append(item.getMatches().get(0).getName());
            }
            examples.add(example.toString());
        }
        return examples;
    }

    private Map<String, String> statusMeanings() {
        Map<String, String> meanings = new LinkedHashMap<>();
        meanings.put(ResolutionStatus.FOUND.name(), "exists in this instance");
        meanings.put(ResolutionStatus.AMBIGUOUS.name(), "several candidates, a human must choose");
        meanings.put(ResolutionStatus.FOUND_IN_SHARED_RESOURCE.name(),
                "not here, but importable from a shared resource instance");
        meanings.put(ResolutionStatus.MISSING.name(), "does not exist, has to be created");
        meanings.put(ResolutionStatus.NOT_CHECKED.name(),
                "could not be checked yet, usually because it depends on something still missing");
        return meanings;
    }

    /**
     * @return only the columns needing attention, one line each.
     * <p>
     * Offending cells are counted rather than quoted: `get_mapping` returns them for the one column
     * a question is actually about.
     */
    private Map<String, Object> mappingSummary(List<ColumnMapping> mappings) {
        List<ColumnMapping> needingAttention = new ArrayList<>();
        for (ColumnMapping mapping : mappings) {
            if (mapping.needsAttention()) {
                needingAttention.add(mapping);
            }
        }

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("columns_total", mappings.size());
        view.put("columns_needing_attention", needingAttention.size());

        List<Map<String, Object>> lines = new ArrayList<>();
        for (ColumnMapping mapping : needingAttention) {
            if (lines.size() == MAX_MAPPING_LINES) {
                view.put("more", needingAttention.size() - MAX_MAPPING_LINES
                        + " further columns, from get_mapping");
                break;
            }
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("column", mapping.getColumn());
            line.put("stands_for", mapping.getRole().name());
            line.put("values_look_like", mapping.getObservedKind().name());
            if (mapping.getResolvedName() != null) {
                line.put("matched_variable", mapping.getResolvedName());
                line.put("expected_type", shortName(mapping.getExpectedDatatype()));
            }
            if (mapping.hasIssues()) {
                line.put("offending_cells", mapping.getIssues().size());
            }
            if (mapping.getSuggestion() != null) {
                line.put("suggestion", mapping.getSuggestion());
            }
            lines.add(line);
        }
        view.put("columns", lines);
        return view;
    }

    /**
     * @return the local part of a datatype URI, e.g. {@code decimal}
     */
    private String shortName(String datatype) {
        if (datatype == null) {
            return null;
        }
        int hash = datatype.lastIndexOf('#');
        return hash >= 0 ? datatype.substring(hash + 1) : datatype;
    }

    private String asJson(Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not serialise a prompt section", e);
            return "{}";
        }
    }
}
