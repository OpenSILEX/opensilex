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
         label="component.common.uri"
         :helpMessage="t('DocumentForm.uri-help')"
       />
     </n-form-item>

     <n-form-item path="description.identifier">
       <InputForm
         v-model:value="modalFormLogic.form.value.description.identifier"
         :label="t('DocumentForm.identifier')"
         type="text"
         :helpMessage="t('DocumentForm.identifier-help')"
         :placeholder="t('DocumentForm.placeholder-identifier')"
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
         :label="t('DocumentForm.title')"
         type="text"
         :required="true"
         :helpMessage="t('DocumentForm.title-help')"
       />
     </n-form-item>

     <n-form-item path="description.date">
       <DateForm
         v-model:value="modalFormLogic.form.value.description.date"
         :label="t('DocumentForm.date')"
         :helpMessage="t('DocumentForm.date-help')"
       />
     </n-form-item>

     <n-form-item path="description.description">
       <TextAreaForm
         v-model:value="modalFormLogic.form.value.description.description"
         :label="t('DocumentForm.description')"
         type="text"
         :helpMessage="t('DocumentForm.description-help')"
         @keydown.enter.stop
       />
     </n-form-item>

     <n-form-item path="description.targets">
       <TagInputForm
         v-model:value="modalFormLogic.form.value.description.targets"
         :baseType="opensilex.Oeso.targets"
         :selected="selected"
         :label="t('DocumentForm.targets')"
         :helpMessage="t('DocumentForm.targets-help')"
         type="text"
       />
     </n-form-item>

     <n-form-item path="description.authors">
       <TagInputForm
         v-model:value="modalFormLogic.form.value.description.authors"
         :baseType="opensilex.Oeso.hasAuthors"
         :placeholder="t('DocumentForm.placeholder-authors')"
         :label="t('DocumentForm.authors')"
         :helpMessage="t('DocumentForm.authors-help')"
         type="text"
       />
     </n-form-item>

     <n-form-item path="description.language">
       <InputForm
         v-model:value="modalFormLogic.form.value.description.language"
         :label="t('DocumentForm.language')"
         type="text"
         :helpMessage="t('DocumentForm.language-help')"
         :placeholder="t('DocumentForm.placeholder-language')"
       />
     </n-form-item>

     <n-form-item path="description.keywords">
       <TagInputForm
         v-model:value="modalFormLogic.form.value.description.keywords"
         :label="t('DocumentForm.keywords')"
         type="text"
         :helpMessage="t('DocumentForm.keywords-help')"
       />
     </n-form-item>

     <n-form-item v-if="modalFormLogic.isEditMode.value" path="description.deprecated">
       <CheckboxForm
         v-model:value="modalFormLogic.form.value.description.deprecated"
         :label="t('DocumentForm.deprecated')"
         :title="t('DocumentForm.deprecated-title')"
         :helpMessage="t('DocumentForm.deprecated-help')"
       />
     </n-form-item>

     <n-form-item v-if="!modalFormLogic.isEditMode.value" path="__contentKind">
       <n-radio-group v-model:value="documentContentType">
         <n-radio :value="DOCUMENT_CONTENT_TYPE_FILE">
           {{ t('DocumentForm.upload-file') }}
         </n-radio>
         <n-radio :value="DOCUMENT_CONTENT_TYPE_EXTERNAL_SOURCE">
           {{ t('DocumentForm.link-external-source') }}
         </n-radio>
       </n-radio-group>
     </n-form-item>

     <n-form-item
       v-if="!modalFormLogic.isEditMode.value && documentContentType === DOCUMENT_CONTENT_TYPE_FILE"
       path="file"
       :show-label="false"
     >
       <FileInputForm
         v-model:file="modalFormLogic.form.value.file"
         :label="t('DocumentForm.file')"
         type="file"
         :helpMessage="t('DocumentForm.file-help')"
         :browse-text="t('DocumentForm.browse')"
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
         :label="t('DocumentForm.external-source')"
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
import { computed, inject, ref, useTemplateRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { FormRules, NForm, NFormItem, NRadio, NRadioGroup } from 'naive-ui'
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
 'description.title': requiredTrimmed("DocumentForm.title"),
 'description.rdf_type': requiredTrimmed("DocumentForm.type"),
 ...(modalFormLogic.isEditMode.value ? {} : {
   file: {
     trigger: ['change', 'blur'],
     validator: (_rule, value) => {
       // si on est en mode "fichier", il faut un File
       if (documentContentType.value === DOCUMENT_CONTENT_TYPE_FILE) {
         return value instanceof File
           ? true
           : new Error(t('validations.required_if', { _field_: t('DocumentForm.file') }) as string)
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
           : new Error(t('validations.required_if', { _field_: t('DocumentForm.external-source') }) as string)
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

defineExpose({
 showCreateForm: modalFormLogic.showCreateForm,
 showEditForm: modalFormLogic.showEditForm
})
</script>

<i18n>
 en:
   DocumentForm:
     uri: URI or URL
     uri-help: Unique document identifier autogenerated OR uncheck to insert a URL to an external file
     type: Type
     type-help: Document Type
     title: Title
     title-help: A title given to the resource
     authors: Authors
     placeholder-authors : Last name, First name
     authors-help: An entity primarily responsible for making the resource. Recommended practice is to identify the creator with a URI. If this is not possible or feasible, a literal value that identifies the creator may be provided.
     language : Language
     language-help: A language of the resource
     placeholder-language: en
     date: Date
     date-help: Creation Date
     format: Format
     format-help: The file format, physical medium, or dimensions of the resource.
     description: Description
     description-help: Description associated to the document metadata
     keywords: Keywords
     keywords-help: A topic of the resource. Typically, the subject will be represented using keywords, key phrases, or classification codes. Recommended best practice is to use a controlled vocabulary.
     targets: Target(s)
     targets-help: List of resources's URI concerned by the document
     targets-error: Concerned item's URI expected
     deprecated: Deprecated
     deprecated-help: Deprecated File
     deprecated-title: Select this option to make deprecated document
     file: Document
     file-help: Document to upload limit to 100MB
     identifier: Identifier
     identifier-help: Recommended practice is to identify the resource by means of a string conforming to an identification system. Examples include International Standard Book Number (ISBN), Digital Object Identifier (DOI), and Uniform Resource Name (URN). Persistent identifiers should be provided as HTTP URIs.
     placeholder-identifier: doi:10.1340/309registries
     browse: Browse
     upload-file: Upload a file
     link-external-source: Link an external source
     external-source: External source
     error:
       document-already-exists: Document already exists
       file-name-too-long: File name is too long

 fr:
   DocumentForm:
     uri: URI ou URL
     uri-help: Identifiant unique du document généré automatiquement OU décochez la case pour lier une URL de fichier externe
     type: Type
     type-help: Type de Document
     title: Titre
     title-help: Titre de la ressource
     authors: Auteurs
     placeholder-authors : Nom, Prénom
     authors-help: Une entité à l'origine de la creation la ressource. La pratique recommandée consiste à identifier le créateur avec un URI. Si cela n'est pas possible ou faisable, une valeur littérale identifiant le créateur peut être fournie.
     language : Langue
     language-help: Langue de la ressource
     placeholder-language: fr
     date: Date
     date-help: Date de création
     format: Format
     format-help: Le format de fichier, le support physique ou les dimensions de la ressource
     description: Description
     description-help: Description associée aux métadonnées du document
     keywords: Mots-clés
     keywords-help: Le(s) sujet(s) de la ressource. En règle générale, le sujet sera représenté à l'aide de mots-clés, d'expressions clés ou de codes de classification. La meilleure pratique recommandée consiste à utiliser un vocabulaire contrôlé.
     targets: Cible(s)
     targets-help: Liste d'URI des ressources concernées par le document
     targets-error: URI de l'élément concerné attendu
     deprecated: Obsolète
     deprecated-help: Fichier obsolète
     deprecated-title: Sélectionnez cette option pour rendre le document obsolète
     file: Document
     file-help: Document à insérer limité à 100MB
     identifier: Identifiant
     identifier-help: La pratique recommandée est d'identifier la ressource au moyen d'une chaîne conforme à un système d'identification. Les exemples incluent le numéro international normalisé du livre (ISBN), l'identificateur d'objet numérique (DOI) et le nom uniforme de ressource (URN). Les identificateurs persistants doivent être fournis sous forme d'URI HTTP.
     placeholder-identifier: doi:10.1340/309registries
     browse: Parcourir
     upload-file: Importer un fichier
     link-external-source: Lier une source externe
     external-source: Source externe
     error:
       document-already-exists: Le document existe déjà
       file-name-too-long: Le nom du fichier est trop long
</i18n>
