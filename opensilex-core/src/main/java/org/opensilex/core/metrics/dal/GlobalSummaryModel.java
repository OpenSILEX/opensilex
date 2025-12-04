//******************************************************************************
//                          GlobalSummaryModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.dal;

import java.net.URI;
import java.time.Instant;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * Global GlobalSummaryModel that regroups information about :
 * OS
 * Devices
 * GeneticResource
 * Variables
 * Each one contains at least one item
 * @author Arnaud Charleroy
 */
abstract public class GlobalSummaryModel extends MongoModel implements Comparable {

    private CountListItemModel scientificObjectsByType;
    public static final String SCIENTIFIC_OBJECT_BY_TYPE_FIELD = "scientificObjectsByType";
  
    private CountListItemModel geneticResourceByType;
    public static final String GENETIC_RESOURCE_TYPE_FIELD = "geneticResourceByType"; 

    private CountListItemModel dataByVariables;
    public static final String DATA_BY_VARIABLES = "dataByVariables";

    private Instant creationDate;
    public static final String CREATION_DATE_FIELD = "creationDate";
    
    private URI user;

    protected String summaryType;
    public static final String SUMMARY_TYPE_FIELD = "summaryType";

    public String getSummaryType() {
        return summaryType;
    }

    public GlobalSummaryModel() {
        this.creationDate = Instant.now();
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    public URI getUser() {
        return user;
    }

    public void setUser(URI user) {
        this.user = user;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public CountListItemModel getScientificObjectsByType() {
        return scientificObjectsByType;
    }

    public void setScientificObjectsByType(CountListItemModel scientificObjectsByType) {
        this.scientificObjectsByType = scientificObjectsByType;
    }

    public CountListItemModel getDataByVariables() {
        return dataByVariables;
    }

    public void setDataByVariables(CountListItemModel dataByVariables) {
        this.dataByVariables = dataByVariables;
    }
  
    public CountListItemModel getGeneticResourceByType() {
        return geneticResourceByType;
    }

    public void setGeneticResourceByType(CountListItemModel geneticResourceByType) {
        this.geneticResourceByType = geneticResourceByType;
    }
}
