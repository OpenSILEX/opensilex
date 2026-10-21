<template>
  <div>
    <InfiniteScrollDropdown
      ref="dropdown"
      v-model:selected="personsProxy"
      :fetchPage="searchPersons"
      :itemLoadingMethod="loadPersons"
      :conversionMethod="personToSelectOption"
      :label="label"
      :helpMessage="helpMessage"
      :multiple="multiple"
      :required="required"
      :placeholder="t('component.person.filter-placeholder')"
      :actionHandler="actionHandler"
      @selectionChange="(option) => emit('selectionChange', option)"
      @clear="emit('clear')"
    />

    <PersonForm
      v-if="canAddPerson"
      ref="personForm"
      :createTitle="t('component.person.add')"
      :editTitle="t('component.person.update')"
      @onCreate="setCreatedPerson"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useStore } from 'vuex'
import type { SelectOption } from 'naive-ui'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

// opensilex-security
import type { SecurityService, PersonDTO } from 'opensilex-security/index'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import { useI18n } from 'vue-i18n'
import InfiniteScrollDropdown from "@/components/common/forms/InfiniteScrollDropdown.vue"
import PersonForm from "@/components/persons/PersonForm.vue"

const props = withDefaults(defineProps<{
  persons?: any // string | string[] | null ?
  label?: string
  required?: boolean
  multiple?: boolean
  helpMessage?: string
  getOnlyPersonsWithoutAccount?: boolean
  personPropertyExistsCondition?: string
  allowAddPerson?: boolean
}>(), {
  required: false,
  multiple: false,
  allowAddPerson: false,
  getOnlyPersonsWithoutAccount: false
})

const emit = defineEmits<{
  (e: 'update:persons', v: any): void
  (e: 'selectionChange', option: SelectOption | SelectOption[] | undefined): void
  (e: 'clear'): void
  (e: 'onCreate'): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()
const store = useStore()

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

const service = computed(() =>
  $opensilex.getService<SecurityService>('opensilex.SecurityService')
)

const dropdown = ref<{ refresh: () => Promise<void> } | null>(null)
const personForm = ref<any>(null)

/** v-model:persons (replaces PropSync + :selected.sync) */
const personsProxy = computed<any>({
  get: () => props.persons,
  set: (v) => emit('update:persons', v)
})

/** Right to add a person */
const canAddPerson = computed(() => {
  return !!props.allowAddPerson && !!user.value?.hasCredential?.(credentials.value?.CREDENTIAL_PERSON_MODIFICATION_ID)
})

const actionHandler = computed(() => (canAddPerson.value ? showCreateForm : undefined))

/** Loads the already selected elements (update form), so that their name can be displayed. */
function loadPersons(uris: string[]) {
  return service.value
    .getPersonsByURI(uris)
    .then((http: HttpResponse<OpenSilexResponse<Array<PersonDTO>>>) => http.response.result)
}

/** Loads one page of results. `page` is zero-based. */
async function searchPersons(searchQuery: string, page: number, pageSize: number) {
  return await service.value.searchPersons(
    searchQuery,
    props.getOnlyPersonsWithoutAccount,
    ['firstName=asc'], // the SPARQL field name, see PersonModel.FIRST_NAME_FIELD
    page,
    pageSize
  )
}

const personToSelectOption = (dto: PersonDTO): SelectOption => {
  let personLabel = `${dto.first_name} ${dto.last_name}`
  if (dto.email) personLabel += ` <${dto.email}>`

  const condition = props.personPropertyExistsCondition

  return {
    label: personLabel,
    value: dto.uri,
    disabled: !!(condition && !(dto as any)[condition])
  }
}

/** Called when the modal creates a person (receives HttpResponse<OpenSilexResponse<string>>) */
async function setCreatedPerson(createdPersonUri: HttpResponse<OpenSilexResponse<string>>) {
  const uri = createdPersonUri?.response?.result
  if (!uri) {
    return
  }

  // Select the freshly created person, appending to the existing selection when multiple.
  personsProxy.value = props.multiple
    ? [...(Array.isArray(props.persons) ? props.persons : []), uri]
    : uri

  // Bring the new person into the loaded results.
  dropdown.value?.refresh()

  emit('onCreate')
}

function showCreateForm() {
  personForm.value?.showCreateForm?.()
}
</script>

<style scoped lang="scss">
</style>
