
// TODO
///*
// * *******************************************************************************
// *                     MetricsDAOTest.java
// * OpenSILEX
// * Copyright © INRAE 2020
// * Creation date: September 28, 2020
// * Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
// * *******************************************************************************
// */
//package org.opensilex.core.metrics.dal;
//
//import org.junit.Test;
//import org.opensilex.core.AbstractMongoIntegrationTest;
//import org.opensilex.nosql.mongodb.MongoDBService;
//import org.opensilex.utils.ListWithPagination;
//
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.time.LocalDate;
//import java.util.Arrays;
//import java.util.List;
//import org.apache.jena.arq.querybuilder.SelectBuilder;
//import org.apache.jena.arq.querybuilder.UpdateBuilder;
//import org.apache.jena.graph.Node;
//import org.apache.jena.graph.Triple;
//import org.apache.jena.vocabulary.RDFS;
//import org.opensilex.core.data.dal.DataDAO;
//import org.opensilex.core.data.dal.DataModel;
//import org.opensilex.core.data.dal.DataProvenanceModel;
//import org.opensilex.core.experiment.dal.ExperimentDAO;
//import org.opensilex.core.experiment.dal.ExperimentModel;
//import org.opensilex.core.metrics.schedule.ScheduleMetrics;
//import org.opensilex.core.ontology.Oeso;
//import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
//import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
//import org.opensilex.core.variable.api.VariableCreationDTO;
//import org.opensilex.core.variable.dal.CharacteristicModel;
//import org.opensilex.core.variable.dal.EntityModel;
//import org.opensilex.core.variable.dal.MethodModel;
//import org.opensilex.core.variable.dal.UnitModel;
//import org.opensilex.core.variable.dal.VariableDAO;
//import org.opensilex.core.variable.dal.VariableModel;
//import org.opensilex.security.account.dal.AccountModel;
//import org.opensilex.sparql.deserializer.SPARQLDeserializers;
//import org.opensilex.sparql.service.SPARQLQueryHelper;
//import org.opensilex.sparql.service.SPARQLResult;
//import org.opensilex.sparql.service.SPARQLService;
//
///**
// * @author Arnaud Charleroy
// */
//public class MetricsDAOTest extends AbstractMongoIntegrationTest {
//
//    public VariableModel getVariableModel() throws Exception {
//
//        SPARQLService service = getSparqlService();
//
//        EntityModel entity = new EntityModel();
//        entity.setName("Artemisia absinthium");
//        entity.setDescription("A plant which was used in the past for building methanol");
//
//        service.create(entity);
//
//        CharacteristicModel characteristic = new CharacteristicModel();
//        characteristic.setName("size");
//        characteristic.setDescription("The size of an object");
//        service.create(characteristic);
//
//        MethodModel method = new MethodModel();
//        method.setName("SVM");
//        method.setDescription("A machine learning based method");
//        service.create(method);
//
//        UnitModel unit = new UnitModel();
//        unit.setName("minute");
//        unit.setDescription("I really need to comment it ?");
//        unit.setSymbol("m");
//        unit.setAlternativeSymbol("mn");
//        service.create(unit);
//
//        VariableModel variableModel = new VariableModel();
//        variableModel.setName(entity.getName() + characteristic.getName());
//        variableModel.setAlternativeName(variableModel.getName() + method.getName() + unit.getName());
//        variableModel.setDescription("A comment about a variable");
//
//        variableModel.setEntity(entity);
//        variableModel.setCharacteristic(characteristic);
//        variableModel.setMethod(method);
//        variableModel.setUnit(unit); 
//        variableModel.setDataType(new URI("xsd:decimal"));
//
//        return variableModel;
//    }
//    
//    @Test
//    public void testCreate() throws URISyntaxException, Exception {
//        MongoDBService mongo = getMongoDBService();
//        SPARQLService sparql = getSparqlService();
//
//        ExperimentModel xp = new ExperimentModel();
//        xp.setName("xp");
//
//        LocalDate currentDate = LocalDate.now();
//        xp.setStartDate(currentDate.minusDays(3));
//        xp.setEndDate(currentDate.plusDays(3));
//        xp.setObjective("Objective");
//
//        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, mongo);
//        xp = experimentDAO.create(xp);
//
//        ScientificObjectModel scModel = new ScientificObjectModel();
//        scModel.setUri(new URI("xp:obj"));
//        scModel.setName("test");
//        scModel.setType(new URI(Oeso.NS + "Berry"));
//
//        UpdateBuilder updateBuilder = new UpdateBuilder();
//
//        Node nodeURI = SPARQLDeserializers.nodeURI(new URI(Oeso.NS + "Berry"));
//
//        Triple insertType = new Triple(nodeURI, RDFS.subClassOf.asNode(), Oeso.ScientificObject.asNode());
//        Triple insertClassType = new Triple(nodeURI, RDFS.Class.asNode(), RDFS.Class.asNode());
//
//        updateBuilder.addInsert(insertType);
//        updateBuilder.addInsert(insertClassType);
//
//        sparql.executeUpdateQuery(updateBuilder);
// 
//        SelectBuilder select = new SelectBuilder();
//        
//        Node uri = SPARQLQueryHelper.makeVar("?uri");
//        Triple selectType = new Triple(uri, RDFS.subClassOf.asNode(), Oeso.ScientificObject.asNode());
//        select.addVar(uri);
//        
//        select.addWhere(selectType); 
//        List<SPARQLResult> executeSelectQuery = sparql.executeSelectQuery(select);
//        
//        VariableDAO  varDAO =  new VariableDAO(sparql, mongo, null);
//         
//        VariableModel varModel = getVariableModel();
//        DataDAO dataDAO = new DataDAO(mongo, sparql, null);
//        DataModel data = new DataModel();
//        DataProvenanceModel dataProv = new DataProvenanceModel();
//        dataProv.setExperiments(Arrays.asList(xp.getUri()));
//        data.setProvenance(dataProv);
//        dataDAO.create(data);
//        
//        
//        ScientificObjectDAO scDAO = new ScientificObjectDAO(sparql, mongo);
//                sparql.create( SPARQLDeserializers.nodeURI(xp.getUri()), scModel);
//        ScientificObjectModel objectByURI = scDAO.getObjectByURI(new URI("xp:obj"),xp.getUri(),opensilex.getDefaultLanguage());
////        scDAO.create(xp.getUri(), xp, scModel.getType(), null, scModel.getName(), null, AccountModel.getAnonymous());
//
//        ScheduleMetrics scheduleMetrics = new ScheduleMetrics();
//
//        MetricsDAO metricsDAO = new MetricsDAO(sparql, mongo);
//        metricsDAO.createExperimentSummary();
//
//        ListWithPagination<ExperimentSummaryModel> experimentSummaries = metricsDAO.getExperimentSummaries(Arrays.asList(xp.getUri()), null, null, 0, 1);
//
//        int total = experimentSummaries.getTotal();
//    }
//
//}
