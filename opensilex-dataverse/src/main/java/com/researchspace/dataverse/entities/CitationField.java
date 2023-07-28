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
public class CitationField {
	private String typeName;
	private String typeClass;
	private boolean multiple;
	private Object value;

	@SuppressWarnings("all")
	public String getTypeName() {
		return this.typeName;
	}

	@SuppressWarnings("all")
	public String getTypeClass() {
		return this.typeClass;
	}

	@SuppressWarnings("all")
	public boolean isMultiple() {
		return this.multiple;
	}

	@SuppressWarnings("all")
	public Object getValue() {
		return this.value;
	}

	@SuppressWarnings("all")
	public void setTypeName(final String typeName) {
		this.typeName = typeName;
	}

	@SuppressWarnings("all")
	public void setTypeClass(final String typeClass) {
		this.typeClass = typeClass;
	}

	@SuppressWarnings("all")
	public void setMultiple(final boolean multiple) {
		this.multiple = multiple;
	}

	@SuppressWarnings("all")
	public void setValue(final Object value) {
		this.value = value;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof CitationField)) return false;
		final CitationField other = (CitationField) o;
		if (!other.canEqual((Object) this)) return false;
		if (this.isMultiple() != other.isMultiple()) return false;
		final Object this$typeName = this.getTypeName();
		final Object other$typeName = other.getTypeName();
		if (this$typeName == null ? other$typeName != null : !this$typeName.equals(other$typeName)) return false;
		final Object this$typeClass = this.getTypeClass();
		final Object other$typeClass = other.getTypeClass();
		if (this$typeClass == null ? other$typeClass != null : !this$typeClass.equals(other$typeClass)) return false;
		final Object this$value = this.getValue();
		final Object other$value = other.getValue();
		if (this$value == null ? other$value != null : !this$value.equals(other$value)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof CitationField;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		result = result * PRIME + (this.isMultiple() ? 79 : 97);
		final Object $typeName = this.getTypeName();
		result = result * PRIME + ($typeName == null ? 43 : $typeName.hashCode());
		final Object $typeClass = this.getTypeClass();
		result = result * PRIME + ($typeClass == null ? 43 : $typeClass.hashCode());
		final Object $value = this.getValue();
		result = result * PRIME + ($value == null ? 43 : $value.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "CitationField(typeName=" + this.getTypeName() + ", typeClass=" + this.getTypeClass() + ", multiple=" + this.isMultiple() + ", value=" + this.getValue() + ")";
	}

	@SuppressWarnings("all")
	public CitationField(final String typeName, final String typeClass, final boolean multiple, final Object value) {
		this.typeName = typeName;
		this.typeClass = typeClass;
		this.multiple = multiple;
		this.value = value;
	}

	@SuppressWarnings("all")
	public CitationField() {
	}
}
