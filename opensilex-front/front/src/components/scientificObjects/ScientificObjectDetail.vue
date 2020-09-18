<template>
  <b-card v-if="selected && selected.uri">
    <template v-slot:header>
      <h3>
        <!-- <opensilex-Icon icon="ik#ik-clipboard" /> -->
        {{$t("ScientificObjectDetail.title")}}
      </h3>
    </template>
    <div>
      <!-- URI -->
      <opensilex-UriView :uri="selected.uri"></opensilex-UriView>
      <!-- Name -->
      <opensilex-StringView label="component.common.name" :value="selected.name"></opensilex-StringView>
      <!-- Type -->
      <opensilex-TypeView :type="selected.type" :typeLabel="selected.typeLabel"></opensilex-TypeView>

      <div v-for="(v, index) in typeProperties" v-bind:key="index">
        <div class="static-field">
          <span class="field-view-title">{{v.definition.name}}:</span>
          <component
            v-if="!v.definition.isList"
            :is="v.definition.viewComponent"
            :value="v.property"
          ></component>
          <ul v-else>
            <li v-for="(prop, propIndex) in v.property" v-bind:key="propIndex">
              <component :is="v.definition.viewComponent" :value="prop"></component>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </b-card>
</template>

<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ScientificObjectDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected;

  typeProperties = [];

  mounted() {
    if (this.selected) {
      this.onSelectionChange();
    }
  }

  @Watch("selected")
  onSelectionChange() {
    this.typeProperties = [];

    let valueByProperties = {};

    for (let i in this.selected.relations) {
      let relation = this.selected.relations[i];
      valueByProperties[relation.property] = relation.value;
    }

    return this.$opensilex
      .getService("opensilex.VueJsOntologyExtensionService")
      .getClassProperties(this.selected.type)
      .then(http => {
        let classModel: any = http.response.result;

        for (let i in classModel.dataProperties) {
          let dataProperty = classModel.dataProperties[i];
          if (valueByProperties[dataProperty.property]) {
            this.typeProperties.push({
              definition: dataProperty,
              property: valueByProperties[dataProperty.property]
            });
          }
        }

        for (let i in classModel.objectProperties) {
          let objectProperty = classModel.objectProperties[i];
          if (valueByProperties[objectProperty.property]) {
            this.typeProperties.push({
              definition: objectProperty,
              property: valueByProperties[objectProperty.property]
            });
          }
        }
      });
  }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  ScientificObjectDetail:
    title: Detail

fr:
  ScientificObjectDetail:
    title: Détail
</i18n>
