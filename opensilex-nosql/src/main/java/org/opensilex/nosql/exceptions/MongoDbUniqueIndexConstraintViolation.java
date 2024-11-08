package org.opensilex.nosql.exceptions;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import org.opensilex.server.response.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class MongoDbUniqueIndexConstraintViolation extends MongoException {

    public MongoDbUniqueIndexConstraintViolation(MongoWriteException exception) {
        super("Duplicate key error due to index unicity constraint. Document(s) already exist(s) in MongoDB database : " + exception.getError());
    }

    public MongoDbUniqueIndexConstraintViolation(MongoBulkWriteException exception) {
        super("Duplicate key error due to index unicity constraint. Document(s) already exist(s) in MongoDB database : " + exception.getWriteErrors());
    }
}
