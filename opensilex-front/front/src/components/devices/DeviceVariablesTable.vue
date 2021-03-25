<template>
  <b-row>
    <b-col>
      <br/>
      <h6 class="mb-3">
        <strong>{{$t('DeviceVariablesTable.title')}}</strong>
      </h6>
      <b-row>
       <b-col md="4">
          <opensilex-AddChildButton
            class="mr-2"
            @click="addEmptyRow"
            variant="outline-primary"
            label="DeviceVariablesTable.add"
          ></opensilex-AddChildButton>
          <span>{{$t('DeviceVariablesTable.add')}}</span>
        </b-col>
      </b-row>
      <b-row>
        <b-col cols="10">
            <VueTabulator
              ref="tabulatorRef"
              class="table-light table-bordered"
              v-model="variablesArray"
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
import { DevicesService } from "opensilex-core/index";

@Component
export default class DeviceVariablesTable extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;
  $papa: any;
  service: DevicesService;

  @Ref("tabulatorRef") readonly tabulatorRef!: any;

  @Prop()
  editMode: boolean;

  @PropSync("relations", {
    default: () => {
      return [];
    },
  })
  variablesArray: any[];

  get tableColumns(): any[] {
    return [
      {
        title: 'Property',
        field: "property",
        formater: "string",
        editor: "input",
        validator: ["required", "unique"],
        widthGrow: 0.5,
        visible: false
      },
      {
        title: "URI",
        field: "value",
        formater: "string",
        editor: "input",
        widthGrow: 0.5,
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
    this.service = this.$opensilex.getService("opensilex.DevicesService");
  }

  options: any = {
    layout: "fitColumns",
    cellHozAlign: "center",
    clipboard: true,
    columns: this.tableColumns,
    maxHeight: "100%",
    index: 0
  };

  removeRow(evt, clickedCell) {
    let columnName = clickedCell.getField();

    if (columnName == "actions") {
      let row = clickedCell.getRow();
      
      var index = 0
      let val = ""
      while (index < this.variablesArray.length && val != row.value){
        val = this.tableColumns[index].value
        index++
      }
      
      console.log(index);

      Vue.delete(this.variablesArray, index);
      row.delete();
    }
  }
  
  addEmptyRow() {
      Vue.set(this.variablesArray, this.variablesArray.length, {
        property: "vocabulary:measures",
        value: null,
      });
  }

  addRow(row) {
    if (row.value != undefined && row.value != null && row.value != "") {
      Vue.set(this.variablesArray, this.variablesArray.length, row);
    }
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  DeviceVariablesTable:
    title: Variables
    add: Add a variable
    

fr:
  DeviceVariablesTable:
    title: Variables
    add: Ajouter une variable       

</i18n>
