<template>
  <div>
    <StringFilter
        v-model:filter="filter"
        @update="updateFilter()"
        placeholder="component.group.filter-placeholder"
        :debounce="300"
        :lazy="false"
    ></StringFilter>

    <TableAsyncView ref="tableRef" :searchMethod="searchGroups" :fields="fields">
      <template v-slot:cell(name)="{data}">
        <UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :noExternalLink="true"
            :isClickable="false"
        ></UriLink>
      </template>

      <template v-slot:cell(user_profiles)="{data}">
        <div>
          {{ $t("component.account.label", data.item.user_profiles.length, {count: data.item.user_profiles.length}) }}
        </div>
      </template>

      <template v-slot:row-details="{data}">
        <strong class="capitalize-first-letter">{{ $t("component.account.accounts") }}:</strong>
        <ul>
          <li
              v-for="userProfile in data.item.user_profiles"
              v-bind:key="userProfile.uri"
          >{{ userProfile.user_name }} ({{ userProfile.profile_name }})
          </li>
        </ul>
      </template>

      <template v-slot:cell(actions)="{data}">
        <n-button-group size="small" class="btn-group btn-group-sm">
          <DetailButton
              @click="onShowDetailClick(data)"
              label="component.group.details"
              :detailVisible="!!data.item._showDetails"
              :small="true"
          ></DetailButton>
          <EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_MODIFICATION_ID)"
              @click="$emit('onEdit', data.item)"
              label="component.group.update"
              :small="true"
          ></EditButton>
          <DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_GROUP_DELETE_ID)"
              @click="deleteGroup(data.item.uri)"
              label="component.group.delete"
              :small="true"
          ></DeleteButton>
        </n-button-group>
      </template>
    </TableAsyncView>
  </div>
</template>

<script setup lang="ts">

import StringFilter from "@/components/common/filters/StringFilter.vue";
import TableAsyncView, {RowWithData} from "@/components/common/views/TableAsyncView.vue";
import type {GroupDTO} from "opensilex-security/index";
import UriLink from "@/components/common/views/UriLink.vue";
import DetailButton from "@/components/common/buttons/DetailButton.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import {NButtonGroup} from "naive-ui";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {computed, inject, ref, useTemplateRef} from "vue";
import {useStore} from "vuex";
import {useRoute} from "vue-router";
import {SecurityService} from "opensilex-security/lib";
import {useI18n} from "vue-i18n";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const store = useStore()
const route = useRoute()
const {t} = useI18n()
const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef')

const user = computed(() => {
  return store.state.user;
})

const credentials = computed(() => {
  return store.state.credentials;
})

const filter = ref<any>('')

function created() {
  let query: any = route.query;
  if (query.filter) {
    filter.value = decodeURIComponent(query.filter);
  }
}

function updateFilter() {
  opensilex.updateURLParameter("filter", filter.value, "");
  refresh();
}

const fields = ref([
  {
    key: "name",
    label: "component.common.name",
    sortable: true
  },
  {
    label: "component.common.description",
    key: "description",
    sortable: true
  },
  {
    label: "component.account.accounts",
    key: "user_profiles"
  },
  {
    label: "component.common.actions",
    key: "actions",
    class: "table-actions"
  }
]);

const emit = defineEmits(["onEdit", "onDelete"]);

function refresh() {
  opensilex.updateURLParameters(filter.value);
  tableRef.value?.setPage(1);
  tableRef.value?.refresh();
}

function onShowDetailClick(data: RowWithData<GroupDTO>) {
  data.item._showDetails = !data.item._showDetails;
}

function updateSelectedGroup() {
  opensilex.updateURLParameters(filter.value)
  refresh()
}

function searchGroups(options) {
  return opensilex
      .getService<SecurityService>("opensilex.SecurityService")
      .searchGroups(
          filter.value,
          options.orderBy,
          options.currentPage,
          options.pageSize
      );
}

function deleteGroup(uri: string) {
  opensilex
      .getService<SecurityService>("opensilex.SecurityService")
      .deleteGroup(uri)
      .then(() => {
        refresh();
        emit("onDelete", uri);
      })
      .catch(opensilex.errorHandler);
}

defineExpose({
  refresh,
  updateSelectedGroup,
  created
    })

</script>

<style scoped lang="scss">
</style>
