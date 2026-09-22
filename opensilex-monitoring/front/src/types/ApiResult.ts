import type HttpResponse from '../lib/HttpResponse';
import type { OpenSilexResponse } from '../lib/HttpResponse';
import type { ErrorResponse } from '../lib/model/errorResponse';

/**
 * Shape of every successful `MonitoringService` call: the codegen's own response wrapper,
 * carrying the platform's `{ metadata, result }` envelope as its parsed body.
 */
export type ApiResult<T> = HttpResponse<OpenSilexResponse<T>>;

/**
 * Shape the generated `HttpClient` throws for a 4xx/5xx response — the same wrapper, but carrying
 * an `ErrorResponse` body instead of the expected result type.
 */
export type ApiErrorResult = HttpResponse<ErrorResponse>;

/** Unwraps the two response layers the generated client wraps every payload in. */
export function unwrapResult<T>(response: ApiResult<T>): T {
    return response.response.result;
}

function isApiErrorResult(value: unknown): value is ApiErrorResult {
    return typeof value === 'object' && value !== null && 'response' in value && 'status' in value;
}

/**
 * Turns whatever the generated client rejected with into a message worth showing on a supervision
 * page. A page that only ever says "something went wrong" is not doing its job: showing the
 * server's own title and message, when there is one, is worth the extra type narrowing.
 */
export function formatApiError(error: unknown, genericMessage: string): string {
    if (!isApiErrorResult(error)) {
        return genericMessage;
    }
    const { status } = error;
    const detail = [error.response?.result?.title, error.response?.result?.message]
        .filter((part): part is string => Boolean(part))
        .join(' — ');
    if (!detail) {
        return `${genericMessage} (HTTP ${status})`;
    }
    return `${genericMessage} — HTTP ${status}: ${detail}`;
}
