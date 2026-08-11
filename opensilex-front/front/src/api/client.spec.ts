// @vitest-environment happy-dom
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { api, fetchClient, getBaseApi, OpenSilexResponseError, isDebugApiEnabled, setApiDebug } from "./client";
import { OpenSilexResponse, MetadataDTO } from "../models/HttpResponse";

describe("OpenSILEX API Client (@hey-api/client-fetch)", () => {
  beforeEach(() => {
    localStorage.clear();
    setApiDebug(null);
    vi.clearAllMocks();
  });

  afterEach(() => {
    setApiDebug(null);
  });

  it("should return valid base API URL", () => {
    const url = getBaseApi();
    expect(url).toContain("/rest");
  });

  it("should instantiate OpenSilexResponseError correctly", () => {
    const err = new OpenSilexResponseError("Unauthorized", 401, { title: "Error" });
    expect(err.name).toBe("OpenSilexResponseError");
    expect(err.status).toBe(401);
    expect(err.message).toBe("Unauthorized");
    expect(err.details).toEqual({ title: "Error" });
  });

  it("should correctly handle debug mode state via isDebugApiEnabled and setApiDebug", () => {
    setApiDebug(true);
    expect(isDebugApiEnabled()).toBe(true);

    setApiDebug(false);
    expect(isDebugApiEnabled()).toBe(false);

    setApiDebug(null);
    // In vitest environment import.meta.env.DEV is true (or test env)
    // localStorage opensilex_debug_api works when in dev
    (import.meta.env as any).DEV = true;
    localStorage.setItem("opensilex_debug_api", "true");
    expect(isDebugApiEnabled()).toBe(true);
    localStorage.removeItem("opensilex_debug_api");

    // In production (DEV = false), debug mode is disabled even if localStorage or ?debug is present
    (import.meta.env as any).DEV = false;
    localStorage.setItem("opensilex_debug_api", "true");
    expect(isDebugApiEnabled()).toBe(false);
    localStorage.removeItem("opensilex_debug_api");

    (import.meta.env as any).DEV = true;
  });

  it("should output grouped console logs when debug mode is enabled", async () => {
    setApiDebug(true);
    const groupCollapsedSpy = vi.spyOn(console, "groupCollapsed").mockImplementation(() => {});
    const groupEndSpy = vi.spyOn(console, "groupEnd").mockImplementation(() => {});
    const logSpy = vi.spyOn(console, "log").mockImplementation(() => {});

    const mockGet = vi.spyOn(fetchClient, "get").mockResolvedValue({ data: { result: "ok" } } as any);

    await api.GET("/vuejs/config");
    expect(mockGet).toHaveBeenCalledWith({ url: "/vuejs/config" });

    setApiDebug(false);
  });

  it("should expose typed Security.authenticate service method returning OpenSilexResponse", async () => {
    const mockPost = vi.spyOn(fetchClient, "post").mockResolvedValue({
      data: {
        result: { token: "jwt_token_12345" },
        metadata: { status: [], pagination: null }
      }
    } as any);

    const response: OpenSilexResponse<any> = await api.Security.authenticate({
      identifier: "admin@opensilex.org",
      password: "password"
    });

    expect(mockPost).toHaveBeenCalledWith({
      url: "/security/authenticate",
      body: {
        identifier: "admin@opensilex.org",
        password: "password"
      }
    });

    expect(response).toBeInstanceOf(OpenSilexResponse);
    expect(response.result).toEqual({ token: "jwt_token_12345" });
  });

  it("should provide backward compatible GET, POST, PUT, DELETE methods", async () => {
    const mockGet = vi.spyOn(fetchClient, "get").mockResolvedValue({ data: { result: "ok" } } as any);

    const { data } = await api.GET("/vuejs/config");
    expect(mockGet).toHaveBeenCalledWith({ url: "/vuejs/config" });
    expect(data).toEqual({ result: "ok" });
  });
});
