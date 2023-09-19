// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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
 * Wrapper over response, can include <code>data</code> or an error<code>message</code> but not both
 * @author rspace
 *
 * @param <T> The datatype of the response
 */
public class DataverseResponse<T> {
	private String status;
	private T data;
	@JsonDeserialize(using = ObjectOrStringMessageDeserializer.class)
	private String message;

	@SuppressWarnings("all")
	public DataverseResponse() {
	}

	@SuppressWarnings("all")
	public String getStatus() {
		return this.status;
	}

	@SuppressWarnings("all")
	public T getData() {
		return this.data;
	}

	@SuppressWarnings("all")
	public String getMessage() {
		return this.message;
	}

	@SuppressWarnings("all")
	public void setStatus(final String status) {
		this.status = status;
	}

	@SuppressWarnings("all")
	public void setData(final T data) {
		this.data = data;
	}

	@JsonDeserialize(using = ObjectOrStringMessageDeserializer.class)
	@SuppressWarnings("all")
	public void setMessage(final String message) {
		this.message = message;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DataverseResponse)) return false;
		final DataverseResponse<?> other = (DataverseResponse<?>) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$status = this.getStatus();
		final Object other$status = other.getStatus();
		if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
		final Object this$data = this.getData();
		final Object other$data = other.getData();
		if (this$data == null ? other$data != null : !this$data.equals(other$data)) return false;
		final Object this$message = this.getMessage();
		final Object other$message = other.getMessage();
		if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DataverseResponse;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $status = this.getStatus();
		result = result * PRIME + ($status == null ? 43 : $status.hashCode());
		final Object $data = this.getData();
		result = result * PRIME + ($data == null ? 43 : $data.hashCode());
		final Object $message = this.getMessage();
		result = result * PRIME + ($message == null ? 43 : $message.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DataverseResponse(status=" + this.getStatus() + ", data=" + this.getData() + ", message=" + this.getMessage() + ")";
	}
}
