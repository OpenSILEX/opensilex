<template>
  <div>
    <div class="siteCountRight">
      <div v-if="paginationInfo.hasResults">
        <strong>
          {{ t('component.common.list.pagination.nbEntries', {
            limit: paginationInfo.start,
            offset: paginationInfo.end,
            totalRow: n(paginationInfo.total)
          }) }}
        </strong>
      </div>
      <div v-else>
        <strong>{{ t('component.common.list.pagination.noEntries') }}</strong>
      </div>
    </div>


    <StringFilter
      :filter="filter"
      @update:filter="(v) => (filter = v)"
      @update="onStringFilterUpdate"
      :placeholder="t('SiteList.filter-placeholder')"
      :lazy="false"
    />

    <TableAsyncView
      ref="tableRef"
      :searchMethod="searchSites"
      :fields="fields"
      defaultSortBy="name"
    >
      <template #cell(name)="{ data }">
        <UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :to="{ path: '/organization/site/details/' + encodeURIComponent(data.item.uri) }"
        />
      </template>

      <template #cell(city)="{ data }">
        {{ data.item.address ? data.item.address.locality : null }}
      </template>

      <template #cell(facilities)="{ data }">
        <FacilitiesModalList
          :facilities="data.item.facilities"
          :currentSite="data.item"
          :hostNameForTitle="data.item.name"
          @onCRUD="refresh"
        />
      </template>

      <template #cell(actions)="{ data }">
        <n-button-group size="small" class="btn-group btn-group-sm">
          <EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_MODIFICATION_ID)"
            @click="emitOnEdit(data.item)"
            label="component.site.update"
            :small="true"
          />
          <DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_ORGANIZATION_DELETE_ID)"
            @click="onDeleteClick(data.item)"
            label="component.site.delete"
            :small="true"
          />
        </n-button-group>
      </template>
    </TableAsyncView>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { useStore } from 'vuex'
import { NButtonGroup } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import { OrganizationsService } from 'opensilex-core/api/organizations.service'
import DTOConverter from '../../../models/DTOConverter'
import StringFilter from "@/components/common/filters/StringFilter.vue";
import TableAsyncView from "@/components/common/views/TableAsyncView.vue";
import UriLink from "@/components/common/views/UriLink.vue";
import FacilitiesModalList from "@/components/facilities/FacilitiesModalList.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";

const emit = defineEmits<{
  (e: 'onEdit', dto: any): void
}>()

const props = withDefaults(
  defineProps<{
    organizationsForFilter?: string[] | null
  }>(),
  { organizationsForFilter: null }
)

const store = useStore()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t, n } = useI18n()
const service = $opensilex.getService<OrganizationsService>('opensilex-core.OrganizationsService')

const tableRef = ref<any>(null)

const paginationInfo = computed(() => {
  return tableRef.value?.getPaginationInfo?.() ?? { start: 0, end: 0, total: 0, hasResults: false }
})

const user = computed(() => store.state.user)
const credentials = computed(() => store.state.credentials)

const filter = ref<string>('')

const fields = computed(() => [
  { key: 'name', label: 'component.common.name', sortable: true },
  { key: 'city', label: t('SiteList.address') },
  { key: 'facilities', label: t('SiteList.facilities') },
  { key: 'actions', label: 'component.common.actions' }
])

onMounted(() => {
  // init filtre depuis URL
  const urlFilter = (store?.state?.route?.query?.filter ?? undefined) as any
  if (urlFilter !== undefined && String(urlFilter).trim() !== '') filter.value = String(urlFilter)
})

function onStringFilterUpdate() {
  $opensilex.updateURLParameter('filter', filter.value, '')
  tableRef.value?.setPage?.(1)
  refresh()
}


async function emitOnEdit(site: any) {
  try {
    const http = await service.getSite(site.uri)
    const editDto = DTOConverter.extractURIFromResourceProperties(http.response.result)
    emit('onEdit', editDto)
  } catch (e) {
    $opensilex.errorHandler(e)
  }
}

async function onDeleteClick(dto: any) {
  try {
    await service.deleteSite(dto.uri)
    refresh()
    $opensilex.showSuccessToast('component.common.delete')
  } catch (e) {
    $opensilex.errorHandler(e)
  }
}

function searchSites(options: any) {
  return service.searchSites(
    filter.value,
    props.organizationsForFilter,
    options.orderBy,
    options.currentPage,
    options.pageSize
  )
}

function refresh() {
  tableRef.value?.refresh?.()
}

defineExpose({
  refresh
})
</script>

<style scoped lang="scss">
.siteCountRight{
  text-align: right;
  white-space: nowrap;
}
</style>

<i18n>
en:
  SiteList:
    address: City
    filter-placeholder: Filter by name
    facilities: Facilities
fr:
  SiteList:
    address: Ville
    filter-placeholder: Filtrer par nom
    facilities: Installations
</i18n>
