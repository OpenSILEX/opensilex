package org.opensilex.core;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.XSD;
import org.junit.Assert;
import org.junit.Test;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.annotation.dal.MotivationModel;
import org.opensilex.core.area.dal.AreaModel;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.logs.dal.LogModel;
import org.opensilex.core.logs.dal.LogsDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.provenance.dal.ProvenanceDAO;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variable.dal.*;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.mail.internet.InternetAddress;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class UriGenerationTest extends AbstractMongoIntegrationTest {


    /**
     * Pattern used to check if some str has an {@link java.util.UUID} form.
     * @see <a href="https://www.code4copy.com/java/validate-uuid-string-java/">Validate UUID in JAVA</a>
     */
    private final static Pattern UUID_REGEX_PATTERN = Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

    private String getOpensilexBaseURI()throws OpenSilexModuleNotFoundException {
        return getOpensilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class).baseURI();
    }

    @Test
    public void testDefaultGraphs() throws OpenSilexModuleNotFoundException, SPARQLException {

        String graphPrefix = getOpensilexBaseURI()+"set/";

        SPARQLService sparql = getSparqlService();

        Assert.assertEquals(graphPrefix+"user",sparql.getDefaultGraph(UserModel.class).toString());
        Assert.assertEquals(graphPrefix+"group",sparql.getDefaultGraph(GroupModel.class).toString());
        Assert.assertEquals(graphPrefix+"profile",sparql.getDefaultGraph(ProfileModel.class).toString());

        Assert.assertEquals(graphPrefix+"project",sparql.getDefaultGraph(ProjectModel.class).toString());
        Assert.assertEquals(graphPrefix+"experiment",sparql.getDefaultGraph(ExperimentModel.class).toString());
        Assert.assertEquals(graphPrefix+"infrastructures",sparql.getDefaultGraph(InfrastructureModel.class).toString());
        Assert.assertEquals(graphPrefix+"infrastructures",sparql.getDefaultGraph(InfrastructureFacilityModel.class).toString());

        Assert.assertEquals(graphPrefix+"variable",sparql.getDefaultGraph(VariableModel.class).toString());
        Assert.assertEquals(graphPrefix+"variable",sparql.getDefaultGraph(EntityModel.class).toString());
        Assert.assertEquals(graphPrefix+"variable",sparql.getDefaultGraph(CharacteristicModel.class).toString());
        Assert.assertEquals(graphPrefix+"variable",sparql.getDefaultGraph(MethodModel.class).toString());
        Assert.assertEquals(graphPrefix+"variable",sparql.getDefaultGraph(UnitModel.class).toString());
        Assert.assertEquals(graphPrefix+"variablesGroup",sparql.getDefaultGraph(VariablesGroupModel.class).toString());

        Assert.assertEquals(graphPrefix+"germplasm",sparql.getDefaultGraph(SpeciesModel.class).toString());
        Assert.assertEquals(graphPrefix+"germplasm",sparql.getDefaultGraph(GermplasmModel.class).toString());
        Assert.assertEquals(graphPrefix+"factor",sparql.getDefaultGraph(FactorModel.class).toString());
        Assert.assertEquals(graphPrefix+"factor",sparql.getDefaultGraph(FactorLevelModel.class).toString());

        Assert.assertEquals(graphPrefix+"document",sparql.getDefaultGraph(DocumentModel.class).toString());
        Assert.assertEquals(graphPrefix+"event",sparql.getDefaultGraph(EventModel.class).toString());
        Assert.assertEquals(graphPrefix+"annotation",sparql.getDefaultGraph(AnnotationModel.class).toString());

        Assert.assertEquals(graphPrefix+"scientific-object",sparql.getDefaultGraph(ScientificObjectModel.class).toString());
        Assert.assertEquals(graphPrefix+"device",sparql.getDefaultGraph(DeviceModel.class).toString());
        Assert.assertEquals(graphPrefix+"area",sparql.getDefaultGraph(AreaModel.class).toString());

        // #TODO graph storage of these models is only handled into OntologyAPI, should find a proper to check graph
//        Assert.assertEquals(graphPrefix+"properties",sparql.getDefaultGraph(ClassModel.class).toString());
//        Assert.assertEquals(graphPrefix+"properties",sparql.getDefaultGraph(DatatypePropertyModel.class).toString());
//        Assert.assertEquals(graphPrefix+"properties",sparql.getDefaultGraph(ObjectPropertyModel.class).toString());
//        Assert.assertEquals(graphPrefix+"properties",sparql.getDefaultGraph(OwlRestrictionModel.class).toString());
    }

    @Test
    public void testUser() throws Exception {

        UserModel model = new UserModel();
        model.setEmail(new InternetAddress("UriGenerationTest@opensilex.com"));
        model.setFirstName("opensilex");
        model.setLastName("opensilex");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/user/opensilex.opensilex";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }


    @Test
    public void testProfile() throws Exception {

        ProfileModel model = new ProfileModel();
        model.setName("name");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/profile/name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testGroup() throws Exception {

        GroupModel model = new GroupModel();
        model.setName("name");
        model.setDescription("description");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/group/name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testProject() throws Exception {

        ProjectModel model = new ProjectModel();
        model.setName("name");
        model.setStartDate(LocalDate.now());

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/project/name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void textExperiment() throws Exception {

        ExperimentModel model = new ExperimentModel();
        model.setName("name");
        model.setStartDate(LocalDate.now());
        model.setObjective("objective");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/experiment/name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testInfra() throws Exception {

        InfrastructureModel model = new InfrastructureModel();
        model.setName("name");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/organization/name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testFacility() throws Exception {

        InfrastructureFacilityModel model = new InfrastructureFacilityModel();
        model.setName("name");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/organization/facility.name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    private VariableModel getVariable(String name) throws Exception {
        EntityModel entity = new EntityModel();
        entity.setName("entity");

        CharacteristicModel characteristic = new CharacteristicModel();
        characteristic.setName("characteristic");

        MethodModel method = new MethodModel();
        method.setName("method");

        UnitModel unit = new UnitModel();
        unit.setName("unit");

        SPARQLService sparql = getSparqlService();
        sparql.create(entity);
        sparql.create(characteristic);
        sparql.create(method);
        sparql.create(unit);

        VariableModel model = new VariableModel();
        model.setEntity(entity);
        model.setCharacteristic(characteristic);
        model.setMethod(method);
        model.setUnit(unit);
        model.setDataType(new URI(XSD.xint.getURI()));
        model.setName(name);
        return model;
    }

    @Test
    public void testVariable() throws Exception {

        VariableModel model = getVariable("entity_characteristic_method_unit");
        getSparqlService().create(model);

        String expectedUri = getOpensilexBaseURI()+"id/variable/entity_characteristic_method_unit";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testEntity() throws Exception {

        EntityModel model = new EntityModel();
        model.setName("name");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/variable/entity.name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testCharacteristic() throws Exception {

        CharacteristicModel model = new CharacteristicModel();
        model.setName("name");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/variable/characteristic.name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testMethod() throws Exception {

        MethodModel model = new MethodModel();
        model.setName("name");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/variable/method.name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testUnit() throws Exception {

        UnitModel model = new UnitModel();
        model.setName("name");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/variable/unit.name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testVariableGroup() throws Exception {

        VariablesGroupModel model = new VariablesGroupModel();
        model.setName("name");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/variablesGroup/name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testGermplasm() throws Exception {

        GermplasmModel model = new GermplasmModel();
        model.setType(new URI(Oeso.Variety.getURI()));
        model.setName("name");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/germplasm/variety.name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testSpecies() throws Exception {

        GermplasmModel model = new GermplasmModel();
        model.setLabel(new SPARQLLabel("name","en"));
        model.setType(new URI(Oeso.Species.getURI()));

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/germplasm/species.name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testFactor() throws Exception {

        ExperimentModel xp = new ExperimentModel();
        xp.setName("xp");
        xp.setStartDate(LocalDate.now());
        xp.setObjective("objective");
        getSparqlService().create(xp);

        FactorModel factor = new FactorModel();
        factor.setName("factor");
        factor.setDescription("description");
        factor.setExperiment(xp);

        getSparqlService().create(factor);
        String expectedFactorUri = getOpensilexBaseURI()+"id/factor/xp.factor";
        Assert.assertEquals(factor.getUri().toString(),expectedFactorUri);

        FactorLevelModel factorLevelModel = new FactorLevelModel();
        factorLevelModel.setName("level");
        factorLevelModel.setFactor(factor);
        getSparqlService().create(factorLevelModel);

        String expectedLevelUri = expectedFactorUri+".level";
        Assert.assertEquals(expectedLevelUri,factorLevelModel.getUri().toString());
    }


    @Test
    public void testScientificObject() throws Exception {

        ExperimentModel xp = new ExperimentModel();
        xp.setName("xp_name");
        xp.setStartDate(LocalDate.now());
        xp.setObjective("objective");

        getSparqlService().create(xp);

        ScientificObjectModel osInXp = new ScientificObjectModel();
        osInXp.setName("os_name");
        osInXp.setExperiment(xp);

        getSparqlService().create(NodeFactory.createURI(xp.getUri().toString()),osInXp);

        String expectedUriInXp = getOpensilexBaseURI()+"id/scientific-object/xp_name/so-os_name";
        Assert.assertEquals(osInXp.getUri().toString(),expectedUriInXp);

        ScientificObjectModel osOutOfXp = new ScientificObjectModel();
        osOutOfXp.setName("os_name_out_of_xp");
        getSparqlService().create(osOutOfXp);

        String expectedUriOutOfXp = getOpensilexBaseURI()+"id/scientific-object/so-os_name_out_of_xp";
        Assert.assertEquals(expectedUriOutOfXp,osOutOfXp.getUri().toString());
    }

    @Test
    public void testDocument() throws Exception {

        ProjectModel target = new ProjectModel();
        target.setName("target");
        target.setStartDate(LocalDate.now());
        getSparqlService().create(target);

        DocumentModel model = new DocumentModel();
        model.setTitle("title");
        model.setTargets(Collections.singletonList(target.getUri()));

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/document/title";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testEvent() throws Exception {

        ScientificObjectModel target = new ScientificObjectModel();
        target.setName("target_event");
        getSparqlService().create(target);

        EventModel model = new EventModel();
        model.setEnd(new InstantModel());
        model.getEnd().setDateTimeStamp(OffsetDateTime.now());
        model.setIsInstant(true);
        model.setTargets(Collections.singletonList(target.getUri()));
        getSparqlService().create(model);

        String expectedUriPrefix = getOpensilexBaseURI()+"id/event/";
        String uriStr = model.getUri().toString();
        Assert.assertTrue(uriStr.startsWith(expectedUriPrefix));

        String specificUriPart = uriStr.substring(expectedUriPrefix.length());
        Assert.assertTrue(UUID_REGEX_PATTERN.matcher(specificUriPart).matches());
    }

    @Test
    public void testInstant() throws Exception {

        InstantModel model = new InstantModel();
        model.setDateTimeStamp(OffsetDateTime.now());
        getSparqlService().create(model);

        String expectedUriPrefix = getOpensilexBaseURI()+"id/instant/";
        String uriStr = model.getUri().toString();
        Assert.assertTrue(uriStr.startsWith(expectedUriPrefix));

        String specificUriPart = uriStr.substring(expectedUriPrefix.length());
        Assert.assertTrue(UUID_REGEX_PATTERN.matcher(specificUriPart).matches());
    }

    @Test
    public void testAnnotations() throws Exception {

        ScientificObjectModel target = new ScientificObjectModel();
        target.setName("target_annotation");
        getSparqlService().create(target);

        AnnotationModel model = new AnnotationModel();
        model.setTargets(Collections.singletonList(target.getUri()));
        model.setCreated(OffsetDateTime.now());
        model.setMotivation(new MotivationModel());
        model.getMotivation().setUri(new URI(OA.describing.getURI()));
        model.setDescription("description");
        getSparqlService().create(model);

        String expectedUriPrefix = getOpensilexBaseURI()+"id/annotation/";
        String uriStr = model.getUri().toString();
        Assert.assertTrue(uriStr.startsWith(expectedUriPrefix));

        String specificUriPart = uriStr.substring(expectedUriPrefix.length());
        Assert.assertTrue(UUID_REGEX_PATTERN.matcher(specificUriPart).matches());
    }

    @Test
    public void testArea() throws Exception {

        AreaModel model = new AreaModel();
        model.setName("name");
        model.setDescription("description");

        getSparqlService().create(model);
        String expectedUri = getOpensilexBaseURI()+"id/area/name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testProvenance() throws Exception {
        ProvenanceDAO dao = new ProvenanceDAO(getMongoDBService(),getSparqlService());

        ProvenanceModel model = new ProvenanceModel();
        model.setName("name");
        dao.create(model);

        String expectedUri = getOpensilexBaseURI()+"id/provenance/name";
        Assert.assertEquals(expectedUri,model.getUri().toString());
    }

    @Test
    public void testData() throws Exception {

        VariableModel variable = getVariable("data_variable");
        getSparqlService().create(variable);

        DataDAO dao = new DataDAO(getMongoDBService(),getSparqlService(),null);

        DataModel model = new DataModel();
        model.setDate(Instant.now());
        model.setValue(5);
        model.setVariable(variable.getUri());
        dao.create(model);

        String expectedUriPrefix = getOpensilexBaseURI()+"id/data/";
        String uriStr = model.getUri().toString();
        Assert.assertTrue(uriStr.startsWith(expectedUriPrefix));
    }

    @Test
    public void testFile(){
        DataFileModel model = new DataFileModel();
    }


    @Test
    public void testLog() throws Exception {

        LogModel model = new LogModel();
        model.setDatetime(LocalDateTime.now());
        model.setRemoteAdress("127.0.0.1");
        model.setUserUri(new URI("opensilex:unknown_user_who_loves_opensilex"));

        LogsDAO dao = new LogsDAO(getMongoDBService());
        dao.create(model);

        String expectedUriPrefix = getOpensilexBaseURI()+"id/log/";
        String uriStr = model.getUri().toString();
        Assert.assertTrue(uriStr.startsWith(expectedUriPrefix));

        String specificUriPart = uriStr.substring(expectedUriPrefix.length());
        Assert.assertTrue(UUID_REGEX_PATTERN.matcher(specificUriPart).matches());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {

        // reset variables, since variable/entity/characteristic/etc are created multiple time
        return Collections.singletonList(VariableModel.class);
    }
}
