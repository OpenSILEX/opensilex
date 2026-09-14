//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.opensilex.core.CoreModule;
import org.opensilex.core.external.opensilex.SharedResourceInstanceService;
import org.opensilex.core.sharedResource.SharedResourceInstanceDTO;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.core.variable.api.VariableGetDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Looks a variable name up on the shared resource instances declared in the {@code core}
 * configuration, so that a variable missing locally can still be pointed at.
 * <p>
 * Nothing is copied here. The report tells the user where the variable lives; importing it stays a
 * deliberate action taken in the variables screen.
 *
 * @author Arnaud Charleroy
 */
public class SharedResourceVariableLookup {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedResourceVariableLookup.class);

    private final List<SharedResourceInstanceDTO> instances;
    private final CoreModule coreModule;
    private final String lang;

    /**
     * One service per instance, created lazily: each one authenticates on first use, and a session
     * may never need any of them.
     */
    private final Map<String, SharedResourceInstanceService> services = new HashMap<>();

    /**
     * Instances that failed, so a single unreachable one is reported once and then left alone.
     */
    private final List<String> failedInstances = new ArrayList<>();

    public SharedResourceVariableLookup(CoreModule coreModule, String lang) {
        this.coreModule = coreModule;
        this.lang = lang;
        List<SharedResourceInstanceDTO> declared;
        try {
            declared = coreModule.getSharedResourceInstancesFromConfiguration(lang);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not read the shared resource instance configuration", e);
            declared = Collections.emptyList();
        }
        this.instances = declared == null ? Collections.emptyList() : declared;
    }

    public boolean isEnable() {
        return !instances.isEmpty();
    }

    public List<SharedResourceInstanceDTO> getInstances() {
        return instances;
    }

    /**
     * @param name the variable name as written in the file
     * @return every match found across the configured instances, tagged with the instance it came
     * from. Empty when nothing matched, or when no instance is configured.
     */
    public List<ResourceReference> search(String name, List<String> outWarnings) {
        List<ResourceReference> matches = new ArrayList<>();
        if (name == null || name.isEmpty()) {
            return matches;
        }

        for (SharedResourceInstanceDTO instance : instances) {
            String instanceUri = instance.getUri() == null ? null : instance.getUri().toString();
            if (instanceUri == null || failedInstances.contains(instanceUri)) {
                continue;
            }
            try {
                matches.addAll(searchOn(instance, instanceUri, name));
            } catch (Exception e) {
                LOGGER.warn("Could not query shared resource instance {}", instanceUri, e);
                failedInstances.add(instanceUri);
                outWarnings.add("The shared resource instance " + label(instance)
                        + " could not be queried, so variables missing locally were not looked up there.");
            }
        }
        return matches;
    }

    private List<ResourceReference> searchOn(SharedResourceInstanceDTO instance, String instanceUri, String name)
            throws Exception {
        SharedResourceInstanceService service = services.computeIfAbsent(instanceUri,
                uri -> new SharedResourceInstanceService(
                        coreModule.getSharedResourceInstanceConfiguration(instance.getUri()), lang));

        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("name", new String[]{name});
        parameters.put("page_size", new String[]{"5"});

        List<ResourceReference> matches = new ArrayList<>();
        for (VariableGetDTO variable : service.search(VariableAPI.PATH, parameters, VariableGetDTO.class).getList()) {
            if (!isExactMatch(name, variable)) {
                continue;
            }
            matches.add(new ResourceReference(variable.getUri(), variable.getName())
                    .setSharedResourceInstance(instanceUri)
                    .setSharedResourceInstanceLabel(label(instance)));
        }
        return matches;
    }

    /**
     * The remote search is a regex, so it returns near misses too. Only an exact name or
     * alternative name counts as a match: a near miss would send the user to the wrong variable.
     */
    private boolean isExactMatch(String name, VariableGetDTO variable) {
        return name.equalsIgnoreCase(variable.getName())
                || name.equalsIgnoreCase(variable.getAlternativeName());
    }

    private String label(SharedResourceInstanceDTO instance) {
        if (instance.getLabel() != null && !instance.getLabel().isEmpty()) {
            return instance.getLabel();
        }
        return String.valueOf(instance.getUri());
    }
}
