//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.log;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * @author Arnaud Charleroy
 */
public class PathExclusionMatcherTest {

    private final PathExclusionMatcher matcher =
            new PathExclusionMatcher(List.of("monitoring", "vuejs/extension", "/swagger/"));

    @Test
    public void excludesTheConfiguredPrefixItself() {
        assertTrue(matcher.isExcluded("monitoring"));
        assertTrue(matcher.isExcluded("/monitoring"));
    }

    @Test
    public void excludesEverythingBelowThePrefix() {
        assertTrue(matcher.isExcluded("monitoring/activity"));
        assertTrue(matcher.isExcluded("vuejs/extension/js/opensilex-monitoring.js"));
    }

    @Test
    public void normalisesLeadingAndTrailingSlashesOnBothSides() {
        assertTrue(matcher.isExcluded("swagger/index.html"));
    }

    @Test
    public void doesNotExcludeAPathThatMerelySharesAWordStart() {
        // The dangerous false positive: naive startsWith would swallow this one too.
        assertFalse(matcher.isExcluded("monitoringOfSomethingElse"));
        assertFalse(matcher.isExcluded("core/monitoring"));
    }

    @Test
    public void keepsEverythingElse() {
        assertFalse(matcher.isExcluded("core/data"));
        assertFalse(matcher.isExcluded("security/authenticate"));
        assertFalse(matcher.isExcluded(null));
    }

    @Test
    public void toleratesAnEmptyConfiguration() {
        assertFalse(new PathExclusionMatcher(null).isExcluded("monitoring"));
        assertFalse(new PathExclusionMatcher(List.of()).isExcluded("monitoring"));
    }
}
