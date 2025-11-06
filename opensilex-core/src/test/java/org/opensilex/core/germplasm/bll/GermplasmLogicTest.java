/*
 * *****************************************************************************
 *                         GermplasmLogicTest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2025.
 * Last Modification: 09/09/2025 13:47
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.core.germplasm.bll;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Before;
import org.junit.Test;

import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Yvan Roux
 * Tthis tests should cover the every Germplasm buisness logic.
 * Most of this logic rules should be found in markdown files. See :
 * - opensilex-doc/src/main/resources/specs/germplasms_import.md
 */
public class GermplasmLogicTest extends TestSuite {

    /**
     * @see GermplasmLogicTest#initBeforeTest() for mocked attributes initialization
     */
    private SPARQLService sparqlMocked;
    private GermplasmDAO daoMocked;

    //#region success tests

    /**
     * No error should be raised.
     */
    @Test
    public void speciesCreateSuccess() throws Exception {
        URI uri = new URI("http://example.org/species/1");

        // dao return empty list, meaning no germplasm already exist with this URI
        when(daoMocked.checkExistence(List.of(uri))).thenReturn(Collections.emptyList());

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(getCorrectGermplasmSpecies(uri)), false);
        String errorMessage = String.format("this simple species should not raise any error. %s errors was found", multipleErrorObject.toDTO().errors.size());
        TestCase.assertFalse(errorMessage, multipleErrorObject.hasErrors());
    }

    @Test
    public void speciesUpdateSuccess() throws Exception {
        URI uri = new URI("http://example.org/species/1");

        // dao return list with the URI, meaning a germplasm already exist with this URI
        when(daoMocked.checkExistence(List.of(uri))).thenReturn(List.of(uri));

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(getCorrectGermplasmSpecies(uri)), true);
        String errorMessage = String.format("this simple species should not raise any error. %s errors was found", multipleErrorObject.toDTO().errors.size());
        TestCase.assertFalse(errorMessage, multipleErrorObject.hasErrors());
    }

    @Test
    public void varietyWithSpeciesCreateSuccess() throws Exception {
        URI speciesUri = new URI("http://example.org/species/1");

        when(daoMocked.checkExistence(List.of(speciesUri))).thenReturn(List.of(speciesUri));

        when(sparqlMocked.uriExists(new URI(Oeso.Species.getURI()), speciesUri)).thenReturn(true);

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel species = getCorrectGermplasmSpecies(speciesUri);
        GermplasmModel variety = getCorrectVariety(null, species);

        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(variety), false);
        String errorMessage = String.format("this simple variety should not raise any error. %s errors was found", multipleErrorObject.toDTO().errors.size());
        TestCase.assertFalse(errorMessage, multipleErrorObject.hasErrors());
    }

    @Test
    public void accessionWithVarietyCreateSuccess() throws Exception {
        URI speciesUri = new URI("http://example.org/species/1");
        URI varietyUri = new URI("http://example.org/variety/1");

        // Mocking species existence with the right type
        when(daoMocked.checkExistence(List.of(speciesUri))).thenReturn(List.of(speciesUri));
        when(sparqlMocked.uriExists(new URI(Oeso.Species.getURI()), speciesUri)).thenReturn(true);
        // Mocking variety existence with the right type
        when(daoMocked.checkExistence(List.of(varietyUri))).thenReturn(List.of(varietyUri));
        when(sparqlMocked.uriExists(new URI(Oeso.Variety.getURI()), varietyUri)).thenReturn(true);

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel species = getCorrectGermplasmSpecies(speciesUri);
        GermplasmModel variety = getCorrectVariety(varietyUri, species);
        GermplasmModel accession = getCorrectGermplasmAccession(null, null, variety);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(accession), false);
        String errorMessage = String.format("this simple accession should not raise any error. %s errors was found", multipleErrorObject.toDTO().errors.size());
        TestCase.assertFalse(errorMessage, multipleErrorObject.hasErrors());
    }

    /**
     * An accession can be created with only a species (no variety).
     */
    @Test
    public void accessionWithSpeciesCreateSuccess() throws Exception {
        URI speciesUri = new URI("http://example.org/species/1");

        // Mocking species existence with the right type
        when(daoMocked.checkExistence(List.of(speciesUri))).thenReturn(List.of(speciesUri));
        when(sparqlMocked.uriExists(new URI(Oeso.Species.getURI()), speciesUri)).thenReturn(true);

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel species = getCorrectGermplasmSpecies(speciesUri);
        GermplasmModel accession = getCorrectGermplasmAccession(null, species, null);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(accession), false);
        String errorMessage = String.format("this simple accession should not raise any error. %s errors was found", multipleErrorObject.toDTO().errors.size());
        TestCase.assertFalse(errorMessage, multipleErrorObject.hasErrors());
    }

    /**
     * it is possible to create a germplasm with a type different than species, variety or accession.
     * this type of germplasm should at list have one of those as parent (accession or variety or species).
     * tryIng with an accession and a variety (not the one that is linked to the accession).
     */
    @Test
    public void otherTypeWithAccessionAndSpeciesAndVarietyCreateSuccess() throws Exception {
        URI speciesUri = new URI("http://example.org/species/1");
        URI variety1Uri = new URI("http://example.org/variety/1");
        URI variety2Uri = new URI("http://example.org/variety/2");
        URI accessionUri = new URI("http://example.org/accession/1");
        URI otherTypeUri = new URI("http://example.org/otherType/1");
        URI otherType = new URI("http://example.org/ontology/OtherType");

        //Mocking new type existence
        when(daoMocked.isGermplasmType(otherType)).thenReturn(true);
        // Mocking species existence with the right type
        when(daoMocked.checkExistence(List.of(speciesUri))).thenReturn(List.of(speciesUri));
        when(sparqlMocked.uriExists(new URI(Oeso.Species.getURI()), speciesUri)).thenReturn(true);
        // Mocking variety1 existence with the right type
        when(daoMocked.checkExistence(List.of(variety1Uri))).thenReturn(List.of(variety1Uri));
        when(sparqlMocked.uriExists(new URI(Oeso.Variety.getURI()), variety1Uri)).thenReturn(true);
        // Mocking variety2 existence with the right type
        when(daoMocked.checkExistence(List.of(variety2Uri))).thenReturn(List.of(variety2Uri));
        when(sparqlMocked.uriExists(new URI(Oeso.Variety.getURI()), variety2Uri)).thenReturn(true);
        // Mocking accession existence with the right type
        when(daoMocked.checkExistence(List.of(accessionUri))).thenReturn(List.of(accessionUri));
        when(sparqlMocked.uriExists(new URI(Oeso.Accession.getURI()), accessionUri)).thenReturn(true);

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel species = getCorrectGermplasmSpecies(speciesUri);
        GermplasmModel variety1 = getCorrectVariety(variety1Uri, species);
        GermplasmModel variety2 = getCorrectVariety(variety2Uri, species);
        GermplasmModel accession = getCorrectGermplasmAccession(accessionUri, species, variety1);
        GermplasmModel otherTypeGermplasm = getCorrectGermplasmOtherType(otherTypeUri, otherType, species, variety2, accession);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(otherTypeGermplasm), false);
        String errorMessage = String.format("this simple germplasm with an other type should not raise any error. %s errors was found", multipleErrorObject.toDTO().errors.size());
        TestCase.assertFalse(errorMessage, multipleErrorObject.hasErrors());
    }

    /**
     * we once had a bug where creating a germplasm with only an accession and a species (no variety) was raising an error.
     */
    @Test
    public void otherTypeWithOnlyAccessionAndSpeciesCreateSuccess() throws Exception {
        URI speciesUri = new URI("http://example.org/species/1");
        URI accessionUri = new URI("http://example.org/accession/1");
        URI otherTypeUri = new URI("http://example.org/otherType/1");
        URI otherType = new URI("http://example.org/ontology/OtherType");

        //Mocking new type existence
        when(daoMocked.isGermplasmType(otherType)).thenReturn(true);
        // Mocking species existence with the right type
        when(daoMocked.checkExistence(List.of(speciesUri))).thenReturn(List.of(speciesUri));
        when(sparqlMocked.uriExists(new URI(Oeso.Species.getURI()), speciesUri)).thenReturn(true);
        // Mocking accession existence with the right type
        when(daoMocked.checkExistence(List.of(accessionUri))).thenReturn(List.of(accessionUri));
        when(sparqlMocked.uriExists(new URI(Oeso.Accession.getURI()), accessionUri)).thenReturn(true);

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel species = getCorrectGermplasmSpecies(speciesUri);
        GermplasmModel accession = getCorrectGermplasmAccession(accessionUri, species, null);
        GermplasmModel otherTypeGermplasm = getCorrectGermplasmOtherType(otherTypeUri, otherType, species, null, accession);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(otherTypeGermplasm), false);
        String errorMessage = String.format("this simple germplasm with an other type should not raise any error. %s errors was found", multipleErrorObject.toDTO().errors.size());
        TestCase.assertFalse(errorMessage, multipleErrorObject.hasErrors());
    }

    /**
     * we once had a bug where creating a germplasm with only an accession and a variety (no species) was raising an error.
     */
    @Test
    public void otherTypeWitheOnlyAccessionAndVarietyCreateSuccess() throws Exception {
        URI varietyUri = new URI("http://example.org/variety/1");
        URI accessionUri = new URI("http://example.org/accession/1");
        URI otherTypeUri = new URI("http://example.org/otherType/1");
        URI otherType = new URI("http://example.org/ontology/OtherType");

        //Mocking new type existence
        when(daoMocked.isGermplasmType(otherType)).thenReturn(true);
        // Mocking variety existence with the right type
        when(daoMocked.checkExistence(List.of(varietyUri))).thenReturn(List.of(varietyUri));
        when(sparqlMocked.uriExists(new URI(Oeso.Variety.getURI()), varietyUri)).thenReturn(true);
        // Mocking accession existence with the right type
        when(daoMocked.checkExistence(List.of(accessionUri))).thenReturn(List.of(accessionUri));
        when(sparqlMocked.uriExists(new URI(Oeso.Accession.getURI()), accessionUri)).thenReturn(true);

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel variety = getCorrectVariety(varietyUri, null);
        GermplasmModel accession = getCorrectGermplasmAccession(accessionUri, null, variety);
        GermplasmModel otherTypeGermplasm = getCorrectGermplasmOtherType(otherTypeUri, otherType, null, variety, accession);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(otherTypeGermplasm), false);
        String errorMessage = String.format("this simple germplasm with an other type should not raise any error. %s errors was found", multipleErrorObject.toDTO().errors.size());
        TestCase.assertFalse(errorMessage, multipleErrorObject.hasErrors());
    }
    //#endregion

    //#region error tests

    @Test
    public void speciesCreateWithURIAlreadyUsedError() throws Exception {
        URI uri = new URI("http://example.org/species/1");

        // dao return list with the URI, meaning a germplasm already exist with this URI
        when(daoMocked.checkExistence(List.of(uri))).thenReturn(List.of(uri));

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(getCorrectGermplasmSpecies(uri)), false);
        String errorMessage = "this species should raise an error because the URI is already used";
        TestCase.assertTrue(errorMessage, multipleErrorObject.hasErrors());
        errorMessage = "this germplasm with an other type should raise only one error. Instead " + multipleErrorObject.toDTO().errors.get(0).errors.size() + " were found";
        TestCase.assertEquals(errorMessage, 1, multipleErrorObject.toDTO().errors.get(0).errors.size());
        String errorDetail = multipleErrorObject.toDTO().errors.get(0).errors.get(0);
        errorMessage = String.format("the error message should contains the message 'URI already exist'. But we instead it was : %s", errorDetail);
        TestCase.assertTrue(errorMessage, errorDetail.toLowerCase().contains("uri already exist"));
    }

    @Test
    public void speciesCreateWithMalformedUriError() throws Exception {
        URI malformedUri = new URI("malformed/uri");

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel species = getCorrectGermplasmSpecies(malformedUri);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(species), false);
        String errorMessage = "this species should raise an error because the URI is malformed";
        TestCase.assertTrue(errorMessage, multipleErrorObject.hasErrors());
        errorMessage = "this germplasm with an other type should raise only one error. Instead " + multipleErrorObject.toDTO().errors.get(0).errors.size() + " were found";
        TestCase.assertEquals(errorMessage, 1, multipleErrorObject.toDTO().errors.get(0).errors.size());
        String errorDetail = multipleErrorObject.toDTO().errors.get(0).errors.get(0);
        errorMessage = String.format("the error message should contains the message 'invalid uri' and the malformed uri '%s'. But we instead it was : %s", malformedUri, errorDetail);
        TestCase.assertTrue(errorMessage, errorDetail.toLowerCase().contains("invalid uri") && errorDetail.contains(malformedUri.toString()));
    }

    @Test
    public void varietyCreateWithoutSpeciesError() throws Exception {
        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel variety = getCorrectVariety(null, null);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(variety), false);
        String errorMessage = "this variety should raise an error because it has no species";
        TestCase.assertTrue(errorMessage, multipleErrorObject.hasErrors());
        errorMessage = "this germplasm with an other type should raise only one error. Instead " + multipleErrorObject.toDTO().errors.get(0).errors.size() + " were found";
        TestCase.assertEquals(errorMessage, 1, multipleErrorObject.toDTO().errors.get(0).errors.size());
        String errorDetail = multipleErrorObject.toDTO().errors.get(0).errors.get(0);
        errorMessage = String.format("the error message should contains the message 'species'. But we instead it was : %s", errorDetail);
        TestCase.assertTrue(errorMessage, errorDetail.toLowerCase().contains("species"));
    }

    @Test
    public void accessionCreateWithoutSpeciesOrVarietyError() throws Exception {
        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel accession = getCorrectGermplasmAccession(null, null, null);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(accession), false);
        String errorMessage = "this accession should raise an error because it has no species or variety";
        TestCase.assertTrue(errorMessage, multipleErrorObject.hasErrors());
        errorMessage = "this germplasm with an other type should raise only one error. Instead " + multipleErrorObject.toDTO().errors.get(0).errors.size() + " were found";
        TestCase.assertEquals(errorMessage, 1, multipleErrorObject.toDTO().errors.get(0).errors.size());
        String errorDetail = multipleErrorObject.toDTO().errors.get(0).errors.get(0);
        errorMessage = String.format("the error message should contains the message 'species' and 'variety'. But we instead it was : %s", errorDetail);
        TestCase.assertTrue(errorMessage, errorDetail.toLowerCase().contains("species") && errorDetail.toLowerCase().contains("variety"));
    }

    @Test
    public void otherTypeCreateWithoutParentError() throws Exception {
        URI germplasmUri = new URI("http://example.org/otherType/1");
        URI otherType = new URI("http://example.org/ontology/OtherType");

        //Mocking new type existence
        when(daoMocked.isGermplasmType(otherType)).thenReturn(true);
        when(daoMocked.checkExistence(List.of(germplasmUri))).thenReturn(Collections.emptyList());

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel otherTypeGermplasm = getCorrectGermplasmOtherType(germplasmUri, otherType, null, null, null);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(otherTypeGermplasm), false);
        String errorMessage = "this germplasm with an other type should raise an error because it has no accession or variety or species";
        TestCase.assertTrue(errorMessage, multipleErrorObject.hasErrors());
        errorMessage = "this germplasm with an other type should raise only one error. Instead " + multipleErrorObject.toDTO().errors.get(0).errors.size() + " were found";
        TestCase.assertEquals(errorMessage, 1, multipleErrorObject.toDTO().errors.get(0).errors.size());
        String errorDetail = multipleErrorObject.toDTO().errors.get(0).errors.get(0);
        errorMessage = String.format("the error message should contains the message 'accession' or 'variety' or 'species'. But we instead it was : %s", errorDetail);
        TestCase.assertTrue(errorMessage, errorDetail.toLowerCase().contains("accession") && errorDetail.toLowerCase().contains("variety") && errorDetail.toLowerCase().contains("species"));
    }

    @Test
    public void varietyCreateWithUnknownSpeciesError() throws Exception {
        URI speciesUri = new URI("http://example.org/species/1");
        URI varietyUri = new URI("http://example.org/variety/1");

        // Mocking species existence
        when(daoMocked.checkExistence(List.of(speciesUri))).thenReturn(Collections.emptyList());
        when(sparqlMocked.uriExists(new URI(Oeso.Species.getURI()), speciesUri)).thenReturn(false);

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel species = getCorrectGermplasmSpecies(speciesUri);
        GermplasmModel variety = getCorrectVariety(varietyUri, species);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(variety), false);
        String errorMessage = "this variety should raise an error because its species is unknown";
        TestCase.assertTrue(errorMessage, multipleErrorObject.hasErrors());
        errorMessage = "this germplasm with an other type should raise only one error. Instead " + multipleErrorObject.toDTO().errors.get(0).errors.size() + " were found";
        TestCase.assertEquals(errorMessage, 1, multipleErrorObject.toDTO().errors.get(0).errors.size());
        String errorDetail = multipleErrorObject.toDTO().errors.get(0).errors.get(0);
        errorMessage = String.format("the error message should contains the message 'no Species found' and the species uri '%s'. But we instead it was : %s", speciesUri, errorDetail);
        TestCase.assertTrue(errorMessage, errorDetail.toLowerCase().contains("no species found") && errorDetail.contains(speciesUri.toString()));
    }

    @Test
    public void otherTypeCreateWithUnknownTypeError() throws Exception {
        URI otherType = new URI("http://example.org/ontology/OtherUnknownType");
        URI germplasmUri = new URI("http://example.org/otherType/1");
        URI speciesUri = new URI("http://example.org/species/1");

        //Mocking new type existence
        when(daoMocked.isGermplasmType(otherType)).thenReturn(false);
        when(daoMocked.checkExistence(List.of(germplasmUri))).thenReturn(Collections.emptyList());
        when(sparqlMocked.uriExists(new URI(Oeso.Species.getURI()), speciesUri)).thenReturn(true);

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel species = getCorrectGermplasmSpecies(speciesUri);
        GermplasmModel otherTypeGermplasm = getCorrectGermplasmOtherType(germplasmUri, otherType, species, null, null);
        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(otherTypeGermplasm), false);
        String errorMessage = "this germplasm with an other type should raise an error because its type is unknown";
        TestCase.assertTrue(errorMessage, multipleErrorObject.hasErrors());
        errorMessage = "this germplasm with an other type should raise only one error. Instead " + multipleErrorObject.toDTO().errors.get(0).errors.size() + " were found";
        TestCase.assertEquals(errorMessage, 1, multipleErrorObject.toDTO().errors.get(0).errors.size());
        String errorDetail = multipleErrorObject.toDTO().errors.get(0).errors.get(0);
        errorMessage = String.format("the error message should contains the message 'rdfType doesn't exist'. But we instead it was : %s", errorDetail);
        TestCase.assertTrue(errorMessage, errorDetail.toLowerCase().contains("rdftype doesn't exist"));
    }

    /**
     * coherency test : if an accession 'A' has a variety and a species, the variety should have the same species as accession 'A'
     */
    @Test
    public void accessionCreateWithIncoherentVarietySpeciesError() throws Exception {
        URI species1Uri = new URI("http://example.org/species/1");
        URI species2Uri = new URI("http://example.org/species/2");
        URI varietyUri = new URI("http://example.org/variety/1");
        URI accessionUri = new URI("http://example.org/accession/1");

        // Mocking species1 existence with the right type
        when(daoMocked.checkExistence(List.of(species1Uri))).thenReturn(List.of(species1Uri));
        when(sparqlMocked.uriExists(new URI(Oeso.Species.getURI()), species1Uri)).thenReturn(true);
        // Mocking species2 existence with the right type
        when(daoMocked.checkExistence(List.of(species2Uri))).thenReturn(List.of(species2Uri));
        when(sparqlMocked.uriExists(new URI(Oeso.Species.getURI()), species2Uri)).thenReturn(true);
        // Mocking variety existence with the right type
        when(daoMocked.checkExistence(List.of(varietyUri))).thenReturn(List.of(varietyUri));
        when(sparqlMocked.uriExists(new URI(Oeso.Variety.getURI()), varietyUri)).thenReturn(true);

        GermplasmLogic logic = new GermplasmLogic(daoMocked, sparqlMocked, null);
        GermplasmModel species1 = getCorrectGermplasmSpecies(species1Uri);
        GermplasmModel species2 = getCorrectGermplasmSpecies(species2Uri);
        GermplasmModel variety = getCorrectVariety(varietyUri, species1);
        GermplasmModel accession = getCorrectGermplasmAccession(accessionUri, species2, variety);

        //when checking for coherency, we get the variety from the dao to get its species. DAO is mocked to return the variety we created and passed to the accession
        when(daoMocked.get(eq(varietyUri), any(), anyBoolean())).thenReturn(variety);

        var multipleErrorObject = logic.checkBeforeCreateOrUpdate(List.of(accession), false);
        String errorMessage = "this accession should raise an error because its variety and itself have different species";
        TestCase.assertTrue(errorMessage, multipleErrorObject.hasErrors());
        errorMessage = "this germplasm with an other type should raise only one error. Instead " + multipleErrorObject.toDTO().errors.get(0).errors.size() + " were found";
        TestCase.assertEquals(errorMessage, 1, multipleErrorObject.toDTO().errors.get(0).errors.size());
        String errorString = multipleErrorObject.toDTO().errors.get(0).errors.get(0);
        errorMessage = String.format("the error message should contains the message 'species doesn't match with the given variety' annd the wrong species uri '%s' . But we instead it was : %s", species2Uri, errorString);
        TestCase.assertTrue(errorMessage, errorString.toLowerCase().contains("species doesn't match with the given variety") && errorString.contains(species2Uri.toString()));
    }

    //#endregion


    @Before
    public void initBeforeTest() throws URISyntaxException, SPARQLException {
        sparqlMocked = mock(SPARQLService.class);
        daoMocked = mock(GermplasmDAO.class);
        when(daoMocked.isGermplasmType(any())).thenReturn(false);
        when(daoMocked.isGermplasmType(new URI(Oeso.Species.getURI()))).thenReturn(true);
        when(daoMocked.isGermplasmType(new URI(Oeso.Accession.getURI()))).thenReturn(true);
        when(daoMocked.isGermplasmType(new URI(Oeso.Variety.getURI()))).thenReturn(true);
    }

    private GermplasmModel getCorrectGermplasmSpecies(URI uri) throws URISyntaxException {
        GermplasmModel germplasm = new GermplasmModel();
        germplasm.setType(new URI(Oeso.Species.getURI()));
        germplasm.setUri(uri);
        germplasm.setName("name");
        germplasm.setWebsite(new URI("http://example.species.com"));
        return germplasm;
    }

    private GermplasmModel getCorrectVariety(URI uri, GermplasmModel species) throws URISyntaxException {
        GermplasmModel germplasm = new GermplasmModel();
        germplasm.setType(new URI(Oeso.Variety.getURI()));
        germplasm.setUri(uri);
        germplasm.setName("name");
        germplasm.setSpecies(species);
        germplasm.setWebsite(new URI("http://example.variety.com"));
        return germplasm;
    }

    private GermplasmModel getCorrectGermplasmAccession(URI uri, GermplasmModel species, GermplasmModel variety) throws URISyntaxException {
        GermplasmModel germplasm = new GermplasmModel();
        germplasm.setType(new URI(Oeso.Accession.getURI()));
        germplasm.setUri(uri);
        germplasm.setName("name");
        germplasm.setSpecies(species);
        germplasm.setVariety(variety);
        germplasm.setWebsite(new URI("http://example.accession.com"));
        return germplasm;
    }

    /**
     * it is possible to create a germplasm with a type different than species, variety or accession.
     */
    private GermplasmModel getCorrectGermplasmOtherType(URI uri, URI type, GermplasmModel species,GermplasmModel variety, GermplasmModel accession) throws URISyntaxException {
        GermplasmModel germplasm = new GermplasmModel();
        germplasm.setType(type);
        germplasm.setUri(uri);
        germplasm.setName("name");
        germplasm.setSpecies(species);
        germplasm.setVariety(variety);
        germplasm.setAccession(accession);
        germplasm.setWebsite(new URI("http://example.otherType.com"));
        return germplasm;
    }
}