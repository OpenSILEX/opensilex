package org.opensilex.olga.dal;

import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataSearchFilter;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class GermplasmDAO  extends MongoReadWriteDao<GermplasmModel, GermplasmSearchFilter> {

    public static final String COLLECTION_NAME = "olgaGermplasms";

    @Inject
    private SPARQLService sparql;

    @Inject
    private MongoDBService nosql;

    public GermplasmDAO(MongoDBServiceV2 mongodb, Class<GermplasmModel> modelClass, String collectionName, String createUriPath) {
        super(mongodb, modelClass, collectionName, createUriPath);
    }

    public void updateGermplasms(List<GermplasmModel> germplasmModels) throws Exception {
        new SparqlMongoTransaction(sparql, nosql.getServiceV2()).execute(session->{

            return 0;
        });
        // update olga germplasms in mongo here
    }

    public List<GermplasmModel> searchGermplasms(String germplasmName) {
        // search by name but could be later extended
        return Collections.singletonList(new GermplasmModel());
    }

    // add index on germplasmName/germplasmDbId
}
