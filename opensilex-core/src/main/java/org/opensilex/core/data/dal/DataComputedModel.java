//******************************************************************************
//                          DataComputedModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import org.opensilex.nosql.mongodb.MongoModel;

import java.time.Instant;
import java.util.UUID;

/**
 * Provides a lightweight data model for data calculations.
 * Only keeps the fields date, value and provenance.
 * @author brice maussang
 */
public class DataComputedModel extends MongoModel {

    private DataProvenanceModel provenance;

    private Instant date;

    private Object value;


    public DataProvenanceModel getProvenance() {
        return provenance;
    }

    public void setProvenance(DataProvenanceModel provenance) {
        this.provenance = provenance;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
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
