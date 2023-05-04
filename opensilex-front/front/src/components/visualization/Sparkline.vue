<template>
  <div class="wrapper">
    <div class="container">
      <div class="canvas-container">
        <canvas ref="canvas" id="sparkline" ></canvas>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {DataSerieGetDTO} from "opensilex-core/model/dataSerieGetDTO";
import {DataSimpleGetDTO} from "opensilex-core/model/dataSimpleGetDTO";

@Component
export default class Sparkline extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Prop({
    default: 300
  })
  maxWidth: number;

  @Prop({
    default: 50
  })
  maxHeight: number;

  @Prop({
    default: []
  })
  dataSerie: Array<DataSimpleGetDTO>;

  @Prop({
    default: false
  })
  simplify: boolean;

  data: Array<number>;

  @Ref("canvas") readonly canvas!: any;

  mounted() {
    this.loadData();
  }

  loadData() {
    this.data = this.dataSerie.map(data => {
      return parseFloat(data.value);
    });

    console.debug(this.data.length);

    if (this.simplify) {
      this.data = this.DouglasPeucker(this.data, 0.01);
    }

    console.debug(this.data.length);

    this.draw();
  }

  perpendicularDistance(p1, p2, p) {
    let top = Math.abs((p2.x - p1.x)*(p1.y - p.y) - (p1.x - p.x)*(p2.y - p1.y));
    let bottom = Math.sqrt((p2.x - p1.x)**2 + (p2.y - p1.y)**2);
    return top / bottom;
  }

  DouglasPeucker(dataList: Array<number>, epsilon) : Array<number> {
    var dmax = 0;
    var index = 0;
    var end = dataList.length;

    for (let i = 2; i < end - 1; ++i) {
      let d = this.perpendicularDistance(
          {x:0, y:dataList[0]},
          {x:end-1, y:dataList[end-1]},
          {x:i, y:dataList[i]});

      if (d > dmax) {
        index = i;
        dmax = d;
      }
    }

    var resultList = [];

    if (dmax > epsilon) {
      var recResult1 = this.DouglasPeucker(dataList.slice(0, index), epsilon);
      var recResult2 = this.DouglasPeucker(dataList.slice(index+1, end), epsilon);

      resultList = recResult1.concat(recResult2);
    }
    else {
      resultList = dataList.slice(0, end);
    }

    return resultList;
  }

  draw() {
    var ctx = this.canvas.getContext('2d');
    var origW = this.canvas.width;
    var origH = this.canvas.height;
    this.canvas.width = this.canvas.width * window.devicePixelRatio;
    this.canvas.height = this.canvas.height * window.devicePixelRatio;
    this.canvas.style.maxWidth = this.maxWidth + 'px';
    this.canvas.style.maxHeight = this.maxHeight + 'px';
    var maxNum = Math.max.apply(null, this.data);
    var minNum = Math.min.apply(null, this.data);
    var linePerPixel = this.canvas.width/this.data.length;
    var diff = maxNum-minNum;
    var diffPerc = this.canvas.height / diff;
    diffPerc -= 1.5;
    if(diff < this.canvas.height) diffPerc = 1;

    // add some padding;
    var bottomPadding = 4;
    ctx.beginPath();
    ctx.strokeStyle = 'rgb(78,141,235)';
    ctx.lineWidth = 3;
    ctx.moveTo(0, origH - (this.data[0] * diffPerc));
    this.data.forEach(function(v,i){
      ctx.lineTo(Math.round(i*linePerPixel), origH - (v*diffPerc) - bottomPadding);
    });
    ctx.stroke();
  }

}
</script>

<style scoped lang="scss">

.wrapper {
  display: table;
  padding: 0;
  width: 100%;
  height: 100%;
  position: absolute;
}

.container {
  display: table-cell;
  vertical-align: middle;
}

.canvas-container {
  position: relative;
  max-width: 1024px;
  min-width: 100px;
  margin: 0 auto;
}

canvas {
  width: 100%;
  height: auto;
  z-index: 0;
}

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
