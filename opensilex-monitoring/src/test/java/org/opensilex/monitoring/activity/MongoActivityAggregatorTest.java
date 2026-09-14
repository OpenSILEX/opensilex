//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.bson.Document;
import org.junit.Test;

/**
 * Guards the aggregation expressions that are built by hand.
 *
 * @author Arnaud Charleroy
 */
public class MongoActivityAggregatorTest {

    @Test
    public void clientErrorBranchIsAPlainRangeCheck() {
        Document expression = MongoActivityAggregator.errorCount(400, 500);

        assertNotNull(expression);
        List<?> cond = (List<?>) expression.get("$cond");
        Document condition = (Document) cond.get(0);
        assertTrue("a 4xx has a status, so no null branch is needed",
                condition.containsKey("$and"));
        assertEquals(1, cond.get(1));
        assertEquals(0, cond.get(2));
    }

    /**
     * The regression this class exists for. The server-error branch has to match calls that ended
     * in an unhandled exception and therefore carry no status at all, which means comparing against
     * null — and {@code List.of} throws a NullPointerException on a null element. Building this
     * expression used to fail on every single request to the activity endpoint.
     */
    @Test
    public void serverErrorBranchAlsoMatchesACallWithNoStatusAtAll() {
        Document expression = MongoActivityAggregator.errorCount(500, 600);

        List<?> cond = (List<?>) expression.get("$cond");
        Document condition = (Document) cond.get(0);

        List<?> alternatives = (List<?>) condition.get("$or");
        assertNotNull("the server error branch must be an $or, not a bare range", alternatives);
        assertEquals(2, alternatives.size());

        Document nullCheck = (Document) alternatives.get(1);
        List<?> operands = (List<?>) nullCheck.get("$eq");
        assertEquals("$status", operands.get(0));
        assertEquals("a missing status must be comparable to null", null, operands.get(1));
    }
}
