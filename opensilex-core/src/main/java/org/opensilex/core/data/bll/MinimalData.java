package org.opensilex.core.data.bll;

import org.opensilex.core.data.dal.ProvEntityModel;

import java.net.URI;
import java.util.List;

/**
 * Minimal infos of a DataModel, used in {@link DataLogic#getFacilitiesToUpdate(List)} to optimize the 1.5.2 migration.
 * The name of this record is quite generic and could be changed in the future if other, similar records need to
 * be created.
 */
public record MinimalData(URI target, URI variable, List<ProvEntityModel> provEntities) {
}
