export type Granularity = 'HOUR' | 'DAY' | 'WEEK' | 'MONTH';

export const GRANULARITIES: Granularity[] = ['HOUR', 'DAY', 'WEEK', 'MONTH'];

const BUCKET_MS: Record<Granularity, number> = {
    HOUR: 3_600_000,
    DAY: 86_400_000,
    WEEK: 604_800_000,
    MONTH: 2_592_000_000,
};

/**
 * Roughly the pixel budget of the plot area. Past it, an extra bucket is sub-pixel and cannot be
 * read, so offering it is offering a slower page for no information.
 */
export const MAX_BUCKETS = 750;

export function bucketCount(granularity: Granularity, fromMs: number, toMs: number): number {
    return Math.ceil(Math.max(0, toMs - fromMs) / BUCKET_MS[granularity]) + 1;
}

export function isAllowed(granularity: Granularity, fromMs: number, toMs: number): boolean {
    return bucketCount(granularity, fromMs, toMs) <= MAX_BUCKETS;
}

/** @returns the finest granularity that stays under the cap for this period */
export function coarsestNeeded(granularity: Granularity, fromMs: number, toMs: number): Granularity {
    const start = GRANULARITIES.indexOf(granularity);
    for (let i = Math.max(0, start); i < GRANULARITIES.length; i++) {
        if (isAllowed(GRANULARITIES[i], fromMs, toMs)) {
            return GRANULARITIES[i];
        }
    }
    return 'MONTH';
}

export type PeriodPreset = '24h' | '7d' | '30d' | '6m' | '1y' | 'custom';

export const PERIOD_PRESETS: PeriodPreset[] = ['24h', '7d', '30d', '6m', '1y', 'custom'];

const PRESET_MS: Record<Exclude<PeriodPreset, 'custom'>, number> = {
    '24h': 86_400_000,
    '7d': 7 * 86_400_000,
    '30d': 30 * 86_400_000,
    '6m': 183 * 86_400_000,
    '1y': 365 * 86_400_000,
};

export function presetRange(preset: Exclude<PeriodPreset, 'custom'>): { from: Date; to: Date } {
    const to = new Date();
    return { from: new Date(to.getTime() - PRESET_MS[preset]), to };
}

/** The granularity that gives a readable series for each preset. */
export function defaultGranularity(preset: PeriodPreset): Granularity {
    switch (preset) {
        case '24h':
            return 'HOUR';
        case '7d':
        case '30d':
            return 'DAY';
        case '6m':
        case '1y':
            return 'WEEK';
        default:
            return 'DAY';
    }
}
