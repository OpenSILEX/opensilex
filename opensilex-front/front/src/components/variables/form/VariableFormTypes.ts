export interface BaseVariableCreationDTO {
    uri?: string,
    name?: string,
    description?: string,
    exact_match?: Array<string>,
    close_match?: Array<string>,
    broad_match?: Array<string>,
    narrow_match?: Array<string>
}

export interface BaseVariableCreateForm {
    showCreateForm(): void,
    showEditForm(dto: BaseVariableCreationDTO, linkedDataCount?: number): void
}