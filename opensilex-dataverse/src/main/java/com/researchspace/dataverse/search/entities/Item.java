// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.search.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
// used to deserialise Json into correct subclasses using value of 'type'
// property
/**
 * /** <pre>
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
 * Base class of SearchHits returned in the <code>items</code> field of a search
 * result.
 * 
 * @author rspace
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@Type(value = DataverseItem.class, name = "dataverse"), @Type(value = DatasetItem.class, name = "dataset"), @Type(value = FileSearchHit.class, name = "file")})
public abstract class Item {
	private String name;
	private String type;
	private String url;
	@JsonProperty("image_url")
	private String imageUrl;

	@SuppressWarnings("all")
	public Item() {
	}

	@SuppressWarnings("all")
	public String getName() {
		return this.name;
	}

	@SuppressWarnings("all")
	public String getType() {
		return this.type;
	}

	@SuppressWarnings("all")
	public String getUrl() {
		return this.url;
	}

	@SuppressWarnings("all")
	public String getImageUrl() {
		return this.imageUrl;
	}

	@SuppressWarnings("all")
	public void setName(final String name) {
		this.name = name;
	}

	@SuppressWarnings("all")
	public void setType(final String type) {
		this.type = type;
	}

	@SuppressWarnings("all")
	public void setUrl(final String url) {
		this.url = url;
	}

	@JsonProperty("image_url")
	@SuppressWarnings("all")
	public void setImageUrl(final String imageUrl) {
		this.imageUrl = imageUrl;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof Item)) return false;
		final Item other = (Item) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
		final Object this$type = this.getType();
		final Object other$type = other.getType();
		if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
		final Object this$url = this.getUrl();
		final Object other$url = other.getUrl();
		if (this$url == null ? other$url != null : !this$url.equals(other$url)) return false;
		final Object this$imageUrl = this.getImageUrl();
		final Object other$imageUrl = other.getImageUrl();
		if (this$imageUrl == null ? other$imageUrl != null : !this$imageUrl.equals(other$imageUrl)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof Item;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		final Object $type = this.getType();
		result = result * PRIME + ($type == null ? 43 : $type.hashCode());
		final Object $url = this.getUrl();
		result = result * PRIME + ($url == null ? 43 : $url.hashCode());
		final Object $imageUrl = this.getImageUrl();
		result = result * PRIME + ($imageUrl == null ? 43 : $imageUrl.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "Item(name=" + this.getName() + ", type=" + this.getType() + ", url=" + this.getUrl() + ", imageUrl=" + this.getImageUrl() + ")";
	}
}
