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
public class Identifier {
	private Long id;
	private String persistentId;

	@SuppressWarnings("all")
	public Long getId() {
		return this.id;
	}

	@SuppressWarnings("all")
	public String getPersistentId() {
		return this.persistentId;
	}

	@SuppressWarnings("all")
	public void setId(final Long id) {
		this.id = id;
	}

	@SuppressWarnings("all")
	public void setPersistentId(final String persistentId) {
		this.persistentId = persistentId;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof Identifier)) return false;
		final Identifier other = (Identifier) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
		final Object this$persistentId = this.getPersistentId();
		final Object other$persistentId = other.getPersistentId();
		if (this$persistentId == null ? other$persistentId != null : !this$persistentId.equals(other$persistentId)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof Identifier;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		final Object $persistentId = this.getPersistentId();
		result = result * PRIME + ($persistentId == null ? 43 : $persistentId.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "Identifier(id=" + this.getId() + ", persistentId=" + this.getPersistentId() + ")";
	}

	@SuppressWarnings("all")
	public Identifier(final Long id, final String persistentId) {
		this.id = id;
		this.persistentId = persistentId;
	}

	@SuppressWarnings("all")
	public Identifier() {
	}
}
