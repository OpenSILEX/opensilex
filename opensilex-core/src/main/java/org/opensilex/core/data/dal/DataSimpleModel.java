//******************************************************************************
//                          DataModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import org.opensilex.nosql.mongodb.MongoModel;

import java.time.Instant;
import java.util.UUID;

/**
 *
 * @author brice
 */
public class DataSimpleModel extends MongoModel {

    private DataSimpleProvenanceModel provenance;

    private Instant date;
    
    private Boolean isDateTime;
    
    private String offset;

    public static final String IS_VALUE_DATE_FIELD = "isValueDate";

    private Object value;
    public static final String VALUE_FIELD = "value";


    public DataSimpleProvenanceModel getProvenance() {
        return provenance;
    }

    public void setProvenance(DataSimpleProvenanceModel provenance) {
        this.provenance = provenance;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public Boolean getIsDateTime() {
        return isDateTime;
    }

    public void setIsDateTime(Boolean isDateTime) {
        this.isDateTime = isDateTime;
    }
    
    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
           
    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    } 

}
