<template>
  <vl-layer-vector>
    <vl-source-vector ref="vectorSourceArea">
      <vl-feature
          v-for="feature in featuresArea"
          :key="feature.id"
          :properties="feature.properties"
      >
        <vl-geom-polygon :coordinates="feature.geometry.coordinates"/>
      </vl-feature>
    </vl-source-vector>
    <vl-style-box>
      <vl-style-stroke color="green"></vl-style-stroke>
      <vl-style-fill color="rgba(200,255,200,0.4)"></vl-style-fill>
    </vl-style-box>
  </vl-layer-vector>
</template>
<script lang="ts">
export default {
  props: {
    featuresArea: {}
  }
}
</script>
