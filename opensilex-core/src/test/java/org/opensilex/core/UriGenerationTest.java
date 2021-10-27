package org.opensilex.core;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.XSD;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.core.area.dal.AreaModel;
import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.experiment.factor.dal.FactorModel;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.core.organisation.dal.InfrastructureModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variable.dal.*;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import javax.mail.internet.InternetAddress;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;

public class UriGenerationTest extends AbstractMongoIntegrationTest {


    private String getOpensilexBaseURI()throws OpenSilexModuleNotFoundException {
        return getOpensilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class).baseURI();
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
        String expectedUri = getOpensilexBaseURI()+"id/organization/facilities.name";
        Assert.assertEquals(model.getUri().toString(),expectedUri);
    }

    @Test
    public void testVariable() throws Exception {

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
        model.setName("entity_characteristic_method_unit");
        sparql.create(model);

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


    }

    @Test
    public void testAnnotations() throws Exception {


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


    }

    @Test
    public void testData() throws Exception {


    }
}
