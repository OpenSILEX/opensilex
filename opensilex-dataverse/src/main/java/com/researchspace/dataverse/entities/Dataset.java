// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.entities;

import java.net.URL;
import java.util.Optional;

/**
 * <pre>
 * Copyright 2016 ResearchSpace
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </pre>
 */
public class Dataset {
	private DatasetVersion datasetVersion;
	private DatasetVersion latestVersion;
	private Long id;
	private String identifier;
	private String protocol;
	private String authority;
	private URL persistentUrl;

	/**
	 * Getter for the DOI String used to identify a dataset for SWORD upload
	 * @return an {@link Optional}. Will be <code>null</code> if <code>persistentURL</code> is not set.
	 */
	public Optional<String> getDoiId() {
		if (persistentUrl == null) {
			return Optional.empty();
		} else {
			return Optional.of(getPersistentUrl().getPath().substring(1));
		}
	}

	@SuppressWarnings("all")
	public DatasetVersion getDatasetVersion() {
		return this.datasetVersion;
	}

	@SuppressWarnings("all")
	public DatasetVersion getLatestVersion() {
		return this.latestVersion;
	}

	@SuppressWarnings("all")
	public Long getId() {
		return this.id;
	}

	@SuppressWarnings("all")
	public String getIdentifier() {
		return this.identifier;
	}

	@SuppressWarnings("all")
	public String getProtocol() {
		return this.protocol;
	}

	@SuppressWarnings("all")
	public String getAuthority() {
		return this.authority;
	}

	@SuppressWarnings("all")
	public URL getPersistentUrl() {
		return this.persistentUrl;
	}

	@SuppressWarnings("all")
	public void setDatasetVersion(final DatasetVersion datasetVersion) {
		this.datasetVersion = datasetVersion;
	}

	@SuppressWarnings("all")
	public void setLatestVersion(final DatasetVersion latestVersion) {
		this.latestVersion = latestVersion;
	}

	@SuppressWarnings("all")
	public void setId(final Long id) {
		this.id = id;
	}

	@SuppressWarnings("all")
	public void setIdentifier(final String identifier) {
		this.identifier = identifier;
	}

	@SuppressWarnings("all")
	public void setProtocol(final String protocol) {
		this.protocol = protocol;
	}

	@SuppressWarnings("all")
	public void setAuthority(final String authority) {
		this.authority = authority;
	}

	@SuppressWarnings("all")
	public void setPersistentUrl(final URL persistentUrl) {
		this.persistentUrl = persistentUrl;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof Dataset)) return false;
		final Dataset other = (Dataset) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
		final Object this$datasetVersion = this.getDatasetVersion();
		final Object other$datasetVersion = other.getDatasetVersion();
		if (this$datasetVersion == null ? other$datasetVersion != null : !this$datasetVersion.equals(other$datasetVersion)) return false;
		final Object this$latestVersion = this.getLatestVersion();
		final Object other$latestVersion = other.getLatestVersion();
		if (this$latestVersion == null ? other$latestVersion != null : !this$latestVersion.equals(other$latestVersion)) return false;
		final Object this$identifier = this.getIdentifier();
		final Object other$identifier = other.getIdentifier();
		if (this$identifier == null ? other$identifier != null : !this$identifier.equals(other$identifier)) return false;
		final Object this$protocol = this.getProtocol();
		final Object other$protocol = other.getProtocol();
		if (this$protocol == null ? other$protocol != null : !this$protocol.equals(other$protocol)) return false;
		final Object this$authority = this.getAuthority();
		final Object other$authority = other.getAuthority();
		if (this$authority == null ? other$authority != null : !this$authority.equals(other$authority)) return false;
		final Object this$persistentUrl = this.getPersistentUrl();
		final Object other$persistentUrl = other.getPersistentUrl();
		if (this$persistentUrl == null ? other$persistentUrl != null : !this$persistentUrl.equals(other$persistentUrl)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof Dataset;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		final Object $datasetVersion = this.getDatasetVersion();
		result = result * PRIME + ($datasetVersion == null ? 43 : $datasetVersion.hashCode());
		final Object $latestVersion = this.getLatestVersion();
		result = result * PRIME + ($latestVersion == null ? 43 : $latestVersion.hashCode());
		final Object $identifier = this.getIdentifier();
		result = result * PRIME + ($identifier == null ? 43 : $identifier.hashCode());
		final Object $protocol = this.getProtocol();
		result = result * PRIME + ($protocol == null ? 43 : $protocol.hashCode());
		final Object $authority = this.getAuthority();
		result = result * PRIME + ($authority == null ? 43 : $authority.hashCode());
		final Object $persistentUrl = this.getPersistentUrl();
		result = result * PRIME + ($persistentUrl == null ? 43 : $persistentUrl.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "Dataset(datasetVersion=" + this.getDatasetVersion() + ", latestVersion=" + this.getLatestVersion() + ", id=" + this.getId() + ", identifier=" + this.getIdentifier() + ", protocol=" + this.getProtocol() + ", authority=" + this.getAuthority() + ", persistentUrl=" + this.getPersistentUrl() + ")";
	}

	@SuppressWarnings("all")
	public Dataset(final DatasetVersion datasetVersion, final DatasetVersion latestVersion, final Long id, final String identifier, final String protocol, final String authority, final URL persistentUrl) {
		this.datasetVersion = datasetVersion;
		this.latestVersion = latestVersion;
		this.id = id;
		this.identifier = identifier;
		this.protocol = protocol;
		this.authority = authority;
		this.persistentUrl = persistentUrl;
	}

	@SuppressWarnings("all")
	public Dataset() {
	}
}
