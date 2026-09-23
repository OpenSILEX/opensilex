export interface DescriptionGeneratorInformation {
    propertyTranslationKey: string
    required: boolean
    example?: string
}

export const MOVE_DESCRIPTION_GENERATOR_BY_HEADER =
    new Map<string, DescriptionGeneratorInformation>([
        ['from', { propertyTranslationKey: 'component.common.geometry.from-help', required: false, example: 'component.common.geometry.from-placeholder' }],
        ['to', { propertyTranslationKey: 'component.common.geometry.to-help', required: false, example: 'component.common.geometry.to-placeholder' }],
        ['coordinates', { propertyTranslationKey: 'component.common.geometry.geometry-help', required: false, example: 'component.common.geometry.coordinates-placeholder' }],
        ['x', { propertyTranslationKey: 'component.common.geometry.x-help', required: false, example: 'component.common.geometry.x-placeholder' }],
        ['y', { propertyTranslationKey: 'component.common.geometry.y-help', required: false, example: 'component.common.geometry.y-placeholder' }],
        ['z', { propertyTranslationKey: 'component.common.geometry.z-help', required: false, example: 'component.common.geometry.z-placeholder' }],
        ['textualPosition', { propertyTranslationKey: 'component.common.geometry.textual-position-help', required: false, example: 'component.common.geometry.textual-position-placeholder' }]
    ])