// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.api.v1;

import java.net.URL;

/**
 * <pre>
 *  Copyright 2016 ResearchSpace
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * </pre>
 * Configures the server, apikey and root dataverse alias
 * @author rspace
 */
public class DataverseConfig {
	private URL serverURL;
	/**
	 * The API Key provided from Dataverse account
	 */
	private String apiKey;
	private String repositoryName;

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DataverseConfig)) return false;
		final DataverseConfig other = (DataverseConfig) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$serverURL = this.getServerURL();
		final Object other$serverURL = other.getServerURL();
		if (this$serverURL == null ? other$serverURL != null : !this$serverURL.equals(other$serverURL)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DataverseConfig;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $serverURL = this.getServerURL();
		result = result * PRIME + ($serverURL == null ? 43 : $serverURL.hashCode());
		return result;
	}

	@SuppressWarnings("all")
	public DataverseConfig(final URL serverURL, final String apiKey, final String repositoryName) {
		this.serverURL = serverURL;
		this.apiKey = apiKey;
		this.repositoryName = repositoryName;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DataverseConfig(serverURL=" + this.getServerURL() + ", apiKey=" + this.getApiKey() + ", repositoryName=" + this.getRepositoryName() + ")";
	}

	@SuppressWarnings("all")
	public URL getServerURL() {
		return this.serverURL;
	}

	/**
	 * The API Key provided from Dataverse account
	 * @return the apiKey
	 */
	@SuppressWarnings("all")
	public String getApiKey() {
		return this.apiKey;
	}

	@SuppressWarnings("all")
	public String getRepositoryName() {
		return this.repositoryName;
	}
}
