package org.opensilex.core.data.dal;


import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.sparql.ontology.dal.OntologyDAO;

public class DAOContext {
    ExperimentDAO experimentDAO;
    OntologyDAO ontologyDAO;
    ScientificObjectDAO scientificObjectDAO;
    DeviceDAO deviceDAO;
    VariableDAO variableDAO;

    public VariableDAO getVariableDAO() {
        return variableDAO;
    }

    public void setVariableDAO(VariableDAO variableDAO) {
        this.variableDAO = variableDAO;
    }

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

    public static DAOContext buildDaoContext(DeviceDAO deviceDAO, OntologyDAO ontologyDAO, ExperimentDAO experimentDAO,
                                             ScientificObjectDAO scientificObjectDAO, VariableDAO variableDAO) {
        DAOContext daoContext = new DAOContext();
        daoContext.setDeviceDAO(deviceDAO);
        daoContext.setOntologyDAO(ontologyDAO);
        daoContext.setExperimentDAO(experimentDAO);
        daoContext.setScientificObjectDAO(scientificObjectDAO);
        daoContext.setVariableDAO(variableDAO);
        return daoContext;
    }
}
