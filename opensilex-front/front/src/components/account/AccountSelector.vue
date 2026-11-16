<template>
  <InfiniteScrollDropdown
    v-model:selected="accountsURI"
    :fetchPage="searchAccounts"
    :itemLoadingMethod="loadAccounts"
    :conversionMethod="accountToSelectOption"
    :label="label"
    :helpMessage="helpMessage"
    :multiple="multiple"
    :required="required"
    :placeholder="t('component.account.filter-placeholder')"
    :noResultsText="t('component.account.filter-search-no-result')"
    @selectionChange="(option) => emit('selectionChange', option)"
    @clear="emit('clear')"
  />
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { useI18n } from 'vue-i18n'
import type { SelectOption } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { SecurityService, AccountGetDTO } from 'opensilex-security/index'
import type { NamedResourceDTO } from 'opensilex-core/index'
import InfiniteScrollDropdown from '@/components/common/forms/InfiniteScrollDropdown.vue'

//#region Public
const emit = defineEmits<{
  selectionChange: [option: SelectOption | SelectOption[] | undefined]
  clear: []
}>()

withDefaults(
  defineProps<{
    label?: string
    helpMessage?: string
    multiple?: boolean
    required?: boolean
  }>(),
  {
    multiple: false,
    required: false
  }
)

const accountsURI = defineModel<string | string[] | null>('accounts')
//#endregion

//#region Private
//#region Plugins & services
const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<SecurityService>('opensilex.SecurityService')
//#endregion

//#region Methods
/** Loads one page of results. `page` is zero-based. */
function searchAccounts(searchQuery: string, page: number, pageSize: number) {
  return service.searchAccounts(searchQuery, undefined, page, pageSize)
}

/** Loads the already selected elements (update form), so that their label can be displayed. */
async function loadAccounts(accountsURIs: string[]): Promise<NamedResourceDTO[]> {
  const http = await service.getAccountsByURI(accountsURIs)
  // InfiniteScrollDropdown types the loaded items as NamedResourceDTO, they are converted by accountToSelectOption
  return http.response.result as unknown as NamedResourceDTO[]
}

function accountToSelectOption(dto: AccountGetDTO): SelectOption {
  const personName = dto.linked_person ? `${dto.person_first_name} ${dto.person_last_name}` : ''
  return {
    label: `${personName} <${dto.email}>`,
    value: dto.uri
  }
}
//#endregion
//#endregion
</script>
