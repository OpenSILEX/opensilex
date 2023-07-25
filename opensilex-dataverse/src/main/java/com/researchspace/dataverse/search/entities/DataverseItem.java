// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.search.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class DataverseItem extends Item {
	String identifier;
	@JsonProperty("published_at")
	String publishedAt;

	public String getType() {
		return "dataverse";
	}

	@SuppressWarnings("all")
	public DataverseItem() {
	}

	@SuppressWarnings("all")
	public String getIdentifier() {
		return this.identifier;
	}

	@SuppressWarnings("all")
	public String getPublishedAt() {
		return this.publishedAt;
	}

	@SuppressWarnings("all")
	public void setIdentifier(final String identifier) {
		this.identifier = identifier;
	}

	@JsonProperty("published_at")
	@SuppressWarnings("all")
	public void setPublishedAt(final String publishedAt) {
		this.publishedAt = publishedAt;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DataverseItem)) return false;
		final DataverseItem other = (DataverseItem) o;
		if (!other.canEqual((Object) this)) return false;
		if (!super.equals(o)) return false;
		final Object this$identifier = this.getIdentifier();
		final Object other$identifier = other.getIdentifier();
		if (this$identifier == null ? other$identifier != null : !this$identifier.equals(other$identifier)) return false;
		final Object this$publishedAt = this.getPublishedAt();
		final Object other$publishedAt = other.getPublishedAt();
		if (this$publishedAt == null ? other$publishedAt != null : !this$publishedAt.equals(other$publishedAt)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DataverseItem;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = super.hashCode();
		final Object $identifier = this.getIdentifier();
		result = result * PRIME + ($identifier == null ? 43 : $identifier.hashCode());
		final Object $publishedAt = this.getPublishedAt();
		result = result * PRIME + ($publishedAt == null ? 43 : $publishedAt.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DataverseItem(super=" + super.toString() + ", identifier=" + this.getIdentifier() + ", publishedAt=" + this.getPublishedAt() + ")";
	}
}
