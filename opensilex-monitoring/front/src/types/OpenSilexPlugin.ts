/**
 * The subset of the host application's `$opensilex` plugin this module actually calls.
 *
 * The real class, `OpenSilexVuePlugin`, lives in `opensilex-front` and is not resolvable from an
 * extension module's own `front/` directory — the same constraint that keeps `vue-i18n` and
 * `vuex` out of reach here (see the comment in `index.ts`). A local, narrow contract stands in
 * for it: it documents exactly the dependency surface this module uses, rather than typing
 * against a several-hundred-line host class the compiler could never actually check an import
 * against.
 */

/** The logged-in account, as the host's `$store.state.user` exposes it. */
export interface OpenSilexUser {
    isAdmin(): boolean;
}

/** The host's vue-i18n `Composer`, narrowed to the two methods this module calls. */
export interface OpenSilexTranslator {
    t(key: string, params?: Record<string, string | number>): string;
    mergeLocaleMessage(locale: string, messages: Record<string, unknown>): void;
}

/** The inversify container the host exposes so a module's generated API client can bind into it. */
export interface OpenSilexServiceContainer {
    bind(serviceId: string): {
        to(implementation: new (...args: never[]) => unknown): { inSingletonScope(): void };
    };
}

export interface OpenSilexPlugin {
    readonly $i18n: OpenSilexTranslator;
    getUser(): OpenSilexUser | undefined;
    getLang(): string;
    getService<T>(serviceId: string): T;
    getServiceContainer(): OpenSilexServiceContainer;
    disableLoader(): void;
    enableLoader(): void;
    showInfoToast(message: string): void;
}
