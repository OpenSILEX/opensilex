//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import org.opensilex.aiimport.profile.miappe.MiappeProfile;
import org.opensilex.aiimport.profile.star.StarProfile;
import org.opensilex.aiimport.profile.vitis.VitisExplorerProfile;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * The profiles known to this instance: the ones shipped here, plus any contributed by another
 * module through {@link ServiceLoader}.
 *
 * @author Arnaud Charleroy
 */
public class ImportProfileRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportProfileRegistry.class);

    private final List<ImportProfile> profiles;

    public ImportProfileRegistry() {
        List<ImportProfile> loaded = new ArrayList<>();
        loaded.add(new VitisExplorerProfile());
        loaded.add(new StarProfile());
        loaded.add(new MiappeProfile());
        loaded.add(new GenericTabularProfile());

        try {
            for (ImportProfile contributed : ServiceLoader.load(ImportProfile.class)) {
                if (findIn(loaded, contributed.getId()).isPresent()) {
                    continue;
                }
                loaded.add(contributed);
            }
        } catch (ServiceConfigurationError e) {
            LOGGER.warn("Could not load contributed import profiles", e);
        }

        this.profiles = Collections.unmodifiableList(loaded);
    }

    public List<ImportProfile> getProfiles() {
        return profiles;
    }

    public Optional<ImportProfile> getById(String id) {
        return findIn(profiles, id);
    }

    /**
     * @return the profile that recognises the workbook best. The generic profile scores 1, so it
     * only wins when nothing else recognises the file.
     */
    public ImportProfile select(WorkbookStructure structure) {
        ImportProfile best = null;
        int bestScore = 0;
        for (ImportProfile profile : profiles) {
            int score = profile.match(structure);
            if (score > bestScore) {
                best = profile;
                bestScore = score;
            }
        }
        return best != null ? best : new GenericTabularProfile();
    }

    private static Optional<ImportProfile> findIn(List<ImportProfile> candidates, String id) {
        return candidates.stream()
                .filter(profile -> profile.getId().equals(id))
                .findFirst();
    }
}
