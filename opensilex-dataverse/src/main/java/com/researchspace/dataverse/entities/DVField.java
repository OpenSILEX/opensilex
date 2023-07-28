// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.entities;

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
public class DVField {
	private String name;
	private String displayName;
	private String title;
	private String type;
	private String watermark;
	private String description;

	@SuppressWarnings("all")
	public DVField() {
	}

	@SuppressWarnings("all")
	public String getName() {
		return this.name;
	}

	@SuppressWarnings("all")
	public String getDisplayName() {
		return this.displayName;
	}

	@SuppressWarnings("all")
	public String getTitle() {
		return this.title;
	}

	@SuppressWarnings("all")
	public String getType() {
		return this.type;
	}

	@SuppressWarnings("all")
	public String getWatermark() {
		return this.watermark;
	}

	@SuppressWarnings("all")
	public String getDescription() {
		return this.description;
	}

	@SuppressWarnings("all")
	public void setName(final String name) {
		this.name = name;
	}

	@SuppressWarnings("all")
	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	@SuppressWarnings("all")
	public void setTitle(final String title) {
		this.title = title;
	}

	@SuppressWarnings("all")
	public void setType(final String type) {
		this.type = type;
	}

	@SuppressWarnings("all")
	public void setWatermark(final String watermark) {
		this.watermark = watermark;
	}

	@SuppressWarnings("all")
	public void setDescription(final String description) {
		this.description = description;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DVField)) return false;
		final DVField other = (DVField) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
		final Object this$displayName = this.getDisplayName();
		final Object other$displayName = other.getDisplayName();
		if (this$displayName == null ? other$displayName != null : !this$displayName.equals(other$displayName)) return false;
		final Object this$title = this.getTitle();
		final Object other$title = other.getTitle();
		if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
		final Object this$type = this.getType();
		final Object other$type = other.getType();
		if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
		final Object this$watermark = this.getWatermark();
		final Object other$watermark = other.getWatermark();
		if (this$watermark == null ? other$watermark != null : !this$watermark.equals(other$watermark)) return false;
		final Object this$description = this.getDescription();
		final Object other$description = other.getDescription();
		if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DVField;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		final Object $displayName = this.getDisplayName();
		result = result * PRIME + ($displayName == null ? 43 : $displayName.hashCode());
		final Object $title = this.getTitle();
		result = result * PRIME + ($title == null ? 43 : $title.hashCode());
		final Object $type = this.getType();
		result = result * PRIME + ($type == null ? 43 : $type.hashCode());
		final Object $watermark = this.getWatermark();
		result = result * PRIME + ($watermark == null ? 43 : $watermark.hashCode());
		final Object $description = this.getDescription();
		result = result * PRIME + ($description == null ? 43 : $description.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DVField(name=" + this.getName() + ", displayName=" + this.getDisplayName() + ", title=" + this.getTitle() + ", type=" + this.getType() + ", watermark=" + this.getWatermark() + ", description=" + this.getDescription() + ")";
	}
}
