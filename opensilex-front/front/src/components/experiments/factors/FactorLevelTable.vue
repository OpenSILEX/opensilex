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
              <opensilex-CSVInputFile
                :headersToCheck="['name', 'description']"
                v-on:updated="uploaded"
              ></opensilex-CSVInputFile>
              <b-button
                class="mb-2 mr-2"
                @click="resetTable"
                variant="outline-secondary"
                >{{ $t("component.common.tabulator.reset-table") }}</b-button
              >
              <opensilex-Button
                class="mb-2 mr-4"
                @click="addEmptyRow"
                variant="outline-dark"
                label="component.factorLevel.add"
                icon
                :small="false"
              ></opensilex-Button>
            </b-row>
          </b-button-group>
        </b-col>
      </b-row>
      <b-row>
        <b-col cols="10">
          <ValidationProvider
            rules="requiredTabulator|badNameTabulator"
            ref="validationProvider"
            :skipIfEmpty="false"
            v-slot="{ errors }"
          >
            <div
              class="error-message alert alert-danger"
              v-if="errors.length > 0"
            >
              {{ $t(errors[0]) }}
            </div>
            <VueTabulator
              ref="tabulatorRef"
              class="table-light table-bordered"
              v-model="internalFactorLevels"
              :options="options"
              @cell-click="cellActions"
            />
          </ValidationProvider>
        </b-col>
      </b-row>
      <!-- <span class="error-message alert alert-info"> Number of factor{{this.internalFactorLevels.length}}</span> -->
    </b-col>
  </b-row>
</template>


<script lang="ts">
import { Component, Prop, Ref, PropSync } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { FactorsService } from "opensilex-core/index";
import { extend } from "vee-validate";
import HttpResponse from "../../../lib/HttpResponse";

extend("requiredTabulator", (value) => {
  let valid = true;
  if (value.length == 0) {
    valid = false;
  } else {
    value.some(function (factorLevel) {
      if (factorLevel.name == null || factorLevel.name.trim() === "") {
        valid = false;
      }
    });
  }

  if (!valid) {
    return "component.factorLevel.errors.factor-empty-levels";
  } else {
    return valid;
  }
});

extend("badNameTabulator", (value) => {
  var substrings = ["-", "+", "=", "<", ">", "=", "?", "/", "*", "&"];
  let valid = true;
  if (value.length != 0) {
    value.some(function (factorLevel) {
      if (factorLevel.name != null && factorLevel.name.trim() !== "") {
        substrings.forEach((substring) => {
          if (factorLevel.name.indexOf(substring) != -1) {
            valid = false;
          }
        });
      }
    });
  }

  if (!valid) {
    return "component.factorLevel.errors.factor-badname-levels";
  } else {
    return valid;
  }
});

@Component
export default class FactorLevelTable extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;
  $papa: any;
  service: FactorsService;
  langs: any = {
    fr: {
      //French language definition
      columns: {
        name: "Nom",
        description: "Description",
        actions: "Supprimer",
      },
      pagination: {
        first: "Premier",
        first_title: "Premier Page",
        last: "Dernier",
        last_title: "Dernier Page",
        prev: "Précédent",
        prev_title: "Précédent Page",
        next: "Prochain",
        next_title: "Prochain Page",
      },
    },
    en: {
      columns: {
        name: "Name", //replace the title of column name with the value "Name"
        description: "Description",
        actions: "Delete",
      },
      pagination: {
        first: "First", //text for the first page button
        first_title: "First Page", //tooltip text for the first page button
        last: "Last",
        last_title: "Last Page",
        prev: "Prev",
        prev_title: "Prev Page",
        next: "Next",
        next_title: "Next Page",
      },
    },
  };

  @Ref("tabulatorRef") readonly tabulatorRef!: any;

  @Ref("validationProvider") readonly validationProvider!: any;

  @Prop({ default: false })
  editMode: boolean;

  @PropSync("factorLevels", {
    default: () => {
      return [];
    },
  })
  internalFactorLevels: any[];

  get tableColumns(): any[] {
    return [
      {
        title: "Generated Uri",
        field: "uri",
        formater: "string",
        widthGrow: 0.5,
        visible: this.editMode,
      },
      {
        title: 'Name<span class="required">*</span>',
        field: "name",
        formater: "string",
        editor: "input",
        validator: ["required", "unique"],
        widthGrow: 0.5,
      },
      {
        title: "description",
        field: "description",
        formater: "string",
        editor: "input",
        widthGrow: 1,
      },
      {
        title: "Delete",
        field: "actions",
        headerSort: false,
        widthGrow: 0.2,
        formatter: function (cell, formatterParams, onRendered) {
          return '<span style="color:red"><!----><svg   aria-hidden="true" focusable="false" data-prefix="fas" data-icon="trash-alt" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" class="svg-inline--fa fa-trash-alt fa-w-14 fa-sm"><path data-v-0514f944="" fill="currentColor" d="M32 464a48 48 0 0 0 48 48h288a48 48 0 0 0 48-48V128H32zm272-256a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zM432 32H312l-9.4-18.7A24 24 0 0 0 281.1 0H166.8a23.72 23.72 0 0 0-21.4 13.3L136 32H16A16 16 0 0 0 0 48v32a16 16 0 0 0 16 16h416a16 16 0 0 0 16-16V48a16 16 0 0 0-16-16z" class=""></path></svg></span>';
        },
      },
    ];
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      (lang) => {
        this.changeTableLang(lang);
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  uploaded(data: any[]) {
    console.debug("uploaded", data);
    let tmpLength = this.internalFactorLevels.length;
    for (let row in data) {
      console.debug(
        "uploaded row ",
        data[row],
        data[row].name,
        this.hasDuplicateName(data[row].name)
      );

      if (!this.hasDuplicateName(data[row].name)) {
        this.addRow(data[row]);
      } else {
        this.$opensilex.showInfoToast(
          "Already existing factor level : " + data[row].name
        );
      }
    }
    if (tmpLength == this.internalFactorLevels.length) {
      this.$opensilex.showInfoToast("Valid file. No data to add");
    }
    if (tmpLength < this.internalFactorLevels.length) {
      this.$opensilex.showSuccessToast("Data successfully loaded");
    }
  }

  options: any = {
    layout: "fitColumns",
    cellHozAlign: "center",
    clipboard: true,
    columns: this.tableColumns,
    maxHeight: "100%", //do not let table get bigger than the height of its parent element
    // pagination: "local", //enable local pagination.
    // paginationSize: 5, // this option can take any positive integer value (default = 10)
    langs: this.langs,
  };

  cellActions(evt, clickedCell) {
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

      if (this.internalFactorLevels.length == 1) {
        this.$opensilex.showWarningToast(
          this.$i18n.t("component.factorLevel.errors.minimum-factor-level")
        );
      } else {
        this.$bvModal
          .msgBoxConfirm(
            this.$i18n.t("component.common.delete-confirmation").toString(),
            {
              cancelTitle: this.$i18n.t("component.common.cancel").toString(),
              okTitle: this.$i18n.t("component.common.delete").toString(),
              okVariant: "danger",
              centered: true,
            }
          )
          .then((confirmation) => {
            if (confirmation) {
              if (factorLevelUri != null) {
                this.deleteFactorLevelRow(factorLevelUri, uriCell);
              } else {
                this.internalFactorLevels = this.internalFactorLevels.filter(
                  (factorLevel) => factorLevel.name !== nameCell.getValue()
                );
              }
            }
          });
      }
    }
  }

  deleteFactorLevelRow(factorLevelUri: string, uriCell: any) {
    console.debug("remove factor level", factorLevelUri);

    this.deleteFactorLevel(factorLevelUri)
      .then(() => {
        this.internalFactorLevels = this.internalFactorLevels.filter(
          (factorLevel) => factorLevel.uri !== uriCell.getValue()
        );
        let message =
          this.$i18n.t("component.factorLevel.label") +
          " " +
          factorLevelUri +
          " " +
          this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
      })
      .catch((error: HttpResponse) => {
        if (error.status == 400) {
          this.$opensilex.showWarningToast(
            this.$i18n.t("component.factorLevel.errors.associated-factor-level")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  hasEmptyValue() {
    if (this.internalFactorLevels.length != 0) {
      if (
        this.internalFactorLevels.some(
          (factorLevel) => factorLevel.name === null || factorLevel.name === ""
        )
      ) {
        return true;
      }
    }
    return false;
  }

  hasDuplicateName(name: string) {
    if (this.internalFactorLevels.length != 0) {
      if (
        this.internalFactorLevels.some(
          (factorLevel) => factorLevel.name === name
        )
      ) {
        return true;
      }
    }
    return false;
  }

  deleteFactorLevel(uri: string) {
    console.debug("delete Factor Level" + uri);
    return this.service.deleteFactorLevel(uri);
  }

  resetTable() {
    this.$bvModal
      .msgBoxConfirm(
        this.$i18n
          .t("component.factorLevel.delete-confirmation-table")
          .toString(),
        {
          cancelTitle: this.$i18n.t("component.common.cancel").toString(),
          okTitle: this.$i18n.t("component.common.delete").toString(),
          okVariant: "danger",
          centered: true,
        }
      )
      .then((confirmation) => {
        if (confirmation) {
          this.internalFactorLevels = [];
        }
      });
  }

  addEmptyRow() {
    console.debug("Add row", "empty row", this.hasEmptyValue());
    if (!this.hasEmptyValue()) {
      Vue.set(this.internalFactorLevels, this.internalFactorLevels.length, {
        uri: null,
        name: null,
        description: null,
      });
    } else {
      this.$opensilex.showWarningToast(
        this.$i18n.t("component.factorLevel.errors.factor-empty-row")
      );
    }
  }
  addRow(row) {
    console.debug("Add row", row, "empty row", this.hasEmptyValue());
    if (row.name != undefined && row.name != null && row.name != "") {
      Vue.set(this.internalFactorLevels, this.internalFactorLevels.length, row);
    }
  }

  changeTableLang(lang: string) {
    let tabulatorInstance = this.tabulatorRef.getInstance();
    tabulatorInstance.setLocale(lang);
  }

  csvExport() {
    let arrData = [{ name: "", description: "" }];
    this.$papa.download(this.$papa.unparse(arrData), "factorLevelTemplate");
  }
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
        factor-badname-levels: Must not contains -,+,=,<,>,=,?,/,*,&
        associated-factor-level : You can't remove a factor level which is associated to a scientific object 
        minimum-factor-level : You must have one factor level a least
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
        factor-badname-levels: Ne doit pas contenir -,+,=,<,>,=,?,/,*,&
        associated-factor-level : Vous ne pouvez pas supprimer un niveau de facteur associé à un objet scientifique
        minimum-factor-level : Vous devez au moins avoir un niveau de facteur
        

</i18n>
