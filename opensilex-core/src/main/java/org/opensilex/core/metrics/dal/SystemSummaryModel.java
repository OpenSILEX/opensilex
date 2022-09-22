//******************************************************************************
//                          SystemSummaryModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.dal;

import org.opensilex.nosql.mongodb.MongoModel;

/**
 * Represent a system metric model
 * @author Arnaud Charleroy
 */
public class SystemSummaryModel extends GlobalSummaryModel {

    private CountListItemModel deviceByType;
    public static final String DEVICE_BY_TYPE_FIELD = "deviceByType";

    private Integer facilities;

    private Integer factors;

    public static String SUMMARY_TYPE = "system";

    private String baseSystemAlias;

    public SystemSummaryModel() {
        this.summaryType = SUMMARY_TYPE;
    }

    public void setBaseSystemAlias(String baseSystemAlias) {
        this.baseSystemAlias = baseSystemAlias;
    }

    public String getBaseSystemAlias() {
        return baseSystemAlias;
    }

    public Integer getFacilities() {
        return facilities;
    }

    public void setFacilities(Integer facilities) {
        this.facilities = facilities;
    }

    public Integer getFactors() {
        return factors;
    }

    public void setFactors(Integer factors) {
        this.factors = factors;
    }

    public CountListItemModel getDeviceByType() {
        return deviceByType;
    }

    public void setDeviceByType(CountListItemModel deviceByType) {
        this.deviceByType = deviceByType;
    }

    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{
            String.valueOf(System.currentTimeMillis())
        };
    }

    @Override
    public int compareTo(Object t) {
        return this.getCreationDate().isAfter(((GlobalSummaryModel) t).getCreationDate()) ? 1 : 0;
    }

}
