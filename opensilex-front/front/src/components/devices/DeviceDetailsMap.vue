<template>
  <div class="container-fluid">
<!--Name and type -->
  <div v-if="showName">
    <opensilex-StringView
        label="component.common.name"
    ></opensilex-StringView>
    <opensilex-UriLink v-if="showName"
                       :to="{ path: '/device/details/' + encodeURIComponent(selected.uri)}"
                       :uri="selected.uri"
                       :value="selected.name + ' (' + selected.rdf_type_name.bold() + ')'"
                       target="_blank"
    ></opensilex-UriLink>
    <p>
      <a id="show" v-on:click="isViewAllInformation = !isViewAllInformation">{{
          $t(
              isViewAllInformation
                  ? "ScientificObjectDetailMap.seeMoreInformation"
                  : "ScientificObjectDetailMap.viewAllInformation"
          )
        }}</a>
    </p>
  </div>
<!-- start_up and removal-->
    <div v-if=" isViewAllInformation || !showName">
      <opensilex-StringView
          v-if="selected.start_up"
          :value="selected.start_up"
          label="device.start_up"
      ></opensilex-StringView>
      <opensilex-StringView
          v-if="selected.removal"
          :value="selected.removal"
          label="device.removal"
      ></opensilex-StringView>
    </div>
  </div>
</template>

<script lang="ts">

import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DeviceDetailsMap extends Vue {
  @Prop()
  selected;

  @Prop()
  showName;

  isViewAllInformation: boolean = false;
}
</script>

<style scoped>

#show {
  color: #007bff;
  cursor: pointer;
}

</style>
<i18n>
en:
  device:
    start_up: Start up
    removal: Removal

fr:
  device:
    start_up: Date de mise en service
    removal: Date de mise hors service
</i18n>