package org.opensilex.core.metrics.dal;

public class SystemSummarySearchFilter extends GlobalSummarySearchFilter {

    @Override
    public String getType() {
        return SystemSummaryModel.SUMMARY_TYPE;
    }

}
