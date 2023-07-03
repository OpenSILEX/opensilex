package org.opensilex.nosql.mongodb.auth.password;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.nosql.mongodb.auth.MongoAuthenticationService;
import org.opensilex.nosql.mongodb.auth.MongoSecurityConfig;
import org.opensilex.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMongoAuthenticationService extends BaseService implements MongoAuthenticationService {

    protected final Logger LOGGER;

    protected AbstractMongoAuthenticationService(MongoSecurityConfig config) {
        super(config);

        if (StringUtils.isEmpty(config.authDB())) {
            throw new IllegalArgumentException("Null or empty authentification database : authDB");
        }
        if (StringUtils.isEmpty(config.login())) {
            throw new IllegalArgumentException("Null or empty login");
        }
        LOGGER = LoggerFactory.getLogger(getClass());
    }


}
