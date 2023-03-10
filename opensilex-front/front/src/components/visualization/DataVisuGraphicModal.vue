<template>
  <b-modal
      ref="graphic-modal"
      size="xl"
      title="Very large model"
  >
    <opensilex-DataVisuGraphic
        v-if="isGraphicLoaded"
        id="graphic"
        ref="visuGraphic"
        :deviceType="false"
        :lType="true"
        :lWidth="2"
        class="DeviceVisualisationGraphic"
        v-bind:class ="{
            'DeviceVisualisationGraphic': false,
            'DeviceVisualisationGraphicWithoutForm': true
          }"
    ></opensilex-DataVisuGraphic>
  </b-modal>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DataVisuGraphicModal extends Vue {

}
</script>

<style scoped>

</style>