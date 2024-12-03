package org.opensilex.core.dataV2.model;


import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.sparql.ontology.dal.OntologyDAO;

public class DAOContext {
    ExperimentDAO experimentDAO;
    OntologyDAO ontologyDAO;
    ScientificObjectDAO scientificObjectDAO;
    DeviceDAO deviceDAO;

    public ExperimentDAO getExperimentDAO() {
        return experimentDAO;
    }

    public void setExperimentDAO(ExperimentDAO experimentDAO) {
        this.experimentDAO = experimentDAO;
    }

    public OntologyDAO getOntologyDAO() {
        return ontologyDAO;
    }

    public void setOntologyDAO(OntologyDAO ontologyDAO) {
        this.ontologyDAO = ontologyDAO;
    }

    public ScientificObjectDAO getScientificObjectDAO() {
        return scientificObjectDAO;
    }

    public void setScientificObjectDAO(ScientificObjectDAO scientificObjectDAO) {
        this.scientificObjectDAO = scientificObjectDAO;
    }

    public DeviceDAO getDeviceDAO() {
        return deviceDAO;
    }

    public void setDeviceDAO(DeviceDAO deviceDAO) {
        this.deviceDAO = deviceDAO;
    }

    public static DAOContext buildDaoContext(DeviceDAO deviceDAO, OntologyDAO ontologyDAO, ExperimentDAO experimentDAO, ScientificObjectDAO scientificObjectDAO) {
        DAOContext daoContext = new DAOContext();
        daoContext.setDeviceDAO(deviceDAO);
        daoContext.setOntologyDAO(ontologyDAO);
        daoContext.setExperimentDAO(experimentDAO);
        daoContext.setScientificObjectDAO(scientificObjectDAO);
        return daoContext;
    }
}
