package org.opensilex.migration;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;
import org.apache.jena.atlas.json.JSON;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.opensilex.core.event.dal.move.MoveEventDAO;
import org.opensilex.core.event.dal.move.PositionModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.update.AbstractOpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

/**
 * <pre>
 * Performs an update on the {@link MoveEventDAO#MOVE_COLLECTION_NAME} collection
 * after change on data-scheme of the {@link org.opensilex.core.event.dal.move.PositionModel}.
 * For any model of {@link org.opensilex.core.event.dal.move.MoveEventNoSqlModel} each nested position must be updated.
 *
 * Indeed, {@link PositionModel#X_FIELD}, {@link PositionModel#Y_FIELD} and {@link PositionModel#Z_FIELD} now
 * use a {@link String} representation instead of {@link Integer}
 *
 * This class execute a single update by using one query without additional I/O (No need to read any existing document from the collection before performing the update)
 *
 * </pre>
 *
 * @author rcolin
 */
public class MongoCustomCoordinatesDataTypeUpdate extends AbstractOpenSilexModuleUpdate {

    private static final String POSITION_PATH = "targetPositions.position.";

    @Override
    public String getDescription() {
        return "Update custom coordinates datatype (`integer` -> `String`) into MongoDB moves collection";
    }

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.parse("2022-08-04T00:00:00+01:00");
    }

    /**
     *
     * The following update will be executed on mongo server
     * <pre>
     * {@code
     * db.move.updateMany({},
     * [
     *   {
     *     $set: {
     *       "targetPositions.position.x": {
     *         $toString: {
     *           $arrayElemAt: [
     *             "$targetPositions.position.x",
     *             0
     *           ]
     *         }
     *       },
     *       "targetPositions.position.y": {
     *         $toString: {
     *           $arrayElemAt: [
     *             "$targetPositions.position.y",
     *             0
     *           ]
     *         }
     *       },
     *       "targetPositions.position.z": {
     *         $toString: {
     *           $arrayElemAt: [
     *             "$targetPositions.position.z",
     *             0
     *           ]
     *         }
     *       },
     *
     *     }
     *   }
     * ])
     * }
     * </pre>
     * @see <a href="https://www.mongodb.com/docs/manual/reference/method/db.collection.updateMany/">MongoDB : array update</a>
     */
    public void execute() throws OpensilexModuleUpdateException {

        Objects.requireNonNull(opensilex);

        // Get access to mongo collection
        MongoDBService mongodb = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);

        // check collection existence
        boolean collectionExist = false;
        for (String collectionName : mongodb.getDatabase().listCollectionNames()) {
            if(collectionName.equals(MoveEventDAO.MOVE_COLLECTION_NAME)){
                collectionExist = true;
                break;
            }
        }
        if(!collectionExist){
            logger.info("No move collection found, no update to apply");
            return;
        }


        MongoCollection<?> moveCollection = mongodb.getDatabase().getCollection(MoveEventDAO.MOVE_COLLECTION_NAME);

        // use a session in order to handle transaction
        // required since an update which apply on multiple document must be executed
        ClientSession session = mongodb.getMongoDBClient().startSession();

        try{
            session.startTransaction();

            Bson update = getUpdateBson();
            logger.info("Applying update : \n{}", JSON.parse(update.toBsonDocument().toString()).toString());

            // perform the update on all document from move collection and commit the update
            UpdateResult updateResult = moveCollection.updateMany(session, new Document(), Collections.singletonList(update));
            session.commitTransaction();

            logger.info("{}", updateResult);

        }catch (Exception e1) {
            try {
                if(session.hasActiveTransaction()){
                    session.abortTransaction();
                }
            } catch (Exception e2) {
                throw new OpensilexModuleUpdateException(this, e2);
            }
            throw new OpensilexModuleUpdateException(this, e1);
        }
        finally {
            session.close();
        }

    }


    /**
     * @return The JSON update document
     * @see <a href="https://www.mongodb.com/docs/manual/reference/operator/update/positional/">MongoDB : array update</a>
     * @see <a href="https://www.mongodb.com/docs/manual/reference/operator/aggregation/toString/">MongoDB : toString operator</a>
     * @see <a href="https://www.mongodb.com/docs/manual/reference/operator/aggregation/arrayElemAt/#mongodb-expression-exp.-arrayElemAt/">MongoDB : get element of array</a>
     */
    private static Bson getUpdateBson(){

        Document updates = new Document();

        // add an update expression for each fields
        String[] fields = {"x", "y", "z"};
        for(String positionField : fields){
            String fieldPath = "$targetPositions.position." + positionField;

            // get String representation of the old attribute value
            Document positionTypeChange = new Document("$toString",
                    new Document("$arrayElemAt", Arrays.asList("$"+fieldPath, 0))
            );
            updates.append(fieldPath, positionTypeChange);
        }

        return new Document("$set", updates);
    }

}
