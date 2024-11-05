package org.opensilex.core.data.bll;

import org.opensilex.core.data.api.DataExportDTO;
import org.opensilex.core.experiment.utils.ExportDataIndex;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class DataWideExportInformation extends DataExportInformation{
    private Map<Instant, Map<ExportDataIndex, List<DataExportDTO>>> dataByIndexAndInstant;

    public Map<Instant, Map<ExportDataIndex, List<DataExportDTO>>> getDataByIndexAndInstant() {
        return dataByIndexAndInstant;
    }

    public void setDataByIndexAndInstant(Map<Instant, Map<ExportDataIndex, List<DataExportDTO>>> dataByIndexAndInstant) {
        this.dataByIndexAndInstant = dataByIndexAndInstant;
    }
}
