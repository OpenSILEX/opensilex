<template>
  <b-row>
    <b-col>
      <h6 class="mb-3">
        <strong>{{$t('component.factorLevel.associated')}}</strong>
      </h6>
      <b-row>
        <b-col>
          <opensilex-AddChildButton
            class="mb-2 mr-4"
            @click="addEmptyRow"
            variant="outline-primary"
            label="component.factorLevel.add"
            :small="false"
          ></opensilex-AddChildButton>
          <!-- <p>{{$t('component.common.tabulator.add-multiple')}}</p> -->
          <b-button-group>
            <b-button
              class="mr-2"
              @click="csvExport"
              variant="outline-primary"
            >{{$t('component.common.import-files.csv-template')}}</b-button>
            <!-- <b-button
              @click="resetTable"
              variant="outline-secondary"
            >{{$t('component.common.tabulator.reset-table')}}</b-button>-->
            <opensilex-CSVInputFile :headersToCheck="['name','comment']" v-on:updated="uploaded"></opensilex-CSVInputFile>
          </b-button-group>
        </b-col>
      </b-row>
      <b-row>
        <b-col cols="10">
          <VueTabulator
            ref="tabulatorRef"
            class="table-light table-bordered"
            v-model="this.internalFactorLevels"
            :options="options"
            @cell-click="removeFactorLevel"
          />
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
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class FactorLevelTable extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;
  service: FactorsService;
  langs: any = {
    fr: {
      //French language definition
      columns: {
        name: "Nom",
        comment: "Description",
        actions: "Supprimer"
      },
      pagination: {
        first: "Premier",
        first_title: "Premier Page",
        last: "Dernier",
        last_title: "Dernier Page",
        prev: "Précédent",
        prev_title: "Précédent Page",
        next: "Prochain",
        next_title: "Prochain Page"
      }
    },
    en: {
      columns: {
        name: "Name", //replace the title of column name with the value "Name"
        comment: "Comment",
        actions: "Delete"
      },
      pagination: {
        first: "First", //text for the first page button
        first_title: "First Page", //tooltip text for the first page button
        last: "Last",
        last_title: "Last Page",
        prev: "Prev",
        prev_title: "Prev Page",
        next: "Next",
        next_title: "Next Page"
      }
    }
  };

  @Ref("tabulatorRef") readonly tabulatorRef!: any;
  @Ref("langInput") readonly langInput!: any;

  @Prop({ default: false })
  editMode: boolean;

  @PropSync("factorLevels", {
    default: () => {
      return [];
    }
  })
  internalFactorLevels: any[];

  tableColumns: any[] = [
    {
      title: "Generated Uri",
      field: "uri",
      formater: "string",
      widthGrow: 0.5,
      visible: this.editMode
    },
    {
      title: "Name",
      field: "name",
      formater: "string",
      editor: "input",
      validator: ["required", "unique"],
      cssClass: "required",
      widthGrow: 0.5
    },
    {
      title: "Comment",
      field: "comment",
      formater: "string",
      editor: "input",
      widthGrow: 1
    },
    {
      title: "Delete",
      field: "actions",
      widthGrow: 0.3,
      formatter: function(cell, formatterParams, onRendered) {
        return '<button data-v-4d3fd064="" data-v-4c148622="" data-v-13e99aa8="" title="component.factor.delete" type="button" class="btn btn-danger btn-sm"><span data-v-0514f944="" data-v-4d3fd064=""><!----><svg data-v-0514f944="" aria-hidden="true" focusable="false" data-prefix="fas" data-icon="trash-alt" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" class="svg-inline--fa fa-trash-alt fa-w-14 fa-sm"><path data-v-0514f944="" fill="currentColor" d="M32 464a48 48 0 0 0 48 48h288a48 48 0 0 0 48-48V128H32zm272-256a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zM432 32H312l-9.4-18.7A24 24 0 0 0 281.1 0H166.8a23.72 23.72 0 0 0-21.4 13.3L136 32H16A16 16 0 0 0 0 48v32a16 16 0 0 0 16 16h416a16 16 0 0 0 16-16V48a16 16 0 0 0-16-16z" class=""></path></svg></span><!----></button>';
      }
    }
  ];

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");
  }

  mounted() {
    this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.changeTableLang(lang);
      }
    );
  }

  uploaded(data) {
    let tmpLength = this.internalFactorLevels.length;
    for (let row in data) {
      this.addRow(data[row]);
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
    langs: this.langs
  };

  removeFactorLevel(evt, clickedCell) {
    console.debug(evt, clickedCell);
    let columnName = clickedCell.getField();
    console.debug(columnName);

    if (columnName == "actions") {
      let row = clickedCell.getRow();
      console.debug("actions row", row);

      var nameCell = row.getCell("name");
      console.debug("name cell value", nameCell.getValue());

      let factorLevelUri = row.uri;

      if (factorLevelUri != null) {
        this.deleteFactorLevel(row.uri)
          .then(() => {
            this.internalFactorLevels = this.internalFactorLevels.filter(
              factorLevel => factorLevel.name !== nameCell.getValue()
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
          factorLevel => factorLevel.name !== nameCell.getValue()
        );
      }
    }
  }

  deleteFactorLevel(uri: string) {
    console.debug("deleteFactor " + uri);
    return this.service.deleteFactor(uri);
  }

  resetTable() {
    this.internalFactorLevels = [];
  }

  addEmptyRow() {
    console.debug("add row");
    this.internalFactorLevels.unshift({
      uri: null,
      name: null,
      comment: null
    });
  }
  addRow(row) {
    console.debug("add row", row);
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
    let csvContent = "data:text/csv;charset=utf-8,";
    csvContent += [
      Object.keys(arrData[0]).join(","),
      ...arrData.map(item => Object.values(item).join(","))
    ]
      .join("\n")
      .replace(/(^\[)|(\]$)/gm, "");

    const data = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", data);
    link.setAttribute("download", "factorLevelTemplate.csv");
    link.click();
    link.remove();
  }
}
</script>

<style scoped lang="scss">
@import "~vue-tabulator/dist/scss/bootstrap/tabulator_bootstrap4";
</style>
