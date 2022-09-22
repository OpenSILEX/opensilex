//******************************************************************************
//                          ExperimentSummaryModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.dal;

import java.net.URI;
import org.opensilex.nosql.mongodb.MongoModel;

/**
 * Represent a experiment metric model
 * @author Arnaud Charleroy
 */
public class ExperimentSummaryModel extends GlobalSummaryModel {

    private URI experimentUri;
    public static final String EXPERIMENT_FIELD = "experimentUri";

    public static final String SUMMARY_TYPE = "experiment";


    public ExperimentSummaryModel() {
        super();
        this.summaryType = SUMMARY_TYPE;
    }

    public void setExperimentUri(URI experimentUri) {
        this.experimentUri = experimentUri;
    }

    public URI getExperimentUri() {
        return experimentUri;
    }
    
   

    @Override
    public String[] getInstancePathSegments(MongoModel instance) {
        return new String[]{
            getExperimentUri().toString(),
            String.valueOf(System.currentTimeMillis())
        };
    }

    @Override
    public int compareTo(Object t) {
        return this.getCreationDate().isAfter(((ExperimentSummaryModel) t).getCreationDate()) ? 1 : 0;
    }

}
