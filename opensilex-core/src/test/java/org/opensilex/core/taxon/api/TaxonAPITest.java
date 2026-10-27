// //******************************************************************************
// //                          OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// // Copyright © INRAE 2026.
// //******************************************************************************
// package org.opensilex.core.taxon.api;

// import com.fasterxml.jackson.core.type.TypeReference;
// import org.junit.Test;
// import org.opensilex.core.AbstractMongoIntegrationTest;
// import org.opensilex.core.ontology.Oeso;
// import org.opensilex.server.response.PaginatedListResponse;

// import javax.ws.rs.core.Response;
// import java.net.URI;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// import static org.junit.Assert.*;

// /**
//  * Taxon API: taxa entered by users, identifiers as a list of "SOURCE:value".
//  */
// public class TaxonAPITest extends AbstractMongoIntegrationTest {

//     private static final String PATH = TaxonAPI.PATH;
//     private static final String URI_PATH = PATH + "/{uri}";
//     private static final String KINGDOMS_PATH = PATH + "/kingdoms";

//     private static final URI SPECIES = URI.create(Oeso.Species.getURI());
//     private static final URI GENUS = URI.create(Oeso.Genus.getURI());
//     private static final URI PLANTAE = URI.create(Oeso.Plantae.getURI());
//     private static final URI BACTERIA = URI.create(Oeso.Bacteria.getURI());

//     private static final TypeReference<PaginatedListResponse<TaxonGetDTO>> TAXA = new TypeReference<>() {};
//     private static final TypeReference<PaginatedListResponse<KingdomDTO>> KINGDOMS = new TypeReference<>() {};

//     // ------------------------------------------------------------------ helpers

//     private TaxonCreationDTO dto(URI type, String name, URI kingdom, String... taxonId) {
//         TaxonCreationDTO dto = new TaxonCreationDTO();
//         dto.setType(type);
//         dto.setName(name);
//         dto.setKingdom(kingdom);
//         dto.setTaxonId(List.of(taxonId));
//         return dto;
//     }

//     private URI create(TaxonCreationDTO dto) throws Exception {
//         Response response = getJsonPostResponseAsAdmin(target(PATH), dto);
//         assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
//         return extractUriFromResponse(response);
//     }

//     private int createStatus(TaxonCreationDTO dto) throws Exception {
//         return getJsonPostResponseAsAdmin(target(PATH), dto).getStatus();
//     }

//     private List<TaxonGetDTO> search(String param, Object value) throws Exception {
//         Map<String, Object> params = new HashMap<>();
//         params.put(param, value);
//         return getSearchResultsAsAdmin(PATH, params, TAXA);
//     }

//     private TaxonUpdateDTO updateOf(URI uri, TaxonCreationDTO from) {
//         TaxonUpdateDTO update = new TaxonUpdateDTO();
//         update.setUri(uri);
//         update.setType(from.getType());
//         update.setName(from.getName());
//         update.setKingdom(from.getKingdom());
//         update.setGenus(from.getGenus());
//         update.setTaxonId(from.getTaxonId());
//         return update;
//     }

//     // ------------------------------------------------------------------ kingdoms

//     @Test
//     public void kingdomsAreTheEightOfTheOntology() throws Exception {
//         List<KingdomDTO> kingdoms = getSearchResultsAsAdmin(KINGDOMS_PATH, new HashMap<>(), KINGDOMS);
//         assertEquals(8, kingdoms.size());
//     }

//     // ------------------------------------------------------------------ creation and search

//     @Test
//     public void createSpeciesAndFindItByKingdomAndByIdentifier() throws Exception {
//         create(dto(SPECIES, "Escherichia coli", BACTERIA, "NCBITaxon:562"));

//         List<TaxonGetDTO> bacteria = search("kingdom", BACTERIA.toString());
//         assertEquals(1, bacteria.size());
//         assertEquals("Escherichia coli", bacteria.get(0).getName());
//         assertEquals(List.of("NCBITaxon:562"), bacteria.get(0).getTaxonId());

//         assertEquals(1, search("taxon_id", "NCBITaxon:562").size());
//         assertEquals(0, search("taxon_id", "NCBITaxon:999").size());
//     }

//     @Test
//     public void taxonCanHaveSeveralIdentifiersAndIsFoundByAnyOfThem() throws Exception {
//         create(dto(SPECIES, "Vitis vinifera", PLANTAE, "NCBITaxon:29760", "TAXREF:129968"));

//         List<TaxonGetDTO> byNcbi = search("taxon_id", "NCBITaxon:29760");
//         List<TaxonGetDTO> byTaxref = search("taxon_id", "TAXREF:129968");
//         assertEquals(1, byNcbi.size());
//         assertEquals(1, byTaxref.size());
//         assertEquals(byNcbi.get(0).getUri(), byTaxref.get(0).getUri());
//         assertEquals(2, byNcbi.get(0).getTaxonId().size());
//     }

//     @Test
//     public void sameNumberInTwoSourcesIsNotAConflict() throws Exception {
//         // The prefix is what makes the identifier unique: NCBITaxon:562 and TAXREF:562 are different
//         create(dto(SPECIES, "Escherichia coli", BACTERIA, "NCBITaxon:562"));
//         create(dto(SPECIES, "Taxon with the same TAXREF number", PLANTAE, "TAXREF:562"));
//     }

//     @Test
//     public void taxonWithoutIdentifierIsAccepted() throws Exception {
//         create(dto(SPECIES, "Bacillus sp. BV-25", BACTERIA));
//     }

//     @Test
//     public void duplicatesAndBlankIdentifiersAreRemoved() throws Exception {
//         create(dto(SPECIES, "Hordeum vulgare", PLANTAE, "NCBITaxon:4513", " NCBITaxon:4513 ", ""));
//         List<TaxonGetDTO> found = search("taxon_id", "NCBITaxon:4513");
//         assertEquals(1, found.size());
//         assertEquals(List.of("NCBITaxon:4513"), found.get(0).getTaxonId());
//     }

//     // ------------------------------------------------------------------ refused creations

//     @Test
//     public void identifierMustBePrefixedBySource() throws Exception {
//         assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createStatus(dto(SPECIES, "Zea mays", PLANTAE, "4577")));
//     }

//     @Test
//     public void identifierIsUniqueAcrossTaxa() throws Exception {
//         create(dto(SPECIES, "Zea mays", PLANTAE, "NCBITaxon:4577"));
//         // Same identifier, even placed second in the list of another taxon
//         assertEquals(Response.Status.CONFLICT.getStatusCode(),
//                 createStatus(dto(SPECIES, "Maize", PLANTAE, "TAXREF:111", "NCBITaxon:4577")));
//     }

//     @Test
//     public void kingdomIsMandatory() throws Exception {
//         assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createStatus(dto(SPECIES, "Zea mays", null, "NCBITaxon:4577")));
//     }

//     @Test
//     public void rankMustBeSpeciesOrGenus() throws Exception {
//         URI family = URI.create(Oeso.NS + "Family");
//         assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createStatus(dto(family, "Vitaceae", PLANTAE)));
//     }

//     // ------------------------------------------------------------------ genus

//     @Test
//     public void genusRules() throws Exception {
//         URI vitis = create(dto(GENUS, "Vitis", PLANTAE, "NCBITaxon:3603"));

//         TaxonCreationDTO otherKingdom = dto(SPECIES, "Vitis bacteria", BACTERIA);
//         otherKingdom.setGenus(vitis);
//         assertEquals("a species and its genus must be in the same kingdom",
//                 Response.Status.BAD_REQUEST.getStatusCode(), createStatus(otherKingdom));

//         TaxonCreationDTO genusWithGenus = dto(GENUS, "Subvitis", PLANTAE);
//         genusWithGenus.setGenus(vitis);
//         assertEquals("only a species can have a genus",
//                 Response.Status.BAD_REQUEST.getStatusCode(), createStatus(genusWithGenus));

//         TaxonCreationDTO vinifera = dto(SPECIES, "Vitis vinifera", PLANTAE, "NCBITaxon:29760");
//         vinifera.setGenus(vitis);
//         create(vinifera);
//     }

//     // ------------------------------------------------------------------ update

//     @Test
//     public void updateReplacesTheListOfIdentifiers() throws Exception {
//         TaxonCreationDTO coli = dto(SPECIES, "Escherichia coli", BACTERIA, "NCBITaxon:562");
//         URI uri = create(coli);

//         TaxonUpdateDTO update = updateOf(uri, coli);
//         update.setTaxonId(List.of("NCBITaxon:562", "TAXREF:999001"));
//         assertEquals(Response.Status.OK.getStatusCode(), getJsonPutResponse(target(PATH), update).getStatus());

//         List<TaxonGetDTO> found = search("taxon_id", "TAXREF:999001");
//         assertEquals(1, found.size());
//         assertEquals(2, found.get(0).getTaxonId().size());
//     }

//     @Test
//     public void kingdomOfAUsedTaxonCannotChange() throws Exception {
//         TaxonCreationDTO vitisDto = dto(GENUS, "Vitis", PLANTAE, "NCBITaxon:3603");
//         URI vitis = create(vitisDto);
//         TaxonCreationDTO vinifera = dto(SPECIES, "Vitis vinifera", PLANTAE, "NCBITaxon:29760");
//         vinifera.setGenus(vitis);
//         create(vinifera);

//         TaxonUpdateDTO update = updateOf(vitis, vitisDto);
//         update.setKingdom(BACTERIA);
//         assertEquals(Response.Status.CONFLICT.getStatusCode(), getJsonPutResponse(target(PATH), update).getStatus());
//     }

//     @Test
//     public void kingdomOfAnUnusedTaxonCanChange() throws Exception {
//         TaxonCreationDTO plasmopara = dto(SPECIES, "Plasmopara viticola", URI.create(Oeso.Fungi.getURI()));
//         URI uri = create(plasmopara);

//         // Data entry error corrected: an oomycete is a Chromista, not a fungus
//         TaxonUpdateDTO update = updateOf(uri, plasmopara);
//         update.setKingdom(URI.create(Oeso.Chromista.getURI()));
//         assertEquals(Response.Status.OK.getStatusCode(), getJsonPutResponse(target(PATH), update).getStatus());
//     }

//     // ------------------------------------------------------------------ read and delete

//     @Test
//     public void getByUriAndUnknownUri() throws Exception {
//         URI uri = create(dto(SPECIES, "Zea mays", PLANTAE, "NCBITaxon:4577"));
//         assertEquals(Response.Status.OK.getStatusCode(), getJsonGetByUriResponseAsAdmin(target(URI_PATH), uri.toString()).getStatus());
//         assertEquals(Response.Status.NOT_FOUND.getStatusCode(),
//                 getJsonGetByUriResponseAsAdmin(target(URI_PATH), "http://opensilex.test/id/taxon/unknown").getStatus());
//     }

//     @Test
//     public void usedTaxonCannotBeDeletedUnusedTaxonCan() throws Exception {
//         URI vitis = create(dto(GENUS, "Vitis", PLANTAE, "NCBITaxon:3603"));
//         TaxonCreationDTO vinifera = dto(SPECIES, "Vitis vinifera", PLANTAE, "NCBITaxon:29760");
//         vinifera.setGenus(vitis);
//         URI viniferaUri = create(vinifera);

//         assertEquals(Response.Status.CONFLICT.getStatusCode(), getDeleteByUriResponse(target(URI_PATH), vitis.toString()).getStatus());
//         assertEquals(Response.Status.OK.getStatusCode(), getDeleteByUriResponse(target(URI_PATH), viniferaUri.toString()).getStatus());
//         assertEquals(Response.Status.OK.getStatusCode(), getDeleteByUriResponse(target(URI_PATH), vitis.toString()).getStatus());
//     }

//     // ------------------------------------------------------------------ legacy

//     @Test
//     public void legacySpeciesFacadeStillAnswers() throws Exception {
//         create(dto(SPECIES, "Hordeum vulgare", PLANTAE, "NCBITaxon:4513"));
//         assertEquals(Response.Status.OK.getStatusCode(), getJsonGetResponseAsAdmin(target("/core/species")).getStatus());
//     }
// }