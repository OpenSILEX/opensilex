import { createClient } from "@hey-api/client-fetch";
import { OpenSilexResponse, MetadataDTO } from "../models/HttpResponse";

/**
 * Custom Error Class representing OpenSILEX API response failures.
 */
export class OpenSilexResponseError extends Error {
  public status: number;
  public details: any;

  constructor(message: string, status: number, details?: any) {
    super(message);
    this.name = "OpenSilexResponseError";
    this.status = status;
    this.details = details;
  }
}

/**
 * Resolves base API URL based on environment (development vs production).
 */
export function getBaseApi(): string {
  if (typeof window !== "undefined" && window.location) {
    if (import.meta.env.DEV) {
      return "http://localhost:8666/rest";
    }
    const splitURI = window.location.href.split("/app");
    return splitURI[0] + "/rest";
  }
  return "http://localhost:8666/rest";
}

import type { Client } from "@hey-api/client-fetch";
import { client as frontClient } from '../lib/generated/client.gen';
import { client as securityClient } from '../../../../opensilex-security/front/src/lib/generated/client.gen';
import { client as coreClient } from '../../../../opensilex-core/front/src/lib/generated/client.gen';

export const registeredClients = new Set<Client>();

/**
 * Dynamically registers and configures any @hey-api/client-fetch instance
 * with base URL, authorization headers, language headers, and response interceptors.
 */
export function registerOpenSilexClient<T extends Client = Client>(clientToRegister: T): T {
  if (!clientToRegister || registeredClients.has(clientToRegister)) {
    return clientToRegister;
  }

  clientToRegister.setConfig({
    baseUrl: getBaseApi()
  });

  clientToRegister.interceptors.request.use(async (request) => {
    const baseApi = getBaseApi();
    if (request.url && request.url.startsWith("/")) {
      const base = baseApi.endsWith("/") ? baseApi.slice(0, -1) : baseApi;
      request.url = `${base}${request.url}`;
    }

    const token = localStorage.getItem("opensilex_token");
    if (token) {
      request.headers.set("Authorization", `Bearer ${token}`);
    }
    if (!request.headers.has("Accept-Language")) {
      request.headers.set("Accept-Language", "fr");
    }
    return request;
  });

  clientToRegister.interceptors.response.use(async (response) => {
    if (response.status === 401) {
      localStorage.removeItem("opensilex_token");
      window.dispatchEvent(new CustomEvent("opensilex:unauthorized"));
    }

    if (!response.ok) {
      let errorPayload: any = {};
      try {
        errorPayload = await response.clone().json();
      } catch {
        errorPayload = { title: response.statusText };
      }

      const errorMessage =
        errorPayload?.message ||
        errorPayload?.result?.message ||
        errorPayload?.title ||
        `HTTP Error ${response.status}: ${response.statusText}`;

      throw new OpenSilexResponseError(errorMessage, response.status, errorPayload);
    }

    return response;
  });

  registeredClients.add(clientToRegister);
  return clientToRegister;
}

export const fetchClient = registerOpenSilexClient(frontClient);
export const client = fetchClient;

if (securityClient) registerOpenSilexClient(securityClient);
if (coreClient) registerOpenSilexClient(coreClient);

/**
 * Main OpenSILEX API client providing both typed SDK service namespaces
 * and backward-compatible HTTP fetch methods.
 */
export const api = {
  // Underlying @hey-api/client-fetch instance
  fetchClient,

  // HTTP Helper Methods for backward compatibility
  async GET<T = any>(url: string, options?: any): Promise<{ data?: T; error?: any }> {
    try {
      const response = await fetchClient.get({ url, ...options });
      return { data: response.data as T };
    } catch (error) {
      return { error };
    }
  },

  async POST<T = any>(url: string, options?: any): Promise<{ data?: T; error?: any }> {
    try {
      const response = await fetchClient.post({ url, ...options });
      return { data: response.data as T };
    } catch (error) {
      return { error };
    }
  },

  async PUT<T = any>(url: string, options?: any): Promise<{ data?: T; error?: any }> {
    try {
      const response = await fetchClient.put({ url, ...options });
      return { data: response.data as T };
    } catch (error) {
      return { error };
    }
  },

  async DELETE<T = any>(url: string, options?: any): Promise<{ data?: T; error?: any }> {
    try {
      const response = await fetchClient.delete({ url, ...options });
      return { data: response.data as T };
    } catch (error) {
      return { error };
    }
  },

  // Typed SDK Service Namespaces
  Security: {
    /**
     * Authenticates credentials and returns typed OpenSilexResponse<any>
     */
    async authenticate(auth: { identifier?: string; password?: string; [key: string]: any }): Promise<OpenSilexResponse<any>> {
      const response = await fetchClient.post({
        url: "/security/authenticate",
        body: auth
      });
      const data: any = response.data;
      const result = data?.result ?? data;
      const metadata = data?.metadata ?? new MetadataDTO(null as any, [], []);
      return new OpenSilexResponse(result, metadata);
    }
  }
};

// Re-export generated SDK functions and type definitions
export * from "../lib/generated";

