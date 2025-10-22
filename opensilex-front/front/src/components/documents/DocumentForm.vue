<template>
  <n-form
    v-if="form?.description"
    ref="formRef"
    :model="form"
    :rules="rules"
    label-placement="top"
    :show-require-mark="true"
    class="documentForm"
  >
    <!-- URI / URL -->
    <opensilex-UriForm
      v-model:uri="form.description.uri"
      :generated="uriGenerated"
      @update:generated="val => (uriGenerated = val)"
      :editMode="editMode"
      label="component.common.uri"
      :helpMessage="t('DocumentForm.uri-help')"
    />
      <!-- :helpMessage="t('component.common.uri-help-message')" -->

    <!-- Identifier -->
    <opensilex-InputForm
      v-model:value="form.description.identifier"
      :label="t('DocumentForm.identifier')"
      type="text"
      :helpMessage="t('DocumentForm.identifier-help')"
      :placeholder="t('DocumentForm.placeholder-identifier')"
      class="documentFormField"
    />

    <!-- Type -->
    <n-form-item path="description.rdf_type" ref="rdfTypeItem">
        <opensilex-TypeForm
        v-model:type="form.description.rdf_type"
        :baseType="opensilex.Oeso.DOCUMENT_TYPE_URI"
        :required="true"
        :helpMessage="t('DocumentForm.type-help')"
        />
    </n-form-item>

    <!-- Title -->
    <n-form-item path="description.title">
        <opensilex-InputForm
        v-model:value="form.description.title"
        :label="t('DocumentForm.title')"
        type="text"
        :required="true"
        :helpMessage="t('DocumentForm.title-help')"
        class="documentFormField"
        />
    </n-form-item>

    <!-- Date -->
    <opensilex-DateForm
      v-model:value="form.description.date"
      label="DocumentForm.date"
      helpMessage="DocumentForm.date-help"
    />

    <!-- Description -->
    <opensilex-TextAreaForm
      v-model:value="form.description.description"
      :label="t('DocumentForm.description')"
      type="text"
      :helpMessage="t('DocumentForm.description-help')"
      @keydown.enter.stop
      class="documentFormField"
    />

    <!-- Targets (URLs/URIs) -->
    <opensilex-TagInputForm
      class="overflow-auto documentFormField"
      v-model:value="form.description.targets"
      :baseType="opensilex.Oeso.targets"
      :selected="selected"
      :label="t('DocumentForm.targets')"
      :helpMessage="t('DocumentForm.targets-help')"
      type="text"
    />
      <!-- style="height: 90px" -->

    <!-- Authors -->
    <opensilex-TagInputForm
      v-model:value="form.description.authors"
      :baseType="opensilex.Oeso.hasAuthors"
      :placeholder="t('DocumentForm.placeholder-authors')"
      :label="t('DocumentForm.authors')"
      :helpMessage="t('DocumentForm.authors-help')"
      type="text"
      class="documentFormField"
    />

    <!-- Language -->
    <opensilex-InputForm
      v-model:value="form.description.language"
      :label="t('DocumentForm.language')"
      type="text"
      :helpMessage="t('DocumentForm.language-help')"
      :placeholder="t('DocumentForm.placeholder-language')"
      class="documentFormField"
    />

    <!-- Keywords -->
    <opensilex-TagInputForm
      v-model:value="form.description.keywords"
      :label="t('DocumentForm.keywords')"
      type="text"
      :helpMessage="t('DocumentForm.keywords-help')"
      class="documentFormField"
    />

    <!-- Deprecated -->
    <opensilex-CheckboxForm
      v-if="editMode"
      v-model:value="form.description.deprecated"
      :label="t('DocumentForm.deprecated')"
      :title="t('DocumentForm.deprecated-title')"
      :helpMessage="t('DocumentForm.deprecated-help')"
    />

    <!-- Choix contenu (création uniquement) -->
    <n-form-item v-if="!editMode" :show-label="false" path="__contentKind">
      <n-radio-group v-model:value="documentContentType">
        <n-radio :value="DOCUMENT_CONTENT_TYPE_FILE">
          {{ t('DocumentForm.upload-file') }}
        </n-radio>
        <n-radio :value="DOCUMENT_CONTENT_TYPE_EXTERNAL_SOURCE">
          {{ t('DocumentForm.link-external-source') }}
        </n-radio>
      </n-radio-group>
    </n-form-item>

    <!-- Fichier -->
    <n-form-item
        v-if="!editMode && documentContentType === DOCUMENT_CONTENT_TYPE_FILE"
        path="file"
        :show-label="false"
        ref="fileItem"
    >
        <opensilex-FileInputForm
        v-model:file="form.file"
        :label="t('DocumentForm.file')"
        type="file"
        :helpMessage="t('DocumentForm.file-help')"
        :browse-text="t('DocumentForm.browse')"
        :required="true"
        rules="size:100000"
        />
    </n-form-item>

    <!-- Source externe -->
    <n-form-item
      v-if="!editMode && documentContentType === DOCUMENT_CONTENT_TYPE_EXTERNAL_SOURCE"
      path="description.source"
    >
        <opensilex-InputForm
        v-model:value="form.description.source"
        :label="t('DocumentForm.external-source')"
        type="text"
        :required="true"
        rules="url"
        />
    </n-form-item>
  </n-form>
</template>

<script setup lang="ts">
import { ref, computed, inject, withDefaults, defineProps, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInst, FormItemInst } from 'naive-ui'
import { NForm, NFormItem, NRadio, NRadioGroup } from 'naive-ui'
import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin'

// ---- props
type DocForm = {
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

const props = withDefaults(defineProps<{
  editMode?: boolean
  form: DocForm
data?: { initialTargets?: string[] }
 }>(), {
   editMode: false,
  form: () => ({ description: { /* … */ }, file: undefined }),
   data: () => ({ initialTargets: [] })
 })

// const { t } = useI18n()
const { t } = useI18n({ useScope: 'local' })
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

// ---- refs / état local
const formRef = ref<FormInst | null>(null)
const uriGenerated = ref<boolean>(true)
const rdfTypeItem = ref<FormItemInst | null>(null)
const fileItem = ref<FormItemInst | null>(null)

type ContentKind = 'file' | 'external-source'
const DOCUMENT_CONTENT_TYPE_FILE: ContentKind = 'file'
const DOCUMENT_CONTENT_TYPE_EXTERNAL_SOURCE: ContentKind = 'external-source'
const documentContentType = ref<ContentKind>(DOCUMENT_CONTENT_TYPE_FILE)

watch(
  () => props.form?.description?.rdf_type,
  () => {
    // Efface l’état d’erreur de l’item "Type" quand l’utilisateur choisit une valeur
    rdfTypeItem.value?.restoreValidation()
  },
  { flush: 'post' }
)

watch(
  () => props.form?.file,
  () => {
    // Efface l’état d’erreur de l’item "Fichier" quand un fichier est sélectionné
    fileItem.value?.restoreValidation()
  },
  { flush: 'post' }
)


// ---- règles
const rules = computed(() => ({
  'description.title': { required: true, message: t('validations.required_if', { _field_: t('DocumentForm.title') }), trigger: ['blur','change'] },
  'description.rdf_type': { required: true, message: t('validations.required_if', { _field_: t('DocumentForm.type') }), trigger: ['change','blur'] },
  ...(props.editMode ? {} : {
   file: {
     trigger: ['change', 'blur'],
     validator: (_rule: any, value: unknown) => {
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
     trigger: ['blur','change'],
     validator: (_rule: any, value: unknown) => {
       // en mode "source", valeur non vide requise
       if (documentContentType.value === DOCUMENT_CONTENT_TYPE_EXTERNAL_SOURCE) {
         return (value != null && String(value).trim() !== '')
           ? true
           : new Error(t('validations.required_if', { _field_: t('DocumentForm.external-source') }) as string)
       }
       return true
     }
   }
 })
}))


// ---- helpers API pour ModalForm
function getEmptyForm(): DocForm {
  return {
    description: {
      uri: undefined,
      identifier: undefined,
      rdf_type: undefined,
      title: undefined,
      date: undefined,
      description: undefined,
      targets: (props.data?.initialTargets?.length
        ? [...props.data.initialTargets]
        : undefined),        // <- single targets field
      authors: undefined,
      language: undefined,
      deprecated: undefined,
      keywords: undefined,
      source: undefined
    },
    file: undefined
  }
}


// function reset () {
//   uriGenerated.value = true
//   // si on bascule de mode de contenu, évite les collisions file/source
//   if (documentContentType.value === DOCUMENT_CONTENT_TYPE_FILE) {
//     props.form.description.source = undefined
//   } else {
//     props.form.file = undefined
//   }
// }

function reset () {
   uriGenerated.value = true
   const f = props.form
   if (!f || !f.description) return
   // nettoyage selon le mode choisi
   if (documentContentType.value === DOCUMENT_CONTENT_TYPE_FILE) {
     delete f.description.source
   } else {
     f.file = undefined
   }
  // si pas encore de cibles, injecter celles du parent
  if (!Array.isArray(f.description.targets) || f.description.targets.length === 0) {
    f.description.targets = [...(props.data?.initialTargets ?? [])]
  }
 }


async function validate () {
  try {
    await formRef.value?.validate()
        // double check, au cas où
        if (!props.editMode && documentContentType.value === DOCUMENT_CONTENT_TYPE_FILE && !props.form.file) {
        return false
        }
        //
    return true
  } catch {
    return false
  }
}


// ---- create / update
async function create (form: DocForm) {
  try {
    // Nettoyage champ non utilisé selon le choix
    if (documentContentType.value === DOCUMENT_CONTENT_TYPE_FILE) {
      delete form.description.source
    } else {
      delete form.file
    }

    const http: any = await opensilex.uploadFileToService('/core/documents', form, null, false)

    if (http.result?.message) {
    if (http.metadata?.status === 409) {
        opensilex.showErrorToast(t('DocumentForm.error.document-already-exists') + ' - ' + http.result.message)
        throw new Error('409')
    } else if (http.metadata?.status === 400) {
        opensilex.showErrorToast(http.result.message)
        throw new Error('400')
    }
    opensilex.showErrorToast(http.result.message)
    throw new Error('create-failed')
    }

    const uri = http.result
    form.description.uri = uri
    return form
  } catch (err: any) {
    opensilex.errorHandler(err)
    return false
  }
}

async function update (form: DocForm) {
  try {
    const http: any = await opensilex.uploadFileToService('/core/documents', form, null, true)
    // uri = http.result (si besoin)
    return form
  } catch (err: any) {
    opensilex.errorHandler(err)
    return false
  }
}

// ---- expose pour ModalForm
defineExpose({
  getEmptyForm,
  reset,
  validate,
  create,
  update
})
</script>

<style scoped>
.documentFormField {
    margin-bottom: 10px;
}
</style>

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

