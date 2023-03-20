package org.opensilex.core.config;

import org.opensilex.config.ConfigDescription;

import java.util.Map;

public interface SharedResourceInstanceItem {

    @ConfigDescription("Shared Resource Instance API base URI (should end with '/rest/')")
    String uri();

    @ConfigDescription("SRI label")
    Map<String, String> label();

    @ConfigDescription("Account name for SRI")
    String accountName();

    @ConfigDescription("Account password for SRI")
    String accountPassword();
}
