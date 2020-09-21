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
            <br />
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
      if (
        valueByProperties[relation.property] &&
        !Array.isArray(valueByProperties[relation.property])
      ) {
        valueByProperties[relation.property] = [
          valueByProperties[relation.property]
        ];
      }

      if (Array.isArray(valueByProperties[relation.property])) {
        valueByProperties[relation.property].push(relation.value);
      } else {
        valueByProperties[relation.property] = relation.value;
      }
    }

    return this.$opensilex
      .getService("opensilex.VueJsOntologyExtensionService")
      .getClassProperties(this.selected.type)
      .then(http => {
        let classModel: any = http.response.result;

        this.loadProperties(classModel.dataProperties, valueByProperties);
        this.loadProperties(classModel.objectProperties, valueByProperties);
      });
  }

  loadProperties(properties, valueByProperties) {
    for (let i in properties) {
      let property = properties[i];
      if (valueByProperties[property.property]) {
        if (
          property.isList &&
          !Array.isArray(valueByProperties[property.property])
        ) {
          this.typeProperties.push({
            definition: property,
            property: [valueByProperties[property.property]]
          });
        } else {
          this.typeProperties.push({
            definition: property,
            property: valueByProperties[property.property]
          });
        }
      } else if (property.isList) {
        this.typeProperties.push({
          definition: property,
          property: []
        });
      }
    }
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
