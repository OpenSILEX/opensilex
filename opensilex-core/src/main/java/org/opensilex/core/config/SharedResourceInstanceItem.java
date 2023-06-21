package org.opensilex.core.config;

import org.opensilex.config.ConfigDescription;

import java.util.Map;

public interface SharedResourceInstanceItem {

    @ConfigDescription("Unique identifier for the SRI")
    String uri();

    @ConfigDescription("Shared Resource Instance API base URI (should end with '/rest/')")
    String apiUrl();

    @ConfigDescription("SRI label")
    Map<String, String> label();

    @ConfigDescription("Account name for SRI")
    String accountName();

    @ConfigDescription("Account password for SRI")
    String accountPassword();
}
