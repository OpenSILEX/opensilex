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
import {Point} from "jspdf";

@Component
export default class Sparkline extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Prop({
    default: 300
  })
  maxWidth: number;

  @Prop({
    default: 100
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

    if (this.simplify) {
      console.debug(this.data.length);
      let points: Array<Point>;
      points = this.dataSerie.map((data, i) => {
        return {x:i, y:parseFloat(data.value)};
      });

      points = this.RDP(points, 50);

      this.data = points.map((p) => {
        return p.y;
      });
      console.debug(this.data.length);
    }

    this.draw();
  }

  /**
   * @param {!Array<pointType>} l
   * @param {number} eps
   */
  RDP (l, eps) {
    const last = l.length - 1;
    const p1 = l[0];
    const p2 = l[last];
    const x21 = p2.x - p1.x;
    const y21 = p2.y - p1.y;

    const [dMax, x] = l.slice(1, last)
        .map(p => Math.abs(y21 * p.x - x21 * p.y + p2.x * p1.y - p2.y * p1.x))
        .reduce((p, c, i) => {
          const v = Math.max(p[0], c);
          return [v, v === p[0] ? p[1] : i + 1];
        }, [-1, 0]);

    if (dMax > eps) {
      return [...this.RDP(l.slice(0, x + 1), eps), ...this.RDP(l.slice(x), eps).slice(1)];
    }
    return [l[0], l[last]];
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
    var linePerPixel = origW / this.data.length;
    var diff = maxNum-minNum;
    var diffPerc = origH / diff;
    diffPerc -= 1.5;

    // add some padding;
    var bottomPadding = 4;
    ctx.beginPath();
    ctx.strokeStyle = 'rgb(78,141,235)';
    ctx.lineWidth = 6;
    ctx.moveTo(0, origH - ((this.data[0] - minNum) * diffPerc));
    this.data.forEach(function(v,i){
      ctx.lineTo((i+1)*linePerPixel, origH - ((v - minNum) * diffPerc));
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
  height: 100%;
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
