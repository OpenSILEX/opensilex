<template>
  <div>
    <StringFilter
        v-model:filter.sync="filter"
        @update="updateFilter()"
        placeholder="component.person.filter-placeholder"
    ></StringFilter>

    <TableAsyncView
        ref="tableRef"
        :searchMethod="searchPersons"
        :fields="fields"
        defaultSortBy="email"
    >

      <template #cell(last_name)="{data}">
        <PersonContact
            :personContact="data.item"
            :customDisplayableName="data.item.last_name"
        ></PersonContact>
      </template>

      <template #cell(orcid)="{data}">
        <UriLink
            v-if="data.item.orcid"
            :uri="data.item.orcid"
        />
      </template>

      <template #cell(email)="{data}">
        <a :href="'mailto:' + data.item.email">{{ data.item.email }}</a>
      </template>

      <template #cell(actions)="{data}">
          <EditButton
              v-if="person.hasCredential(credentials.CREDENTIAL_PERSON_MODIFICATION_ID)"
              @click="emit('onEdit', data.item)"
              label="component.person.update"
              :small="true"
          ></EditButton>
      </template>
    </TableAsyncView>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, ref, onMounted, useTemplateRef} from "vue";
import { useStore } from "vuex";
import { useRoute } from 'vue-router';
import { SecurityService } from "opensilex-security/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OpenSilexStore} from "../../models/Store";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import TableAsyncView from "@/components/common/views/TableAsyncView.vue";
import PersonContact from "@/components/persons/PersonContact.vue";
import UriLink from "@/components/common/views/UriLink.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import {PersonDTO} from "opensilex-security/model/personDTO";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const service = opensilex.getService<SecurityService>("opensilex-core.SecurityService");
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

const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef');

//#region Emits
const emit = defineEmits<{
  (e: "onEdit", payload: PersonDTO): void
}>()
//#endregion


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
    opensilex.updateURLParameter("filter", filter.value, "");
    refresh();
  }

defineExpose({
  refresh,
})

</script>
