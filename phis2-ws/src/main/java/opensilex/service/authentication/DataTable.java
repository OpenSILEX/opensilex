//******************************************************************************
//                                 DataTable.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: May 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr,
//         morgane.vidal@inra.fr
//******************************************************************************
package opensilex.service.authentication;

/**
 *
 * @author cherimon
 * @Update AC 05/16 Mise à jour de la connexion aux données
 */
public class DataTable {

    private String instanceNumber, observationVariableId, observationVariableDbId, season, observationValue, observationTimeStamp, collectionFacilityLabel, collector;

    public DataTable() {
    }

    public DataTable(String instanceNumber, String observationVariableId, String observationVariableDbId, String season, String observationValue, String observationTimeStamp, String collectionFacilityLabel, String collector) {
        this.instanceNumber = instanceNumber;
        this.observationVariableId = observationVariableId;
        this.observationVariableDbId = observationVariableDbId;
        this.season = season;
        this.observationValue = observationValue;
        this.observationTimeStamp = observationTimeStamp;
        this.collectionFacilityLabel = collectionFacilityLabel;
        this.collector = collector;
    }
    
    public void setInstanceNumber(String instanceNumber) {
        if (instanceNumber != null) {
            this.instanceNumber = instanceNumber;
        } else {
            this.instanceNumber = "";
        }
    }

    public void setObservationVariableId(String observationVariableId) {
        if (observationVariableId != null) {
            this.observationVariableId = observationVariableId;
        } else {
            this.observationVariableId = "";
        }
    }

    public void setObservationVariableDbId(String observationVariableDbId) {
        if (observationVariableDbId != null) {
            this.observationVariableDbId = observationVariableDbId;
        } else {
            this.observationVariableDbId = "";
        }
    }

    public void setSeason(String season) {
        if (season != null) {
            this.season = season;
        } else {
            this.season = "";
        }
    }

    public void setObservationValue(String observationValue) {
        if (observationValue != null) {
            this.observationValue = observationValue;
        } else {
            this.observationValue = "";
        }
    }

    public void setObservationTimeStamp(String observationTimeStamp) {
        if (observationTimeStamp != null) {
            this.observationTimeStamp = observationTimeStamp;
        } else {
            this.observationTimeStamp = "";
        }
    }

    public void setCollectionFacilityLabel(String collectionFacilityLabel) {
        if (collectionFacilityLabel != null) {
            this.collectionFacilityLabel = collectionFacilityLabel;
        } else {
            this.collectionFacilityLabel = "";
        }
    }

    public void setCollector(String collector) {
        if (collector != null) {
            this.collector = collector;
        } else {
            this.collector = "";
        }
    }

    public String getInstanceNumber() {
        return instanceNumber;
    }

    public String getObservationVariableId() {
        return observationVariableId;
    }

    public String getObservationVariableDbId() {
        return observationVariableDbId;
    }

    public String getSeason() {
        return season;
    }

    public String getObservationValue() {
        return observationValue;
    }

    public String getObservationTimeStamp() {
        return observationTimeStamp;
    }

    public String getCollectionFacilityLabel() {
        return collectionFacilityLabel;
    }

    public String getCollector() {
        return collector;
    }

    
    
    

//    public void fillDataTable(ResultSet resultat) throws SQLException {
//        setInstanceNumber(resultat.getString("date") + ":" + resultat.getString("sensor") + ":" + resultat.getString("codeVariable"));
//        setObservationVariableId(resultat.getString("codeVariable"));
//        setObservationVariableDbId(resultat.getString("codeVariable"));
//        setSeason(resultat.getString("season"));
//        setObservationValue(resultat.getString("value"));
//        setObservationTimeStamp(resultat.getString("date"));
//        setCollectionFacilityLabel(resultat.getString("site"));
//        setCollector(resultat.getString("sensor"));
//    }
}
