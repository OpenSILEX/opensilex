<template>
  <div v-if="item">
    <div v-if="item.properties.nature === 'Area'">
      <opensilex-AreaDetails
          :showName="showName"
          :uri="item.properties.uri"
          :withBasicProperties="false"
      />
    </div>
    <div v-if="item.properties.nature === 'Device'">
      <opensilex-DeviceDetailsMap
          :selected="item.properties.details"
          :showName="showName"
      />
    </div>
    <div v-if="(item.properties.nature === 'ScientificObjects') & detailsSO">
      <template v-if="withBasicProperties">
        <opensilex-ScientificObjectDetailMap
            v-if="item.properties.OS"
            :experiment="experiment"
            :selected="item.properties.OS"
            :withBasicProperties="true"
        ></opensilex-ScientificObjectDetailMap>
      </template>
      <template v-else>
        <opensilex-ScientificObjectDetailProperties
            v-if="item.properties.OS"
            :experiment="experiment"
            :selected="item.properties.OS"
            :withBasicProperties="false"
        ></opensilex-ScientificObjectDetailProperties>
      </template>
    </div>
  </div>
</template>
<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DisplayInformationAboutItem extends Vue {
  $opensilex: any;

  @Prop()
  item;

  @Prop()
  detailsSO;

  @Prop()
  experiment;

  @Prop()
  withBasicProperties;

  @Prop({
    default: false,
  })
  showName;
}
</script>
<style lang="scss" scoped>
p {
  font-size: 115%;
  margin-top: 1em;
}
</style>