// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.entities.facade;

import java.net.URI;

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
public class DatasetTopicClassification {
	private String topicClassValue;
	private String topicClassVocab;
	private URI topicClassVocabURI;

	@SuppressWarnings("all")
	DatasetTopicClassification(final String topicClassValue, final String topicClassVocab, final URI topicClassVocabURI) {
		this.topicClassValue = topicClassValue;
		this.topicClassVocab = topicClassVocab;
		this.topicClassVocabURI = topicClassVocabURI;
	}


	@SuppressWarnings("all")
	public static class DatasetTopicClassificationBuilder {
		@SuppressWarnings("all")
		private String topicClassValue;
		@SuppressWarnings("all")
		private String topicClassVocab;
		@SuppressWarnings("all")
		private URI topicClassVocabURI;

		@SuppressWarnings("all")
		DatasetTopicClassificationBuilder() {
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetTopicClassificationBuilder topicClassValue(final String topicClassValue) {
			this.topicClassValue = topicClassValue;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetTopicClassificationBuilder topicClassVocab(final String topicClassVocab) {
			this.topicClassVocab = topicClassVocab;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetTopicClassificationBuilder topicClassVocabURI(final URI topicClassVocabURI) {
			this.topicClassVocabURI = topicClassVocabURI;
			return this;
		}

		@SuppressWarnings("all")
		public DatasetTopicClassification build() {
			return new DatasetTopicClassification(this.topicClassValue, this.topicClassVocab, this.topicClassVocabURI);
		}

		@Override
		@SuppressWarnings("all")
		public String toString() {
			return "DatasetTopicClassification.DatasetTopicClassificationBuilder(topicClassValue=" + this.topicClassValue + ", topicClassVocab=" + this.topicClassVocab + ", topicClassVocabURI=" + this.topicClassVocabURI + ")";
		}
	}

	@SuppressWarnings("all")
	public static DatasetTopicClassificationBuilder builder() {
		return new DatasetTopicClassificationBuilder();
	}

	@SuppressWarnings("all")
	public String getTopicClassValue() {
		return this.topicClassValue;
	}

	@SuppressWarnings("all")
	public String getTopicClassVocab() {
		return this.topicClassVocab;
	}

	@SuppressWarnings("all")
	public URI getTopicClassVocabURI() {
		return this.topicClassVocabURI;
	}

	@SuppressWarnings("all")
	public void setTopicClassValue(final String topicClassValue) {
		this.topicClassValue = topicClassValue;
	}

	@SuppressWarnings("all")
	public void setTopicClassVocab(final String topicClassVocab) {
		this.topicClassVocab = topicClassVocab;
	}

	@SuppressWarnings("all")
	public void setTopicClassVocabURI(final URI topicClassVocabURI) {
		this.topicClassVocabURI = topicClassVocabURI;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DatasetTopicClassification)) return false;
		final DatasetTopicClassification other = (DatasetTopicClassification) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$topicClassValue = this.getTopicClassValue();
		final Object other$topicClassValue = other.getTopicClassValue();
		if (this$topicClassValue == null ? other$topicClassValue != null : !this$topicClassValue.equals(other$topicClassValue)) return false;
		final Object this$topicClassVocab = this.getTopicClassVocab();
		final Object other$topicClassVocab = other.getTopicClassVocab();
		if (this$topicClassVocab == null ? other$topicClassVocab != null : !this$topicClassVocab.equals(other$topicClassVocab)) return false;
		final Object this$topicClassVocabURI = this.getTopicClassVocabURI();
		final Object other$topicClassVocabURI = other.getTopicClassVocabURI();
		if (this$topicClassVocabURI == null ? other$topicClassVocabURI != null : !this$topicClassVocabURI.equals(other$topicClassVocabURI)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DatasetTopicClassification;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $topicClassValue = this.getTopicClassValue();
		result = result * PRIME + ($topicClassValue == null ? 43 : $topicClassValue.hashCode());
		final Object $topicClassVocab = this.getTopicClassVocab();
		result = result * PRIME + ($topicClassVocab == null ? 43 : $topicClassVocab.hashCode());
		final Object $topicClassVocabURI = this.getTopicClassVocabURI();
		result = result * PRIME + ($topicClassVocabURI == null ? 43 : $topicClassVocabURI.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DatasetTopicClassification(topicClassValue=" + this.getTopicClassValue() + ", topicClassVocab=" + this.getTopicClassVocab() + ", topicClassVocabURI=" + this.getTopicClassVocabURI() + ")";
	}
}
