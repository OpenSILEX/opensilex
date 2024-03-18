/*******************************************************************************
 *                         ObservationLogicTest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 18/03/2024
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.observation.bll;

import org.junit.Before;
import org.junit.Test;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.observation.bll.model.ObservationBLM;
import org.opensilex.core.observation.dal.ObservationDAO;
import org.opensilex.core.observation.proto_ext.datatype.ObservationDatatypeRegistry;
import org.opensilex.core.observation.proto_ext.datatype.integer.ObservationIntegerValue;
import org.opensilex.core.observation.proto_ext.datatype.integer.ObservationURIValue;

import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * todo This probably should be a unit test. How to mock the dao ?
 * @author Valentin Rigolle
 */
public class ObservationLogicTest extends AbstractMongoIntegrationTest {
    private ObservationLogic logic;

    @Before
    public void before() {
        logic = new ObservationLogic(new ObservationDAO(getMongoDBServiceV2()));
    }

    @Override
    protected List<String> getCollectionsToClearNames() {
        return List.of(ObservationDAO.COLLECTION_NAME);
    }

    @Test
    public void testCreateSimpleUri() {
        var obs = new ObservationBLM<ObservationURIValue>();
        obs.setDatatype(ObservationDatatypeRegistry.OBSERVATION_URI_DATATYPE);
        var value = new ObservationURIValue();
        value.setUriValue(URI.create("test:example"));
        obs.setValue(value);
        logic.create(obs);
        var result = logic.search();
        assertEquals(1, result.getTotal());
        assertEquals(value, result.getList().get(0).getValue());
    }

    @Test
    public void testCreateSimpleInteger() {
        var obs = new ObservationBLM<ObservationIntegerValue>();
        obs.setDatatype(ObservationDatatypeRegistry.OBSERVATION_INTEGER_DATATYPE);
        var value = new ObservationIntegerValue();
        value.setIntegerValue(85);
        obs.setValue(value);
        logic.create(obs);
        var result = logic.search();
        assertEquals(1, result.getTotal());
        assertEquals(value, result.getList().get(0).getValue());
    }

    @Test
    public void testCreateMultiple() {
        var uriObs = new ObservationBLM<ObservationURIValue>();
        uriObs.setDatatype(ObservationDatatypeRegistry.OBSERVATION_URI_DATATYPE);
        var uriValue = new ObservationURIValue();
        uriValue.setUriValue(URI.create("test:example"));
        uriObs.setValue(uriValue);

        var intObs = new ObservationBLM<ObservationIntegerValue>();
        intObs.setDatatype(ObservationDatatypeRegistry.OBSERVATION_INTEGER_DATATYPE);
        var intValue = new ObservationIntegerValue();
        intValue.setIntegerValue(85);
        intObs.setValue(intValue);

        logic.create(uriObs);
        logic.create(intObs);

        var result = logic.search();
        assertEquals(2, result.getTotal());
    }
}
