<template>
  <b-row>
    <b-col>
      <h6 class="mb-3">
        <strong
          >{{ $t("component.factorLevel.associated") }}
          <span class="required">*</span></strong
        >
      </h6>
      <p>{{ $t("component.factorLevel.associated-help") }}</p>
      <p v-if="editMode" class="alert-info">
        {{ $t("component.factorLevel.alert-help") }}
      </p>
      <b-row>
        <b-col>
          <!-- <p>{{$t('component.common.tabulator.add-multiple')}}</p> -->
          <b-button-group>
            <b-row class="ml-1">
              <b-button
                class="mb-2 mr-2"
                @click="csvExport"
                variant="outline-primary"
                >{{
                  $t("component.common.import-files.csv-template")
                }}</b-button
              >
              <CSVInputFile
                :headersExactMatch="['name', 'description']"
                v-on:updated="uploaded"
              ></CSVInputFile>
              <b-button
                class="mb-2 mr-2"
                @click="resetTable"
                variant="outline-secondary"
                >{{ $t("component.common.tabulator.reset-table") }}</b-button
              >
              <Button
                class="mb-2 mr-4"
                @click="addEmptyRow"
                variant="outline-dark"
                label="component.factorLevel.add"
                :small="false"
              ></Button>
            </b-row>
          </b-button-group>
        </b-col>
      </b-row>
      <b-row>
        <b-col cols="10">
          <div ref="table" class="tab"></div>
        </b-col>
      </b-row>
      <!-- <span class="error-message alert alert-info"> Number of factor{{this.internalFactorLevels.length}}</span> -->
    </b-col>
  </b-row>
</template>


<script setup lang="ts">

import Vue, {computed, inject, onMounted, onBeforeUnmount, ref, watch, useTemplateRef} from "vue";
// @ts-ignore
import HttpResponse from "../../../lib/HttpResponse";
import {ColumnDefinition, TabulatorFull as Tabulator} from "tabulator-tables";
import 'tabulator-tables/dist/css/tabulator.min.css';
import Button from "@/components/common/buttons/Button.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {FactorLevelGetDTO} from "opensilex-core/model/factorLevelGetDTO";
import Papa from 'papaparse'

// extend("requiredTabulator", (value) => {
//   let valid = true;
//   if (value.length == 0) {
//     valid = false;
//   } else {
//     value.some(function (factorLevel) {
//       if (factorLevel.name == null || factorLevel.name.trim() === "") {
//         valid = false;
//       }
//     });
//   }
//
//   if (!valid) {
//     return "component.factorLevel.errors.factor-empty-levels";
//   } else {
//     return valid;
//   }
// });
//
// extend("badNameTabulator", (value) => {
//   var substrings = ["-", "+", "=", "<", ">", "=", "?", "/", "*", "&"];
//   let valid = true;
//   if (value.length != 0) {
//     value.some(function (factorLevel) {
//       if (factorLevel.name != null && factorLevel.name.trim() !== "") {
//         substrings.forEach((substring) => {
//           if (factorLevel.name.indexOf(substring) != -1) {
//             valid = false;
//           }
//         });
//       }
//     });
//   }
//
//   if (!valid) {
//     return "component.factorLevel.errors.factor-badname-levels";
//   } else {
//     return valid;
//   }
// });

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const store = useStore()
const { t } = useI18n()
const bvModal = ref<any>()

const table = useTemplateRef<HTMLDivElement>('table')

interface Props {
  editMode?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  editMode: false,
});

const factorLevels = defineModel('factorLevels')


const internalFactorLevels = ref<FactorLevelGetDTO[]>([]);

const tableColumns = computed<ColumnDefinition[]>(() => {
  let editMode;
  return [
    {
      title: "Generated Uri",
      field: "uri",
      widthGrow: 0.5,
      visible: editMode.value,
    },
    {
      title:
          t("component.factorLevel.name") +
          ' <span class="required">*</span>',
      field: "name",
      editor: "input",
      validator: ["required", "unique"],
      widthGrow: 0.5,
    },
    {
      title: t("component.factorLevel.description").toString(),
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

const tabulator = ref<Tabulator | null>(null);

watch(
    internalFactorLevels,
    (value: FactorLevelGetDTO[]) => {
      tabulator.value?.replaceData(value);
    }
);

const service = opensilex.getService("opensilex.FactorsService");

const langUnwatcher = ref<(() => void) | null>(null);

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
     * add factor levels from CSV in the internalFactorLevels field. Add only none empty and unique factor levels
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
          internalFactorLevels.value.concat(validated_factors)
      );

      internalFactorLevels.value = validated_factors;

      opensilex.showSuccessToast("Data successfully loaded");
    }

function remove_blanks_factors(
    factors: Array<FactorLevelGetDTO>
) {
  return factors.filter(
      (factor) => factor.name !== null && factor.name !== ""
  );
}

  const options = ref<any>(
      {
        layout: "fitColumns",
        cellHozAlign: "center",
        clipboard: true,
        columns: tableColumns.value,
        maxHeight: "100%", //do not let table get bigger than the height of its parent element
        // pagination: "local", //enable local pagination.
        // paginationSize: 5, // this option can take any positive integer value (default = 10)
        langs: langs.value,
      }
  )

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

    if (internalFactorLevels.value.length == 1) {
      opensilex.showWarningToast(
          t("component.factorLevel.errors.minimum-factor-level")
      );
    } else {
      bvModal
          .msgBoxConfirm(
              t("component.common.delete-confirmation").toString(),
              {
                cancelTitle: t("component.common.cancel").toString(),
                okTitle: t("component.common.delete").toString(),
                okVariant: "danger",
                centered: true,
              }
          )
          .then((confirmation) => {
            if (confirmation) {
              if (factorLevelUri != null) {
                deleteFactorLevelRow(
                    factorLevelUri,
                    uriCell
                );
              } else {
                internalFactorLevels.value =
                    internalFactorLevels.value.filter(
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
        internalFactorLevels.value = internalFactorLevels.value.filter(
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

function hasEmptyValue(): boolean {
  if (internalFactorLevels.value.length != 0) {
    if (
        internalFactorLevels.value.some(
            (factorLevel) =>
                factorLevel.name === null || factorLevel.name === ""
        )
    ) {
      return true;
    }
  }
  return false;
}

  function deleteFactorLevel(uri: string) : any {
    console.debug("delete Factor Level" + uri);
    return service.value.deleteFactorLevel(uri);
  }

function resetTable(): void {
  bvModal
      .msgBoxConfirm(
          t("component.factorLevel.delete-confirmation-table").toString(),
          {
            cancelTitle: t("component.common.cancel").toString(),
            okTitle: t("component.common.delete").toString(),
            okVariant: "danger",
            centered: true,
          }
      )
      .then((confirmation) => {
        if (confirmation) {
          internalFactorLevels.value = [];
        }
      });
}

  function addEmptyRow() : void  {
    console.debug("Add row", "empty row", hasEmptyValue());
    if (!hasEmptyValue()) {
      internalFactorLevels.value = internalFactorLevels.value.concat({
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
    let arrData = [{ name: "", description: "" }];
    Papa.download(Papa.unparse(arrData), "factorLevelTemplate");
  }

  function instanciateTabulator() {
    tabulator.value = new Tabulator(table.value, {
      data: internalFactorLevels.value, //link data to table
      reactiveData: true, //enable data reactivity
      columns: tableColumns.value, //define table columns
      layout: "fitColumns",
      layoutColumnsOnNewData: true,
      index: "uri",
    });

    tabulator.value.on("cellClick", (e, cell) => {
      cellActions(e, cell);
    });
  }

</script>

<style scoped lang="scss">
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
        associated-factor-level : You can't remove a factor level which is associated to a scientific object
        minimum-factor-level : You must have one factor level a least
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
        associated-factor-level : Vous ne pouvez pas supprimer un niveau de facteur associé à un objet scientifique
        minimum-factor-level : Vous devez au moins avoir un niveau de facteur
      delete: supprimer


</i18n>
