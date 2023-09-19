// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.entities.facade;

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
public class DatasetContributor {
	private String name;
	private ContributorType type;

	@SuppressWarnings("all")
	DatasetContributor(final String name, final ContributorType type) {
		this.name = name;
		this.type = type;
	}


	@SuppressWarnings("all")
	public static class DatasetContributorBuilder {
		@SuppressWarnings("all")
		private String name;
		@SuppressWarnings("all")
		private ContributorType type;

		@SuppressWarnings("all")
		DatasetContributorBuilder() {
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetContributorBuilder name(final String name) {
			this.name = name;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetContributorBuilder type(final ContributorType type) {
			this.type = type;
			return this;
		}

		@SuppressWarnings("all")
		public DatasetContributor build() {
			return new DatasetContributor(this.name, this.type);
		}

		@Override
		@SuppressWarnings("all")
		public String toString() {
			return "DatasetContributor.DatasetContributorBuilder(name=" + this.name + ", type=" + this.type + ")";
		}
	}

	@SuppressWarnings("all")
	public static DatasetContributorBuilder builder() {
		return new DatasetContributorBuilder();
	}

	@SuppressWarnings("all")
	public String getName() {
		return this.name;
	}

	@SuppressWarnings("all")
	public ContributorType getType() {
		return this.type;
	}

	@SuppressWarnings("all")
	public void setName(final String name) {
		this.name = name;
	}

	@SuppressWarnings("all")
	public void setType(final ContributorType type) {
		this.type = type;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DatasetContributor)) return false;
		final DatasetContributor other = (DatasetContributor) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
		final Object this$type = this.getType();
		final Object other$type = other.getType();
		if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DatasetContributor;
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
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DatasetContributor(name=" + this.getName() + ", type=" + this.getType() + ")";
	}
}
