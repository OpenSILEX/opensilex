package org.opensilex.core.germplasm.dal;

import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.metadata.MetaDataDaoV2;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.group.dal.GroupModel;

import java.util.List;
import java.util.Set;

public class GermplasmMetadataDAO extends MetaDataDaoV2<GermplasmMetadataModel> {
    public GermplasmMetadataDAO(MongoDBServiceV2 mongodb, String collectionName) {
        super(mongodb, GermplasmMetadataModel.class, collectionName);
    }

    public GermplasmMetadataDAO(MongoDBService mongodb, String collectionName) {
        super(mongodb, GermplasmMetadataModel.class, collectionName);
    }

    /**
     * Get distinct keys based on user access rights.
     */
    public Set<String> getDistinctKeys(AccountModel account, List<GroupModel> groups) {
        var filter = Filters.or(
                new Document(GermplasmMetadataModel.IS_PUBLIC_FIELD, true),
                new Document(GermplasmMetadataModel.PUBLISHER_FIELD, account.getUri()),
                Filters.in(
                        GermplasmMetadataModel.GROUPS_FIELD,
                        groups.stream().map(group -> group.getUri().toString()).toList()
                )
        );
        return getDistinctKeys(filter);
    }
}
