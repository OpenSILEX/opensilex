/**
 * Builds the `metadata` search parameter of the services that filter on attributes stored in MongoDB
 * (germplasm, devices).
 *
 * Without a value, every element having the attribute matches: the API treats an empty value as "the attribute exists".
 */
export function toMetadataFilter(attribute?: string, value?: string): string | undefined {
  if (!attribute) {
    return undefined
  }

  // Empty string rather than undefined, as JSON.stringify drops undefined fields
  return JSON.stringify({ [attribute]: value ?? '' })
}
