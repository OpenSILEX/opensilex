<template>
  <div>
    <FormSelector
      ref="personSelector"
      :label="label"
      :helpMessage="helpMessage"
      v-model:selected="personsProxy"
      :multiple="multiple"
      :itemLoadingMethod="loadPersons"
      :required="required"
      :searchMethod="searchPersons"
      :conversionMethod="personToSelectNode"
      :placeholder="t('component.person.filter-placeholder')"
      noResultsText="component.person.filter-search-no-result"
      :actionHandler="actionHandler"
      @select="onSelect"
      @deselect="onDeselect"
    />

    <ModalForm
      v-if="canAddPerson"
      :static="false"
      ref="personForm"
      component="opensilex-PersonForm"
      createTitle="PersonView.create"
      editTitle="PersonView.update"
      icon="ik#ik-user"
      @onCreate="setCreatedPerson"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useStore } from 'vuex'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

// opensilex-security
import type { SecurityService, PersonDTO } from 'opensilex-security/index'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import { useI18n } from 'vue-i18n'
import FormSelector from "@/components/common/forms/FormSelector.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";

// types
type SelectNode = { label: string; id: string; isDisabled?: boolean }

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
  (e: 'select', v: any): void
  (e: 'deselect', v: any): void
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

const personSelector = ref<any>(null)
const personForm = ref<any>(null)

/** v-model:persons (remplace PropSync + :selected.sync) */
const personsProxy = computed<any>({
  get: () => props.persons,
  set: (v) => emit('update:persons', v)
})

/** droit d'ajout */
const canAddPerson = computed(() => {
  return !!props.allowAddPerson && !!user.value?.hasCredential?.(credentials.value?.CREDENTIAL_PERSON_MODIFICATION_ID)
})

const actionHandler = computed(() => (canAddPerson.value ? showCreateForm : null))

/** Load selected persons by URI(s) */
function loadPersons(personsURI: any) {
  return service.value
    .getPersonsByURI(personsURI)
    .then((http: HttpResponse<OpenSilexResponse<Array<PersonDTO>>>) => http.response.result)
}

/** Search persons */
async function searchPersons(searchQuery: string, page: number) {
  return await service.value.searchPersons(
    searchQuery,
    props.getOnlyPersonsWithoutAccount,
    undefined,
    page,
    0
  )
}

/** DTO -> node */
function personToSelectNode(dto: PersonDTO): SelectNode {
  let personLabel = `${dto.first_name} ${dto.last_name}`
  if (dto.email) personLabel += ` <${dto.email}>`

  let disabled = false
  const cond = props.personPropertyExistsCondition
  if (cond && !(dto as any)[cond]) disabled = true

  return {
    label: personLabel,
    id: dto.uri,
    isDisabled: disabled
  }
}

/** Called when modal creates a person (receives HttpResponse<OpenSilexResponse<string>>) */
async function setCreatedPerson(createdPersonUri: HttpResponse<OpenSilexResponse<string>>) {
  const uri = createdPersonUri?.response?.result
  if (!uri) return

  const createdPerson = (await service.value.getPerson(uri)).response.result

  // sélectionner dans le FormSelector
  personSelector.value?.select?.(personToSelectNode(createdPerson))

  emit('onCreate')
}

function showCreateForm() {
  personForm.value?.showCreateForm?.()
}

function onSelect(value: any) {
  emit('select', value)
}
function onDeselect(value: any) {
  emit('deselect', value)
}
</script>

<style scoped lang="scss">
</style>
