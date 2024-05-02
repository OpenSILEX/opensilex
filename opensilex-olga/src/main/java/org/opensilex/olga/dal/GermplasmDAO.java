/*
 * *****************************************************************************
 *                         GermplasmDAO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 02/05/2024 10:32
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.olga.dal;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.conversions.Bson;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataSearchFilter;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.nosql.mongodb.dao.MongoReadWriteDao;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opensilex.core.data.dal.DataProvenanceModel.*;

public class GermplasmDAO  extends MongoReadWriteDao<GermplasmModel, GermplasmSearchFilter> {

    public static final String COLLECTION_NAME = "olgaGermplasms";

    public static final String MONGO_NAME_FIELD = "germplasmName";

    public static final String MONGO_ID_FIELD = "germplasmDbId";

    public static final Class<GermplasmModel> MODEL_CLASS = GermplasmModel.class;

    public static final String CREATE_URI_PATH = "";

    public GermplasmDAO(MongoDBServiceV2 mongodb) {
        super(mongodb, MODEL_CLASS, COLLECTION_NAME, CREATE_URI_PATH);
    }

    public static Map<Bson, IndexOptions> getIndexes() {

        Bson nameIndex = Indexes.ascending(MONGO_NAME_FIELD);

        Map<Bson, IndexOptions> indexes = new HashMap<>();

        // index on field : germplasmName
        indexes.put(nameIndex, null);

        return indexes;
    }


    @Override
    public String idField() {
        return MONGO_ID_FIELD;
    }

    public List<GermplasmModel> searchGermplasms(String germplasmName) {
        // TODO : search by name but could be later extended
        return Collections.singletonList(new GermplasmModel());
    }

    // TODO : add index on germplasmName/germplasmDbId
}
