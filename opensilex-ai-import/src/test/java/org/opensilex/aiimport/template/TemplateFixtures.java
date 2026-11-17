//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template;

import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.profile.custom.SheetKind;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Templates built by hand, shaped like what a user teaches the module.
 *
 * @author Arnaud Charleroy
 */
public final class TemplateFixtures {

    public static final URI AUTHOR = URI.create("http://opensilex.test/id/account/alice");
    public static final URI PLOT = URI.create("http://www.opensilex.org/vocabulary/oeso#Plot");
    public static final String ALTITUDE = "http://example.org/vocabulary#altitude";

    private TemplateFixtures() {
    }

    /**
     * A plan of plots, and the measures made on them.
     */
    public static FileTemplate planAndMeasures() {
        FileTemplate template = FileTemplate.fromSheets("plan et mesures", List.of(
                new TemplateSheet("plan", List.of("plot_id", "variete", "traitement", "altitude"), SheetKind.OBJECTS,
                        Map.of("plot_id", DescribedRole.OBJECT, "variete", DescribedRole.GERMPLASM,
                                "traitement", DescribedRole.FACTOR_LEVEL, "altitude", DescribedRole.OBJECT_PROPERTY),
                        PLOT, Map.of("altitude", ALTITUDE)),
                new TemplateSheet("mesures", List.of("plot_id", "date", "hauteur_cm"), SheetKind.DATA,
                        Map.of("plot_id", DescribedRole.OBJECT, "date", DescribedRole.DATE,
                                "hauteur_cm", DescribedRole.VARIABLE),
                        null, Map.of())));
        return template.ownedBy(AUTHOR, "Alice");
    }
}
