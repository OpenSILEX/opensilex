import createClient, { type Middleware } from "openapi-fetch";
import type { paths } from "./schema"; // Generated via `npm run gen:types`

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
 * 1. Request Middleware: Injects dynamic JWT authentication and preferred locale.
 */
const requestMiddleware: Middleware = {
  async onRequest({ request }) {
    const token = localStorage.getItem("opensilex_token");
    if (token) {
      request.headers.set("Authorization", `Bearer ${token}`);
    }
    if (!request.headers.has("Accept-Language")) {
      request.headers.set("Accept-Language", "fr");
    }
    return request;
  }
};

/**
 * 2. Approach A - Response Middleware: Intercepts custom responses & handles errors globally.
 */
const responseMiddleware: Middleware = {
  async onResponse({ response, request }) {
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

      // Handle 401 Unauthorized globally (e.g. clear session & dispatch logout event)
      if (response.status === 401) {
        localStorage.removeItem("opensilex_token");
        window.dispatchEvent(new CustomEvent("opensilex:unauthorized"));
      }

      throw new OpenSilexResponseError(errorMessage, response.status, errorPayload);
    }

    return response;
  }
};

/**
 * Typed OpenAPI Fetch Client for OpenSILEX REST endpoints.
 */
export const api = createClient<paths>({
  baseUrl: "/rest"
});

// Register middlewares
api.use(requestMiddleware);
api.use(responseMiddleware);
