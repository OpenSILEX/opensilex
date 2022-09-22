<template>
  <li class="item" v-if="show">
    <div class="card" style="margin: 4px; max-width: 100px;" :class="{ 'redBorder' : showBorder}">
      <span v-if="annotations.length > 0"
            class="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle">
      </span>
      <div style=" position:relative;">
        <b-dropdown
            size="lg"
            variant="link"
            toggle-class="text-decoration-none"
            no-caret
            dropdown
            style=" position:absolute;  top:2px;right:2px; z-index:40; "
        >
          <template v-slot:button-content>
            <a class="btn btn-icon btn-primary" href="#" role="button">
              <i class="icon ik ik-edit"></i>
            </a>
          </template>
          <b-dropdown-item href="#" @click="imageDetails">{{ $t("VisuImageSingle.details") }}</b-dropdown-item>
          <b-dropdown-item href="#" @click="imageAnnotate">{{
              $i18n.t("VisuImageSingle.annotate-image")
            }}
          </b-dropdown-item>
          <b-dropdown-item href="#" @click.stop="imageDelete">{{
              $i18n.t("VisuImageSingle.hide-image")
            }}
          </b-dropdown-item>
        </b-dropdown>
      </div>

      <b-card-img
          :src="content"
          class="card-img-top"
          @click="imageClicked"
          @mouseover="imageHover"
          @mouseleave="imageUnHovered"
      >
      </b-card-img>

      <div class="card-body">
        <b-card-text>{{ objectType }}</b-card-text>
        <b-card-text>{{ objectUri }}</b-card-text>
        <b-card-text>{{ objectAlias }}</b-card-text>
        <b-card-text>{{ formatedDateValue }}</b-card-text>
      </div>
    </div>
  </li>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import {Image} from "./image";
import {DataService} from "opensilex-core/api/data.service";
import {data} from "browserslist";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {DataFileGetDTO} from "opensilex-core/model/dataFileGetDTO";
import {AnnotationGetDTO} from "opensilex-core/model/annotationGetDTO";
import AnnotationList from "../annotations/list/AnnotationList.vue";
import {AnnotationsService} from "opensilex-core/api/annotations.service";

@Component
export default class VisuImageSingle extends Vue {
  show: boolean = true;
  @Prop()
  image: Image;
  @Prop()
  index: any;


  $opensilex: any;
  $store: any;
  annotationService: AnnotationsService;
  showBorder = false;
  content: string = "";
  objectType: string = "";
  objectUri: string = "";
  objectAlias: string = "";
  formatedDateValue: string = "";
  annotations: Array<AnnotationGetDTO> = [];


  imageHover() {
    this.$emit("imageIsHovered", {
      serie: this.image.serieIndex,
      point: this.image.imageIndex
    });
  }

  imageUnHovered() {
    this.$emit("imageIsUnHovered", {
      serie: this.image.serieIndex,
      point: this.image.imageIndex
    });
  }

  imageClicked() {

    this.$emit("imageIsClicked", this.index);
  }

  imageDelete() {
    this.$emit("imageIsDeleted", this.index);
  }

  imageDetails() {
    this.$emit("onImageDetails", this.image.imageUri);
  }

  imageAnnotate() {
    this.$emit("onImageAnnotate", this.image.imageUri);
  }

  onImagePointMouseEnter(indexes) {
    if (
        this.image.serieIndex === indexes.serieIndex &&
        this.image.imageIndex === indexes.imageIndex
    ) {
      this.showBorder = true;
    }
  }

  onImagePointMouseOut() {
    this.showBorder = false;
  }

  getAnnotations(uri: string) {
    this.annotationService.searchAnnotations(
        undefined,
        uri,
        undefined,
        undefined,
        undefined,
        0,
        0)
        .then((http: HttpResponse<OpenSilexResponse<Array<AnnotationGetDTO>>>) => {
              const annotations = http.response.result as Array<AnnotationGetDTO>;
              this.annotations = annotations
            }
        );
  }


  created() {
    this.content = this.image.src;
    this.annotationService = this.$opensilex.getService("opensilex.AnnotationsService");
    this.formatedDateValue = this.formatedDate(this.image.date);
    this.objectType = this.image.type.split(":")[1];
    this.getAnnotations(this.image.imageUri);
    if (this.image.objectAlias) {
      this.objectAlias = this.image.objectAlias;
    } else {
      this.objectUri = this.image.objectUri;
    }

  }

  mounted() {
    //this.getObjectAlias();
  }

  /*  getObjectAlias() {
    let service: ScientificObjectsService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );
    const result = service
      .getScientificObjectsBySearch(
        1,
        0,
        this.image.objectUri,
        undefined,
        undefined,
        undefined
      )
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ScientificObjectDTO>>>) => {
          const res = http.response.result as any;
          this.image.objectAlias = res.data[0].label;
          this.objectAlias = res.data[0].label;
        }
      )
      .catch(error => {
        console.log(error);
      });
  }
 */
  formatedDate(date) {
    const newDate = new Date(date);
    const options = {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    };
    return newDate.toLocaleDateString("fr-FR", options);
  }

}
</script>

<style scoped lang="scss">
.card .card-body {
  padding: 0;
}

.item {
  display: inline-block;
  width: 100px;
  margin: 0 2px;
}

.card {
  margin-bottom: 5px;
}

p {
  font-size: 9px;
  white-space: normal;
}

.card-text {
  margin-bottom: 0;
}

img {
  width: 100% !important;
  max-width: 400px;
}

.redBorder {
  border: solid 2px orange;
}

.close-icon {
  cursor: pointer;
}

img {
  cursor: zoom-in;
}

/* remove if using in grid system */
</style>
<i18n>
en:
  VisuImageSingle:
    details: Image details
    annotate-image: Annotate image
    hide-image: Hide image
fr:
  VisuImageSingle:
    details: Détails de l'image
    annotate-image: Annoter l'image
    hide-image: Masquer l'image
</i18n>
