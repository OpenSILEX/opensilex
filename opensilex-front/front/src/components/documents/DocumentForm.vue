<template>
 <Modal ref="modalRef">
   <template #header>
     <FormHeader :title="modalFormLogic.formTitle.value" icon="bi#bi-file-earmark-text" />
   </template>

   <n-form
     ref="formRef"
     :rules="rules"
     :model="modalFormLogic.form.value"
     label-placement="top"
     :show-require-mark="true"
     size="large"
   >
     <n-form-item path="description.uri">
       <UriForm
         :uri.sync="modalFormLogic.form.value.description.uri"
         :generated.sync="uriGenerated"
         :editMode="modalFormLogic.isEditMode.value"
         label="component.common.uri-or-url"
         :helpMessage="t('component.document.uri-help')"
       />
     </n-form-item>

     <n-form-item path="description.identifier">
       <InputForm
         v-model:value="modalFormLogic.form.value.description.identifier"
         :label="t('component.document.identifier')"
         type="text"
         :helpMessage="t('component.document.identifier-help')"
         :placeholder="t('component.document.placeholder-identifier')"
       />
     </n-form-item>

     <n-form-item path="description.rdf_type">
       <TypeForm
         :key="modalFormLogic.form.value.description.rdf_type ?? 'no-type'"
         v-model:type="modalFormLogic.form.value.description.rdf_type"
         :baseType="opensilex.Oeso.DOCUMENT_TYPE_URI"
         :required="true"
       />
     </n-form-item>

     <n-form-item path="description.title">
       <InputForm
         v-model:value="modalFormLogic.form.value.description.title"
         :label="t('component.document.title')"
         type="text"
         :required="true"
         :helpMessage="t('component.document.title-help')"
       />
     </n-form-item>

     <n-form-item path="description.date">
       <DateForm
         v-model:value="modalFormLogic.form.value.description.date"
         :label="t('component.document.date')"
         :helpMessage="t('component.document.date-help')"
       />
     </n-form-item>

     <n-form-item path="description.description">
       <TextAreaForm
         v-model:value="modalFormLogic.form.value.description.description"
         :label="t('component.document.description')"
         type="text"
         :helpMessage="t('component.document.description-help')"
         @keydown.enter.stop
       />
     </n-form-item>

     <n-form-item path="description.targets">
       <TagInputForm
         v-model:value="modalFormLogic.form.value.description.targets"
         :baseType="opensilex.Oeso.targets"
         :label="t('component.document.targets')"
         :helpMessage="t('component.document.targets-help')"
         type="text"
       />
     </n-form-item>

     <n-form-item path="description.authors">
       <TagInputForm
         v-model:value="modalFormLogic.form.value.description.authors"
         :baseType="opensilex.Oeso.hasAuthors"
         :placeholder="t('component.document.placeholder-authors')"
         :label="t('component.document.authors')"
         :helpMessage="t('component.document.authors-help')"
         type="text"
       />
     </n-form-item>

     <n-form-item path="description.language">
       <InputForm
         v-model:value="modalFormLogic.form.value.description.language"
         :label="t('component.document.language')"
         type="text"
         :helpMessage="t('component.document.language-help')"
         :placeholder="t('component.document.placeholder-language')"
       />
     </n-form-item>

     <n-form-item path="description.keywords">
       <TagInputForm
         v-model:value="modalFormLogic.form.value.description.keywords"
         :label="t('component.document.keywords')"
         type="text"
         :helpMessage="t('component.document.keywords-help')"
       />
     </n-form-item>

     <n-form-item v-if="modalFormLogic.isEditMode.value" path="description.deprecated">
       <CheckboxForm
         v-model:value="modalFormLogic.form.value.description.deprecated"
         :label="t('component.document.deprecated')"
         :title="t('component.document.deprecated-title')"
         :helpMessage="t('component.document.deprecated-help')"
       />
     </n-form-item>

     <n-form-item v-if="!modalFormLogic.isEditMode.value" path="__contentKind">
       <n-radio-group v-model:value="documentContentType">
         <n-radio :value="DOCUMENT_CONTENT_TYPE_FILE">
           {{ t('component.document.upload-file') }}
         </n-radio>
         <n-radio :value="DOCUMENT_CONTENT_TYPE_EXTERNAL_SOURCE">
           {{ t('component.document.link-external-source') }}
         </n-radio>
       </n-radio-group>
     </n-form-item>

     <n-form-item
       v-if="!modalFormLogic.isEditMode.value && documentContentType === DOCUMENT_CONTENT_TYPE_FILE"
       path="file"
       ref="fileItem"
     >
       <FileInputForm
         v-model:file="modalFormLogic.form.value.file"
         :label="t('component.document.file')"
         type="file"
         :helpMessage="t('component.document.file-help')"
         :browse-text="t('component.document.browse')"
         :required="true"
         rules="size:100000"
       />
     </n-form-item>

     <n-form-item
       v-if="!modalFormLogic.isEditMode.value && documentContentType === DOCUMENT_CONTENT_TYPE_EXTERNAL_SOURCE"
       path="description.source"
     >
       <InputForm
         v-model:value="modalFormLogic.form.value.description.source"
         :label="t('component.document.external-source')"
         type="text"
         :required="true"
         rules="url"
       />
     </n-form-item>
   </n-form>

   <template #footer>
     <FormFooter @cancel="modalFormLogic.hide" @submit="modalFormLogic.submit" />
   </template>
 </Modal>
</template>

<script setup lang="ts">
import {computed, inject, ref, useTemplateRef, watch} from 'vue'
import { useI18n } from 'vue-i18n'
import {FormItemInst, FormRules, NForm, NFormItem, NRadio, NRadioGroup} from 'naive-ui'
import Modal from '@/components/common/views/Modal.vue'
import FormHeader from '@/components/common/forms/FormHeader.vue'
import FormFooter from '@/components/common/forms/FormFooter.vue'
import UriForm from '@/components/common/forms/UriForm.vue'
import InputForm from '@/components/common/forms/InputForm.vue'
import TypeForm from '@/components/common/forms/TypeForm.vue'
import DateForm from '@/components/common/forms/DateForm.vue'
import TextAreaForm from '@/components/common/forms/TextAreaForm.vue'
import TagInputForm from '@/components/common/forms/TagInputForm.vue'
import CheckboxForm from '@/components/common/forms/CheckboxForm.vue'
import FileInputForm from '@/components/common/forms/FileInputForm.vue'
import useModalFormLogic, { type ModalFormEmits, type ModalFormProps } from '@/composables/useModalFormLogic'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import {requiredTrimmed} from "@/models/FormFieldsFormatter";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";

interface DocumentFormModel {
  description: {
    uri?: string
    identifier?: string
    rdf_type?: string
    title?: string
    date?: string
    description?: string
    targets?: string[]
    authors?: string[]
    language?: string
    deprecated?: boolean
    keywords?: string[]
    source?: string
  },
  file?: File | undefined
}

const emit = defineEmits<ModalFormEmits>()
const props = defineProps<ModalFormProps>()

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef')
const formRef = useTemplateRef<InstanceType<typeof NForm>>('formRef')
const fileItem = ref<FormItemInst | null>(null)
const uriGenerated = ref(true)

type ContentKind = 'file' | 'external-source'
const DOCUMENT_CONTENT_TYPE_FILE: ContentKind = 'file'
const DOCUMENT_CONTENT_TYPE_EXTERNAL_SOURCE: ContentKind = 'external-source'
const documentContentType = ref<ContentKind>(DOCUMENT_CONTENT_TYPE_FILE)

const modalFormLogic = useModalFormLogic<DocumentFormModel>({
  modalRef,
  nFormRef: formRef,
  getEmptyForm,
  create,
  update,
  reset,
  props,
  emit
})

const rules = computed<FormRules>(() => ({
 'description.title': requiredTrimmed("component.document.title"),
 'description.rdf_type': requiredTrimmed("component.common.type"),
 ...(modalFormLogic.isEditMode.value ? {} : {
   file: {
     validator: (_rule, value) => {
       // si on est en mode "fichier", il faut un File
       if (documentContentType.value === DOCUMENT_CONTENT_TYPE_FILE) {
         return value instanceof File
           ? true
           : new Error(t('validations.required_if', { _field_: t('component.document.file') }) as string)
       }
       // sinon (mode source externe), pas d'obligation sur file
       return true
     }
   },
   'description.source': {
     trigger: ['blur', 'change'],
     validator: (_rule, value) => {
       // en mode "source", valeur non vide requise
       if (documentContentType.value === DOCUMENT_CONTENT_TYPE_EXTERNAL_SOURCE) {
         return value != null && String(value).trim() !== ''
           ? true
           : new Error(t('validations.required_if', { _field_: t('component.document.external-source') }) as string)
       }
       return true
     }
   }
 })
}))

function getEmptyForm(): DocumentFormModel {
 return {
   description: {
     uri: undefined,
     identifier: undefined,
     rdf_type: undefined,
     title: undefined,
     date: undefined,
     description: undefined,
     targets: [],
     authors: [],
     language: undefined,
     deprecated: false,
     keywords: [],
     source: undefined
   },
   file: undefined
 }
}

function reset(): void {
 uriGenerated.value = true
 if (!modalFormLogic.form.value?.description) return

delete modalFormLogic.form.value.description.source
modalFormLogic.form.value.file = undefined
}

async function create(formData: DocumentFormModel): Promise<HttpResponse<OpenSilexResponse>> {
  // Nettoyage champ non utilisé selon le choix
  if (documentContentType.value === DOCUMENT_CONTENT_TYPE_FILE) {
   delete formData.description.source
 } else {
   delete formData.file
 }

return  opensilex.uploadFileToService('/core/documents', formData, null, false) as Promise<HttpResponse<OpenSilexResponse>>
}

async function update(formData: DocumentFormModel): Promise<HttpResponse<OpenSilexResponse>> {
 return  await opensilex.uploadFileToService('/core/documents', formData, null, true) as Promise<HttpResponse<OpenSilexResponse>>
}

watch(() => modalFormLogic.form.value.file,
    () => {
      // Efface l’état d’erreur de l’item "Fichier" quand un fichier est sélectionné
      fileItem.value?.restoreValidation()
    },
    { flush: 'post' }
)

defineExpose(modalFormLogic.exposed)

</script>
