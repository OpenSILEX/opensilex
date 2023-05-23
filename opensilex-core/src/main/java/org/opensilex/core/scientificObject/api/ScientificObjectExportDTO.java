package org.opensilex.core.scientificObject.api;

import org.opensilex.core.scientificObject.dal.ScientificObjectSearchFilter;

/**
 * Specialization of {@link ScientificObjectSearchFilter} in case of CSV export.
 * By default, the pageSize is set to 0, to ensure that the pageSize is set during the API call (don't rely on default pageSize value here).
 * - It allow to perform an export-all without specifying some pageSize
 * - It ensures that, in case of selected URIs, the pageSize value is not in conflict with the number of selected URIs
 *
 * @author rcolin
 */
public class ScientificObjectExportDTO extends ScientificObjectSearchFilter {
    public ScientificObjectExportDTO() {
        super();
        this.setPageSize(0); // by default set not page size, in case of export all
    }
}
