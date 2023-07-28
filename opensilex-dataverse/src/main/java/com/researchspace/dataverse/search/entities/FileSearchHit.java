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
public class FileSearchHit extends Item {
	@JsonProperty("file_id")
	private String fileId;
	@JsonProperty("dataset_citation")
	private String datasetCitation;
	@JsonProperty("file_content_type")
	private String fileContentType;
	private String description;
	private String md5;
	@JsonProperty("size_in_bytes")
	private int size;
	@JsonProperty("published_at")
	private String publishedAt;

	public String getType() {
		return "file";
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof FileSearchHit)) return false;
		final FileSearchHit other = (FileSearchHit) o;
		if (!other.canEqual((Object) this)) return false;
		if (!super.equals(o)) return false;
		if (this.getSize() != other.getSize()) return false;
		final Object this$fileId = this.getFileId();
		final Object other$fileId = other.getFileId();
		if (this$fileId == null ? other$fileId != null : !this$fileId.equals(other$fileId)) return false;
		final Object this$datasetCitation = this.getDatasetCitation();
		final Object other$datasetCitation = other.getDatasetCitation();
		if (this$datasetCitation == null ? other$datasetCitation != null : !this$datasetCitation.equals(other$datasetCitation)) return false;
		final Object this$fileContentType = this.getFileContentType();
		final Object other$fileContentType = other.getFileContentType();
		if (this$fileContentType == null ? other$fileContentType != null : !this$fileContentType.equals(other$fileContentType)) return false;
		final Object this$description = this.getDescription();
		final Object other$description = other.getDescription();
		if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
		final Object this$md5 = this.getMd5();
		final Object other$md5 = other.getMd5();
		if (this$md5 == null ? other$md5 != null : !this$md5.equals(other$md5)) return false;
		final Object this$publishedAt = this.getPublishedAt();
		final Object other$publishedAt = other.getPublishedAt();
		if (this$publishedAt == null ? other$publishedAt != null : !this$publishedAt.equals(other$publishedAt)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof FileSearchHit;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = super.hashCode();
		result = result * PRIME + this.getSize();
		final Object $fileId = this.getFileId();
		result = result * PRIME + ($fileId == null ? 43 : $fileId.hashCode());
		final Object $datasetCitation = this.getDatasetCitation();
		result = result * PRIME + ($datasetCitation == null ? 43 : $datasetCitation.hashCode());
		final Object $fileContentType = this.getFileContentType();
		result = result * PRIME + ($fileContentType == null ? 43 : $fileContentType.hashCode());
		final Object $description = this.getDescription();
		result = result * PRIME + ($description == null ? 43 : $description.hashCode());
		final Object $md5 = this.getMd5();
		result = result * PRIME + ($md5 == null ? 43 : $md5.hashCode());
		final Object $publishedAt = this.getPublishedAt();
		result = result * PRIME + ($publishedAt == null ? 43 : $publishedAt.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "FileSearchHit(super=" + super.toString() + ", fileId=" + this.getFileId() + ", datasetCitation=" + this.getDatasetCitation() + ", fileContentType=" + this.getFileContentType() + ", description=" + this.getDescription() + ", md5=" + this.getMd5() + ", size=" + this.getSize() + ", publishedAt=" + this.getPublishedAt() + ")";
	}

	@SuppressWarnings("all")
	public FileSearchHit() {
	}

	@SuppressWarnings("all")
	public String getFileId() {
		return this.fileId;
	}

	@SuppressWarnings("all")
	public String getDatasetCitation() {
		return this.datasetCitation;
	}

	@SuppressWarnings("all")
	public String getFileContentType() {
		return this.fileContentType;
	}

	@SuppressWarnings("all")
	public String getDescription() {
		return this.description;
	}

	@SuppressWarnings("all")
	public String getMd5() {
		return this.md5;
	}

	@SuppressWarnings("all")
	public int getSize() {
		return this.size;
	}

	@SuppressWarnings("all")
	public String getPublishedAt() {
		return this.publishedAt;
	}

	@JsonProperty("file_id")
	@SuppressWarnings("all")
	public void setFileId(final String fileId) {
		this.fileId = fileId;
	}

	@JsonProperty("dataset_citation")
	@SuppressWarnings("all")
	public void setDatasetCitation(final String datasetCitation) {
		this.datasetCitation = datasetCitation;
	}

	@JsonProperty("file_content_type")
	@SuppressWarnings("all")
	public void setFileContentType(final String fileContentType) {
		this.fileContentType = fileContentType;
	}

	@SuppressWarnings("all")
	public void setDescription(final String description) {
		this.description = description;
	}

	@SuppressWarnings("all")
	public void setMd5(final String md5) {
		this.md5 = md5;
	}

	@JsonProperty("size_in_bytes")
	@SuppressWarnings("all")
	public void setSize(final int size) {
		this.size = size;
	}

	@JsonProperty("published_at")
	@SuppressWarnings("all")
	public void setPublishedAt(final String publishedAt) {
		this.publishedAt = publishedAt;
	}
}
