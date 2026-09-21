package org.opensilex.core.device.dal;

import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper to DeviceDAO that caches the results of {@link DeviceDAO#isDeviceType(URI)}.
 * <p>
 * This wrapper only exposes one method for the moment : {@link #isDeviceType(URI)}. If you need to cache other results
 * from the device DAO, consider making another class adapted to the new use case, and renaming this one to avoid
 * confusion and keeping the responsibilities separated.
 */
public class CachedDeviceDAO {
    private final Map<String, Boolean> checkedDevices;
    private final DeviceDAO dao;

    public CachedDeviceDAO(DeviceDAO dao) {
        this.checkedDevices = new HashMap<>();
        this.dao = dao;
    }

    /**
     * Cached version of {@link DeviceDAO#isDeviceType(URI)}
     */
    public boolean isDeviceType(URI uri) throws SPARQLException {
        var key = SPARQLDeserializers.getShortURI(uri);
        var result = checkedDevices.get(SPARQLDeserializers.getShortURI(uri));
        if (result == null) {
            result = dao.isDeviceType(uri);
            checkedDevices.put(key, result);
        }
        return result;
    }
}
