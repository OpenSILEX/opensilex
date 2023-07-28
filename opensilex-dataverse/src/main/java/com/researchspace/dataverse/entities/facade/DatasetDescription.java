// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.entities.facade;

import java.util.Date;

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
public class DatasetDescription {
	private String description;
	private Date date;

	@SuppressWarnings("all")
	DatasetDescription(final String description, final Date date) {
		if (description == null) {
			throw new NullPointerException("description is marked non-null but is null");
		}
		this.description = description;
		this.date = date;
	}


	@SuppressWarnings("all")
	public static class DatasetDescriptionBuilder {
		@SuppressWarnings("all")
		private String description;
		@SuppressWarnings("all")
		private Date date;

		@SuppressWarnings("all")
		DatasetDescriptionBuilder() {
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetDescriptionBuilder description(final String description) {
			if (description == null) {
				throw new NullPointerException("description is marked non-null but is null");
			}
			this.description = description;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetDescriptionBuilder date(final Date date) {
			this.date = date;
			return this;
		}

		@SuppressWarnings("all")
		public DatasetDescription build() {
			return new DatasetDescription(this.description, this.date);
		}

		@Override
		@SuppressWarnings("all")
		public String toString() {
			return "DatasetDescription.DatasetDescriptionBuilder(description=" + this.description + ", date=" + this.date + ")";
		}
	}

	@SuppressWarnings("all")
	public static DatasetDescriptionBuilder builder() {
		return new DatasetDescriptionBuilder();
	}

	@SuppressWarnings("all")
	public String getDescription() {
		return this.description;
	}

	@SuppressWarnings("all")
	public Date getDate() {
		return this.date;
	}

	@SuppressWarnings("all")
	public void setDescription(final String description) {
		if (description == null) {
			throw new NullPointerException("description is marked non-null but is null");
		}
		this.description = description;
	}

	@SuppressWarnings("all")
	public void setDate(final Date date) {
		this.date = date;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DatasetDescription)) return false;
		final DatasetDescription other = (DatasetDescription) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$description = this.getDescription();
		final Object other$description = other.getDescription();
		if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
		final Object this$date = this.getDate();
		final Object other$date = other.getDate();
		if (this$date == null ? other$date != null : !this$date.equals(other$date)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DatasetDescription;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $description = this.getDescription();
		result = result * PRIME + ($description == null ? 43 : $description.hashCode());
		final Object $date = this.getDate();
		result = result * PRIME + ($date == null ? 43 : $date.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DatasetDescription(description=" + this.getDescription() + ", date=" + this.getDate() + ")";
	}
}
