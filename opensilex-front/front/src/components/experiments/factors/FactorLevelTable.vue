<template>
      <h6 class="mb-3">
        <strong
        >{{ $t("component.menu.experimentalDesign.factorLevels") }}
          <span class="required">*</span></strong
        >
      </h6>
      <p>{{ $t("component.experiment.associated-factor-help") }}</p>
      <p v-if="editMode" class="divHelpMsg">
        {{ $t("component.menu.experimentalDesign.factorLevel-alert-help") }}
      </p>
          <!-- <p>{{$t('component.common.tabulator.add-multiple')}}</p> -->
  <n-button-group size="small" class="btn-group btn-group-sm">
  <n-space>
    <n-button
        ghost
        class="mb-2 csv-button//Button load csv"
        @click="csvExport"
        type="primary"
    >
      {{ $t("component.common.import-files.csv-template") }}
    </n-button>

      <CSVInputFile
          :headersExactMatch="['name', 'description']"
          v-on:updated="uploaded"
       button-label="component.common.tabulator.load-csv">
      </CSVInputFile>


    <n-button
        ghost
        class="mb-2 reset-button"
        @click="resetTable"
        type="secondary"
    >
      {{ $t("component.common.tabulator.reset-table") }}
    </n-button>

    <n-button
        class="mb-2 addLine"
        @click="addEmptyRow"
        variant="outline-dark"
        :small="false"
        type="tertiary"
    >
      {{ $t("component.experiment.factor-level-add") }}
    </n-button>
  </n-space>
  </n-button-group>
  <n-alert
          v-if="props.showFactorLevelsWarning"
          type="error"
          :show-icon="false"
          :closable="false"
          class="factor-level-alert"
      >
        {{ t("component.factorLevel.errors.minimum-factor-level") }}
      </n-alert>
          <div ref="table" id="table" class="tab"></div>
</template>


<script setup lang="ts">

import {computed, inject, onMounted, onBeforeUnmount, ref, watch, useTemplateRef} from "vue";
import HttpResponse from "../../../lib/HttpResponse";
import {ColumnDefinition, TabulatorFull as Tabulator} from "tabulator-tables";
import 'tabulator-tables/dist/css/tabulator.min.css';
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {FactorLevelGetDTO} from "opensilex-core/model/factorLevelGetDTO";
import Papa from 'papaparse'
import {NButtonGroup, useDialog} from "naive-ui";
import {NAlert} from "naive-ui";
import {NSpace} from "naive-ui";
import {FactorsService} from "opensilex-core/api/factors.service";
import CSVInputFile from "@/components/common/forms/CSVInputFile.vue";

//#region Public

const props = defineProps<{
  editMode?: boolean;
  showFactorLevelsWarning?: boolean;
}>();

//#endregion

//#region Private

//#region Data and computed
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const store = useStore()
const {t} = useI18n()
const table = useTemplateRef<HTMLDivElement>('table')
const dialog = useDialog()
const factorLevels = defineModel<FactorLevelGetDTO[]>('factorLevels', {
  default: []
})
const tabulator = ref<Tabulator | null>(null);
const service = opensilex.getService<FactorsService>("opensilex.FactorsService");
const langUnwatcher = ref<(() => void) | null>(null);

const tableColumns = computed<ColumnDefinition[]>(() => {
  return [
    {
      title: "Generated Uri",
      field: "uri",
      widthGrow: 0.5,
      visible: props.editMode,
    },
    {
      title:
          t("component.experiment.label") +
          ' <span class="required">*</span>',
      field: "name",
      editor: "input",
      validator: ["required", "unique"],
      widthGrow: 0.5,
    },
    {
      title: t("component.document.description").toString(),
      field: "description",
      editor: "input",
      widthGrow: 1,
    },
    {
      title: t("component.factorLevel.delete").toString(),
      field: "actions",
      headerSort: false,
      widthGrow: 0.2,
      formatter: function (cell, formatterParams, onRendered) {
        return '<span style="color:red"><!----><svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="trash-alt" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" class="svg-inline--fa fa-trash-alt fa-w-14 fa-sm"><path data-v-0514f944="" fill="currentColor" d="M32 464a48 48 0 0 0 48 48h288a48 48 0 0 0 48-48V128H32zm272-256a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zM432 32H312l-9.4-18.7A24 24 0 0 0 281.1 0H166.8a23.72 23.72 0 0 0-21.4 13.3L136 32H16A16 16 0 0 0 0 48v32a16 16 0 0 0 16 16h416a16 16 0 0 0 16-16V48a16 16 0 0 0-16-16z" class=""></path></svg></span>';
      },
    },
  ];
});

//#endregion

watch(
    factorLevels,
    (value: FactorLevelGetDTO[]) => {
      tabulator.value?.replaceData(value);
    }
);

onMounted(() => {
  langUnwatcher.value = store.watch(
      () => store.getters.language,
      (lang) => {
        instanciateTabulator();
      }
  );

  instanciateTabulator();
});

onBeforeUnmount(() => {
  langUnwatcher.value?.();
});

/**
 * add factor levels from CSV in the factorLevels field. Add only none empty and unique factor levels
 * @param factor_levels factor levels to add
 */
function uploaded(factor_levels: any[]) {
  let validated_factors: any[] = [];

  factor_levels.forEach((row) => {
    if (
        validated_factors.some(
            (factor) => factor.name === row.name
        )
    ) {
      opensilex.showInfoToast(
          "Duplicated factor level : " + row.name
      );

      return;
    }

    validated_factors.push(row);
  });

  validated_factors = remove_blanks_factors(
      factorLevels.value.concat(validated_factors)
  );

  factorLevels.value = validated_factors;

  opensilex.showSuccessToast("Data successfully loaded");
}

function remove_blanks_factors(
    factors: Array<FactorLevelGetDTO>
) {
  return factors.filter(
      (factor) => factor.name !== null && factor.name !== ""
  );
}

function cellActions(evt: any, clickedCell: any): void {
  console.debug(evt, clickedCell);

  let columnName = clickedCell.getField();
  console.debug(columnName);

  if (columnName == "actions") {
    let row = clickedCell.getRow();
    console.debug("actions row", row);

    let nameCell = row.getCell("name");
    console.debug("name value", row.getCell("name").getValue());

    let uriCell = row.getCell("uri");
    console.debug("uri value", row.getCell("uri").getValue());

    let factorLevelUri = row.getCell("uri").getValue();

    if (factorLevels.value.length == 1) {
      opensilex.showWarningToast(
          t("component.factorLevel.errors.minimum-factor-level")
      );
    } else {
      dialog.error({
        content: t("component.common.delete-confirmation").toString(),
        positiveText: t("component.common.delete").toString(),
        negativeText: t("component.common.cancel").toString(),
        showIcon: false,
        closable: false,

        onPositiveClick: () => {
          if (factorLevelUri != null) {
            deleteFactorLevelRow(
                factorLevelUri,
                uriCell
            );
          } else {
            factorLevels.value = factorLevels.value.filter(
                (factorLevel) =>
                    factorLevel.name !== nameCell.getValue()
            );
          }
        }
      });
    }
  }
}

function deleteFactorLevelRow(
    factorLevelUri: string,
    uriCell: any
): void {
  console.debug("remove factor level", factorLevelUri);

  deleteFactorLevel(factorLevelUri)
      .then(() => {
        factorLevels.value = factorLevels.value.filter(
            (factorLevel) => factorLevel.uri !== uriCell.getValue()
        );

        let message =
            t("component.factorLevel.label") +
            " " +
            factorLevelUri +
            " " +
            t("component.common.success.delete-success-message");

        opensilex.showSuccessToast(message);
      })
      .catch((error: HttpResponse) => {
        if (error.status == 400) {
          opensilex.showWarningToast(
              t("component.factorLevel.errors.associated-factor-level")
          );
        } else {
          opensilex.errorHandler(error);
        }
      });
}

function validateFactorLevels(): boolean {
  if (factorLevels.value.length === 0) {
    opensilex.showWarningToast(
        t("component.factorLevel.errors.factor-empty-levels")
    );
    return false;
  }

  if (hasEmptyValue()) {
    opensilex.showWarningToast(
        t("component.factorLevel.errors.factor-empty-row")
    );
    return false;
  }

  return true;
}

function hasEmptyValue(): boolean {
  if (factorLevels.value.length != 0) {
    if (
        factorLevels.value.some(
            (factorLevel) =>
                factorLevel.name === null || factorLevel.name === ""
        )
    ) {
      return true;
    }
  }
  return false;
}

function deleteFactorLevel(uri: string): any {
  console.debug("delete Factor Level", uri);
  return service.deleteFactorLevel(uri);
}

function resetTable(): void {
  dialog.error({
    content: t("component.factorLevel.delete-confirmation-table").toString(),
    positiveText: "Reset",
    negativeText: t("component.common.cancel").toString(),
    showIcon: false,
    closable: false,

    onPositiveClick: () => {
      factorLevels.value = [];
    }
  });
}

function addEmptyRow(): void {
  console.debug("Add row", "empty row", hasEmptyValue());
  if (!hasEmptyValue()) {
    factorLevels.value.push({
      uri: null,
      name: null,
      description: null,
    });
  } else {
    opensilex.showWarningToast(
        t("component.factorLevel.errors.factor-empty-row")
    );
  }
}


function csvExport(): void {
  const csv = Papa.unparse([{ name: "", description: "" }]);
  const blob = new Blob([csv], { type: "text/csv" });

  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = "factorLevelTemplate.csv";
  link.click();

  URL.revokeObjectURL(link.href);
}


function instanciateTabulator() {
  tabulator.value = new Tabulator(table.value, {
    data: factorLevels.value, //link data to table
    reactiveData: true, //enable data reactivity
    columns: tableColumns.value, //define table columns
    layout: "fitColumns",
    layoutColumnsOnNewData: true,
    index: "uri",
  });

  tabulator.value.on("dataChanged", (data) => {
    factorLevels.value = data
  })

  tabulator.value.on("cellClick", (e, cell) => {
    cellActions(e, cell);
  });
}
//#endregion

</script>

<style scoped lang="scss">

// Button reset
.reset-button {
  color: #808080;
  border-color: #808080;
  --n-border: 1px solid #808080 !important;
}

.reset-button:hover {
  color: #ffffff;
  background-color: #808080;
  border-color: #808080;
}

//Button download CSV
.csv-button {
  color: #2080f0;
  border-color: #2080f0;

  --n-border: 1px solid #2080f0 !important;
  --n-border-hover: 1px solid #2080f0 !important;
  --n-border-pressed: 1px solid #2080f0 !important;
}

.csv-button:hover {
  background-color: #2080f0 !important;
  color: #ffffff !important;
}

// Button add line
.addLine{
  color: #212529;
  --n-border: 1px solid #212529 !important;
}
.addLine:hover{
  color: #FFFFFF;
  background-color: #212529;
  --n-border: #212529;
  --n-border-hover: 1px solid #212529!important;
  --n-border-pressed: 1px solid #212529 !important;
}

// add padding 0, for respect the width table in factorForm
#table{
  padding: 0;
  margin-top: 10px; // The margin, so the error message isn't right up against the table.
}
</style>

<i18n>
en:
  component:
    factorLevel:
      uri: URI
      name-placeholder: Enter factor level name
      selector-placeholder: Please select a factor to search factor levels
      filter-placeholder: Use this field to search factor levels
      label: factor level
      add: Add row
      associated: Levels
      associated-help: Levels describe the possible values of a factor
      alert-help: Levels can be linked to scientific objects be careful when update
      unique-name: Name is required and must be unique
      update: update factor level
      name: name
      factor: factor
      description: description
      hasFactor: has factor
      delete-confirmation-table: This action is final, are you sure you want to empty this table?
      errors:
        factor-already-exists: Factor level already exists with this URI.
        factor-empty-row: You can't add several empty rows
        factor-empty-levels: Missing factor levels
        factor-badname-levels: "Must not contain -,+,=,<>,=,?,/,*,&"
        associated-factor-level: You can't remove a factor level which is associated to a scientific object
        minimum-factor-level: You must have one factor level a least
      delete: delete
fr:
  component:
    factorLevel:
      uri: URI
      name-placeholder: Renseignez le nom d'un niveau de facteur
      factor-selector-placeholder: Sélectionnez un nom de facteur
      filter-placeholder: Utilisez ce change pour filter vos niveau de facteur
      label: niveau de facteur
      add: Ajouter une ligne
      associated: Niveaux de facteurs associés
      associated-help: Les niveaux décrivent les valeurs possibles d'un facteur
      alert-help: Les niveaux peuvent être liés à des objets scientifique faite attention lors de leur édition
      unique-name: Le nom est requis et doit être unique
      update: Modifier un niveau de facteur
      name: nom
      factor: facteur
      description: description
      hasFactor: est lié au facteur
      delete-confirmation-table: Cette action est définitive, merci de confirmer la suppression des éléments du tableau.
      errors:
        factor-already-exists: URI du niveau de facteur déjà existante.
        factor-empty-row: Vous ne pouvez pas ajouter plusieurs lignes vides
        factor-empty-levels: Niveaux de facteurs manquants
        factor-badname-levels: "Ne doit pas contenir -,+,=,<>,=,?,/,*,&"
        associated-factor-level: Vous ne pouvez pas supprimer un niveau de facteur associé à un objet scientifique
        minimum-factor-level: Vous devez au moins avoir un niveau de facteur
      delete: supprimer


</i18n>
