<template>
  <div>
    <opensilex-StringFilter
        v-model:filter.sync="filter"
        @update="updateFilter()"
        placeholder="component.person.filter-placeholder"
    ></opensilex-StringFilter>

    <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="searchPersons"
        :fields="fields"
        defaultSortBy="email"
    >

      <template #cell(last_name)="{data}">
        <opensilex-PersonContact
            :personContact="data.item"
            :customDisplayableName="data.item.last_name"
        ></opensilex-PersonContact>
      </template>

      <template #cell(orcid)="{data}">
        <opensilex-UriLink
            v-if="data.item.orcid"
            :uri="data.item.orcid"
        />
      </template>

      <template #cell(email)="{data}">
        <a :href="'mailto:' + data.item.email">{{ data.item.email }}</a>
      </template>

      <template #cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
              v-if="person.hasCredential(credentials.CREDENTIAL_PERSON_MODIFICATION_ID)"
              @click="$emit('onEdit', data.item)"
              label="component.person.update"
              :small="true"
          ></opensilex-EditButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, onMounted } from "vue";
import { useStore } from "vuex";
import { useRoute } from 'vue-router';
import { SecurityService } from "opensilex-security/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OpenSilexStore} from "../../models/Store";

const $opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const service = $opensilex.getService<SecurityService>("opensilex-core.SecurityService");
const store = useStore() as OpenSilexStore;
const route = useRoute();
    
//#region Data and computed
const person = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);


let filter = ref("");
const fields = [
  {key: "last_name", label: "component.person.last-name", sortable: true},
  {key: "first_name", label: "component.person.first-name", sortable: true},
  {key: "email", label: "component.person.email", sortable: true},
  {key: "orcid", label: "component.person.orcid"},
  {key: "affiliation", label: "component.person.affiliation"},
  {key: "phone_number", label: "component.person.phone_number"},
  {key: "actions", label: "component.common.actions", class: "table-actions"}
];
//#endregion

const tableRef = ref();

onMounted(() => {
  let query: any = route.query;
  if (query.filter) {
      filter.value = decodeURIComponent(query.filter);
    }
  });

  function refresh() {
    tableRef.value.refresh();
  }

  function searchPersons(options) {
    return service
        .searchPersons(
            filter.value,
            false,
            options.orderBy,
            options.currentPage,
            options.pageSize
        )
  }

  function updateFilter() {
    $opensilex.updateURLParameter("filter", filter, "");
    refresh();
  }

</script>
