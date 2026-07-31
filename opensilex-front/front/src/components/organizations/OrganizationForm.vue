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
            :editMode="modalFormLogic.isEditMode.value"
            :generated.sync="uriGenerated"
        ></UriForm>
      </n-form-item>

      <!-- Name -->
      <n-form-item path="name">
        <InputForm
          v-model:value="modalFormLogic.form.value.name"
          label="component.common.name"
          type="text"
          :required="true"
          :placeholder="t('OrganizationForm.form-name-placeholder')"
        />
      </n-form-item>

      <!-- Type -->
      <n-form-item path="rdf_type">
        <TypeForm
          v-model:type="modalFormLogic.form.value.rdf_type"
          :baseType="opensilex.Foaf.ORGANIZATION_TYPE_URI"
          :ignoreRoot="false"
          :required="true"
          :tree="true"
          :placeholder="t('OrganizationForm.form-type-placeholder')"
        />
      </n-form-item>

      <!-- Parents -->
      <FormSelector
        v-if="parentOptionsReady"
        v-model:selected="modalFormLogic.form.value.parents"
        :options="parentOptions"
        :multiple="true"
        label="component.common.parent"
        :placeholder="t('OrganizationForm.form-parent-placeholder')"
      />

      <!-- Groups -->
      <GroupSelector
        v-model:groups="modalFormLogic.form.value.groups"
        :label="t('component.group.title')"
        :multiple="true"
      />

      <!-- Facilities -->
      <FacilitySelector
        v-model:facilities="modalFormLogic.form.value.facilities"
        :label="t('component.facility.title')"
        :multiple="true"
      />
    </n-form>

    <template #footer>
      <FormFooter @cancel="modalFormLogic.hide" @submit="modalFormLogic.submit" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {computed, inject, onMounted, ref, useTemplateRef} from "vue";
import { NForm, NFormItem } from "naive-ui";
import type OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import type HttpResponse from "../../lib/HttpResponse";
import type { OpenSilexResponse } from "../../lib/HttpResponse";
import type { OrganizationDagDTO } from "opensilex-core/index";
import { OrganizationsService } from "opensilex-core/api/organizations.service";
import type { OrganizationCreationDTO } from "opensilex-core/model/organizationCreationDTO";
import type { OrganizationUpdateDTO } from "opensilex-core/model/organizationUpdateDTO";
import { useI18n } from "vue-i18n";
import {required, requiredTrimmed} from "../../models/FormFieldsFormatter";
import UriForm from "@/components/common/forms/UriForm.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import TypeForm from "@/components/common/forms/TypeForm.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import FormHeader from "@/components/common/forms/FormHeader.vue";
import FormFooter from "@/components/common/forms/FormFooter.vue";
import GroupSelector from "@/components/groups/GroupSelector.vue";
import FacilitySelector from "@/components/facilities/FacilitySelector.vue";
import Modal from "@/components/common/views/Modal.vue";
import useModalFormLogic from "@/composables/useModalFormLogic";

//#region Public
type OrganizationFormModel = OrganizationCreationDTO & {
  uri: string | null;
  rdf_type_name?: string;
};

const emit = defineEmits<{
  (e: 'onUpdate', payload: HttpResponse<OpenSilexResponse>): void
  (e: 'onCreate', payload: HttpResponse<OpenSilexResponse>): void
  (e: 'onSuccess'): void
}>();

const props = defineProps<{
  createTitle: string,
  editTitle: string
}>();
//#endregion

//#region Private

//#region Plugin and services
const opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const service = opensilex.getService<OrganizationsService>(
  "opensilex-core.OrganizationsService"
);
const { t } = useI18n();
//#endregion

const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef');
const nFormRef = useTemplateRef<InstanceType<typeof NForm>>('formRef');

//#region Datas
let uriGenerated = ref<boolean>(true);
const parentOrganizations = ref<OrganizationDagDTO[]>([]);
//#endregion

//#region Computed
const rules = computed(() => ({
  "name": requiredTrimmed("component.common.name"),
  "rdf_type": required("component.common.type")
}));

const parentOptionsReady = computed(() => parentOrganizations.value.length > 0);

const parentOptions = computed(() => {
  if (modalFormLogic.isEditMode.value) {
    return opensilex.buildTreeFromDag(parentOrganizations.value, {
      disableSubTree: modalFormLogic.form.value.uri,
    });
  }
  return opensilex.buildTreeFromDag(parentOrganizations.value);
});
//#endregion

//#region modalFormLogic composable
const modalFormLogic = useModalFormLogic<OrganizationFormModel>({
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
  onSuccess: () => emit('onSuccess'),
});
//#endregion

//#region Methods
onMounted(() => {
  loadParentOrganizations();
});

function getEmptyForm(): OrganizationFormModel {
  return {
    uri: null,
    rdf_type: null,
    name: "",
    parents: [],
    groups: [],
    facilities: [],
  };
}

async function reset(): Promise<void> {
  uriGenerated.value = true;
}

function cleanFormBeforeSend(targetForm: OrganizationCreationDTO | OrganizationUpdateDTO) {
  targetForm.parents = (targetForm.parents || []).filter((parent) => parent);
}

async function loadParentOrganizations(): Promise<void> {
  const http = await service.searchOrganizations();
  parentOrganizations.value = http.response.result;
}

async function create(targetForm: OrganizationFormModel) {
  cleanFormBeforeSend(targetForm);
  return await service.createOrganization(targetForm);
}

async function update(targetForm: OrganizationFormModel) {
  cleanFormBeforeSend(targetForm);
  delete targetForm.rdf_type_name;
  return service.updateOrganization(targetForm);
}
//#endregion

//#endregion
defineExpose({
  showCreateForm: modalFormLogic.showCreateForm,
  showEditForm: modalFormLogic.showEditForm
});
</script>

<style scoped lang="scss">

</style>

<i18n>
en:
  OrganizationForm:
    name: The organization
    organization-uri: Organization URI
    form-name-placeholder: Enter organization name
    form-type-placeholder: Select organization type
    form-parent-placeholder: Select parent organization
    organization-already-exists: Organization already exists with this URI
fr:
  OrganizationForm:
    name: L'organisation
    organization-uri: URI de l'organisation
    form-name-placeholder: Saisir le nom de l'organisation
    form-type-placeholder: Sélectionner le type d'organisation
    form-parent-placeholder: Sélectionner l'organisation parente
    organization-already-exists: Une organisation existe déjà avec cette URI
</i18n>