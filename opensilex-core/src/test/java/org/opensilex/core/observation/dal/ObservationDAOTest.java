/*******************************************************************************
 *                         ObservationDAOTest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.dal;

import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.observation.dal.model.ObservationDalModel;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;

import java.net.URISyntaxException;

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

    @Test
    public void testCreateSimple1() {
        var obs = new ObservationDalModel();
        obs.setValue("21");
        try {
            dao.create(obs);
        } catch (NoSQLAlreadyExistingUriException | URISyntaxException e) {
            fail();
        }
        var result = dao.search(new ObservationDalSearchFilter());
        assertEquals(1, result.getTotal());
        assertEquals("21", result.getList().get(0).getValue());
    }
}
