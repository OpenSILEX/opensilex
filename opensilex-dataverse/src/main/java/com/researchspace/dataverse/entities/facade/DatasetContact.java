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
public class DatasetContact {
	private String datasetContactEmail;
	private String datasetContactAffiliation;
	private String datasetContactName;

	@SuppressWarnings("all")
	DatasetContact(final String datasetContactEmail, final String datasetContactAffiliation, final String datasetContactName) {
		if (datasetContactEmail == null) {
			throw new NullPointerException("datasetContactEmail is marked non-null but is null");
		}
		this.datasetContactEmail = datasetContactEmail;
		this.datasetContactAffiliation = datasetContactAffiliation;
		this.datasetContactName = datasetContactName;
	}


	@SuppressWarnings("all")
	public static class DatasetContactBuilder {
		@SuppressWarnings("all")
		private String datasetContactEmail;
		@SuppressWarnings("all")
		private String datasetContactAffiliation;
		@SuppressWarnings("all")
		private String datasetContactName;

		@SuppressWarnings("all")
		DatasetContactBuilder() {
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetContactBuilder datasetContactEmail(final String datasetContactEmail) {
			if (datasetContactEmail == null) {
				throw new NullPointerException("datasetContactEmail is marked non-null but is null");
			}
			this.datasetContactEmail = datasetContactEmail;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetContactBuilder datasetContactAffiliation(final String datasetContactAffiliation) {
			this.datasetContactAffiliation = datasetContactAffiliation;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetContactBuilder datasetContactName(final String datasetContactName) {
			this.datasetContactName = datasetContactName;
			return this;
		}

		@SuppressWarnings("all")
		public DatasetContact build() {
			return new DatasetContact(this.datasetContactEmail, this.datasetContactAffiliation, this.datasetContactName);
		}

		@Override
		@SuppressWarnings("all")
		public String toString() {
			return "DatasetContact.DatasetContactBuilder(datasetContactEmail=" + this.datasetContactEmail + ", datasetContactAffiliation=" + this.datasetContactAffiliation + ", datasetContactName=" + this.datasetContactName + ")";
		}
	}

	@SuppressWarnings("all")
	public static DatasetContactBuilder builder() {
		return new DatasetContactBuilder();
	}

	@SuppressWarnings("all")
	public String getDatasetContactEmail() {
		return this.datasetContactEmail;
	}

	@SuppressWarnings("all")
	public String getDatasetContactAffiliation() {
		return this.datasetContactAffiliation;
	}

	@SuppressWarnings("all")
	public String getDatasetContactName() {
		return this.datasetContactName;
	}

	@SuppressWarnings("all")
	public void setDatasetContactEmail(final String datasetContactEmail) {
		if (datasetContactEmail == null) {
			throw new NullPointerException("datasetContactEmail is marked non-null but is null");
		}
		this.datasetContactEmail = datasetContactEmail;
	}

	@SuppressWarnings("all")
	public void setDatasetContactAffiliation(final String datasetContactAffiliation) {
		this.datasetContactAffiliation = datasetContactAffiliation;
	}

	@SuppressWarnings("all")
	public void setDatasetContactName(final String datasetContactName) {
		this.datasetContactName = datasetContactName;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DatasetContact)) return false;
		final DatasetContact other = (DatasetContact) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$datasetContactEmail = this.getDatasetContactEmail();
		final Object other$datasetContactEmail = other.getDatasetContactEmail();
		if (this$datasetContactEmail == null ? other$datasetContactEmail != null : !this$datasetContactEmail.equals(other$datasetContactEmail)) return false;
		final Object this$datasetContactAffiliation = this.getDatasetContactAffiliation();
		final Object other$datasetContactAffiliation = other.getDatasetContactAffiliation();
		if (this$datasetContactAffiliation == null ? other$datasetContactAffiliation != null : !this$datasetContactAffiliation.equals(other$datasetContactAffiliation)) return false;
		final Object this$datasetContactName = this.getDatasetContactName();
		final Object other$datasetContactName = other.getDatasetContactName();
		if (this$datasetContactName == null ? other$datasetContactName != null : !this$datasetContactName.equals(other$datasetContactName)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DatasetContact;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $datasetContactEmail = this.getDatasetContactEmail();
		result = result * PRIME + ($datasetContactEmail == null ? 43 : $datasetContactEmail.hashCode());
		final Object $datasetContactAffiliation = this.getDatasetContactAffiliation();
		result = result * PRIME + ($datasetContactAffiliation == null ? 43 : $datasetContactAffiliation.hashCode());
		final Object $datasetContactName = this.getDatasetContactName();
		result = result * PRIME + ($datasetContactName == null ? 43 : $datasetContactName.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DatasetContact(datasetContactEmail=" + this.getDatasetContactEmail() + ", datasetContactAffiliation=" + this.getDatasetContactAffiliation() + ", datasetContactName=" + this.getDatasetContactName() + ")";
	}
}
