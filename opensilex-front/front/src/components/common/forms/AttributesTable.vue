<template>
  <b-row>
    <b-col>
      <h6 class="mb-3">
        <strong>{{$t('AttributesTable.title')}}</strong>
      </h6>
      <b-row class="ml-2">
        <b-col md="4">
          <opensilex-AddChildButton
            class="mr-2 addChildButton"
            @click="addEmptyRow"
            label="AttributesTable.add"
          ></opensilex-AddChildButton>
          <span>{{$t('AttributesTable.add')}}</span>
        </b-col>
      </b-row>
      <b-row>
        <b-col cols="10">
          <div ref="table" class="tab"> </div>
        </b-col>
      </b-row>
    </b-col>
  </b-row>
</template>


<script lang="ts">

type Attribute = {
  attribute: string,
  value: string
}

import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {CellComponent, ColumnDefinition, TabulatorFull as Tabulator} from 'tabulator-tables';

// @ts-ignore

@Component
export default class AttributesTable extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;

  @Ref("langInput") readonly langInput!: any;
  @Ref("table") readonly table!: any;

  @Prop()
  editMode: boolean;

  @Prop()
  attributesArray: Array<Attribute>;

  private tabulatorData: Array<Attribute> = [];

  // defined as computed to set titles reactive to lang switch
  private get tableColumns(): ColumnDefinition[] {
    return [
      {
        title: this.$t("AttributesTable.attribute") + '<span class="required">*</span>',
        field: "attribute",
        editor: "input",
        validator: ["required", "unique"],
        widthGrow: 0.5,
      },
      {
        title: this.$t("AttributesTable.value").toString(),
        field: "value",
        editor: "input",
        validator: ["required", "unique"],
        widthGrow: 0.5
      },
      {
        title: this.$t("AttributesTable.delete").toString(),
        field: "actions",
        widthGrow: 0.3,
        formatter: function (cell, formatterParams, onRendered) {
          return '<span style="color:red"><!----><svg   aria-hidden="true" focusable="false" data-prefix="fas" data-icon="trash-alt" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" class="svg-inline--fa fa-trash-alt fa-w-14 fa-sm"><path data-v-0514f944="" fill="currentColor" d="M32 464a48 48 0 0 0 48 48h288a48 48 0 0 0 48-48V128H32zm272-256a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zM432 32H312l-9.4-18.7A24 24 0 0 0 281.1 0H166.8a23.72 23.72 0 0 0-21.4 13.3L136 32H16A16 16 0 0 0 0 48v32a16 16 0 0 0 16 16h416a16 16 0 0 0 16-16V48a16 16 0 0 0-16-16z" class=""></path></svg></span>';
        }
      }
    ]
  }

  private tabulator: Tabulator= null;

  private langUnwatcher;

  @Watch("tabulatorData")
  newData(value: Array<Attribute>) {
    this.tabulator.replaceData(value);
  }
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      () => {
        //change columns translation on lang change
        this.instanciateTabulator()
      }
    );
    this.tabulatorData = this.attributesArray;
    this.instanciateTabulator()
  }


  beforeDestroy() {
    this.langUnwatcher();
  }

  public resetTable() {
      this.tabulatorData = this.attributesArray ?? [];
      this.instanciateTabulator()
  }

  addEmptyRow() {
    let emptyRows = this.tabulatorData.filter( rowData => {
      return rowData.attribute === undefined;
    });
    if (emptyRows.length === 0) {
      this.tabulatorData = this.tabulatorData.concat([{attribute: undefined, value: undefined}])
    }
  }

  removeRow(attribute: string) {
      this.tabulatorData = this.tabulatorData.filter( rowData => {
        return rowData.attribute !== attribute;
      });
  }

  pushAttributes(): { [key: string]: string; } {
    let attributes = {};
    
    let data = this.tabulatorData;
    for (let y = 0; y < data.length; y++) {
      
      if (data[y].attribute !== null) {
        let key = data[y].attribute;
        attributes[key] = data[y].value;
      }
      
    }
    return attributes;
  }

  /**
   * Read properties/value from metadata and create an object {attribute: string, value: string} into attributes
   * @param metadata input metadata
   * @param attributes output metadata. Attributes is reset and updated inside this function
   */
  static readAttributes(metadata: { [key: string]: string; } ): Array<{ attribute: string, value: string }> {

    let attributesArray = [];
    if (metadata) {
      for (const property in metadata) {
        let att = {
          attribute: property,
          value: metadata[property]
        }
        attributesArray.push(att);
      }
    }

    return attributesArray;
  }

  private instanciateTabulator() {
    this.tabulator = new Tabulator(this.table, {
      data: this.tabulatorData, //link data to table
      reactiveData: true, //enable data reactivity
      columns: this.tableColumns, //define table columns
      layout: "fitColumns",
      layoutColumnsOnNewData: true,
      index: "attribute",
    });

    this.tabulator.on("cellClick", (e, cell) => {
      if (cell.getField() === "actions") {
        this.removeRow(cell.getRow().getData().attribute);
      }
    });
  }

}
</script>

<style lang="scss">
.tabulator-row > .tabulator-cell {
  text-align: center;}
</style>

<i18n>

en:
  AttributesTable:
    title: Additional attributes
    add: Add an attribute
    attribute: Attribute
    value: Value
    delete: Delete
    

fr:
  AttributesTable:
    title: Attributs supplémentaires
    add: Ajouter un attribut
    attribute: Attribut
    value: Valeur
    delete: Supprimer

</i18n>
