<template>
  <b-row>
    <b-col>
      <h6 class="mb-3">
        <strong>{{ $t("component.factorLevel.associated") }}</strong>
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
                :headersToCheck="['name', 'comment']"
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
            rules="requiredTabulator"
            ref="validationProvider"
            :skipIfEmpty="false"
            v-slot="{ errors }"
          >
            <div class="error-message alert alert-danger">{{ errors[0] }}</div>
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
import { FactorsService } from "opensilex-core/index";
import HttpResponse, {
  OpenSilexResponse,
} from "opensilex-security/HttpResponse";
import ValidationProvider from "vee-validate";
import { extend } from "vee-validate";

extend("requiredTabulator", (value) => {
  let valid = true;
  value.some(function (factorLevel) {
    if (factorLevel.name == null || factorLevel.name.trim() === "") {
      valid = false;
    }
  });
  if (!valid) {
    return "Missing factor level name";
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
        comment: "Description",
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
        comment: "Comment",
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
        title: "Comment",
        field: "comment",
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

          // return '<button class="btn btn-danger btn-sm" style="padding: 0px 0px"><span data-v-4d3fd064=""><!----><svg  aria-hidden="true" focusable="false" data-prefix="fas" style="" data-icon="trash-alt" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" class="svg-inline--fa fa-trash-alt fa-w-14 fa-sm"><path data-v-0514f944="" fill="currentColor" d="M32 464a48 48 0 0 0 48 48h288a48 48 0 0 0 48-48V128H32zm272-256a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zM432 32H312l-9.4-18.7A24 24 0 0 0 281.1 0H166.8a23.72 23.72 0 0 0-21.4 13.3L136 32H16A16 16 0 0 0 0 48v32a16 16 0 0 0 16 16h416a16 16 0 0 0 16-16V48a16 16 0 0 0-16-16z" class=""></path></svg></span><!----></button>';
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
      this.$opensilex.showSuccessToast("Data sucessfully loaded");
    }
  }

  options: any = {
    layout: "fitColumns",
    cellHozAlign: "center",
    clipboard: true,
    columns: this.tableColumns,
    maxHeight: "100%", //do not let table get bigger than the height of its parent element
    pagination: "local", //enable local pagination.
    paginationSize: 5, // this option can take any positive integer value (default = 10)
    langs: this.langs,
  };

  cellActions(evt, clickedCell) {
    console.debug(evt, clickedCell);
    let columnName = clickedCell.getField();
    console.debug(columnName);

    if (columnName == "actions") {
      let row = clickedCell.getRow();
      console.debug("actions row", row);

      var nameCell = row.getCell("name");
      console.debug("name value", row.getCell("name").getValue());

      let factorLevelUri = row.uri;

      if (factorLevelUri != null) {
        this.deleteFactorLevel(row.uri)
          .then(() => {
            this.internalFactorLevels = this.internalFactorLevels.filter(
              (factorLevel) => factorLevel.name !== nameCell.getValue()
            );
            let message =
              this.$i18n.t("component.factorLevel.label") +
              " " +
              factorLevelUri +
              " " +
              this.$i18n.t("component.common.success.delete-success-message");
            this.$opensilex.showSuccessToast(message);
          })
          .catch(this.$opensilex.errorHandler);
      } else {
        this.internalFactorLevels = this.internalFactorLevels.filter(
          (factorLevel) => factorLevel.name !== nameCell.getValue()
        );
      }
    }
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
    console.debug("deleteFactor " + uri);
    return this.service.deleteFactor(uri);
  }

  resetTable() {
    this.internalFactorLevels = [];
  }

  addEmptyRow() {
    console.debug("Add row", "empty row", this.hasEmptyValue());
    if (!this.hasEmptyValue()) {
      this.internalFactorLevels.unshift({
        uri: null,
        name: null,
        comment: null,
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
      this.internalFactorLevels.unshift(row);
    }
  }

  changeTableLang(lang: string) {
    let tabulatorInstance = this.tabulatorRef.getInstance();
    tabulatorInstance.setLocale(lang);
  }

  csvExport() {
    let arrData = [{ name: "", comment: "" }];
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
      factorLevel: factor level
      add: Add row
      associated: Levels
      associated-help: Levels describe the possible values of a factor
      alert-help: Levels can be linked to scientific objects be careful when update
      unique-name: Name is required and must be unique
      update: update factor level
      name: name
      factor: factor
      comment: comment
      hasFactor: has factor
      errors:
        factor-already-exists: Factor level already exists with this URI.
        factor-empty-row: You can't add several empty rows
fr:
  component:
    factorLevel:
      uri: URI
      name-placeholder: Renseignez le nom d'un niveau de facteur
      factor-selector-placeholder: Sélectionnez un nom de facteur
      filter-placeholder: Utilisez ce change pour filter vos niveau de facteur
      factorLevel: niveau de facteur
      add: Ajouter une ligne
      associated: Niveaux de facteurs associés
      associated-help: Les niveaux décrivent les valeurs possibles d'un facteur
      alert-help: Les niveaux peuvent être liés à des objets scientifique faite attention lors de leur édition
      unique-name: Le nom est requis et doit être unique
      update: Modifier un niveau de facteur
      name: nom
      factor: facteur
      comment: description
      hasFactor: est lié au facteur
      errors:
        factor-already-exists: URI du niveau de facteur déjà existante.
        factor-empty-row: Vous ne pouvez pas ajouter plusieurs lignes vides

</i18n>
