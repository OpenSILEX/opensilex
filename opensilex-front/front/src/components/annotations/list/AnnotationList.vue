<template>
  <div>
    <!-- Actions -->
    <div class="pageActionsBtns">
      <opensilex-CreateButton
        v-if="user.hasCredential(modificationCredentialId)"
        :label="t('Annotation.add')"
        @click="annotationModalForm?.showCreateForm?.([target])"
        class="createButton greenThemeColor"
      />
    </div>
    <n-space class="listActionButtons">
        <div class="displayAndListSelectionCount">
            <div v-if="showCount">
                <div v-if="hasResults">
                    <strong>
                        <span class="ml-1">
                            {{ t('component.common.list.pagination.nbEntries', {
                            limit: start,
                            offset: end,
                            totalRow: n(total)
                            }) }}
                        </span>
                    </strong>
                </div>
                <div v-else>
                    <strong>
                        <span class="ml-1">{{ t('component.common.list.pagination.noEntries') }}</span>
                    </strong>
                </div>
            </div>
        </div>
    </n-space>

    <!-- Table -->
    <opensilex-PageContent v-if="renderComponent">
        <template #default>
            <opensilex-TableAsyncView
                ref="tableRef"
                :searchMethod="search"
                :fields="fields"
                :isSelectable="isSelectable"
            >
                <!-- colonnes -->
                <template #cell(published)="{ data }">
                    <opensilex-TextView :value="formatDate(data.item.published)" label="" />
                </template>

                <template #cell(publisher)="{ data }">
                    <opensilex-PersonContact
                        v-if="data.item.publisher && accountsByUri.get(data.item.publisher)"
                        :personContact="accountsByUri.get(data.item.publisher)"
                        :customDisplayableName="getAccountNames(data.item.publisher)"
                    />
                </template>

                <template #cell-description="{ data }">
                    <opensilex-TextView v-if="data.item.description" :value="data.item.description" />
                </template>

                <template v-if="displayTargetColumn" #cell-targets="{ data }">
                    <opensilex-TextView :value="data.item.targets?.[0]" />
                </template>

                <template v-if="enableActions" #cell(actions)="{ data }">
                    <div class="action-group">
                        <opensilex-DetailButton
                            v-if="!modificationCredentialId || user.hasCredential(modificationCredentialId)"
                            @click="showDetails(data.item)"
                            label="Annotation.details"
                            :title="t('Annotation.details')"
                            :small="true"
                        />
                        <opensilex-EditButton
                            v-if="!modificationCredentialId || user.hasCredential(modificationCredentialId)"
                            @click="editAnnotation(data.item)"
                            label="Annotation.edit"
                            :small="true"
                        />
                        <opensilex-DeleteButton
                            v-if="!deleteCredentialId || user.hasCredential(deleteCredentialId)"
                            @click="deleteAnnotation(data.item.uri)"
                            label="Annotation.delete"
                            :small="true"
                        />
                    </div>
                </template>
            </opensilex-TableAsyncView>
        </template>
    </opensilex-PageContent>

    <!-- Détails en modal -->
    <opensilex-AnnotationDetails
      v-if="selectedAnnotation"
      :value="isModalVisible"
      :annotationDetails="selectedAnnotation"
      @close="isModalVisible = false"
    />

    <!-- Formulaire création/édition -->
    <opensilex-AnnotationModalForm
      ref="annotationModalForm"
      @onCreate="onAnnotationCreated"
      @onUpdate="onAnnotationUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';

import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin';
import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse';
import {AnnotationGetDTO} from 'opensilex-core/index';
import type { AnnotationsService } from 'opensilex-core/api/annotations.service';
import type { SecurityService } from 'opensilex-security/api/security.service';
import type { UserGetDTO } from 'opensilex-security/index';
import type { AccountGetDTO } from 'opensilex-security/model/accountGetDTO';

// Props
const props = withDefaults(defineProps<{
  isSelectable?: boolean;
  modificationCredentialId?: string;
  deleteCredentialId?: string;
  enableActions?: boolean;
  displayTargetColumn?: boolean;
  columnsToDisplay?: Set<string>;
  filter?: {
    bodyValue?: string;
    motivation?: string;
    published?: string;
    publisher?: string;
  };
  target?: string;
  showCount?: boolean;
}>(), {
  isSelectable: false,
  enableActions: true,
  displayTargetColumn: true,
  columnsToDisplay: () => new Set(['published', 'description', 'publisher']),
  filter: () => ({
    bodyValue: undefined,
    motivation: undefined,
    published: undefined,
    publisher: undefined
  }),
  showCount: true
});

// Emits
const emit = defineEmits<{
  (e: 'onDelete', uri: string): void
  (e: 'changed', payload?: { reason: 'create' | 'update' | 'delete' }): void
}>()

// Services / env
const store = useStore();
const { t, n } = useI18n();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const annotationService = opensilex.getService<AnnotationsService>('opensilex.AnnotationsService');
const securityService = opensilex.getService<SecurityService>('opensilex.SecurityService');

/** Bandeau "Affiche X à Y des Z éléments" */
const serverTotal = ref(0);
const pagination = ref({ page: 1, pageSize: 10 });

const paginationInfo = computed(() => {
  const total = serverTotal.value;
  const page = pagination.value.page;
  const pageSize = pagination.value.pageSize;
  const start = total === 0 ? 0 : (page - 1) * pageSize + 1;
  const end = Math.min(page * pageSize, total);
  return { start, end, total, hasResults: total > 0 };
});
const start = computed(() => paginationInfo.value.start);
const end = computed(() => paginationInfo.value.end);
const total = computed(() => paginationInfo.value.total);
const hasResults = computed(() => paginationInfo.value.hasResults);


// Store bindings
const user = computed(() => store.state.user);

// Refs
const tableRef = ref<any>(null);
const annotationModalForm = ref<any>(null);

// État local
const accountsByUri = ref<Map<string, AccountGetDTO>>(new Map());
const renderComponent = ref(true);
const selectedAnnotation = ref<AnnotationGetDTO | null>(null);
const isModalVisible = ref(false);

// Formatters
function formatDate(dateStr: string): string {
  return opensilex.$dateTimeFormatter.formatLocalFixedDateTime(dateStr);
}

// Champs table
const fields = computed(() => {
  const f: Array<{ key: string; label: string; sortable: boolean }> = [];
  if (props.columnsToDisplay.has('published'))   f.push({ key: 'published',   label: t('Annotation.published'),   sortable: true  });
  if (props.columnsToDisplay.has('publisher'))   f.push({ key: 'publisher',   label: t('Annotation.publisher'),   sortable: false });
  if (props.columnsToDisplay.has('description')) f.push({ key: 'description', label: t('Annotation.description'), sortable: true  });
  if (props.columnsToDisplay.has('targets'))     f.push({ key: 'targets',     label: t('Annotation.targets'),     sortable: true  });
  if (props.enableActions)                       f.push({ key: 'actions',     label: t('component.common.actions'), sortable: false });
  return f;
});

// Recherche (pour TableAsyncView)
function search(options: { orderBy: string[]; currentPage: number; pageSize: number; }) {

  // mémorise la pagination courante
  pagination.value.page = (options.currentPage ?? 0) + 1;
  pagination.value.pageSize = options.pageSize ?? 10;

  return new Promise((resolve, reject) => {
    annotationService.searchAnnotations(
      props.filter.bodyValue,
      props.target,
      props.filter.motivation,
      props.filter.publisher,
      options.orderBy,
      options.currentPage,
      options.pageSize
    )
    .then((http: HttpResponse<OpenSilexResponse<Array<AnnotationGetDTO>>>) => {

        // total côté serveur
        serverTotal.value = http?.response?.metadata?.pagination?.totalCount ?? (http?.response?.result?.length ?? 0);

      const results = http.response.result ?? [];
      if (results.length === 0) {
        resolve(http);
      } else {
        buildUsersIndexPromise(results, reject).then(() => resolve(http));
      }
    })
    .catch(reject);
  });
}

function onAnnotationCreated() {
  console.log("annoList - anno created")
  refresh()
  emit('changed', { reason: 'create' })
}

function onAnnotationUpdated() {
  refresh()
  emit('changed', { reason: 'update' })
}

// Indexer les comptes pour affichage publisher
function buildUsersIndexPromise(
  annotations: Array<AnnotationGetDTO>,
  reject: (e:any)=>void
): Promise<void | HttpResponse<OpenSilexResponse<Array<UserGetDTO>>>> {
  accountsByUri.value = new Map();
  const uniqueUsers = new Set<string>();
  annotations.forEach(a => { if (a.publisher) uniqueUsers.add(a.publisher); });

  if (uniqueUsers.size === 0) return Promise.resolve();

  return securityService.getAccountsByURI(Array.from(uniqueUsers))
    .then(http => {
      http.response.result.forEach((acc: AccountGetDTO) => {
        accountsByUri.value.set(acc.uri, acc);
      });
    })
    .catch(reject);
}

function getAccountNames(accountUri: string): string | undefined {
  if (!accountUri) return undefined;
  const acc = accountsByUri.value.get(accountUri);
  if (!acc) return undefined;
  return acc.linked_person ? `${acc.person_first_name} ${acc.person_last_name}` : acc.email;
}

function refresh() {
    console.log("annotationList refresh")
  tableRef.value?.refresh?.();
}

// Edit / Details / Delete
// function editAnnotation(annotation: AnnotationGetDTO) {
//   const copy = JSON.parse(JSON.stringify(annotation));
//   annotationModalForm.value?.showEditForm?.(copy);
// }
async function editAnnotation(annotation: AnnotationGetDTO) {
  try {
    const uri = annotation?.uri
    if (!uri) return

    const http = await annotationService.getAnnotation(uri)
    const selectedAnnotation = http.response.result

    annotationModalForm.value?.showEditForm?.(JSON.parse(JSON.stringify(selectedAnnotation)))
  } catch (e) {
    opensilex.errorHandler(e)
  }
}

function showDetails(annotation: AnnotationGetDTO) {
  selectedAnnotation.value = {
    uri: annotation.uri,
    motivation: annotation.motivation ? { name: annotation.motivation.name } : undefined,
    published: annotation.published,
    publisher: getAccountNames(annotation.publisher) as any,
    description: annotation.description
  } as AnnotationGetDTO;
  isModalVisible.value = true;
}

function deleteAnnotation(uri: string) {
  annotationService.deleteAnnotation(uri)
    .then(() => nextTick(() => refresh()))
    .then(() => {
      const message = `${t('Annotation.name')} ${uri} ${t('component.common.success.delete-success-message')}`;
      opensilex.showSuccessToast(message);
      emit('onDelete', uri);
      emit('changed', { reason: 'delete' })
    })
    .catch(opensilex.errorHandler);
}

// Re-montage quand la cible change (pour forcer TableAsyncView à relire la prop)
watch(() => props.target, () => {
  renderComponent.value = false;
  nextTick(() => { renderComponent.value = true; });
});

// Refresh auto quand la langue change
const unwatchLang = store.watch(
  () => store.getters.language,
  () => refresh()
);
onBeforeUnmount(() => { unwatchLang && unwatchLang(); });

defineExpose({ refresh });
</script>

<style scoped lang="scss">
.pageActionsBtns { margin-left: 10px; margin-bottom: 10px; }
.action-group { display: inline-flex; gap: .35rem; }

.listActionButtons {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  width: 100%;
  margin: 8px 0 12px;       /* espace sous la barre -> évite le chevauchement */
}
</style>

<i18n>
en:
  Annotation:
    name: The annotation
    add: Add annotation
    edit: Edit annotation
    delete: Delete annotation
    details: Details annotation
    motivation: Motivation
    motivation-placeholder: Select a motivation
    motivation-help: Intent or motivation for the creation of the Annotation.
    description: Description
    publisher: Publisher
    published: Date
    target: Target
    list-title: Annotations
    already-exist: the annotation already exist
    image: Image annotations
fr:
  Annotation:
    name: L'annotation
    add: Ajouter une annotation
    edit: Éditer l'annotation
    delete: Supprimer l'annotation
    details: Détailler l'annotation
    motivation: Motivation
    motivation-placeholder: Sélectionnez une motivation
    motivation-help: "Intention ou motivation guidant la création de l'annotation"
    description: Description
    published: Date
    publisher: Publieur
    target: Cible
    list-title: Annotations
    already-exist: l'annotation existe déjà
    image: Annotations d'image
</i18n>
