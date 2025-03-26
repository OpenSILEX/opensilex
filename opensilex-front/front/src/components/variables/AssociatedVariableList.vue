<template>
  <opensilex-Card label="AssociatedVariablesList.relatedVariables" icon="ik#ik-layers">
    <template v-slot:body>
      <opensilex-TableView
          ref="tableRef"
          :globalFilterField="true"
          :items="variableList"
          :fields="fields"
          defaulStortBy="name"
          :defaultPageSize="5"
      >
        <template v-slot:cell(name)="{data}">
          <opensilex-UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :to="{path:'/variable/details/'+encodeURIComponent(data.item.uri)}"
            ></opensilex-UriLink>
        </template>
      </opensilex-TableView>

    </template>
  </opensilex-Card>

</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from 'vue';
import {NamedResourceDTOVariableModel} from "opensilex-core/model/namedResourceDTOVariableModel";

@Component({})
export default class AssociatedVariableList extends Vue {
  $opensilex: any;
  $i18n: any;
  $store: any;
  @Ref("tableRef")readonly tableRef!:any;

  @Prop()
  variableList: Array<NamedResourceDTOVariableModel>;

  fields = [
    {
      key: "name",
      label:"component.common.name"
    }
  ];
}
</script>

<style scoped lang="scss">
.variables-list {
  text-overflow: ellipsis;
  overflow:hidden;
  white-space: nowrap;
  display: inline-block;
  max-width: 150px;
}
</style>

<i18n>
en:
  AssociatedVariablesList:
    variablesNameFilter: Search on variables name
    relatedVariables: Related Variables

fr:
  AssociatedVariablesList:
    variablesNameFilter: Chercher sur le nom de variable
    relatedVariables: variables connexes

</i18n>