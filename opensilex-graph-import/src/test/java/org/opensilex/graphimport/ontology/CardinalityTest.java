//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Cardinalities read from the raw restriction fields, where the platform's {@code isList()} takes an
 * exact 1 for a list.
 *
 * @author Arnaud Charleroy
 */
public class CardinalityTest {

    @Test
    public void anExactOneIsRequiredAndSingle() {
        Cardinality exactOne = Cardinality.of(1, null, null);

        assertEquals(new Cardinality(1, 1), exactOne);
        assertTrue(exactOne.isRequired());
        assertFalse(exactOne.isMultiValued());
    }

    @Test
    public void anExactTwoOrAMaximumOfThreeHoldSeveralValues() {
        assertTrue(Cardinality.of(2, null, null).isMultiValued());
        assertTrue(Cardinality.of(null, null, 3).isMultiValued());
        assertFalse(Cardinality.of(null, null, 3).isRequired());
        assertEquals("[0..3]", Cardinality.of(null, null, 3).toString());
    }

    @Test
    public void noBoundsAllowAnything() {
        assertEquals(Cardinality.ANY, Cardinality.of(null, 0, null));
        assertTrue(Cardinality.ANY.isMultiValued());
        assertTrue(Cardinality.ANY.accepts(0));
        assertTrue(Cardinality.ANY.accepts(1000));
        assertEquals("[0..*]", Cardinality.ANY.toString());
    }

    @Test
    public void twoRestrictionsApplyTogether() {
        Cardinality both = Cardinality.of(null, 1, null).intersect(Cardinality.of(null, null, 2));

        assertEquals(new Cardinality(1, 2), both);
        assertFalse(both.accepts(0));
        assertTrue(both.accepts(2));
        assertFalse(both.accepts(3));
    }

    @Test
    public void contradictoryBoundsAllowNothing() {
        Cardinality contradiction = Cardinality.of(null, 2, 1);

        assertFalse(contradiction.isSatisfiable());
        assertFalse(contradiction.accepts(1));
        assertFalse(contradiction.accepts(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void aNegativeMinimumIsRefused() {
        new Cardinality(-1, null);
    }
}
