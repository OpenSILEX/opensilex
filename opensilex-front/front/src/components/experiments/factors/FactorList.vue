<template>
  <div>
    <SearchFilterField
      v-if="searchBar"
      @clear="resetSearch()"
      @search="updateFilters()"
      label="component.factor.list.filter.label"
    >
      <template v-slot:filters>
        <FilterField>
          <b-form-group>
            <label for="name">{{
              $t("component.factor.list.filter.name")
            }}</label>
            <StringFilter
              id="name"
              v-model:filter="filter.name"
              placeholder="component.factor.name-placeholder"
            ></StringFilter>
          </b-form-group>
        </FilterField>

        <FilterField>
          <b-form-group>
            <b-input-group>
              <!-- Factor categories -->
              <FactorCategorySelector
                label="component.factor.list.filter.category"
                placeholder="component.factor.names.category-placeholder"
                :category.sync="filter.category"
              ></FactorCategorySelector>
            </b-input-group>
          </b-form-group>
        </FilterField>

        <FilterField v-if="experiment == null">
          <b-form-group>
            <b-input-group>
              <!-- Experiments -->
              <ExperimentSelector
                label="component.factor.list.filter.experiment"
                :multiple="false"
                :experiments.sync="filter.experiment"
              ></ExperimentSelector>
            </b-input-group>
          </b-form-group>
        </FilterField>
      </template>
    </SearchFilterField>

    <TableAsyncView
      ref="tableRef"
      :searchMethod="searchFactors"
      :fields="fields"
      defaultSortBy="name"
      :isSelectable="isSelectable"
      labelNumberOfSelectedRow="FactorList.selected"
      iconNumberOfSelectedRow="ik#ik-feather"
    >
      <template v-slot:head(name)="{ data }">{{ $t(data.label) }}</template>
      <template v-slot:head(description)="{ data }">{{
        $t(data.name)
      }}</template>
      <template v-slot:head(category)="{ data }">{{ $t(data.label) }}</template>
      <!-- <template v-slot:head(uri)="{data}">{{$t(data.label)}}</template> -->
      <template v-slot:head(actions)="{ data }">{{ $t(data.label) }}</template>
      <template v-slot:cell(name)="{ data }">
        <UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :to="{ path: '/' +encodeURIComponent(experiment) + '/factor/details/' + encodeURIComponent(data.item.uri) }"
        ></UriLink>
      </template>
      <template v-slot:cell(category)="{ data }"
        ><span class="capitalize-first-letter">{{
          opensilex.getFactorCategoryName(data.value)
        }}</span></template
      >
      <template v-slot:cell(actions)="{ data }">
        <b-button-group size="sm">
          <EditButton
            v-if="
              user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)
            "
            :small="true"
            label="component.common.list.buttons.update"
            @click="$emit('onEdit', data.item.uri)"
          ></EditButton>
          <InteroperabilityButton
            v-if="
              user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)
            "
            :small="true"
            label="component.common.list.buttons.interoperability"
            @click="$emit('onInteroperability', data.item.uri)"
           disabled></InteroperabilityButton>
          <DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_DELETE_ID)"
            :small="true"
            label="component.common.list.buttons.delete"
            @click="$emit('onDelete', data.item)"
          ></DeleteButton>
        </b-button-group>
      </template>
    </TableAsyncView>
  </div>
</template>

<script setup lang="ts">

// @ts-ignore
import {FactorsfactorService} from "core/index";
import TableAsyncView from "@/components/common/views/TableAsyncView.vue";
import ExperimentSelector from "@/components/experiments/ExperimentSelector.vue";
import UriLink from "@/components/common/views/UriLink.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import InteroperabilityButton from "@/components/common/buttons/InteroperabilityButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import {computed, inject, onBeforeUnmount, onMounted, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {useRoute} from "vue-router";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import {FactorsService} from "opensilex-core/api/factors.service";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const factorService = opensilex.getService<FactorsService>('opensilex.FactorsService')
const store = useStore()
const route = useRoute()


interface Props {
  experiment: string | null
  isSelectable: boolean
  noActions: boolean
  searchBar: boolean
}

const props = withDefaults(defineProps<Props>(), {
  experiment: null,
  isSelectable: false,
  noActions: false,
  searchBar: false,
})

let langUnwatcher: (() => void) | undefined

onMounted(() => {
  langUnwatcher = store.watch(
      (state, getters) => getters.language,
      async () => {
        await opensilex.loadFactorCategories()
        refresh()
      }
  )
})

onBeforeUnmount(() => {
  langUnwatcher?.()
})

 function beforeDestroy() {
    langUnwatcher();
  }

  const user = computed(() => {
    return store.state.user
  })

const credentials = computed(() => {
  return store.state.credentials
})

  const filter = {
    uri: "",
    name: "",
    description: "",
    experiment: null,
    category: "",
  };

  function resetSearch() {
    resetFilters();
    refresh();
  }

  function resetFilters() {
    filter.uri = null;
    filter.name = "";
    filter.description = "";
    filter.experiment = null;
    filter.category = "";
    // Only if search and reset button are use in list
    if (props.experiment != null) {
      filter.experiment = props.experiment;
    }
  }
const emit = defineEmits(['onDetails', 'onDelete', 'onDetails', 'onInteroperability', 'onEdit'])

function onDetails(uri: string) {
  emit('onDetails', uri)
}

function created() {
  const query: any = route.query

  resetFilters()

  for (const [key] of Object.entries(filter)) {
    if (query[key]) {
      filter[key] = decodeURIComponent(query[key] as string)
    }
  }
}

  function updateFilters() {
    for (let [key, value] of Object.entries(filter)) {
      opensilex.updateURLParameter(key, value, "");
    }
    refresh();
  }

  const fields = computed(() =>
  {
    let tableFields: any = [
      {
        key: "name",
        label: "component.experiment.label",
        sortable: true,
      },
      {
        key: "category",
        label: "component.experiment.category",
        sortable: true,
      },
      {
        key: "description",
        label: "component.experiment.description",
        sortable: false,
      },
    ];
    if (!props.noActions) {
      tableFields.push({
        key: "actions",
        label: "component.common.actions",
      });
    }
    return tableFields;
  })

const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef')


  function onItemUnselected(row) {
    tableRef.value.onItemUnselected(row);
  }
  function onItemSelected(row) {
    tableRef.value.onItemSelected(row);
  }

  function refresh() {
    tableRef.value.refresh();
  }

  function getSelected() {
    return tableRef.value.getSelected();
  }

  function searchFactors(options) {
    return factorService.searchFactors(
      opensilex.prepareGetParameter(filter.name), // name
      opensilex.prepareGetParameter(filter.description), // description
      opensilex.prepareGetParameter(filter.category), // category
      opensilex.prepareGetParameter(filter.experiment), // experiment
      options.orderBy, // orderBy
      options.currentPage, // page
      options.pageSize // pageSize
    );
  }

</script>

<style scoped lang="scss">
a {
  color: #007bff;
}
.uri-info {
  text-overflow: ellipsis;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  max-width: 300px;
}
</style>

<i18n>

en:
  FactorList:
    selected: Selected factor
    selectLabel: Select factor
  component: 
    factor: 
      list:
        name: Name
        category: Category
        description: description
        filter: 
          label: Filter factors
          name: Name
          experiment: Experiment
          category: Category
          name-placeholder: Irrigation, Shading, ...
          experiment-placeholder: Select experiment
            
fr:
  FactorList:
    selected: Facteur(s) séléectionné(s)
    selectLabel: Sélection de facteurs
  component: 
    factor: 
      list:
        name: Nom
        category: Categorie
        description: description
        filter: 
          label: Filtrer les facteurs
          name: Nom
          category: Catégorie
          experiment: Expérimentation
          name-placeholder: Irrigation, Ombrage, ...
          experiment-placeholder: Sélectionner experimentation

</i18n>