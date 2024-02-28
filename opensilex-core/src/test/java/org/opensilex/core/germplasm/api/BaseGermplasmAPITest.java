package org.opensilex.core.germplasm.api;

import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.integration.test.ServiceDescription;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class BaseGermplasmAPITest extends AbstractMongoIntegrationTest {

    public static final String path = "/core/germplasm";

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

    static {
        try {
            get = new ServiceDescription(
                    GermplasmAPI.class.getMethod("getGermplasm", URI.class),
                    path + "/{uri}"
            );
            search = new ServiceDescription(
                    GermplasmAPI.class.getMethod(
                            "searchGermplasm",
                            String.class, URI.class, String.class, String.class, Integer.class,
                            URI.class, URI.class, URI.class, URI.class, String.class, URI.class,
                            List.class, List.class, List.class, String.class, List.class, int.class, int.class
                    ),
                    path
            );
            create = new ServiceDescription(
                    GermplasmAPI.class.getMethod(
                            "createGermplasm", GermplasmCreationDTO.class, Boolean.class
                    ),
                    path
            );
            update = new ServiceDescription(
                    GermplasmAPI.class.getMethod(
                            "updateGermplasm", GermplasmUpdateDTO.class
                    ),
                    path
            );
            delete = new ServiceDescription(
                    GermplasmAPI.class.getMethod(
                            "deleteGermplasm", URI.class
                    ),
                    path + "/{uri}"
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static GermplasmCreationDTO getCreationSpeciesDTO() throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setName("testSpecies");
        germplasmDTO.setRdfType(new URI(Oeso.Species.toString()));
        return germplasmDTO;
    }

    protected GermplasmCreationDTO getCreationVarietyDTO(URI speciesURI) throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setName("testVariety");
        germplasmDTO.setRdfType(new URI(Oeso.Variety.toString()));
        germplasmDTO.setSpecies(speciesURI);
        return germplasmDTO;
    }

    protected GermplasmCreationDTO getCreationAccessionDTO(URI varietyURI) throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setName("testAccession");
        germplasmDTO.setRdfType(new URI(Oeso.Accession.toString()));
        germplasmDTO.setVariety(varietyURI);
        return germplasmDTO;
    }

    protected GermplasmCreationDTO getCreationLotDTO(URI accessionURI) throws URISyntaxException {
        GermplasmCreationDTO germplasmDTO = new GermplasmCreationDTO();
        germplasmDTO.setName("testLot");
        germplasmDTO.setRdfType(new URI(Oeso.PlantMaterialLot.toString()));
        germplasmDTO.setAccession(accessionURI);
        return germplasmDTO;
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
}
