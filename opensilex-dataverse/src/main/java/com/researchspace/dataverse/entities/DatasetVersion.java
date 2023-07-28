// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.entities;

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
public class DatasetVersion {
	private Long id;
	private String versionState;
	private String productionDate;
	private Date lastUpdateTime;
	private Date createTime;
	private DataSetMetadataBlock metadataBlocks;
	private int versionNumber;
	private int versionMinorNumber;

	@SuppressWarnings("all")
	public DatasetVersion() {
	}

	@SuppressWarnings("all")
	public Long getId() {
		return this.id;
	}

	@SuppressWarnings("all")
	public String getVersionState() {
		return this.versionState;
	}

	@SuppressWarnings("all")
	public String getProductionDate() {
		return this.productionDate;
	}

	@SuppressWarnings("all")
	public Date getLastUpdateTime() {
		return this.lastUpdateTime;
	}

	@SuppressWarnings("all")
	public Date getCreateTime() {
		return this.createTime;
	}

	@SuppressWarnings("all")
	public DataSetMetadataBlock getMetadataBlocks() {
		return this.metadataBlocks;
	}

	@SuppressWarnings("all")
	public int getVersionNumber() {
		return this.versionNumber;
	}

	@SuppressWarnings("all")
	public int getVersionMinorNumber() {
		return this.versionMinorNumber;
	}

	@SuppressWarnings("all")
	public void setId(final Long id) {
		this.id = id;
	}

	@SuppressWarnings("all")
	public void setVersionState(final String versionState) {
		this.versionState = versionState;
	}

	@SuppressWarnings("all")
	public void setProductionDate(final String productionDate) {
		this.productionDate = productionDate;
	}

	@SuppressWarnings("all")
	public void setLastUpdateTime(final Date lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
	}

	@SuppressWarnings("all")
	public void setCreateTime(final Date createTime) {
		this.createTime = createTime;
	}

	@SuppressWarnings("all")
	public void setMetadataBlocks(final DataSetMetadataBlock metadataBlocks) {
		this.metadataBlocks = metadataBlocks;
	}

	@SuppressWarnings("all")
	public void setVersionNumber(final int versionNumber) {
		this.versionNumber = versionNumber;
	}

	@SuppressWarnings("all")
	public void setVersionMinorNumber(final int versionMinorNumber) {
		this.versionMinorNumber = versionMinorNumber;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DatasetVersion)) return false;
		final DatasetVersion other = (DatasetVersion) o;
		if (!other.canEqual((Object) this)) return false;
		if (this.getVersionNumber() != other.getVersionNumber()) return false;
		if (this.getVersionMinorNumber() != other.getVersionMinorNumber()) return false;
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
		final Object this$versionState = this.getVersionState();
		final Object other$versionState = other.getVersionState();
		if (this$versionState == null ? other$versionState != null : !this$versionState.equals(other$versionState)) return false;
		final Object this$productionDate = this.getProductionDate();
		final Object other$productionDate = other.getProductionDate();
		if (this$productionDate == null ? other$productionDate != null : !this$productionDate.equals(other$productionDate)) return false;
		final Object this$lastUpdateTime = this.getLastUpdateTime();
		final Object other$lastUpdateTime = other.getLastUpdateTime();
		if (this$lastUpdateTime == null ? other$lastUpdateTime != null : !this$lastUpdateTime.equals(other$lastUpdateTime)) return false;
		final Object this$createTime = this.getCreateTime();
		final Object other$createTime = other.getCreateTime();
		if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
		final Object this$metadataBlocks = this.getMetadataBlocks();
		final Object other$metadataBlocks = other.getMetadataBlocks();
		if (this$metadataBlocks == null ? other$metadataBlocks != null : !this$metadataBlocks.equals(other$metadataBlocks)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DatasetVersion;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		result = result * PRIME + this.getVersionNumber();
		result = result * PRIME + this.getVersionMinorNumber();
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		final Object $versionState = this.getVersionState();
		result = result * PRIME + ($versionState == null ? 43 : $versionState.hashCode());
		final Object $productionDate = this.getProductionDate();
		result = result * PRIME + ($productionDate == null ? 43 : $productionDate.hashCode());
		final Object $lastUpdateTime = this.getLastUpdateTime();
		result = result * PRIME + ($lastUpdateTime == null ? 43 : $lastUpdateTime.hashCode());
		final Object $createTime = this.getCreateTime();
		result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
		final Object $metadataBlocks = this.getMetadataBlocks();
		result = result * PRIME + ($metadataBlocks == null ? 43 : $metadataBlocks.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DatasetVersion(id=" + this.getId() + ", versionState=" + this.getVersionState() + ", productionDate=" + this.getProductionDate() + ", lastUpdateTime=" + this.getLastUpdateTime() + ", createTime=" + this.getCreateTime() + ", metadataBlocks=" + this.getMetadataBlocks() + ", versionNumber=" + this.getVersionNumber() + ", versionMinorNumber=" + this.getVersionMinorNumber() + ")";
	}
}
