//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.aiimport.mapping.ColumnRole;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The table of roles is the one place that says what a role does, so what it promises has to hold
 * for every entry — including one added tomorrow.
 *
 * @author Arnaud Charleroy
 */
public class DescribedRoleTest {

    private static JsonNode french;
    private static JsonNode english;

    @BeforeClass
    public static void readTheLanguageFiles() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        french = mapper.readTree(new File("front/src/lang/ai-import-fr.json"));
        english = mapper.readTree(new File("front/src/lang/ai-import-en.json"));
    }

    @Test
    public void everyRoleAndEveryKindHasItsLabelInBothLanguages() {
        List<String> keys = new ArrayList<>();
        for (DescribedRole role : DescribedRole.values()) {
            keys.add(role.labelKey());
        }
        for (SheetKind kind : SheetKind.values()) {
            keys.add(kind.labelKey());
        }
        assertNoneMissing(keys);
    }

    @Test
    public void everyRequirementAndEveryLimitHasItsMessageInBothLanguages() {
        List<String> keys = new ArrayList<>();
        for (DescribedRole role : DescribedRole.values()) {
            for (SheetKind kind : SheetKind.values()) {
                if (role.isRequiredBy(kind)) {
                    keys.add(DescriptionRules.MISSING + role.name());
                }
            }
            if (role.isSinglePerSheet()) {
                keys.add(DescriptionRules.SEVERAL + role.name());
            }
        }
        assertFalse("the table asks nothing of any sheet, so this test would pass on anything", keys.isEmpty());
        assertNoneMissing(keys);
    }

    @Test
    public void aRoleASheetRequiresIsAcceptedByThatSheet() {
        for (DescribedRole role : DescribedRole.values()) {
            for (SheetKind kind : SheetKind.values()) {
                if (role.isRequiredBy(kind)) {
                    assertTrue(role + " is required by " + kind + " and must be accepted by it",
                            role.isAcceptedBy(kind));
                }
            }
        }
    }

    @Test
    public void noRoleIsOfferedThatNoSheetCanHave() {
        for (DescribedRole role : DescribedRole.values()) {
            boolean accepted = false;
            for (SheetKind kind : SheetKind.values()) {
                accepted |= kind.isRead() && role.isAcceptedBy(kind);
            }
            assertTrue(role + " is accepted by no kind of sheet", accepted);
        }
    }

    @Test
    public void everySheetKindThatReadsRowsAsObjectsOrFacilitiesHasABaseClass() {
        assertTrue(SheetKind.OBJECTS.baseType().isPresent());
        assertTrue(SheetKind.OBJECTS_AND_DATA.baseType().isPresent());
        assertTrue(SheetKind.FACILITIES.baseType().isPresent());
        assertFalse(SheetKind.DATA.baseType().isPresent());
        assertFalse(SheetKind.FACTORS.baseType().isPresent());
        assertFalse("objects and facilities are told apart by their class",
                SheetKind.OBJECTS.baseType().equals(SheetKind.FACILITIES.baseType()));
    }

    @Test
    public void theDictionaryRolesAreDescribedInTheseTermsWhenThisToolKnowsThem() {
        assertEquals(DescribedRole.OBJECT, DescribedRole.from(ColumnRole.OBJECT).get());
        assertEquals(DescribedRole.GERMPLASM, DescribedRole.from(ColumnRole.GERMPLASM).get());
        assertFalse("a season is not something this tool describes",
                DescribedRole.from(ColumnRole.SEASON).isPresent());
        assertFalse(DescribedRole.from(ColumnRole.UNKNOWN).isPresent());
    }

    @Test
    public void aRoleAndAKindAreParsedFromTheirNameInAnyCase() {
        assertEquals(DescribedRole.FACTOR_LEVEL, DescribedRole.parse(" factor_level ").get());
        assertEquals(SheetKind.OBJECTS_AND_DATA, SheetKind.parse("objects_and_data").get());
        assertFalse(DescribedRole.parse("nonsense").isPresent());
        assertFalse(SheetKind.parse("").isPresent());
        assertFalse(SheetKind.parse(null).isPresent());
    }

    private void assertNoneMissing(List<String> keys) {
        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            if (lookup(french, key) == null) {
                missing.add(key + " (fr)");
            }
            if (lookup(english, key) == null) {
                missing.add(key + " (en)");
            }
        }
        assertTrue("keys with no translation: " + missing, missing.isEmpty());
    }

    private static String lookup(JsonNode root, String key) {
        JsonNode node = root;
        for (String part : key.split("\\.")) {
            node = node == null ? null : node.get(part);
        }
        assertNotNull(root);
        return node == null || !node.isTextual() ? null : node.asText();
    }
}
