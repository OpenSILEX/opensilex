<template>
  <div>
     <StringFilter
      v-model:filter="filter"
      @update="updateFilter()"
      placeholder="component.account.filter-placeholder"
      :debounce="300"
      :lazy="false"
    ></StringFilter>

    <TableAsyncView
      ref="tableRef"
      :searchMethod="searchAccounts"
      :fields="fields"
      defaultSortBy="email"
    >
      <template #cell(uri)="{data}">
        <UriLink
          :uri="data.item.uri"
          :value="data.item.uri"
          :noExternalLink="true"
          :isClickable="false"
        ></UriLink>
      </template>

      <template #cell(last_name)="{data}">
        <PersonContact
          v-if="personByAccountUri[data.item.uri]"
          :personContact="personByAccountUri[data.item.uri]"
        />
        <div v-else/>
      </template>

      <template #cell(email)="{data}">
        <a :href="'mailto:' + data.item.email">{{ data.item.email }}</a>
      </template>

      <template #cell(admin)="{data}">
        <span class="capitalize-first-letter" v-if="data.item.admin">{{ t("component.common.yes") }}</span>
        <span class="capitalize-first-letter" v-if="!data.item.admin">{{ t("component.common.no") }}</span>
      </template>

      <template #row-details="{data}">
        <strong class="capitalize-first-letter">{{ t("component.account.user-groups") }}:</strong>
        <ul>
          <li
            v-for="groupDetail in groupDetailsByAccountUri[data.item.uri]"
            :key="groupDetail.uri"
          >{{ groupDetail.name }}</li>
        </ul>
      </template>

      <template #cell(actions)="{data}">
        <b-button-group size="sm">
          <div class="checkEnable"
            :title="data.item.enable ? t('component.account.enable') : t('component.account.disable')">
            <b-check
              @change="changeEnable(data.item)"
              v-if="displayEnableButton(data.item)"
              :checked="data.item.enable"
              variant="outline-success"
              switch
            ></b-check>
          </div>
          <DetailButton
            @click="showUsersGroups(data)"
            label="component.account.details"
            :detailVisible="data.detailsShowing"
            :small="true"
          ></DetailButton>
          <EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_ACCOUNT_MODIFICATION_ID)"
            @click="emit('onEdit', data.item)"
            label="component.account.update"
            :small="true"
          ></EditButton>
          <DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_ACCOUNT_DELETE_ID)"
            @click="deleteAccount(data.item.uri)"
            label="component.account.delete"
            :small="true"
          ></DeleteButton>
        </b-button-group>
      </template>
    </TableAsyncView>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, ref, onMounted, useTemplateRef} from "vue";
import {useStore} from "vuex";
import {useRoute} from "vue-router";
import {SecurityService} from "opensilex-security/index";
import {PersonDTO} from "opensilex-security/model/personDTO";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import {AccountUpdateDTO} from "opensilex-security/model/accountUpdateDTO";
import {AccountGetDTO} from "opensilex-security/model/accountGetDTO";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import {SlotDetails} from "@/components/common/views/TableAsyncView.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {OpenSilexStore} from "@/models/Store";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import TableAsyncView from "@/components/common/views/TableAsyncView.vue";
import PersonContact from "@/components/persons/PersonContact.vue";
import UriLink from "@/components/common/views/UriLink.vue";
import DetailButton from "@/components/common/buttons/DetailButton.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import {useI18n} from "vue-i18n";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const service = opensilex.getService<SecurityService>("opensilex-core.SecurityService");
const store = useStore() as OpenSilexStore;
const route = useRoute();
const {t} = useI18n();

//#region Refs
const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef');
//#endregion

//#region Data and computed
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

const filter = ref<string>("");

const fields = [
  {key: "uri", label: "component.common.uri"},
  {key: "last_name", label: "component.account.linked-person"},
  {key: "email", label: "component.account.email", sortable: true},
  {key: "admin", label: "component.account.admin", sortable: true},
  {label: "component.common.actions", key: "actions", class: "table-actions"}
];

const personByAccountUri = ref<{[id: string]: PersonDTO | null}>({});
const groupDetailsByAccountUri = ref<{[id: string]: NamedResourceDTO[]}>({});
//#endregion

//#region Emits
const emit = defineEmits<{
  (e: "onEdit", payload: AccountGetDTO): void
}>();
//#endregion

//#region Event handlers and watchers
onMounted(() => {
  const query: any = route.query;
  if (query.filter) {
    filter.value = decodeURIComponent(query.filter);
  }
});

function updateFilter(): void {
  opensilex.updateURLParameter("filter", filter.value, "");
  refresh();
}

function refresh(): void {
  tableRef.value?.refresh();
}

async function searchAccounts(options: any): Promise<any> {
  const accountsResponse = await service.searchAccounts(
    filter.value,
    options.orderBy,
    options.currentPage,
    options.pageSize
  );

  const key_personUri_value_accountUri: {[id: string]: string} = {};

  accountsResponse.response.result.forEach((account: any) => {
    personByAccountUri.value[account.uri] = null;
    if (account.linked_person) {
      key_personUri_value_accountUri[account.linked_person] = account.uri;
    }
  });

  await mapPersonsWithAccount(key_personUri_value_accountUri);

  return accountsResponse;
}

async function mapPersonsWithAccount(key_personUri_value_accountUri: {[id: string]: string}): Promise<void> {
  if (Object.keys(key_personUri_value_accountUri).length !== 0) {
    const personsResponse = await service.getPersonsByURI(Object.keys(key_personUri_value_accountUri));
    personsResponse.response.result.forEach((person: PersonDTO) => {
      const accountUri = key_personUri_value_accountUri[opensilex.getLongUri(person.uri)];
      personByAccountUri.value[accountUri] = person;
    });
  }
}

function deleteAccount(uri: string): void {
  opensilex.showLoader();
  service.deleteAccount(uri)
    .then(() => refresh())
    .catch((error: any) => opensilex.errorHandler(error))
    .finally(() => opensilex.hideLoader());
}

function changeEnable(dto: AccountUpdateDTO): void {
  dto.enable = !dto.enable;
  service.updateAccount(dto)
    .catch((error: any) => opensilex.errorHandler(error));
}

function displayEnableButton(accountRow: any): boolean {
  const isUserConnected = accountRow.email === user.value.email;
  return user.value.hasCredential(credentials.value.CREDENTIAL_ACCOUNT_MODIFICATION_ID)
    && !accountRow.admin
    && !isUserConnected;
}

async function showUsersGroups(data: SlotDetails<AccountGetDTO>): Promise<void> {
  const accountUri: string = data.item.uri;

  if (!groupDetailsByAccountUri.value[accountUri]) {
    const groupResponse: HttpResponse<OpenSilexResponse<NamedResourceDTO[]>> = await service.getUserGroups(accountUri);
    const groups: NamedResourceDTO[] = groupResponse.response.result;
    groupDetailsByAccountUri.value[accountUri] = groups;
  }

  data.toggleDetails();
}
//#endregion

//#region Exposed methods
defineExpose({
  refresh
});
//#endregion
</script>

<style scoped lang="scss">

/* without this rules the checkbox is not center on the height of the div*/
.checkEnable {
  display: flex;
  align-items: center;
}
</style>
