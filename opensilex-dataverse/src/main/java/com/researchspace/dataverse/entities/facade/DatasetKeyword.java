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
public class DatasetKeyword {
	private String value;
	private String vocabulary;
	private URI vocabularyURI;

	@SuppressWarnings("all")
	DatasetKeyword(final String value, final String vocabulary, final URI vocabularyURI) {
		if (value == null) {
			throw new NullPointerException("value is marked non-null but is null");
		}
		this.value = value;
		this.vocabulary = vocabulary;
		this.vocabularyURI = vocabularyURI;
	}


	@SuppressWarnings("all")
	public static class DatasetKeywordBuilder {
		@SuppressWarnings("all")
		private String value;
		@SuppressWarnings("all")
		private String vocabulary;
		@SuppressWarnings("all")
		private URI vocabularyURI;

		@SuppressWarnings("all")
		DatasetKeywordBuilder() {
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetKeywordBuilder value(final String value) {
			if (value == null) {
				throw new NullPointerException("value is marked non-null but is null");
			}
			this.value = value;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetKeywordBuilder vocabulary(final String vocabulary) {
			this.vocabulary = vocabulary;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetKeywordBuilder vocabularyURI(final URI vocabularyURI) {
			this.vocabularyURI = vocabularyURI;
			return this;
		}

		@SuppressWarnings("all")
		public DatasetKeyword build() {
			return new DatasetKeyword(this.value, this.vocabulary, this.vocabularyURI);
		}

		@Override
		@SuppressWarnings("all")
		public String toString() {
			return "DatasetKeyword.DatasetKeywordBuilder(value=" + this.value + ", vocabulary=" + this.vocabulary + ", vocabularyURI=" + this.vocabularyURI + ")";
		}
	}

	@SuppressWarnings("all")
	public static DatasetKeywordBuilder builder() {
		return new DatasetKeywordBuilder();
	}

	@SuppressWarnings("all")
	public String getValue() {
		return this.value;
	}

	@SuppressWarnings("all")
	public String getVocabulary() {
		return this.vocabulary;
	}

	@SuppressWarnings("all")
	public URI getVocabularyURI() {
		return this.vocabularyURI;
	}

	@SuppressWarnings("all")
	public void setValue(final String value) {
		if (value == null) {
			throw new NullPointerException("value is marked non-null but is null");
		}
		this.value = value;
	}

	@SuppressWarnings("all")
	public void setVocabulary(final String vocabulary) {
		this.vocabulary = vocabulary;
	}

	@SuppressWarnings("all")
	public void setVocabularyURI(final URI vocabularyURI) {
		this.vocabularyURI = vocabularyURI;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DatasetKeyword)) return false;
		final DatasetKeyword other = (DatasetKeyword) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$value = this.getValue();
		final Object other$value = other.getValue();
		if (this$value == null ? other$value != null : !this$value.equals(other$value)) return false;
		final Object this$vocabulary = this.getVocabulary();
		final Object other$vocabulary = other.getVocabulary();
		if (this$vocabulary == null ? other$vocabulary != null : !this$vocabulary.equals(other$vocabulary)) return false;
		final Object this$vocabularyURI = this.getVocabularyURI();
		final Object other$vocabularyURI = other.getVocabularyURI();
		if (this$vocabularyURI == null ? other$vocabularyURI != null : !this$vocabularyURI.equals(other$vocabularyURI)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DatasetKeyword;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $value = this.getValue();
		result = result * PRIME + ($value == null ? 43 : $value.hashCode());
		final Object $vocabulary = this.getVocabulary();
		result = result * PRIME + ($vocabulary == null ? 43 : $vocabulary.hashCode());
		final Object $vocabularyURI = this.getVocabularyURI();
		result = result * PRIME + ($vocabularyURI == null ? 43 : $vocabularyURI.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DatasetKeyword(value=" + this.getValue() + ", vocabulary=" + this.getVocabulary() + ", vocabularyURI=" + this.getVocabularyURI() + ")";
	}
}
