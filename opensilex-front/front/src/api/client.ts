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

const requestStartTimes = new WeakMap<Request, number>();
let apiDebugOverride: boolean | null = null;

/**
 * Returns true if API Client Debug Mode is active.
 * Debug mode is active ONLY IF Vite is in DEV mode (import.meta.env.DEV)
 * AND ?debug parameter is present in URL (or localStorage "opensilex_debug_api" === "true").
 */
export function isDebugApiEnabled(): boolean {
  if (apiDebugOverride !== null) {
    return apiDebugOverride;
  }
  const isDev = Boolean(import.meta.env?.DEV);
  if (!isDev) {
    return false;
  }
  if (typeof window !== "undefined") {
    const debugStorage = localStorage.getItem("opensilex_debug_api");
    if (debugStorage !== null) {
      return debugStorage === "true";
    }
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.has("debug");
  }
  return false;
}

/**
 * Manually toggle API Client Debug Mode at runtime.
 */
export function setApiDebug(enabled: boolean | null): void {
  apiDebugOverride = enabled;
  if (typeof window !== "undefined") {
    if (enabled === null) {
      localStorage.removeItem("opensilex_debug_api");
    } else {
      localStorage.setItem("opensilex_debug_api", String(enabled));
    }
  }
}

import type { Client } from "@hey-api/client-fetch";
import { client as frontClient } from '../lib/generated/client.gen';
import { client as securityClient } from '../../../../opensilex-security/front/src/lib/generated/client.gen';
import { client as coreClient } from '../../../../opensilex-core/front/src/lib/generated/client.gen';

export const registeredClients = new Set<Client>();

/**
 * Resolves active authorization token from localStorage or cookies.
 */
export function getAuthToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  const localToken = localStorage.getItem("opensilex_token");
  if (localToken) {
    return localToken;
  }
  if (document.cookie) {
    const cookies = document.cookie.split(";");
    for (const c of cookies) {
      const [key, val] = c.trim().split("=");
      if (key && key.startsWith("opensilex-token") && val) {
        return decodeURIComponent(val);
      }
    }
  }
  return null;
}

/**
 * Dynamically registers and configures any @hey-api/client-fetch instance
 * with base URL, authorization headers, language headers, debug logging, and response interceptors.
 */
export function registerOpenSilexClient<T extends Client = Client>(clientToRegister: T): T {
  if (!clientToRegister || registeredClients.has(clientToRegister)) {
    return clientToRegister;
  }

  clientToRegister.setConfig({
    baseUrl: getBaseApi()
  });

  clientToRegister.interceptors.request.use(async (request, options) => {
    const baseApi = getBaseApi();
    if (request.url && request.url.startsWith("/")) {
      const base = baseApi.endsWith("/") ? baseApi.slice(0, -1) : baseApi;
      request.url = `${base}${request.url}`;
    }

    const token = getAuthToken();
    if (token) {
      request.headers.set("Authorization", `Bearer ${token}`);
    }
    if (!request.headers.has("Accept-Language")) {
      request.headers.set("Accept-Language", "fr");
    }

    requestStartTimes.set(request, performance.now());

    if (isDebugApiEnabled()) {
      const method = request.method?.toUpperCase() || "GET";
      const url = request.url;
      const title = `%c🚀 [API Req] ${method} ${url}`;
      const style = "color: #0288D1; font-weight: bold;";

      if (typeof console.groupCollapsed === "function") {
        console.groupCollapsed(title, style);
      } else {
        console.log(title, style);
      }

      const headersObj: Record<string, string> = {};
      request.headers.forEach((val, key) => {
        if (key.toLowerCase() === "authorization" && val.startsWith("Bearer ")) {
          const bearerToken = val.substring(7);
          headersObj[key] = `Bearer ${bearerToken.substring(0, 10)}...`;
        } else {
          headersObj[key] = val;
        }
      });

      console.log("URL:", url);
      console.log("Method:", method);
      console.log("Headers:", headersObj);
      if (options?.body) {
        console.log("Body:", options.body);
      }
      if (options?.query) {
        console.log("Query:", options.query);
      }

      if (typeof console.groupEnd === "function") {
        console.groupEnd();
      }
    }

    return request;
  });

  clientToRegister.interceptors.response.use(async (response, request) => {
    const startTime = requestStartTimes.get(request);
    const duration = startTime ? `${(performance.now() - startTime).toFixed(1)}ms` : "N/A";

    if (isDebugApiEnabled()) {
      const method = request?.method?.toUpperCase() || "GET";
      const status = response.status;
      const isOk = response.ok;
      const icon = isOk ? "✅" : "⚠️";
      const style = isOk
        ? "color: #2E7D32; font-weight: bold;"
        : "color: #ED6C02; font-weight: bold;";
      const title = `%c${icon} [API Res ${status}] ${method} ${response.url} (${duration})`;

      if (typeof console.groupCollapsed === "function") {
        console.groupCollapsed(title, style);
      } else {
        console.log(title, style);
      }

      console.log("Status:", status, response.statusText);
      console.log("Duration:", duration);

      try {
        const clone = response.clone();
        const data = await clone.json();
        console.log("Response Payload:", data);
      } catch {
        // Body was non-JSON or empty
      }

      if (typeof console.groupEnd === "function") {
        console.groupEnd();
      }
    }

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

