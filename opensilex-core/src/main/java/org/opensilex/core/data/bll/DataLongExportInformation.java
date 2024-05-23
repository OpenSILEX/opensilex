package org.opensilex.core.data.bll;

import org.opensilex.core.data.api.DataGetDTO;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

public class DataLongExportInformation extends DataExportInformation{
    private HashMap<Instant, List<DataGetDTO>> dataByInstant;

    public HashMap<Instant, List<DataGetDTO>> getDataByInstant() {
        return dataByInstant;
    }

    public void setDataByInstant(HashMap<Instant, List<DataGetDTO>> dataByInstant) {
        this.dataByInstant = dataByInstant;
    }
}
