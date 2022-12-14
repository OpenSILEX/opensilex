//******************************************************************************
//                          ExperimentDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.dataverse.api;

import org.opensilex.core.experiment.dal.ExperimentModel;

import java.net.URI;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

/**
 * @author Gabriel Besombes
 */
public class DataverseAPIPostDatasetDTO {

    public String topic;
    public Date productionDate;
    protected URI uri; // this is for opensilex's internal use only and doesn't fit in the dataverse schema

    protected Licence datasetLicence; // TODO : set this to constant to begin with

    protected String name;

    public DataverseAPIPostDatasetDTO(ExperimentModel experimentModel){
        this.name = experimentModel.getName();
        this.uri = experimentModel.getUri();
        this.topic = experimentModel.getObjective();
        this.productionDate = Date.from(
                experimentModel.getStartDate()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
        );
    }

    static class Licence {

        protected String name;

        protected URI uri;

        public Licence(String name, URI uri){
            this.name = name;
            this.uri = uri;
        }
    }

    static class MetadataBlock {

        protected String name;

        protected String displayName;

        protected ArrayList<Field> field;

        // TODO : the constructor for this is a bit harder. Need to know how flexible the content of this class should be

        static class Field {

            protected String typeName;

            protected Boolean multiple;

            protected String typeClass; // TODO : should probably set a list of possible values for this

            // TODO : for value there are different possible types (either list or not depending on the value of multiple)

            public Field(String typeName, Boolean multiple, String typeClass) {
                this.typeName = typeName;
                this.multiple = multiple;
                this.typeClass = typeClass;
            }
        }
    }
}
