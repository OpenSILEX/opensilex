<template>
  <n-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-placement="top"
    :show-require-mark="true"
  >
    <!-- URI -->
    <opensilex-UriForm
      v-model:uri="form.uri"
      :label="t('OrganizationForm.organization-uri')"
      helpMessage="component.common.uri-help-message"
      :editMode="editMode"
      v-model:generated="uriGenerated"
    />

    <!-- Name -->
    <n-form-item path="name" class="compact-form-item">
      <opensilex-InputForm
        path="name"
        v-model:value="form.name"
        label="component.common.name"
        type="text"
        :required="true"
        :placeholder="t('OrganizationForm.form-name-placeholder')"
      />
    </n-form-item>

    <!-- Type -->
    <n-form-item path="rdf_type" class="compact-form-item">
      <opensilex-TypeForm
        v-model:type="form.rdf_type"
        :baseType="$opensilex.Foaf.ORGANIZATION_TYPE_URI"
        :ignoreRoot="false"
        :required="true"
        :placeholder="t('OrganizationForm.form-type-placeholder')"
      />
    </n-form-item>

    <!-- Parents -->
    <opensilex-FormSelector
      v-if="parentOptionsReady"
      v-model:selected="form.parents"
      :options="parentOptions"
      :multiple="true"
      label="component.common.parent"
      :placeholder="t('OrganizationForm.form-parent-placeholder')"
    />

    <!-- Groupes -->
    <opensilex-GroupSelector
      v-model:groups="form.groups"
      :label="t('OrganizationForm.form-group-label')"
      :multiple="true"
    />

    <!-- Facilities -->
    <opensilex-FacilitySelector
      v-model:facilities="form.facilities"
      :label="t('OrganizationForm.form-facilities-label')"
      :multiple="true"
    />
  </n-form>
</template>

<script setup lang="ts">
import { computed, inject, ref, onMounted } from "vue";
import { NForm, NFormItem } from "naive-ui";
import type OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import type HttpResponse from "../../lib/HttpResponse";
import type { OpenSilexResponse } from "../../lib/HttpResponse";
import type { OrganizationDagDTO } from "opensilex-core/index";
import { OrganizationsService } from "opensilex-core/api/organizations.service";
import type { OrganizationCreationDTO } from "opensilex-core/model/organizationCreationDTO";
import type { OrganizationUpdateDTO } from "opensilex-core/model/organizationUpdateDTO";
import { useI18n } from "vue-i18n";
import { requiredTrimmed } from "../../models/FormFieldsFormatter";

type OrganizationFormModel = OrganizationCreationDTO & {
  rdf_type_name?: string;
};

const props = withDefaults(
  defineProps<{
    editMode?: boolean;
    form: OrganizationFormModel;
  }>(),
  {
    editMode: false,
    form: () => ({
      uri: null,
      rdf_type: null,
      name: "",
      parents: [],
      groups: [],
      facilities: [],
    }),
  }
);

const { t } = useI18n();
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const service = $opensilex.getService<OrganizationsService>(
  "opensilex-core.OrganizationsService"
);

const formRef = ref();
const uriGenerated = ref(true);
const parentOrganizations = ref<OrganizationDagDTO[]>([]);

const form = computed({
  get: () => props.form,
  set: () => {},
});

const rules = computed(() => ({
  name: requiredTrimmed("component.common.name"),
  rdf_type: {
    required: true,
    message: t("validations.required_if", {
      _field_: t("OrganizationForm.form-type-label"),
    }),
    trigger: ["change", "blur"],
  },
}));

onMounted(() => {
  loadParentOrganizations();
});

function getEmptyForm(): OrganizationCreationDTO {
  return {
    uri: null,
    rdf_type: null,
    name: "",
    parents: [],
    groups: [],
    facilities: [],
  };
}

function setParentOrganizations(organizations: OrganizationDagDTO[]) {
  parentOrganizations.value = organizations;
}

function loadParentOrganizations() {
  return service
    .searchOrganizations()
    .then((http: HttpResponse<OpenSilexResponse<OrganizationDagDTO[]>>) => {
      setParentOrganizations(http.response.result);
    })
    .catch($opensilex.errorHandler);
}

const parentOptionsReady = computed(() => parentOrganizations.value.length > 0);

function reset() {
  uriGenerated.value = true;
  if (parentOrganizations.value == null) {
    loadParentOrganizations();
  }
}

function init() {
  if (parentOrganizations.value == null) {
    loadParentOrganizations();
  }
}

const parentOptions = computed(() => {
  if (props.editMode) {
    return $opensilex.buildTreeFromDag(parentOrganizations.value, {
      disableSubTree: form.value.uri,
    });
  }

  return $opensilex.buildTreeFromDag(parentOrganizations.value);
});

function cleanFormBeforeSend(targetForm: OrganizationCreationDTO | OrganizationUpdateDTO) {
  targetForm.parents = (targetForm.parents || []).filter((parent) => parent);
}

async function validateForm() {
  try {
    await formRef.value?.validate();
    return true;
  } catch {
    return false;
  }
}

async function create(targetForm: OrganizationCreationDTO) {
  const isValid = await validateForm();
  if (!isValid) {
    return Promise.reject(new Error("Form validation failed"));
  }

  cleanFormBeforeSend(targetForm);

  try {
    const http = await $opensilex
      .getService<OrganizationsService>("opensilex.OrganizationsService")
      .createOrganization(targetForm);

    const uri = (http as HttpResponse<OpenSilexResponse<string>>).response.result;
    targetForm.uri = uri;
    return targetForm;
  } catch (error: any) {
    if (error?.status === 409) {
      $opensilex.errorHandler(
        error,
        t("OrganizationForm.organization-already-exists")
      );
    } else {
      $opensilex.errorHandler(error);
    }
    return Promise.reject(error);
  }
}

async function update(targetForm: OrganizationUpdateDTO & { rdf_type_name?: string }) {
  const isValid = await validateForm();
  if (!isValid) {
    return Promise.reject(new Error("Form validation failed"));
  }

  cleanFormBeforeSend(targetForm);
  delete targetForm.rdf_type_name;

  try {
    await $opensilex
      .getService<OrganizationsService>("opensilex.OrganizationsService")
      .updateOrganization(targetForm);

    const message =
      t("OrganizationForm.name") +
      " " +
      targetForm.name +
      " " +
      t("component.common.success.update-success-message");

    $opensilex.showSuccessToast(message);
    return targetForm;
  } catch (error) {
    $opensilex.errorHandler(error);
    return Promise.reject(error);
  }
}

defineExpose({
  reset,
  init,
  getEmptyForm,
  create,
  update,
  validate: validateForm,
});
</script>

<style scoped lang="scss">
// neutralisation des classes injectées par naive dans les <n-form-item> qui créent des espaces indésirés entre les champs
:deep(.compact-form-item) {
  --n-label-height: 0px !important;
  --n-label-padding: 0 !important;
}
</style>

<i18n>
en:
  OrganizationForm:
    name: The organization
    organization-uri: Organization URI
    form-name-placeholder: Enter organization name
    form-type-placeholder: Select organization type
    form-type-label: Type
    form-parent-placeholder: Select parent organization
    organization-already-exists: Organization already exists with this URI
    form-group-label: Groups
    form-facilities-label: Facilities
fr:
  OrganizationForm:
    name: L'organisation
    organization-uri: URI de l'organisation
    form-name-placeholder: Saisir le nom de l'organisation
    form-type-placeholder: Sélectionner le type d'organisation
    form-type-label: Type
    form-parent-placeholder: Sélectionner l'organisation parente
    organization-already-exists: Une organisation existe déjà avec cette URI
    form-group-label: Groupes
    form-facilities-label: Installations environnementales
</i18n>