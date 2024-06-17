package org.opensilex.core.data.dal;

import org.bson.Document;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.*;

/**
 * Some static functions to build complex filters for data searches
 */
public class DataFilterBuilder {

    public static void appendExperimentUserAccessFilter(Document filter, AccountModel user, List<URI> experiments, SPARQLService sparql, MongoDBService mongoDBService) throws Exception {
        String experimentField = "provenance.experiments";

        //user access
        if (!user.isAdmin()) {
            Set<URI> userExperiments = new ExperimentDAO(sparql, mongoDBService).getUserExperiments(user);

            if (experiments != null && !experiments.isEmpty()) {

                //Transform experiments and userExperiments in long format to compare the two lists
                Set<URI> longUserExp = new HashSet<>();
                for (URI exp:userExperiments) {
                    longUserExp.add(new URI(SPARQLDeserializers.getExpandedURI(exp)));
                }
                Set <URI> longExpURIs = new HashSet<>();
                for (URI exp:experiments) {
                    longExpURIs.add(new URI(SPARQLDeserializers.getExpandedURI(exp)));
                }
                longExpURIs.retainAll(longUserExp); //keep in the list only the experiments the user has access to

                if (longExpURIs.isEmpty()) {
                    throw new Exception("you can't access to the given experiments");
                } else {
                    Document inFilter = new Document();
                    inFilter.put("$in", longExpURIs);
                    filter.put("provenance.experiments", inFilter);
                }
            } else {
                Document filterOnExp = new Document(experimentField, new Document("$in", userExperiments));
                Document notExistingExpFilter = new Document(experimentField, new Document("$exists", false));
                Document emptyExpFilter = new Document(experimentField, new ArrayList());
                List<Document> expFilter = Arrays.asList(filterOnExp, notExistingExpFilter, emptyExpFilter);

                filter.put("$or", expFilter);
            }
        } else {
            if (experiments != null && !experiments.isEmpty()) {
                Document inFilter = new Document();
                inFilter.put("$in", experiments);
                filter.put("provenance.experiments", inFilter);
            }
        }
    }
}
