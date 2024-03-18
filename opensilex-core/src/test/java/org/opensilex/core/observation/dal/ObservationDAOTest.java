/*******************************************************************************
 *                         ObservationDAOTest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.dal;

import org.bson.BsonInt64;
import org.bson.BsonString;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.observation.dal.model.ObservationModel;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;

import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @todo This probably should be a unit test. How to mock mongo service ?
 * @author Valentin Rigolle
 */
public class ObservationDAOTest extends AbstractMongoIntegrationTest {
    private ObservationDAO dao;

    @Before
    public void before() {
        dao = new ObservationDAO(getMongoDBServiceV2());
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(ObservationDAO.COLLECTION_NAME);
    }

    /**
     * todo should this method be called "test...String" instead ? Because the value that is actually checked is
     *      BsonString. That also raises the question of the role of the datatype here. / Maybe the datatype has
     *      nothing to do here, we just test the value. Other tests should cover the datatype (for whatever test we
     *      might want to to on the datatype). EDIT : I reverted the name to String, but I choose to keep the
     *      discussion for the future.
     */
    @Test
    public void testCreateSimpleString() {
        var obs = new ObservationModel();
        obs.setValue(new BsonString("test:example"));
        try {
            dao.create(obs);
        } catch (NoSQLAlreadyExistingUriException | URISyntaxException e) {
            fail();
        }
        var result = dao.search(new ObservationSearchFilter());
        assertEquals(1, result.getTotal());
        assertEquals("test:example", result.getList().get(0).getValue().asString().getValue());
    }

    @Test
    public void testCreateSimpleInteger() {
        var obs = new ObservationModel();
        obs.setValue(new BsonInt64(85));
        try {
            dao.create(obs);
        } catch (NoSQLAlreadyExistingUriException | URISyntaxException e) {
            fail();
        }
        var result = dao.search(new ObservationSearchFilter());
        assertEquals(1, result.getTotal());
        assertEquals(85, result.getList().get(0).getValue().asInt64().getValue());
    }
}
