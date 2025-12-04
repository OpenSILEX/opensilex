package org.opensilex.core.geneticResource.api;

import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.geneticResource.dal.GeneticResourceDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.integration.test.ServiceDescription;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class BaseGeneticResourceAPITest extends AbstractMongoIntegrationTest {

    public static final String path = "/core/geneticResource";

    public static final String uriPath = path + "/{uri}";
    public static final String searchPath = path;
    public static final String createPath = path;
    public static final String updatePath = path;
    public static final String deletePath = path + "/{uri}";

    protected static final ServiceDescription get;
    protected static final ServiceDescription search;
    protected static final ServiceDescription create;
    protected static final ServiceDescription update;
    protected static final ServiceDescription delete;
    protected static final ServiceDescription getAttributes;
    protected static final ServiceDescription getAttributeValues;

    static {
        try {
            get = new ServiceDescription(
                    GeneticResourceAPI.class.getMethod("getGeneticResource", URI.class),
                    path + "/{uri}"
            );
            search = new ServiceDescription(
                    GeneticResourceAPI.class.getMethod(
                            "searchGeneticResource",
                            String.class, URI.class, String.class, String.class, Integer.class,
                            URI.class, URI.class, URI.class, URI.class , String.class, URI.class,
                            List.class,List.class,List.class, String.class,Boolean.class, List.class, int.class, int.class
                    ),
                    path
            );
            create = new ServiceDescription(
                    GeneticResourceAPI.class.getMethod(
                            "createGeneticResource", GeneticResourceCreationDTO.class, Boolean.class
                    ),
                    path
            );
            update = new ServiceDescription(
                    GeneticResourceAPI.class.getMethod(
                            "updateGeneticResource", GeneticResourceUpdateDTO.class
                    ),
                    path
            );
            delete = new ServiceDescription(
                    GeneticResourceAPI.class.getMethod(
                            "deleteGeneticResource", URI.class
                    ),
                    path + "/{uri}"
            );
            getAttributes = new ServiceDescription(
                    GeneticResourceAPI.class.getMethod("getGeneticResourceAttributes"),
                    path + "/attributes"
            );
            getAttributeValues = new ServiceDescription(
                    GeneticResourceAPI.class.getMethod("getGeneticResourceAttributeValues", String.class, String.class, int.class, int.class),
                    path + "/attributes/{attribute}"
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static GeneticResourceCreationDTO getCreationSpeciesDTO() throws URISyntaxException {
        return getCreationSpeciesDTO(null);
    }

    protected static GeneticResourceCreationDTO getCreationSpeciesDTO(Map<String, String> metadatas) throws URISyntaxException {
        GeneticResourceCreationDTO geneticResourceDTO = new GeneticResourceCreationDTO();
        geneticResourceDTO.setName("testSpecies");
        geneticResourceDTO.setIsPublic(true);
        geneticResourceDTO.setRdfType(new URI(Oeso.Species.toString()));
        geneticResourceDTO.setMetadata(metadatas);
        return geneticResourceDTO;
    }

    protected GeneticResourceCreationDTO getCreationVarietyDTO(URI speciesURI) throws URISyntaxException {
        GeneticResourceCreationDTO geneticResourceDTO = new GeneticResourceCreationDTO();
        geneticResourceDTO.setName("testVariety");
        geneticResourceDTO.setIsPublic(true);
        geneticResourceDTO.setRdfType(new URI(Oeso.Variety.toString()));
        geneticResourceDTO.setSpecies(speciesURI);
        return geneticResourceDTO;
    }

    protected GeneticResourceCreationDTO getCreationAccessionDTO(URI varietyURI) throws URISyntaxException {
        GeneticResourceCreationDTO geneticResourceDTO = new GeneticResourceCreationDTO();
        geneticResourceDTO.setName("testAccession");
        geneticResourceDTO.setRdfType(new URI(Oeso.Accession.toString()));
        geneticResourceDTO.setVariety(varietyURI);
        return geneticResourceDTO;
    }

    protected GeneticResourceCreationDTO getCreationLotDTO(URI accessionURI) throws URISyntaxException {
        GeneticResourceCreationDTO geneticResourceDTO = new GeneticResourceCreationDTO();
        geneticResourceDTO.setName("testLot");
        geneticResourceDTO.setRdfType(new URI(Oeso.PlantMaterialLot.toString()));
        geneticResourceDTO.setAccession(accessionURI);
        return geneticResourceDTO;
    }


    protected URI createSpecies() throws Exception {
        return new UserCallBuilder(create).setBody(getCreationSpeciesDTO()).buildAdmin().executeCallAndReturnURI();
    }
    protected URI createVariety(URI speciesUri) throws Exception {
        return new UserCallBuilder(create).setBody(getCreationVarietyDTO(speciesUri)).buildAdmin().executeCallAndReturnURI();
    }
    protected URI createAccession(URI varietyUri) throws Exception {
        return new UserCallBuilder(create).setBody(getCreationAccessionDTO(varietyUri)).buildAdmin().executeCallAndReturnURI();
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(GeneticResourceDAO.ATTRIBUTES_COLLECTION_NAME);
    }
}
