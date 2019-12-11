/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.user;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.opensilex.server.user.dal.UserModel;
import org.opensilex.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vidalmor
 */
public class UserRegistryService implements Service {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserRegistryService.class);

    private Map<URI, UserModel> userRegistry = new HashMap<>();
    private Map<URI, Thread> schedulerRegistry = new HashMap<>();

    public boolean hasUser(UserModel user) {
        return hasUserURI(user.getUri());
    }

    public void addUser(UserModel user, long expireMs) {
        URI userURI = user.getUri();

        if (hasUserURI(userURI)) {
            removeUserByURI(userURI);
        }
        
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(expireMs);
                LOGGER.debug("User connection timeout: " + userURI);
                removeUser(user);
            } catch (InterruptedException e) {
                LOGGER.debug("Revoke user: " + userURI + " - " + e.getMessage());
            }
        });
        
        t.start();
        
        userRegistry.put(userURI, user);
        schedulerRegistry.put(userURI, t);

        LOGGER.debug("Register user: " + userURI);
    }

    public UserModel removeUser(UserModel user) {
        return removeUserByURI(user.getUri());
    }

    public UserModel removeUserByURI(URI userURI) {
        if (hasUserURI(userURI)) {
            LOGGER.debug("Unregister user: " + userURI);
            schedulerRegistry.get(userURI).interrupt();
            schedulerRegistry.remove(userURI);
            return userRegistry.remove(userURI);
        }

        return null;
    }

    public boolean hasUserURI(URI userURI) {
        return userRegistry.containsKey(userURI);
    }

    public UserModel getUserByUri(URI userURI) {
        return userRegistry.get(userURI);
    }
}
