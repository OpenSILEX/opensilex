// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
package com.researchspace.dataverse.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;

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
@JsonFormat
public class DataversePost {
	private String id;
	private String alias;
	private String name;
	private String affiliation;
	private String permissionRoot;
	private String description;
	private String ownerId;
	private Date creationDate;
	private List<DataverseContacts> dataverseContacts;

	@SuppressWarnings("all")
	public DataversePost() {
	}

	@SuppressWarnings("all")
	public String getId() {
		return this.id;
	}

	@SuppressWarnings("all")
	public String getAlias() {
		return this.alias;
	}

	@SuppressWarnings("all")
	public String getName() {
		return this.name;
	}

	@SuppressWarnings("all")
	public String getAffiliation() {
		return this.affiliation;
	}

	@SuppressWarnings("all")
	public String getPermissionRoot() {
		return this.permissionRoot;
	}

	@SuppressWarnings("all")
	public String getDescription() {
		return this.description;
	}

	@SuppressWarnings("all")
	public String getOwnerId() {
		return this.ownerId;
	}

	@SuppressWarnings("all")
	public Date getCreationDate() {
		return this.creationDate;
	}

	@SuppressWarnings("all")
	public List<DataverseContacts> getDataverseContacts() {
		return this.dataverseContacts;
	}

	@SuppressWarnings("all")
	public void setId(final String id) {
		this.id = id;
	}

	@SuppressWarnings("all")
	public void setAlias(final String alias) {
		this.alias = alias;
	}

	@SuppressWarnings("all")
	public void setName(final String name) {
		this.name = name;
	}

	@SuppressWarnings("all")
	public void setAffiliation(final String affiliation) {
		this.affiliation = affiliation;
	}

	@SuppressWarnings("all")
	public void setPermissionRoot(final String permissionRoot) {
		this.permissionRoot = permissionRoot;
	}

	@SuppressWarnings("all")
	public void setDescription(final String description) {
		this.description = description;
	}

	@SuppressWarnings("all")
	public void setOwnerId(final String ownerId) {
		this.ownerId = ownerId;
	}

	@SuppressWarnings("all")
	public void setCreationDate(final Date creationDate) {
		this.creationDate = creationDate;
	}

	@SuppressWarnings("all")
	public void setDataverseContacts(final List<DataverseContacts> dataverseContacts) {
		this.dataverseContacts = dataverseContacts;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DataversePost)) return false;
		final DataversePost other = (DataversePost) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
		final Object this$alias = this.getAlias();
		final Object other$alias = other.getAlias();
		if (this$alias == null ? other$alias != null : !this$alias.equals(other$alias)) return false;
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
		final Object this$affiliation = this.getAffiliation();
		final Object other$affiliation = other.getAffiliation();
		if (this$affiliation == null ? other$affiliation != null : !this$affiliation.equals(other$affiliation)) return false;
		final Object this$permissionRoot = this.getPermissionRoot();
		final Object other$permissionRoot = other.getPermissionRoot();
		if (this$permissionRoot == null ? other$permissionRoot != null : !this$permissionRoot.equals(other$permissionRoot)) return false;
		final Object this$description = this.getDescription();
		final Object other$description = other.getDescription();
		if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
		final Object this$ownerId = this.getOwnerId();
		final Object other$ownerId = other.getOwnerId();
		if (this$ownerId == null ? other$ownerId != null : !this$ownerId.equals(other$ownerId)) return false;
		final Object this$creationDate = this.getCreationDate();
		final Object other$creationDate = other.getCreationDate();
		if (this$creationDate == null ? other$creationDate != null : !this$creationDate.equals(other$creationDate)) return false;
		final Object this$dataverseContacts = this.getDataverseContacts();
		final Object other$dataverseContacts = other.getDataverseContacts();
		if (this$dataverseContacts == null ? other$dataverseContacts != null : !this$dataverseContacts.equals(other$dataverseContacts)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DataversePost;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		final Object $alias = this.getAlias();
		result = result * PRIME + ($alias == null ? 43 : $alias.hashCode());
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		final Object $affiliation = this.getAffiliation();
		result = result * PRIME + ($affiliation == null ? 43 : $affiliation.hashCode());
		final Object $permissionRoot = this.getPermissionRoot();
		result = result * PRIME + ($permissionRoot == null ? 43 : $permissionRoot.hashCode());
		final Object $description = this.getDescription();
		result = result * PRIME + ($description == null ? 43 : $description.hashCode());
		final Object $ownerId = this.getOwnerId();
		result = result * PRIME + ($ownerId == null ? 43 : $ownerId.hashCode());
		final Object $creationDate = this.getCreationDate();
		result = result * PRIME + ($creationDate == null ? 43 : $creationDate.hashCode());
		final Object $dataverseContacts = this.getDataverseContacts();
		result = result * PRIME + ($dataverseContacts == null ? 43 : $dataverseContacts.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DataversePost(id=" + this.getId() + ", alias=" + this.getAlias() + ", name=" + this.getName() + ", affiliation=" + this.getAffiliation() + ", permissionRoot=" + this.getPermissionRoot() + ", description=" + this.getDescription() + ", ownerId=" + this.getOwnerId() + ", creationDate=" + this.getCreationDate() + ", dataverseContacts=" + this.getDataverseContacts() + ")";
	}
}
