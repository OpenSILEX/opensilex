<template>
  <div>
    <StringFilter
        v-model:filter="filter"
        @update="updateFilter()"
        placeholder="component.profile.filter-placeholder"
        :debounce="300"
        :lazy="false"
    ></StringFilter>

    <TableAsyncView
        ref="tableRef"
        :searchMethod="searchProfiles"
        :fields="fields"
    >
      <template #cell(credentials)="{ data }">
        <div>{{ t("component.profile.credential", data.item.credentials.length) }}</div>
      </template>

      <template #row-details="{ data }">
        <strong class="capitalize-first-letter">{{ t("component.profile.credentials") }}:</strong>
        <div class="row">
          <div
              class="col-md-4"
              v-for="credentialGroup in filterCredentialGroups(data.item.credentials)"
              :key="credentialGroup.group_id"
          >
            <div class="card">
              <div class="card-body">
                <strong>{{ t(credentialGroup.group_key_name) }}</strong>
                <ul>
                  <li
                      v-for="credential in credentialGroup.credentials"
                      v-bind:key="credential.id"
                  >{{ credential.name }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </template>

      <template #cell(name)="{data}">
        <UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :noExternalLink="true"
            :isClickable="isClickable"
            @click="data.toggleDetails()"
        ></UriLink>
      </template>

      <template #cell(actions)="{data}">
        <n-button-group size="small" class="btn-group btn-group-sm">
          <DetailButton
              @click="showDetails(data)"
              label="component.profile.details"
              :detailVisible="data.item._showDetails"
              :small="true"
          ></DetailButton>
          <EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_PROFILE_MODIFICATION_ID)"
              @click="emit('onEdit', data.item)"
              label="component.profile.update"
              :small="true"
          ></EditButton>
          <DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_PROFILE_DELETE_ID)"
              @click="deleteProfile(data.item.uri)"
              label="component.profile.delete"
              :small="true"
          ></DeleteButton>
        </n-button-group>
      </template>
    </TableAsyncView>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, onMounted, ref, useTemplateRef} from "vue";
import {useStore} from "vuex";
import {useRoute} from 'vue-router';
import {SecurityService} from "opensilex-security/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import TableAsyncView from "@/components/common/views/TableAsyncView.vue";
import UriLink from "@/components/common/views/UriLink.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import DetailButton from "@/components/common/buttons/DetailButton.vue";
import {ProfileGetDTO} from "opensilex-security/model/profileGetDTO";
import {CredentialsGroupDTO} from "opensilex-security/model/credentialsGroupDTO";
import {TableField} from "@/components/common/views/TableField";
import {NButtonGroup} from "naive-ui";
import {useI18n} from "vue-i18n";

//#region Public
const props = defineProps<{
  isClickable?: boolean
}>()

const emit = defineEmits<{
  (e: "onEdit", payload: ProfileGetDTO): void
  (e: "onDelete", uri: string): void
}>()
//#endregion

const opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const service = opensilex.getService<SecurityService>("opensilex-core.SecurityService");
const store = useStore();
const route = useRoute();
const {t} = useI18n();

//#region Data and computed
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

const filter = ref("");
const credentialsGroups = ref<Array<CredentialsGroupDTO>>([]);

const fields: Array<TableField> = [
  {key: "name", label: "component.common.name", sortable: true},
  {key: "credentials", label: "component.profile.credentials", resizable: true},
  {key: "actions", label: "component.common.actions", resizable: false, naiveProps: {width: 100}}
];
//#endregion

const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef');

onMounted(async () => {
  const query = route.query;
  if (typeof query.filter === "string") {
    filter.value = decodeURIComponent(query.filter);
  }
  credentialsGroups.value = await opensilex.getCredentials();
});

function filterCredentialGroups(credentials: Array<string>): Array<CredentialsGroupDTO> {
  let filteredGroups: Array<CredentialsGroupDTO> = [];
  console.log("Initial groups", credentialsGroups.value);
  for (const group of credentialsGroups.value) {
    let transformedGroup = {
      group_id: group.group_id,
      group_key_name: group.group_key_name,
      credentials: group.credentials
          .filter(credential => credentials.indexOf(credential.id) >= 0)
          .map(credential => ({ id: credential.id, name: t(credential.name) }))
    };

    if (transformedGroup.credentials.length > 0) {
      filteredGroups.push(transformedGroup);
    }
  }
  console.log("Filtered groups", filteredGroups);
  return filteredGroups;
}

function updateFilter() {
  opensilex.updateURLParameter("filter", filter.value, "");
  refresh();
}

function refresh() {
  tableRef.value.refresh();
}

function searchProfiles(options) {
  return service.searchProfiles(
      filter.value,
      options.orderBy,
      options.currentPage,
      options.pageSize
  );
}

function deleteProfile(uri: string) {
  service.deleteProfile(uri)
      .then(() => {
        refresh();
        emit("onDelete", uri);
      })
      .catch(opensilex.errorHandler);
}

function showDetails(data) {
  data.item._showDetails = !data.item._showDetails;
}

defineExpose({
  refresh,
})
</script>

<style scoped lang="scss">
</style>
