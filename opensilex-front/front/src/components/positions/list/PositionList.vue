<template>
  <div>
    <div class="card">
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
                <span class="ml-1">
                  {{ t('component.common.list.pagination.noEntries') }}
                </span>
              </strong>
            </div>
          </div>
        </div>
      </n-space>

      <opensilex-PageContent v-if="renderComponent">
        <template #default>
          <div class="card-body">
            <opensilex-TableAsyncView
              ref="tableRef"
              :searchMethod="search"
              :fields="fields"
              defaultSortBy="end"
              :defaultSortDesc="true"
              :isSelectable="isSelectable"
            >
              <template #cell(end)="{ data }">
                <opensilex-DateView :value="data.item.end" />
              </template>

              <template v-if="enableActions" #cell(actions)="slotProps">
                <div class="action-group">
                  <opensilex-DetailButton
                  :detailVisible="getDetailsVisible(slotProps)"
                  :small="true"
                  :label="t('Position.details')"
                  @click="showDetails(slotProps)"
                  />
                  <opensilex-EditButton
                  v-if="!modificationCredentialId || user.hasCredential(modificationCredentialId)"
                  @click="editEvent(slotProps.data.item)"
                  label='component.common.list.buttons.update'
                  :small="true"
                  />
                  <opensilex-DeleteButton
                  v-if="!deleteCredentialId || user.hasCredential(deleteCredentialId)"
                  @click="deleteEvent(slotProps.data.item.uri)"
                  label="component.common.list.buttons.delete"
                  :small="true"
                  />
                </div>
              </template>

              <template #row-details="{ data }">
                <ul>
                  <li v-if="data.item.location?.to">
                    <opensilex-UriLink
                      :uri="data.item.location.to"
                      :value="data.item.location.to"
                      :to="{ path: '/facility/details/' + encodeURIComponent(data.item.location.to) }"
                      target="_blank"
                    />
                  </li>

                  <li v-if="data.item.location && (data.item.location.x || data.item.location.y || data.item.location.z)">
                    {{ customCoordinatesText(data.item.location) }}
                  </li>

                  <li v-if="data.item.location?.text">
                    {{ data.item.location.text }}
                  </li>

                  <li v-if="data.item.location?.geojson">
                    <opensilex-GeometryCopy
                      label=""
                      :value="data.item.location.geojson"
                    />
                  </li>
                </ul>
              </template>
            </opensilex-TableAsyncView>
          </div>
        </template>
      </opensilex-PageContent>

      <opensilex-EventModalView
        modalSize="lg"
        ref="eventModalView"
      />

      <opensilex-EventModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
        ref="modalForm"
        :target="target"
        defaultEventType="oeev:Move"
        @onCreate="refresh"
        @onUpdate="refresh"
      />

      <opensilex-EventCsvForm
        v-if="renderCsvForm"
        ref="csvForm"
        @csvImported="onImport"
        :targets="[target]"
        :isMove="true"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, ref, watch } from 'vue';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';

import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin';
import type { PositionsService } from 'opensilex-core/api/positions.service';
import type { EventsService } from 'opensilex-core/api/events.service';
import type { LocationObservationDTO } from 'opensilex-core/model/locationObservationDTO';
import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse';

const props = withDefaults(defineProps<{
  isSelectable?: boolean;
  modificationCredentialId?: string;
  deleteCredentialId?: string;
  enableActions?: boolean;
  columnsToDisplay?: Set<string>;
  displayFilters?: boolean;
  displayTitle?: boolean;
  target?: string;
  showCount?: boolean;
}>(), {
  isSelectable: false,
  enableActions: true,
  columnsToDisplay: () => new Set(['end']),
  displayFilters: false,
  displayTitle: false,
  showCount: true
});

const emit = defineEmits<{
  (e: 'onDelete', uri: string): void;
  (e: 'changed', payload?: { reason: 'create' | 'update' | 'delete' }): void;
}>();

const store = useStore();
const { t, n } = useI18n();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const positionService = opensilex.getService<PositionsService>('opensilex.PositionsService');
const eventService = opensilex.getService<EventsService>('opensilex.EventsService');

const tableRef = ref<any>(null);
const eventModalView = ref<any>(null);
const modalForm = ref<any>(null);
const csvForm = ref<any>(null);

const renderComponent = ref(true);
const renderCsvForm = ref(false);
const minPageSize = 5;

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

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

const fields = computed(() => {
  const tableFields: Array<{ key: string; label: string; sortable: boolean }> = [];

  if (props.columnsToDisplay.has('end')) {
    tableFields.push({
      key: 'end',
      label: t('Position.end'),
      sortable: true
    });
  }

  if (props.enableActions) {
    tableFields.push({
      key: 'actions',
      label: t('component.common.actions'),
      sortable: false
    });
  }

  return tableFields;
});

function refresh() {
  tableRef.value?.refresh?.();
}

function search(options: {
  orderBy: string[];
  currentPage: number;
  pageSize: number;
}) {
  pagination.value.page = (options.currentPage ?? 0) + 1;
  pagination.value.pageSize = options.pageSize ?? 10;

  return positionService.searchPositionHistory(
    props.target,
    undefined,
    undefined,
    options.orderBy,
    options.currentPage,
    options.pageSize < minPageSize ? minPageSize : options.pageSize
  ).then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
    serverTotal.value =
      http?.response?.metadata?.pagination?.totalCount ??
      http?.response?.result?.length ??
      0;

    return http;
  });
}

function showForm() {
  nextTick(() => {
    modalForm.value?.showCreateForm?.();
  });
}

function showCsvForm() {
  renderCsvForm.value = true;
  nextTick(() => {
    csvForm.value?.show?.();
  });
}

function successMessage() {
  return t('Position.data-imported');
}

function deleteEvent(uri: string) {
  eventService.deleteEvent(uri)
    .then(() => {
      refresh();

      const message = `${t('Position.name')} ${uri} ${t('component.common.success.delete-success-message')}`;
      opensilex.showSuccessToast(message);

      emit('onDelete', uri);
      emit('changed', { reason: 'delete' });
    })
    .catch(opensilex.errorHandler);
}

function getDetailsVisible(slotProps: any): boolean {
  return !!slotProps?.data?.item?._showDetails;
}

function showDetails(slotProps: any) {
  if (!slotProps?.data?.item) return;
  slotProps.data.item._showDetails = !slotProps.data.item._showDetails;
}

function editEvent(item: any) {
  if (!modalForm.value) {
    console.warn('modalForm ref indisponible');
    return;
  }

  if (typeof modalForm.value.showEditForm !== 'function') {
    console.warn('showEditForm non disponible sur modalForm', modalForm.value);
    return;
  }

  modalForm.value.showEditForm(item.uri, opensilex.Oeev.MOVE_TYPE_URI, item);
}

function customCoordinatesText(location?: LocationObservationDTO): string | undefined {
  if (!location) {
    return undefined;
  }

  let customCoordinates = '';

  if (location.x) {
    customCoordinates += 'X : ' + location.x;
  }

  if (location.y) {
    if (customCoordinates.length > 0) {
      customCoordinates += ', ';
    }
    customCoordinates += 'Y : ' + location.y;
  }

  if (location.z) {
    if (customCoordinates.length > 0) {
      customCoordinates += ', ';
    }
    customCoordinates += 'Z : ' + location.z;
  }

  if (customCoordinates.length === 0) {
    return undefined;
  }

  return '(' + customCoordinates + ')';
}

function onImport() {
  refresh();
  emit('changed', { reason: 'create' });
}

watch(() => props.target, () => {
  renderComponent.value = false;
  nextTick(() => {
    renderComponent.value = true;
  });
});

defineExpose({
  refresh,
  showForm,
  showCsvForm,
  successMessage
});
</script>

<style scoped lang="scss">
.createButton {
  margin-left: 10px;
  margin-top: 10px;
}

.pageActions {
  margin-bottom: 10px;
  margin-left: -10px;
}

.action-group {
  display: inline-flex;
  gap: 0.35rem;
}

.listActionButtons {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  width: 100%;
  margin: 8px 0 12px;
}
</style>

<i18n>
en:
  Position:
    name: The event
    list-title: Positions
    end: Arrival date
    data-imported: Observations have been imported successfully
    details: Show or hide position details
fr:
  Position:
    name: "L'événement"
    list-title: Positions
    end: "Date d'arrivée"
    data-imported: observations ont été importées avec succès
    details: Afficher ou masquer les détails de la position
</i18n>