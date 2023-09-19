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
public class DatasetItem extends Item {
	@JsonProperty("global_id")
	private String globalId;
	@JsonProperty("published_at")
	private String publishedAt;
	private String description;
	private String citation;
	private String citationHtml;

	public String getType() {
		return "dataset";
	}

	@SuppressWarnings("all")
	public DatasetItem() {
	}

	@SuppressWarnings("all")
	public String getGlobalId() {
		return this.globalId;
	}

	@SuppressWarnings("all")
	public String getPublishedAt() {
		return this.publishedAt;
	}

	@SuppressWarnings("all")
	public String getDescription() {
		return this.description;
	}

	@SuppressWarnings("all")
	public String getCitation() {
		return this.citation;
	}

	@SuppressWarnings("all")
	public String getCitationHtml() {
		return this.citationHtml;
	}

	@JsonProperty("global_id")
	@SuppressWarnings("all")
	public void setGlobalId(final String globalId) {
		this.globalId = globalId;
	}

	@JsonProperty("published_at")
	@SuppressWarnings("all")
	public void setPublishedAt(final String publishedAt) {
		this.publishedAt = publishedAt;
	}

	@SuppressWarnings("all")
	public void setDescription(final String description) {
		this.description = description;
	}

	@SuppressWarnings("all")
	public void setCitation(final String citation) {
		this.citation = citation;
	}

	@SuppressWarnings("all")
	public void setCitationHtml(final String citationHtml) {
		this.citationHtml = citationHtml;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DatasetItem)) return false;
		final DatasetItem other = (DatasetItem) o;
		if (!other.canEqual((Object) this)) return false;
		if (!super.equals(o)) return false;
		final Object this$globalId = this.getGlobalId();
		final Object other$globalId = other.getGlobalId();
		if (this$globalId == null ? other$globalId != null : !this$globalId.equals(other$globalId)) return false;
		final Object this$publishedAt = this.getPublishedAt();
		final Object other$publishedAt = other.getPublishedAt();
		if (this$publishedAt == null ? other$publishedAt != null : !this$publishedAt.equals(other$publishedAt)) return false;
		final Object this$description = this.getDescription();
		final Object other$description = other.getDescription();
		if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
		final Object this$citation = this.getCitation();
		final Object other$citation = other.getCitation();
		if (this$citation == null ? other$citation != null : !this$citation.equals(other$citation)) return false;
		final Object this$citationHtml = this.getCitationHtml();
		final Object other$citationHtml = other.getCitationHtml();
		if (this$citationHtml == null ? other$citationHtml != null : !this$citationHtml.equals(other$citationHtml)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DatasetItem;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = super.hashCode();
		final Object $globalId = this.getGlobalId();
		result = result * PRIME + ($globalId == null ? 43 : $globalId.hashCode());
		final Object $publishedAt = this.getPublishedAt();
		result = result * PRIME + ($publishedAt == null ? 43 : $publishedAt.hashCode());
		final Object $description = this.getDescription();
		result = result * PRIME + ($description == null ? 43 : $description.hashCode());
		final Object $citation = this.getCitation();
		result = result * PRIME + ($citation == null ? 43 : $citation.hashCode());
		final Object $citationHtml = this.getCitationHtml();
		result = result * PRIME + ($citationHtml == null ? 43 : $citationHtml.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DatasetItem(super=" + super.toString() + ", globalId=" + this.getGlobalId() + ", publishedAt=" + this.getPublishedAt() + ", description=" + this.getDescription() + ", citation=" + this.getCitation() + ", citationHtml=" + this.getCitationHtml() + ")";
	}
}
