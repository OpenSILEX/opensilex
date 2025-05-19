<template>
  <div class="variable-list-container">
    <div class="table-responsive">
      <table class="table table-striped table-hover">
        <thead>
          <tr>
            <th>{{ $t('component.common.name') }}</th>
            <th>{{ $t('VariableView.entity') }}</th>
            <th>{{ $t('VariableView.characteristic') }}</th>
            <th>{{ $t('VariableView.method') }}</th>
            <th>{{ $t('VariableView.unit') }}</th>
            <th v-if="!noActions">{{ $t('component.common.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="data in variables" :key="data.item.uri">
            <tr>
              <td>
                <router-link :to="getDetailsPageUrl(data.item)">
                  {{ data.item.name }}
                </router-link>
                <br>
                <small class="text-muted">{{ data.item.alternative_name }}</small>
              </td>
              <td>{{ data.item.entity?.name }}</td>
              <td>{{ data.item.characteristic?.name }}</td>
              <td>{{ data.item.method?.name }}</td>
              <td>{{ data.item.unit?.name }}</td>
              <td v-if="!noActions">
                <div class="btn-group btn-group-sm" role="group">
                  <opensilex-DetailButton
                    label="component.common.details-label"
                    :small="true"
                    @click="loadVariablesGroupFromVariable(data)"
                    :detailVisible="data.detailsShowing"
                  />
                  <opensilex-EditButton
                    @click="$emit('edit', data.item.uri)"
                    label="component.common.list.buttons.update"
                    :small="true"
                  />
                  <button 
                    class="btn btn-outline-danger" 
                    @click="$emit('delete', data.item)"
                  >
                    {{ $t('component.common.delete') }}
                  </button>
                </div>
              </td>
            </tr>

            <tr v-if="data.detailsShowing">
              <td :colspan="noActions ? 5 : 6">
                <div>    
                  <template v-if="variableGroupsList[data.item.uri] !== undefined">
                    <ul v-if="variableGroupsList[data.item.uri].length > 0">
                      <p>{{ t('VariableList.variablesGroup') }}</p>
                      <li v-for="group in variableGroupsList[data.item.uri]" :key="group.uri">
                        {{ group.name }} ({{ group.uri }})
                      </li>
                    </ul>
                    <p v-else>{{ t('VariableList.not-used-in-variablesGroup') }}</p>
                  </template>
                </div>
              </td>
            </tr>

          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, onMounted, ref, inject, reactive } from 'vue';
import { VariableGetDTO } from 'opensilex-core/model/variableGetDTO';
import { VariablesService } from 'opensilex-core';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import { useI18n } from 'vue-i18n';

export default defineComponent({
  name: 'VariableList',
  props: {
    noActions: {
      type: Boolean,
      default: false
    },
  },
  emits: ['edit', 'delete'],
  setup(props, { emit }) {
    const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");
    const { t } = useI18n();
    const $service = ref<VariablesService | null>(null);
    const variables = ref<Array<{
      item: VariableGetDTO;
      detailsShowing: boolean;
    }>>([]);
    const variableGroupsList = reactive<Record<string, { uri: string; name: string }[]>>({});

    const getDetailsPageUrl = (variable: VariableGetDTO) => {
      return `/variable/details/${encodeURIComponent(variable.uri)}`;
    };

    const loadAllVariables = async () => {
      if (!$opensilex) {
        console.error("OpenSilex plugin not available");
        return;
      }

      $service.value = $opensilex.getService<VariablesService>("opensilex.VariablesService");

      try {
        const response = await $service.value.searchVariables(
          undefined, undefined, undefined, undefined, undefined,
          undefined, undefined, undefined, undefined, undefined,
          undefined, undefined, undefined, undefined,
          undefined, // orderBy
          0, // currentPage
          0  // pageSize: 0 to get everything
        );

        const result = response.response.result || [];

        variables.value = result.map(variable => ({
          item: variable,
          detailsShowing: false
        }));

      } catch (error) {
        console.error("Error loading variables", error);
      }
    };

    const loadVariablesGroupFromVariable = async (data: {
      item: VariableGetDTO;
      detailsShowing: boolean;
    }) => {
      const uri = data.item.uri;

      if (!data.detailsShowing) {
        try {
          $opensilex?.disableLoader();
          const response = await $service.value?.searchVariablesGroups(
            undefined, uri, ['name=asc']
          );

          const list = (response?.response.result || []).map((group: any) => ({
            uri: group.uri,
            name: group.name
          }));

          variableGroupsList[uri] = list;
          data.detailsShowing = true;
        } catch (error) {
          $opensilex?.errorHandler(error);
        } finally {
          $opensilex?.enableLoader();
        }
      } else {
        data.detailsShowing = false;
      }
    };

    onMounted(() => {
      loadAllVariables();
    });

    return {
      variables,
      variableGroupsList,
      getDetailsPageUrl,
      loadVariablesGroupFromVariable,
      t
    };
  }
});
</script>

<style scoped>
.variable-list-container {
  width: 100%;
}

.table {
  margin-bottom: 1rem;
}

.btn-group-sm > .btn {
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
}
</style>

<i18n>

en:
    VariableList:
        name-placeholder: Enter variable name
        label-filter: Search variables
        label-filter-placeholder: "Search variables, plant height, plant, humidity, image processing, percentage, air.*humidity, etc.
            This filter apply on variable name."
        selected: Selected Variables
        add-groupVariable: Add to an existing group of variables
        add-newGroupVariable: Add to a new group of variables
        export-variables: Export variable list
        export-variables-details: Export detailed variable list
        import-variables-from-shared-resources: Import from the shared source
        variablesGroup: Variable used in one or many groups of variables
        not-used-in-variablesGroup: Variable not used in any group of variables
        selected-all: All variables
        display: Display
        withoutGroup: Not in group
        withoutGroup-info: Select the checkbox to filter the variables that are not included in the selected group
fr:
    VariableList:
        name-placeholder: Entrer un nom de variable
        label-filter: Chercher une variable
        label-filter-placeholder: "Rechercher des variables : Hauteur de plante, plante, humidité, analyse d'image, pourcentage, air.*humidité, etc.
            Ce filtre s'applique au nom d'une variable."
        selected: Variables Sélectionnées
        add-groupVariable: Ajouter à un groupe de variables existant
        add-newGroupVariable: Ajouter à un nouveau groupe de variables
        export-variables: Exporter la liste de variables
        export-variables-details: Exporter la liste détaillée de variables
        import-variables-from-shared-resources: Importer depuis la source partagée
        variablesGroup: La variable est utilisée dans un ou plusieurs groupe de variables 
        not-used-in-variablesGroup: La variable n'est utilisée dans aucun groupe de variables
        selected-all: Toutes les variables
        display: Affichage
        withoutGroup: Pas dans ce groupe
        withoutGroup-info: Cocher la case pour filtrer les variables qui n'appartiennent pas au groupe sélectionné

</i18n>
