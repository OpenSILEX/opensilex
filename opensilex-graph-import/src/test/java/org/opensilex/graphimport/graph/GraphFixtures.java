//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.opensilex.aiimport.workbook.SheetSources;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.graphimport.GraphImportModule;
import org.opensilex.graphimport.binding.ImportDecisions;
import org.opensilex.graphimport.interpret.InterpretFixtures;
import org.opensilex.graphimport.interpret.Interpretation;
import org.opensilex.graphimport.interpret.InterpretationServices;
import org.opensilex.graphimport.interpret.Interpreter;
import org.opensilex.graphimport.interpret.alias.LearnedAliases;
import org.opensilex.graphimport.validate.GraphValidation;
import org.opensilex.graphimport.validate.ValidationReport;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.opensilex.graphimport.interpret.InterpretFixtures.GERMPLASM;

/**
 * Workbooks built with POI, read by the platform's reader, interpreted and turned into a graph, with
 * an instance that holds a few germplasm.
 *
 * @author Arnaud Charleroy
 */
public final class GraphFixtures {

    public static final URI SYRAH = URI.create("http://test.opensilex.org/id/germplasm/syrah");
    public static final URI SYRAH_CLONE = URI.create("http://test.opensilex.org/id/germplasm/syrah-470");

    private GraphFixtures() {
    }

    /** One sheet: its name, its header and its rows, as text cells; null for an empty cell. */
    public record SheetSpec(String name, List<String> header, List<List<String>> rows) {

        public static SheetSpec of(String name, List<String> header, String[]... rows) {
            return new SheetSpec(name, header, Arrays.stream(rows).map(Arrays::asList).toList());
        }
    }

    /** Writes the sheets into an .xlsx file and reads it back as the platform does. */
    public static WorkbookStructure workbook(SheetSpec... sheets) throws Exception {
        File file = Files.createTempFile("graph-import", ".xlsx").toFile();
        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file.toPath())) {
            for (SheetSpec spec : sheets) {
                Sheet sheet = workbook.createSheet(spec.name());
                write(sheet.createRow(0), spec.header());
                for (int index = 0; index < spec.rows().size(); index++) {
                    write(sheet.createRow(index + 1), spec.rows().get(index));
                }
            }
            workbook.write(out);
        }
        try {
            return SheetSources.read(file, "parcelles.xlsx");
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    private static void write(Row row, List<String> cells) {
        for (int index = 0; index < cells.size(); index++) {
            if (cells.get(index) != null) {
                row.createCell(index).setCellValue(cells.get(index));
            }
        }
    }

    /**
     * An instance with the germplasm Syrah, and two germplasm both named "Grenache"; "Syrha" is close
     * to Syrah.
     */
    public static InstanceIndex instance() {
        return new InstanceIndex() {
            @Override
            public Map<String, Set<URI>> lookUp(URI expectedClass, Collection<String> names) {
                Map<String, Set<URI>> found = new LinkedHashMap<>();
                if (!GERMPLASM.equals(expectedClass)) {
                    return found;
                }
                for (String name : names) {
                    String key = InstanceIndex.key(name);
                    if (key.equals("syrah")) {
                        found.put(key, Set.of(SYRAH));
                    } else if (key.equals("grenache")) {
                        found.put(key, new LinkedHashSet<>(List.of(SYRAH_CLONE, SYRAH)));
                    } else if (key.equals("p1")) {
                        found.put(key, Set.of(SYRAH_CLONE));
                    }
                }
                return found;
            }

            @Override
            public Map<String, List<Suggestion>> near(URI expectedClass, Collection<String> names) {
                Map<String, List<Suggestion>> close = new LinkedHashMap<>();
                for (String name : names) {
                    if (GERMPLASM.equals(expectedClass) && InstanceIndex.key(name).equals("syrha")) {
                        close.put("syrha", List.of(new Suggestion(SYRAH, null, "Syrah", 0.8)));
                    }
                }
                return close;
            }
        };
    }

    /** Interprets the workbook with the decisions, then builds its graph. */
    public static ImportGraph graph(InterpretationServices services, WorkbookStructure workbook,
                                    ImportDecisions decisions) throws Exception {
        Interpretation interpretation = new Interpreter(services, new GraphImportModule().getEvidenceSources())
                .interpret(workbook, decisions, null);
        return new GraphBuilder(services).build(workbook, interpretation, decisions);
    }

    /** Checks the graph with the module's validators. */
    public static ValidationReport validate(InterpretationServices services, ImportGraph graph) throws Exception {
        return GraphValidation.run(graph, new GraphImportModule().getGraphValidators(), services);
    }

    public static InterpretationServices services() {
        return InterpretFixtures.services(instance(), LearnedAliases.NONE);
    }
}
