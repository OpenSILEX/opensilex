<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="formTitle" icon="bi#bi-geo-alt"/>
    </template>

    <n-form
        ref="formRef"
        :rules="rules"
        :model="form"
        label-placement="top"
        :show-require-mark="true"
        size="large"
    >
      <!-- URI -->
      <n-form-item>
        <UriForm
            v-model:uri="form.uri"
            label="component.common.uri"
            helpMessage="component.common.uri-help-message"
            :editMode="isEditMode"
            :generated="true"
        />
      </n-form-item>

      <!-- Name -->
      <n-form-item path="name">
        <InputForm
            v-model:value="form.name"
            label="component.common.name"
            type="text"
            :required="true"
            :placeholder="t('SiteForm.form-name-placeholder')"
        />
      </n-form-item>

      <!-- Description -->
      <n-form-item>
        <InputForm
            v-model:value="form.description"
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
          v-model:organizations="form.organizations"
          :multiple="true"
          :required="true"
      />

      <!-- Facilities -->
      <FacilitySelector
          :label="t('SiteForm.facilities')"
          v-model:facilities="form.facilities"
          :multiple="true"
      />

      <!-- Groups -->
      <GroupSelector
          :label="t('SiteForm.groups')"
          v-model:groups="form.groups"
          :multiple="true"
          :helpMessage="t('SiteForm.groups-help-message')"
      />

      <!-- Address toggle -->
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

      <!-- Address -->
      <n-form-item v-if="hasAddress">
        <AddressForm
            v-if="hasAddress"
            v-model:address="form.address"
        />
      </n-form-item>

    </n-form>

    <template #footer>
      <FormFooter @cancel="hide" @submit="submit"/>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {computed, inject, useTemplateRef} from 'vue'
import {useI18n} from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type {OrganizationsService} from 'opensilex-core/api/organizations.service'
import type {SiteCreationDTO} from 'opensilex-core/index'
import type {SiteUpdateDTO} from 'opensilex-core/model/siteUpdateDTO'
import {NForm, NFormItem} from 'naive-ui'
import {requiredNotEmpty, requiredTrimmed} from '@/models/FormFieldsFormatter'

import UriForm from "@/components/common/forms/UriForm.vue"
import InputForm from "@/components/common/forms/InputForm.vue"
import OrganizationSelector from "@/components/organizations/OrganizationSelector.vue"
import FacilitySelector from "@/components/facilities/FacilitySelector.vue"
import GroupSelector from "@/components/groups/GroupSelector.vue"
import AddressForm from "@/components/common/forms/AddressForm.vue"
import FormHeader from "@/components/common/forms/FormHeader.vue"
import FormFooter from "@/components/common/forms/FormFooter.vue"
import useModalFormLogic, {ModalFormEmits, ModalFormProps} from "@/composables/useModalFormLogic"
import Modal from "@/components/common/views/Modal.vue"

//#region Public
const emit = defineEmits<ModalFormEmits>();
const props = defineProps<ModalFormProps & {
  initialOrganizations?: Array<string>
}>();
//#endregion

//#region Private
const {t} = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex');
const service = opensilex.getService<OrganizationsService>('opensilex.OrganizationsService');

//#region Data & computed
const hasAddress = computed(() => !!form.value.address);

const rules = computed(() => ({
  name: requiredTrimmed('component.common.name'),
  organizations: requiredNotEmpty('component.organization.title')
}));
//#endregion

//#region modalFormLogic composable
const {form, formTitle, exposed, isEditMode, submit, hide} = useModalFormLogic<SiteCreationDTO>({
  modalRef: useTemplateRef<InstanceType<typeof Modal>>('modalRef'),
  nFormRef: useTemplateRef<InstanceType<typeof NForm>>('formRef'),
  getEmptyForm: () => ({
    uri: undefined,
    rdf_type: undefined,
    name: undefined,
    description: undefined,
    address: undefined,
    organizations: props.initialOrganizations ?? [],
    facilities: [],
    groups: []
  }),
  create: service.createSite.bind(service),
  update: (form) => {
    delete form.rdf_type_name;
    return service.updateSite(form as SiteUpdateDTO);
  },
  props,
  emit
})
//#endregion

//#region Methods
function toggleAddress(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  form.value.address = checked
      ? (form.value.address ?? {})
      : undefined
}

//#endregion

//#endregion

defineExpose(exposed);
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
