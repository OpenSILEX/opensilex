package org.opensilex.nosql.exceptions;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import org.opensilex.server.response.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class MongoDbUniqueIndexConstraintViolation extends WebApplicationException {

    public MongoDbUniqueIndexConstraintViolation(MongoWriteException exception) {
        super(exception);
    }

    public MongoDbUniqueIndexConstraintViolation(MongoBulkWriteException exception) {
        super(exception);
    }

    @Override
    public Response getResponse() {
        return new ErrorResponse(Response.Status.BAD_REQUEST, "Duplicate key error due to index unicity constraint. Document(s) already exist(s) in MongoDB database", getCause().getMessage())
                .getResponse();
    }
}
