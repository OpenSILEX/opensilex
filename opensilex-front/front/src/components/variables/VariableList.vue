<template>
  <n-data-table
    :columns="columns"
    :data="variables"
    :row-key="rowKey"
    :pagination="{ pageSize: 10 }"
    :expanded-row-keys="expandedRowKeys"
    @update:expanded-row-keys="handleExpandedRowChange"
    :sorter="defaultSorter"
  />
</template>

<script lang="ts" setup>
import { ref, h, inject, reactive, onMounted, resolveComponent } from 'vue';
import { useI18n } from 'vue-i18n';
import { NButton, NTag, NDataTable } from 'naive-ui';
import { VariablesService } from 'opensilex-core';
import { VariableGetDTO } from 'opensilex-core/model/variableGetDTO';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import EditButton from './../common/buttons/EditButton.vue';

const { t } = useI18n();
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const $service = ref<VariablesService | null>(null);

const variables = ref<Array<{ item: VariableGetDTO }>>([]);
const variableGroupsList = reactive<Record<string, { uri: string; name: string }[]>>({});
const expandedRowKeys = ref<string[]>([]);

const rowKey = (row: { item: VariableGetDTO }) => row.item.uri;

const loadAllVariables = async () => {
  if (!$opensilex) return;

  $service.value = $opensilex.getService<VariablesService>('opensilex.VariablesService');
  try {
    const response = await $service.value.searchVariables(undefined, undefined, undefined, undefined, undefined,
      undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined,
      undefined, 0, 0
    );

    const result = response.response.result || [];
    variables.value = result.map(variable => ({ item: variable }));
  } catch (error) {
    console.error("Error loading variables", error);
  }
};

const defaultSorter = ref({
  columnKey: 'item.name',
  order: 'ascend'
});


const toggleExpand = async (uri: string) => {
  const index = expandedRowKeys.value.indexOf(uri);
  if (index === -1) {
    expandedRowKeys.value.push(uri);
    await handleExpandedRowChange([...expandedRowKeys.value]);
  } else {
    expandedRowKeys.value.splice(index, 1);
  }
};


const handleExpandedRowChange = async (keys: string[]) => {
  expandedRowKeys.value = keys;

  for (const uri of keys) {
    if (!variableGroupsList[uri]) {
      try {
        $opensilex?.disableLoader();
        const response = await $service.value?.searchVariablesGroups(undefined, uri, ['name=asc']);
        variableGroupsList[uri] = (response?.response.result || []).map(group => ({
          uri: group.uri,
          name: group.name
        }));
      } catch (error) {
        $opensilex?.errorHandler(error);
      } finally {
        $opensilex?.enableLoader();
      }
    }
  }
};

const emit = defineEmits<{
  (e: 'edit', uri: string): void,
  (e: 'delete', item: VariableGetDTO): void
}>();


// COLUMNS

//  const uri = row.item.uri;
//       const groups = variableGroupsList[uri];
//       console.log("groups ", groups)
// if (groups.length === 0) {
//   return h('p', {}, t('VariableList.not-used-in-variablesGroup'));
// }
//       return h('div', {}, [
//         h('p', {}, t('VariableList.variablesGroup')),
//         h('ul', {}, groups.map(group =>
//           h('li', { key: group.uri }, `${group.name} (${group.uri})`)
//         ))
//       ]);
//     }
//   },
//   {
//     title: t('component.common.name'),



function createColumns(t: Function, emit: Function, loadVariablesGroupFromVariable: Function) {
  return [
    {
      type: 'expand',
      expandable: () => true,
      renderExpand: (row: any) => {
         const uri = row.item.uri;
         console.log("uri ", uri)
      const groups = variableGroupsList[uri];
        // const groups = row.groups || [];
console.log("groups ", groups)
        if (groups.length === 0) {
          return h('p', {}, t('VariableList.not-used-in-variablesGroup'));
        }

        return h('ul', {}, groups.map(group =>
          h('li', { key: group.uri }, `${group.name} (${group.uri})`)
        ));
      }
    },
    {
      title: t('component.common.name'),
      key: 'item.name',
      sortable: true,
      sorter: (a, b) => a.item.name.localeCompare(b.item.name),
      render(row: any) {
        return h('div', {}, [
          h(
            'router-link',
            { to: `/variable/details/${encodeURIComponent(row.item.uri)}` },
            { default: () => row.item.name }
          ),
          h('br'),
          h('small', { class: 'text-muted' }, row.item.alternative_name)
        ]);
      }
    },
    {
      title: t('VariableView.entity'),
      key: 'item.entity.name',
      sortable: true,
      sorter: (a, b) => (a.item.entity?.name || '').localeCompare(b.item.entity?.name || ''),
      render: row => row.item.entity?.name
    },
    {
      title: t('VariableView.characteristic'),
      key: 'item.characteristic.name',
      sortable: true,
      sorter: (a, b) => (a.item.characteristic?.name || '').localeCompare(b.item.characteristic?.name || ''),
      render: row => row.item.characteristic?.name
    },
    {
      title: t('VariableView.method'),
      key: 'item.method.name',
      sortable: true,
      sorter: (a, b) => (a.item.method?.name || '').localeCompare(b.item.method?.name || ''),
      render: row => row.item.method?.name
    },
    {
      title: t('VariableView.unit'),
      key: 'item.unit.name',
      sortable: true,
      sorter: (a, b) => (a.item.unit?.name || '').localeCompare(b.item.unit?.name || ''),
      render: row => row.item.unit?.name
    },
    {
      title: t('component.common.actions'),
      key: 'actions',
      render(row) {
  const EditButton = resolveComponent('opensilex-EditButton');
  const DetailButton = resolveComponent('opensilex-DetailButton');
  return h('div', { class: 'btn-group btn-group-sm', role: 'group' }, [
    h(DetailButton, {
      label: 'component.common.details-label',
      small: true,
      detailVisible: expandedRowKeys.value.includes(row.item.uri),
      onClick: () => loadVariablesGroupFromVariable(row.item.uri)
    }),
    h(EditButton, {
      label: 'component.common.list.buttons.update',
      small: true,
      onClick: () => emit('edit', row.item.uri)
    }),
    h(
      'button',
      {
        class: 'btn btn-outline-danger',
        onClick: () => emit('delete', row.item)
      },
      t('component.common.delete')
    )
  ]);
}

    }
  ];
}



const columns = createColumns(t, emit, toggleExpand);



onMounted(() => {
  loadAllVariables();
});
</script>

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
        variablesGroup: Variable utilisé dans un ou plusieurs groupe de variables
        not-used-in-variablesGroup: Variable n'est utilisé dans aucun groupe de variables
        selected-all: Toutes les variables
        display: Affichage
        withoutGroup: Pas dans ce groupe
        withoutGroup-info: Cocher la case pour filtrer les variables qui n'appartiennent pas au groupe sélectionné

</i18n>

