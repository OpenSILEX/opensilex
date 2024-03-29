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
          <VueTabulator
            ref="tabulatorRef"
            class="table-light table-bordered"
            v-model="attributesArray"
            :options="options"
            @cell-click="removeRow"
          />
        </b-col>
      </b-row>
    </b-col>
  </b-row>
</template>


<script lang="ts">
import { Component, Prop, Ref, PropSync } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore

@Component
export default class AttributesTable extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;
  langs: any = {
    fr: {
      //French language definition
      columns: {
        attribute: 'Attribut<span class="required">*</span>',
        value: "Valeur",
        actions: "Supprimer",
      }    
    },
    en: {
      columns: {
        attribute: 'Attribute<span class="required">*</span>',
        value: "Value",
        actions: "Delete",
      }
    },
  };

  @Ref("tabulatorRef") readonly tabulatorRef!: any;
  @Ref("langInput") readonly langInput!: any;

  @Prop()
  editMode: boolean;

  @Prop()
  attributesArray;

  tableColumns: any[] = [
    {
      title: 'attribute<span class="required">*</span>',
      field: "attribute",
      formater: "string",
      editor: "input",
      validator: ["required", "unique"],
      widthGrow: 0.5,
    },
    {
      title: "Value",
      field: "value",
      formater: "string",
      editor: "input",
      validator: ["required", "unique"],
      widthGrow: 0.5
    },
    {
      title: "Delete",
      field: "actions",
      widthGrow: 0.3,
      formatter: function(cell, formatterParams, onRendered) {
          return '<span style="color:red"><!----><svg   aria-hidden="true" focusable="false" data-prefix="fas" data-icon="trash-alt" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" class="svg-inline--fa fa-trash-alt fa-w-14 fa-sm"><path data-v-0514f944="" fill="currentColor" d="M32 464a48 48 0 0 0 48 48h288a48 48 0 0 0 48-48V128H32zm272-256a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zM432 32H312l-9.4-18.7A24 24 0 0 0 281.1 0H166.8a23.72 23.72 0 0 0-21.4 13.3L136 32H16A16 16 0 0 0 0 48v32a16 16 0 0 0 16 16h416a16 16 0 0 0 16-16V48a16 16 0 0 0-16-16z" class=""></path></svg></span>';
      }
    }
  ];
  
  created() {
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

  options: any = {
    layout: "fitColumns",
    cellHozAlign: "center",
    clipboard: true,
    columns: this.tableColumns,
    maxHeight: "100%", //do not let table get bigger than the height of its parent element
    langs: this.langs
  };

  resetTable() {
    this.attributesArray = [];
  }

  addEmptyRow() {
    console.debug("add row");
    let tabulatorInstance = this.tabulatorRef.getInstance();
    tabulatorInstance.addRow({
      attribute: null,
      value: null
    });
  }

  addRow(row) {
    console.debug("add row", row);
    if (row.attribute != undefined && row.attribute != null && row.attribute != "") {
      this.attributesArray.unshift(row);
    }
  }

  removeRow(evt, clickedCell) {
    let columnName = clickedCell.getField();
    console.debug(columnName);

    if (columnName == "actions") {
      let row = clickedCell.getRow();
      row.delete();
    }
  }

  pushAttributes(): { [key: string]: string; } {
    let attributes = {};
    
    let data = this.tabulatorRef.getInstance().getData();
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
  static readAttributes(
      metadata: { [key: string]: string; },
      attributes: Array<{ attribute: string, value: string }>) {

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

    // use splice() and push() method to ensure Vue reactivity
    attributes.splice(0);
    attributes.push(... attributesArray);
  }

  changeTableLang(lang: string) {
    let tabulatorInstance = this.tabulatorRef.getInstance();
    tabulatorInstance.setLocale(lang);
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  AttributesTable:
    title: Additional attributes
    add: Add an attribute
    attribute: Attribute
    

fr:
  AttributesTable:
    title: Attributs supplémentaires
    add: Ajouter un attribut
    attribute: Attribut

</i18n>
