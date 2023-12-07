export interface BaseExternalReferencesDTO {
    uri?: string,
    name?: string,
    description?: string,
    exact_match?: Array<string>,
    close_match?: Array<string>,
    broad_match?: Array<string>,
    narrow_match?: Array<string>
}

export interface BaseExternalReferencesForm {
    showCreateForm(): void,
    showEditForm(dto: BaseExternalReferencesDTO, linkedDataCount?: number): void
}
