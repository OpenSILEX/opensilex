<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="modalFormLogic.formTitle.value" icon="bi#bi-geo-alt" />
    </template>

    <n-form
      ref="formRef"
      :rules="rules"
      :model="modalFormLogic.form.value"
      label-placement="top"
      :show-require-mark="true"
      size="large"
    >
      <!-- URI -->
      <n-form-item>
        <UriForm
          :uri.sync="modalFormLogic.form.value.uri"
          label="component.common.uri"
          helpMessage="component.common.uri-help-message"
          :editMode="modalFormLogic.editMode.value"
          v-model:generated="uriGenerated"
        />
      </n-form-item>

      <!-- Name -->
      <n-form-item path="name">
        <InputForm
          v-model:value="modalFormLogic.form.value.name"
          label="component.common.name"
          type="text"
          :required="true"
          :placeholder="t('SiteForm.form-name-placeholder')"
        />
      </n-form-item>

      <!-- Description -->
      <n-form-item>
        <InputForm
          v-model:value="modalFormLogic.form.value.description"
          label="component.common.description"
          type="text"
          :placeholder="t('component.common.description')"
        />
      </n-form-item>

      <!-- Organizations -->
        <OrganizationSelector
          path="organizations"
          ref="organizationSelectorRef"
          :label="t('SiteForm.organizations')"
          v-model:organizations="modalFormLogic.form.value.organizations"
          :multiple="true"
          :required="true"
          checkStrategy="all"
        />

      <!-- Facilities -->
        <FacilitySelector
          :label="t('SiteForm.facilities')"
          v-model:facilities="modalFormLogic.form.value.facilities"
          :multiple="true"
        />

      <!-- Groups -->
        <GroupSelector
          :label="t('SiteForm.groups')"
          v-model:groups="modalFormLogic.form.value.groups"
          :multiple="true"
          :helpMessage="t('SiteForm.groups-help-message')"
        />

      <!-- Address toggle -->
      <n-form-item>
        <div class="form-check form-switch my-2">
          <input
            class="form-check-input"
            type="checkbox"
            role="switch"
            id="site-address-toggle"
            :checked="hasAddress"
            @change="toggleAddress"
          />
          <label class="form-check-label" for="site-address-toggle">
            {{ t('SiteForm.toggleAddress') }}
          </label>
        </div>
      </n-form-item>

      <!-- Address -->
      <n-form-item v-if="hasAddress">
        <AddressForm
            v-if="hasAddress"
            v-model:address="modalFormLogic.form.value.address"
        />
      </n-form-item>

    </n-form>

    <template #footer>
      <FormFooter @cancel="modalFormLogic.hide" @submit="modalFormLogic.submit" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, inject, ref, useTemplateRef } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { OrganizationsService } from 'opensilex-core/api/organizations.service'
import type { SiteCreationDTO } from 'opensilex-core/index'
import type { SiteUpdateDTO } from 'opensilex-core/model/siteUpdateDTO'
import type HttpResponse from '@/lib/HttpResponse'
import type { OpenSilexResponse } from '@/lib/HttpResponse'
import { NForm, NFormItem } from 'naive-ui'
import {requiredArray, requiredTrimmed} from '@/models/FormFieldsFormatter'

import UriForm from "@/components/common/forms/UriForm.vue"
import InputForm from "@/components/common/forms/InputForm.vue"
import OrganizationSelector from "@/components/organizations/OrganizationSelector.vue"
import FacilitySelector from "@/components/facilities/FacilitySelector.vue"
import GroupSelector from "@/components/groups/GroupSelector.vue"
import AddressForm from "@/components/common/forms/AddressForm.vue"
import FormHeader from "@/components/common/forms/FormHeader.vue"
import FormFooter from "@/components/common/forms/FormFooter.vue"
import useModalFormLogic from "@/composables/useModalFormLogic"
import Modal from "@/components/common/views/Modal.vue"

//#region Public

const emit = defineEmits<{
  (e: 'onUpdate', payload: HttpResponse<OpenSilexResponse>): void
  (e: 'onCreate', payload: HttpResponse<OpenSilexResponse>): void
  (e: 'onSuccess'): void
}>()

const props = defineProps<{
  createTitle: string,
  editTitle: string
}>();
//#endregion

//#region Private
const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

//#region Data & computed
const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef')
const nFormRef = useTemplateRef<InstanceType<typeof NForm>>('formRef')
const organizationSelectorRef = ref<any>(null)

const uriGenerated = ref(true)


const hasAddress = computed(() => !!modalFormLogic.form.value.address)

const rules = computed(() => ({
  name: requiredTrimmed('component.common.name'),
  organizations: requiredArray('component.organization.title')
}))
//#endregion

//#region modalFormLogic composable
const modalFormLogic = useModalFormLogic<SiteCreationDTO>({
  modalRef,
  nFormRef,
  getEmptyForm,
  create,
  update,
  reset,
  addTitle: props.createTitle,
  editTitle: props.editTitle,
  onCreate: (res) => emit('onCreate', res),
  onUpdate: (res) => emit('onUpdate', res),
  onSuccess: () => emit('onSuccess')
})
//#endregion

//#region Methods
function getEmptyForm(): SiteCreationDTO {
  return {
    uri: undefined,
    rdf_type: undefined,
    name: undefined,
    description: undefined,
    address: undefined,
    organizations: [],
    facilities: [],
    groups: []
  } as any}

function reset() {
  organizationSelectorRef.value?.reset?.()
}

async function create(form: SiteCreationDTO) {
  const service = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService')
  return  await service.createSite(form) as unknown as HttpResponse<OpenSilexResponse<string>>
}

async function update(form: SiteUpdateDTO) {
  delete (form as any).rdf_type_name
  const service = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService')
  return await service.updateSite(form)
}

function toggleAddress(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  modalFormLogic.form.value.address = checked
    ? (modalFormLogic.form.value.address ?? {})
    : undefined
}
//#endregion

//#endregion

defineExpose({
  showCreateForm: modalFormLogic.showCreateForm,
  showEditForm: modalFormLogic.showEditForm
})
</script>


<style scoped></style>

<i18n>
en:
  SiteForm:
    organizations: Organizations
    facilities: Facilities
    groups: Groups
    groups-help-message: "Selected groups will have access to this site"
    toggleAddress: "Address"
    siteAlreadyExists: Site already exists
    form-name-placeholder: Enter object name
    form-type-placeholder: Select object type

fr:
  SiteForm:
    organizations: Organisations
    facilities: Installations environnementales
    groups: Groupes
    groups-help-message: "Les groupes sélectionnés auront accès à ce site"
    toggleAddress: "Adresse"
    siteAlreadyExists: Ce site existe déjà
    form-name-placeholder: Saisir le nom de l'objet
    form-type-placeholder: Sélectionner le type de l'objet
</i18n>
