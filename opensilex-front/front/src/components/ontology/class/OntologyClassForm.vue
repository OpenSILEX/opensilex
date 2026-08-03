<template>
  <Modal ref="modal">
    <template #header>
      <FormHeader :title="formTitle" icon="bi#bi-geo-alt"/>
    </template>

    <n-form
        ref="nForm"
        v-if="form.name_translations"
        :model="form"
        :rules="rules"
    >
      <n-form-item path="uri">
        <InputForm
            v-model:value="form.uri"
            label="component.common.uri"
            type="text"
            :disabled="isEditMode"
            :required="true"
        ></InputForm>
      </n-form-item>

      <FormSelector
          v-model:selected="form.parent"
          path="parent"
          :options="parentOptions"
          checkStrategy="parent"
          :required="true"
          label="component.common.parent"
          :filterable="true"
      ></FormSelector>

      <n-form-item path="name_translations.en">
        <InputForm
            v-model:value="form.name_translations.en"
            :label="t('component.ontology.class.label.en')"
            type="text"
            :required="true"
        ></InputForm>
      </n-form-item>

      <n-form-item path="comment_translations.en">
        <TextAreaForm
            v-model:value="form.comment_translations.en"
            :label="t('component.ontology.class.comment.en')"
            :required="true"
            @keydown.native.enter.stop
        ></TextAreaForm>
      </n-form-item>

      <n-form-item path="name_translations.fr">
        <InputForm
            v-model:value="form.name_translations.fr"
            :label="t('component.ontology.class.label.fr')"
            type="text"
            :required="true"
        ></InputForm>
      </n-form-item>

      <n-form-item path="comment_translations.fr">
        <TextAreaForm
            v-model:value="form.comment_translations.fr"
            :label="t('component.ontology.class.comment.fr')"
            :required="true"
            @keydown.native.enter.stop
        ></TextAreaForm>
      </n-form-item>

      <IconForm
          v-model:value="form.icon"
          :label="t('component.ontology.class.icon')"
      ></IconForm>
    </n-form>

    <template #footer>
      <FormFooter @cancel="hide" @submit="submit"/>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {computed, inject, ref, useTemplateRef, watch, watchEffect} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import {VueJsOntologyExtensionService, VueRDFTypeDTO} from "@/lib";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import {FormRules, NForm, NFormItem} from "naive-ui";
import {OntologyService} from "opensilex-core/api/ontology.service";
import InputForm from "@/components/common/forms/InputForm.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import TextAreaForm from "@/components/common/forms/TextAreaForm.vue";
import IconForm from "@/components/common/forms/IconForm.vue";
import {required} from "@/models/FormFieldsFormatter";
import useModalFormLogic, {ModalFormEmits, ModalFormProps} from "@/composables/useModalFormLogic";
import FormHeader from "@/components/common/forms/FormHeader.vue";
import FormFooter from "@/components/common/forms/FormFooter.vue";
import Modal from "@/components/common/views/Modal.vue";

//#region Public
const props = defineProps<ModalFormProps & {
  parentUri: string
}>();

const emit = defineEmits<ModalFormEmits>();
//#endregion

//#region Private
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const ontologyService = opensilex.getService<OntologyService>("opensilex-core.OntologyService");
const service = opensilex.getService<VueJsOntologyExtensionService>("opensilex.VueJsOntologyExtensionService");
const {t} = useI18n();

const rules: FormRules = {
  uri: required("component.common.uri"),
  parent: required("component.common.parent"),
  name_translations: {
    en: required("component.ontology.class.label.en"),
    fr: required("component.ontology.class.label.fr"),
  },
  comment_translations: {
    en: required("component.ontology.class.comment.en"),
    fr: required("component.ontology.class.comment.fr"),
  }
}

const availableParents = ref<Array<any>>([]);

const parentOptions = computed(() => {
  if (isEditMode) {
    return opensilex.buildTreeListOptions(availableParents.value, {
      disableSubTree: form.value.uri
    });
  } else {
    return opensilex.buildTreeListOptions(availableParents.value);
  }
})

const {form, formTitle, showCreateForm, showEditForm, isEditMode, submit, hide} = useModalFormLogic<VueRDFTypeDTO>({
  modalRef: useTemplateRef<InstanceType<typeof Modal>>("modal"),
  nFormRef: useTemplateRef("nForm"),
  getEmptyForm: () =>  ({
    uri: undefined,
    parent: undefined,
    name_translations: {en: "", fr: ""},
    comment_translations: {en: "", fr: ""},
    icon: undefined,
    is_abstract: false
  }),
  create: service.createRDFType.bind(service),
  update: service.updateRDFType.bind(service),
  props,
  emit
});

watch(() => props.parentUri, () => {
  console.log("watch parent uri", props.parentUri)
  if (props.parentUri) {
    console.log("inside if")
    ontologyService.searchSubClassesOf(props.parentUri, undefined, false).then(http => {
      if (http.response.result.length > 0) {
        availableParents.value = http.response.result;
      }
    })
  }
}, { immediate: true });
//#endregion

defineExpose({
  showCreateForm,
  showEditForm
})
</script>

<style scoped lang="scss">
</style>
