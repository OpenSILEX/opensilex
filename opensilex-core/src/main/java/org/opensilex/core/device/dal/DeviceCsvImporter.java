package org.opensilex.core.device.dal;

import org.apache.commons.compress.utils.Sets;
import org.opensilex.sparql.csv.AbstractCsvImporter;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;

import java.util.Set;

/**
 * @author rcolin
 */
public class DeviceCsvImporter extends AbstractCsvImporter<DeviceModel> {


    public DeviceCsvImporter(SPARQLService sparql) throws SPARQLException {
        super(sparql, DeviceModel.class, sparql.getDefaultGraphURI(DeviceModel.class), DeviceModel::new);
    }

    @Override
    public Set<String> getCustomColumns() {
        return Sets.newHashSet("position");
    }
}
