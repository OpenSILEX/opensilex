<template>
  <canvas id="sparkline" :width="width" :height="height"></canvas>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class Sparkline extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Prop({
    default: 150
  })
  width: number;

  @Prop({
    default: 50
  })
  height: number;

  created() {

  }

}
</script>

<style scoped lang="scss">

</style>


<i18n>
fr:
  Sparkline:
    provenanceDetail : Details de la provenance
    dataAnnotation : Annoter la donnée
    scientificObjectAnnotation : Ajouter une annotation à l' objet scientifique
    addEvent : Ajouter un evenement
    scatterPlotView : Mode nuage de points
    chartLineView : Mode courbe
    fullscreen : Plein ecran
    download : Télecharger l'image
    rightClick : click droit sur un point pour ajouter un evénement ou une annotation

en:
  Sparkline:
    provenanceDetail : Provenance detail
    dataAnnotation : Annotate data
    scientificObjectAnnotation : Add scientific object's annotation
    addEvent : Add an event
    scatterPlotView : Scatter plot view
    chartLineView : Chart line view
    fullscreen : Fullscreen
    download : Download image
    rightClick : right click on a point to add event or annotation
</i18n>
