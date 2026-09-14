//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.star;

import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reconciles the plot identifiers of the design sheet with those of the observation sheets.
 * <p>
 * STAR writes them two ways. The plot sheet composes them as block then treatment — {@code A1},
 * {@code A10}, {@code C2} — while the data sheets compose them as treatment then block —
 * {@code 1A}, {@code 10A}, {@code 2C}. Nothing matches, so no observation can be attached.
 * <p>
 * This is not one workbook's mistake: the reference template does it too, so it is the format that
 * makes its own two sheets disagree. Worth saying plainly to whoever maintains the standard, and
 * worth reconciling rather than refusing, since recomposing the plot sheet's treatment and block
 * resolves every plot.
 * <p>
 * Nothing is applied silently. The reconciliation is offered, described, and used only once
 * confirmed — a measurement filed against the wrong plot is worse than a measurement not filed.
 *
 * @author Arnaud Charleroy
 */
public class PlotIdReconciliation {

    public static final String COLUMN_PLOT = "plot_id";
    public static final String COLUMN_TREATMENT = "xp_trt_code";
    public static final String COLUMN_BLOCK = "block_code";

    /**
     * Data identifier, lowercased, to the plot identifier of the design sheet.
     */
    private final Map<String, String> dataToPlot = new LinkedHashMap<>();

    private final Set<String> unmatchedDataIds = new LinkedHashSet<>();

    private int directMatches;
    private int recomposedMatches;
    private int declaredPlots;

    /**
     * @param plotSheet  the design sheet declaring the plots
     * @param dataSheets the observation sheets referring to them
     */
    public PlotIdReconciliation(SheetStructure plotSheet, List<SheetStructure> dataSheets) {
        if (plotSheet == null) {
            return;
        }

        Set<String> declared = new LinkedHashSet<>();
        Map<String, String> byRecomposition = new LinkedHashMap<>();
        for (List<String> row : plotSheet.getRows()) {
            String plot = plotSheet.cell(row, COLUMN_PLOT);
            if (plot.isEmpty()) {
                continue;
            }
            declared.add(plot.toLowerCase(Locale.ROOT));

            String treatment = plotSheet.cell(row, COLUMN_TREATMENT);
            String block = plotSheet.cell(row, COLUMN_BLOCK);
            if (!treatment.isEmpty() && !block.isEmpty()) {
                byRecomposition.put((treatment + block).toLowerCase(Locale.ROOT), plot);
            }
        }
        declaredPlots = declared.size();

        for (String used : distinctPlotIds(dataSheets)) {
            String key = used.toLowerCase(Locale.ROOT);
            if (declared.contains(key)) {
                dataToPlot.put(key, used);
                directMatches++;
            } else if (byRecomposition.containsKey(key)) {
                dataToPlot.put(key, byRecomposition.get(key));
                recomposedMatches++;
            } else {
                unmatchedDataIds.add(used);
            }
        }
    }

    private Set<String> distinctPlotIds(List<SheetStructure> dataSheets) {
        Set<String> used = new LinkedHashSet<>();
        if (dataSheets == null) {
            return used;
        }
        for (SheetStructure sheet : dataSheets) {
            used.addAll(sheet.distinctValues(COLUMN_PLOT));
        }
        return used;
    }

    /**
     * @return the plot identifier of the design sheet for an identifier used in the data, or
     * {@code null} when it matches nothing
     */
    public String resolve(String dataPlotId) {
        return dataPlotId == null ? null : dataToPlot.get(dataPlotId.toLowerCase(Locale.ROOT));
    }

    /**
     * @return true when the data sheets use a different convention from the plot sheet, which is
     * what makes a reconciliation necessary rather than merely possible
     */
    public boolean isNeeded() {
        return recomposedMatches > 0;
    }

    /**
     * @return true when every identifier used in the data resolves to a declared plot
     */
    public boolean isComplete() {
        return unmatchedDataIds.isEmpty() && !dataToPlot.isEmpty();
    }

    public int getDirectMatches() {
        return directMatches;
    }

    public int getRecomposedMatches() {
        return recomposedMatches;
    }

    public int getResolvedCount() {
        return dataToPlot.size();
    }

    public int getDeclaredPlots() {
        return declaredPlots;
    }

    public List<String> getUnmatchedDataIds() {
        return new ArrayList<>(unmatchedDataIds);
    }

    /**
     * @return what the reconciliation would do, in one sentence for the user, or {@code null} when
     * there is nothing to reconcile
     */
    public String describe() {
        if (!isNeeded()) {
            return null;
        }
        StringBuilder description = new StringBuilder()
                .append("The plot sheet writes its identifiers as block then treatment (A1, A10) ")
                .append("while the data sheets write them as treatment then block (1A, 10A), so no ")
                .append("observation matches a plot as things stand. Recomposing the plot sheet's ")
                .append("treatment and block resolves ")
                .append(getResolvedCount()).append(" of ").append(getResolvedCount() + unmatchedDataIds.size())
                .append(" identifiers");
        if (directMatches > 0) {
            description.append(", ").append(directMatches)
                    .append(" of which already matched directly");
        }
        description.append(". The reference template has the same mismatch, so this is the STAR ")
                .append("model making its two sheets disagree, not an error in your data. Confirm ")
                .append("and the observations will be attached accordingly.");
        return description.toString();
    }
}
