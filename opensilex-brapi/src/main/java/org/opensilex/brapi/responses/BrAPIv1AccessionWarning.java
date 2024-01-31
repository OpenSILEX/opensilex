package org.opensilex.brapi.responses;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.server.response.JsonResponse;
import org.opensilex.server.response.StatusDTO;
import org.opensilex.server.response.StatusLevel;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.ontology.store.OntologyStore;

import java.net.URI;

public class BrAPIv1AccessionWarning {

    public static final URI ACCESSION_URI = URI.create(Oeso.DOMAIN + "#" + "Accession");

    public static final URI GERMPLASM_URI = URI.create(Oeso.Germplasm.getURI());

    public BrAPIv1AccessionWarning() {
    }

    public static void setAccessionWarningIfNeeded(JsonResponse responseClass) throws SPARQLException {
        OntologyStore ontologyStore = SPARQLModule.getOntologyStoreInstance();
        if (!ontologyStore.classExist(
                ACCESSION_URI,
                GERMPLASM_URI
        )) {
            responseClass.addMetadataStatus(new StatusDTO(
                    "This service will not return any information related to 'Germplasms' as the 'Accession' notion doesn't exist in your ontology",
                    StatusLevel.WARNING
            ));
        }
    }
}
