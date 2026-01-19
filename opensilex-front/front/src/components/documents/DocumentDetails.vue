<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="bi#bi-file-earmark-text"
      :hasIcon="true"
      :title="document?.title || ''"
      :description="t('DocumentDetails.title')"
      class="detail-element-header"
    />

    <opensilex-PageActions :returnButton="true" />

    <opensilex-PageContent v-if="canDisplayDocument">
      <div class="details-grid">
        <!-- Col gauche : infos -->
        <div class="left-col">
          <opensilex-Card label="component.common.informations" icon="bi-clipboard">
            <template #rightHeader>
              <span
                v-if="document?.deprecated"
                class="badge badge-pill badge-warning"
                :title="t('DocumentDetails.deprecated')"
              >
                <i class="bi-exclamation-triangle mr-1"></i>
                {{ t('DocumentDetails.deprecated') }}
              </span>
              <opensilex-EditButton
                v-if="user?.hasCredential?.(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
                @click="onUpdateClick"
                :label="t('DocumentDetails.update')"
                :small="true"
              />
            </template>

            <template #body>
              <div class="details-fields">
                <opensilex-UriView :uri="document?.uri" :allowCopy="true" />
                <opensilex-StringView :label="t('DocumentDetails.identifier')" :value="document?.identifier" />
                <opensilex-StringView :label="t('DocumentDetails.type')" :value="document?.rdf_type_name" />
                <opensilex-StringView :label="t('DocumentDetails.docTitle')" :value="document?.title" />
                <opensilex-StringView :label="t('DocumentDetails.date')" :value="document?.date" />
                <opensilex-TextView :label="t('DocumentDetails.description')" :value="document?.description" />

                <!-- Targets -->
                <opensilex-StringView
                    class="overflow-auto"
                    style="height: 100px, display: content"
                    :label="t('DocumentDetails.targets')"
                    :uri="document?.targets"
                >
                    <div v-for="target in targetsTypes" :key="target.uri">
                    <opensilex-UriLink
                        v-if="target.rdf_types?.includes(opensilex.Oeso.EXPERIMENT_TYPE_URI)"
                        :uri="target.uri"
                        :value="target.uri"
                        :to="{ path: '/experiment/details/' + encodeURIComponent(target.uri) }"
                    />
                    <opensilex-UriLink
                        v-else-if="target.rdf_types?.includes(opensilex.Oeso.DEVICE_TYPE_URI)"
                        :uri="target.uri"
                        :value="target.uri"
                        :to="{ path: '/device/details/' + encodeURIComponent(target.uri) }"
                    />
                    <opensilex-UriLink
                        v-else-if="target.rdf_types?.includes(opensilex.Oeso.PROJECT_TYPE_URI)"
                        :uri="target.uri"
                        :value="target.uri"
                        :to="{ path: '/project/details/' + encodeURIComponent(target.uri) }"
                    />
                    <opensilex-UriLink
                        v-else-if="target.rdf_types?.includes(opensilex.Oeso.GERMPLASM_TYPE_URI)"
                        :uri="target.uri"
                        :value="target.uri"
                        :to="{ path: '/germplasm/details/' + encodeURIComponent(target.uri) }"
                    />
                    <opensilex-UriLink
                        v-else-if="target.rdf_types?.includes(opensilex.Oeso.VARIABLESGROUP_TYPE_URI)"
                        :uri="target.uri"
                        :url="getVariableGroupPageUrl(target.uri)"
                    />
                    <opensilex-UriLink
                        v-else
                        :uri="target.uri"
                        :value="target.uri"
                    />
                    </div>
                </opensilex-StringView>

                <!-- Authors -->
                <opensilex-StringView :label="t('DocumentDetails.authors')" :value="document?.authors">
                    <span v-if="document?.authors?.length">
                    <span v-for="a in document.authors" :key="a">{{ a }}<br /></span>
                    </span>
                </opensilex-StringView>
                <!-- Language -->
                <opensilex-StringView :label="t('DocumentDetails.language')" :value="document?.language" />
                <!-- Format -->
                <opensilex-StringView :label="t('DocumentDetails.format')" :value="document?.format" />
                <!-- Keyword -->
                <opensilex-StringView :label="t('DocumentDetails.keywords')" :value="document?.keywords">
                    <span v-if="document?.keywords?.length">
                    <span v-for="k in document.keywords" :key="k">{{ k }} - </span>
                    </span>
                </opensilex-StringView>

                <opensilex-UriView
                    v-if="document?.source"
                    title="DocumentDetails.source"
                    :url="document?.source"
                    :value="document?.source"
                />

                <opensilex-MetadataView
                    v-if="document?.publisher?.uri"
                    :publisher="document.publisher"
                    :publicationDate="document.publication_date"
                    :lastUpdatedDate="document.last_updated_date"
                />
              </div>
            </template>
          </opensilex-Card>
        </div>

        <!-- Col droite : fichier -->
        <div class="right-col">
          <opensilex-Card
            v-if="hasFile"
            :label="t('DocumentDetails.file')"
            icon="bi-download"
          >
            <template #body>
              <div class="button-zone">
                <button class="btn btn-secondary" @click="previewFile(document.uri, document.title, document.format)">
                  {{ t('DocumentDetails.preview') }}
                </button>
                &nbsp;
                <button class="btn btn-secondary" @click="loadFile(document.uri, document.title, document.format)">
                  {{ t('DocumentDetails.download') }}
                </button>
              </div>
              <div id="preview" ref="previewEl"></div>
            </template>
          </opensilex-Card>
        </div>
      </div>
    </opensilex-PageContent>

    <!-- Formulaire d’édition -->
    <opensilex-ModalForm
      ref="documentFormRef"
      component="opensilex-DocumentForm"
      :editTitle="t('DocumentDetails.title')"
      :createTitle="t('DocumentDetails.title')"
      icon="bi#bi-file-text"
      @onUpdate="refresh"
    />
  </div>
</template>



<script setup lang="ts">
import { ref, computed, inject, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router'
import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin'
import type { DocumentsService } from 'opensilex-core/api/documents.service'
import HttpResponse, { OpenSilexResponse } from 'opensilex-security/HttpResponse'

type DocumentGetDTO = {
  uri?: string
  identifier?: string
  rdf_type?: string
  rdf_type_name?: string
  title?: string
  date?: string
  description?: string | null
  targets?: string[] | null
  authors?: string[] | null
  language?: string | null
  format?: string | null
  deprecated?: boolean | null
  keywords?: string[] | null
  source?: string | null
  publisher?: any
  publication_date?: string | null
  last_updated_date?: string | null
}

const { t } = useI18n()
const store = useStore()
const route = useRoute()
const router = useRouter()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

const documentFormRef = ref<any | null>(null)
const previewEl = ref<HTMLElement | null>(null)

let docService: DocumentsService
const uri = ref<string>('')

const canDisplayDocument = ref(true)
const document = ref<DocumentGetDTO>({})
const targetsTypes = ref<any[]>([])

const hasFile = computed(() => !document.value?.source)

function refresh() {
  if (uri.value) loadDocument(uri.value)
}

function getVariableGroupPageUrl(u: string): string {
  const short = (opensilex as any).getShortUri?.(u) ?? u
  return (opensilex as any).getURL?.('variables/?elementType=VariableGroup&selected=' + encodeURIComponent(short))
}

async function loadDocument(u: string) {
  try {
    const http = await docService.getDocumentMetadata(u) as HttpResponse<OpenSilexResponse<DocumentGetDTO>>
    document.value = http.response.result
    if (Array.isArray(document.value.targets) && document.value.targets.length > 0) {
      loadTargetsTypes()
    }
    canDisplayDocument.value = true
  } catch (e) {
    canDisplayDocument.value = false
    opensilex.errorHandler(e)
  }
}

function loadFile(u: string, title?: string | null, format?: string | null) {
  const path = '/core/documents/' + encodeURIComponent(u)
  opensilex.downloadFilefromService(path, title ?? '', format ?? '')
}

function previewFile(u: string, title?: string | null, format?: string | null) {
  const path = '/core/documents/' + encodeURIComponent(u)
  opensilex.previewFilefromGetService(path, title ?? '', format ?? '')
}

function onUpdateClick() {
  const doc = document.value
  const form = {
    description: {
      uri: doc.uri,
      identifier: doc.identifier,
      rdf_type: doc.rdf_type,
      title: doc.title,
      date: doc.date,
      description: doc.description ?? undefined,
      targets: doc.targets ?? undefined,
      authors: doc.authors ?? undefined,
      language: doc.language ?? undefined,
      format: doc.format ?? undefined,
      deprecated: doc.deprecated ?? undefined,
      keywords: doc.keywords ?? undefined
    }
  }
  documentFormRef.value?.showEditForm?.(form)
}

async function deleteDocument(u: string) {
  try {
    await docService.deleteDocument(u)
    router.back()
  } catch (e) {
    opensilex.errorHandler(e)
  }
}

async function loadTargetsTypes() {
  try {
    const ontologyService = opensilex.getService('opensilex.OntologyService')
    const Oeso = opensilex.Oeso
    const types = [Oeso.GERMPLASM_TYPE_URI, Oeso.DEVICE_TYPE_URI, Oeso.PROJECT_TYPE_URI, Oeso.EXPERIMENT_TYPE_URI, Oeso.VARIABLESGROUP_TYPE_URI]
    const body = { uris: document.value.targets }
    const http = await ontologyService.checkURIsTypes(types, body) as HttpResponse<OpenSilexResponse<any>>
    targetsTypes.value = http.response.result
  } catch (e) {
    opensilex.errorHandler(e)
  }
}

/* lifecycle */
onMounted(() => {
  docService = opensilex.getService('opensilex.DocumentsService')
  uri.value = decodeURIComponent(String(route.params.uri || ''))
  if (uri.value) loadDocument(uri.value)
})
</script>

<style scoped>
.details-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.left-col, .right-col { min-width: 0; }

.button-zone { margin-bottom: 8px; }

.badge-warning { 
    background: #f0ad4e; 
    color: #000; 
}

.details-fields {
  display: flex;
  flex-direction: column;
  gap: 8px; /* espace régulier entre lignes */
}
.details-fields > * {
  width: 100%;
  min-width: 0;
  clear: both;/* Si composants internes utilisent des floats, on les neutralise */
}
</style>

<i18n>
en:
  DocumentDetails:
    title: Document
    description: Description
    uri: URI
    docTitle: Title
    type: Type
    date: Date
    authors: Authors
    targets: Target(s)
    language: Language
    format: Format
    keywords: Keywords
    deprecated: Deprecated
    identifier: Identifier
    backToList: Go back to Document list
    file: File
    no-such-file: No file provided
    download: Download File
    preview: Preview File
    update: Update Document
    delete: Delete Document
    source: Source

fr:
  DocumentDetails:
    title: Document
    description: Description
    uri: URI
    docTitle: Titre
    type: Type
    date: Date
    authors: Auteurs
    targets: Cible(s)
    language: Langue
    format: Format
    keywords: Mots-clés
    deprecated: Obsolète
    identifier: Identifiant
    backToList: Retourner à la liste des documents
    file: Fichier
    no-such-file: Aucun fichier associé
    download: Télécharger le fichier
    preview: Aperçu du fichier
    update: Modifier Document
    delete: Supprimer Document
    source: Source
</i18n>
