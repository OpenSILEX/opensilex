<template>
  <div v-if="selected && selected.uri">
    <div v-for="(v, index) in typeProperties" v-bind:key="index">
      <div v-if="!v.definition.isList" class="static-field">
        <span class="field-view-title">{{ v.definition.name }}</span>
        <component
          :is="v.definition.view_component"
          :value="v.property"
        ></component>
      </div>
      <div v-else-if="v.property && v.property.length > 0" class="static-field">
        <span class="field-view-title">{{ v.definition.name }}</span>
        <ul>
          <br />
          <li v-for="(prop, propIndex) in v.property" v-bind:key="propIndex">
            <component
              :is="v.definition.view_component"
              :value="prop"
            ></component>
          </li>
        </ul>
      </div>
    </div>

    <!-- Geometry -->
    <opensilex-GeometryCopy
      v-if="selected.geometry"
      :value="selected.geometry"
    ></opensilex-GeometryCopy>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ScientificObjectDetailAdvanced extends Vue {
  $opensilex: any;

  @Prop()
  selected;

  @Prop()
  typeProperties;
}
</script>