<template>
  <div>
    <opensilex-VisuImageGrid
        ref="visuImageGrid"
        :key="key"
        :images="images"
        @imageIsHovered=" $emit('imageIsHovered', $event)"
        @imageIsUnHovered=" $emit('imageIsUnHovered', $event)"
        @imageIsDeleted="$emit('imageIsDeleted', $event)"
        @onImageAnnotate="$emit('onImageAnnotate', $event)"
        @onImageDetails="$emit('onImageDetails', $event)"
        @onAnnotationDetails="$emit('onAnnotationDetails', $event)"
        @imageIsClicked="onImageIsClicked"
    ></opensilex-VisuImageGrid>
    <vue-easy-lightbox
        :visible="show"
        :imgs="images"
        :index="slide"
        @hide="handleHide"
    ></vue-easy-lightbox>

    <opensilex-DataProvenanceModalView
        ref="dataProvenanceModalView"
        :datafile="true"
    ></opensilex-DataProvenanceModalView>

    <b-modal :title="$t('Annotation.image')" id="modal-center" v-model="showAnnotationsModal" hide-footer centered
             no-fade size="lg">
      <b-card>
        <b-list-group>
          <b-list-group-item
              v-for="(annotation, index) in annotations"
              href="#"
              class="flex-column align-items-start"
              v-bind:key="index"
          >
            <div class="d-flex w-100 justify-content-between">
              <h5 class="mb-1">{{ annotation.motivation.name }}</h5>
              <small class="text-muted">{{ annotation.created }}</small>
            </div>

            <p class="mb-1">{{ annotation.description }}</p>

            <small class="text-muted">{{ annotation.author }}</small>
          </b-list-group-item>
        </b-list-group>
      </b-card>
    </b-modal>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {AnnotationGetDTO} from "opensilex-core/model/annotationGetDTO";
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {ProvenanceGetDTO} from "opensilex-core/model/provenanceGetDTO";
import {DataFileGetDTO} from "opensilex-core/model/dataFileGetDTO";
import {DataService} from "opensilex-core/api/data.service";
import {data} from "browserslist";

@Component
export default class VisuImages extends Vue {
  $opensilex: any;
  annotationService: AnnotationsService;
  dataService: DataService;
  key = 0; // fix VisuImageGrid reload
  images = [];
  imagesUri = [];
  image: any;
  imageData: any;
  slide = 0;
  show: boolean = false;
  showAnnotationsModal: boolean = false;
  showImages: boolean = true;
  objectType: string = "";
  objectUri: string = "";
  formatedDateValue: string = "";
  annotations: Array<AnnotationGetDTO> = [];

  @Ref("visuImageGrid") readonly visuImageGrid!: any;
  @Ref("dataProvenanceModalView") readonly dataProvenanceModalView!: any;

  onImagePointMouseEnter(toSend) {
    this.visuImageGrid.onImagePointMouseEnter(toSend);
  }

  onImagePointMouseOut() {
    this.visuImageGrid.onImagePointMouseOut();
  }

  onImagePointClick(toReturn) {
    this.visuImageGrid.onImagePointClick(toReturn);
  }

  onImageIsDeleted(index) {
    this.deleteImage(index);
    this.imagesUri.splice(index, 1)
  }

  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    this.annotationService = this.$opensilex.getService("opensilex.AnnotationsService");
  }

  getAnnotations(uri: string) {
    return this.annotationService
        .searchAnnotations(undefined, uri, undefined, undefined, undefined, 0, 0)
        .then(
            (http: HttpResponse<OpenSilexResponse<Array<AnnotationGetDTO>>>) => {
              const annotations = http.response.result as Array<AnnotationGetDTO>;
              this.annotations = annotations;
            }
        );
  }

  showAnnotations(dataUri) {
    this.getAnnotations(dataUri);
  }

  getProvenance(uri) {
    if (uri != undefined && uri != null) {
      return this.dataService
          .getProvenance(uri)
          .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
            return http.response.result;
          });
    }
  }

  getDataFileDescription(uri) {
    if (uri != undefined && uri != null) {
      return this.dataService
          .getDataFileDescription(uri)
          .then((http: HttpResponse<OpenSilexResponse<DataFileGetDTO>>) => {
            return http.response.result;
          });
    }
  }

  showDataProvenanceDetailsModal(item) {
    return this.getDataFileDescription(item)
        .then(imageData => {
          this.imageData = imageData
          return this.getProvenance(imageData.provenance.uri)
        }).then(response => {
          let value = {
            provenance: response,
            data: this.imageData
          }

          this.dataProvenanceModalView.setProvenance(value);
          this.dataProvenanceModalView.show();
        });
  }


  onImageDetails(index) {
    this.showDataProvenanceDetailsModal(index);
  }

  onAnnotationDetails(index) {
    this.showAnnotationsModal = true;
    this.showAnnotations(index);
  }

  onImageIsClicked(index) {
    this.slide = index;
    this.image = this.images[index].url;
    this.show = true;

  }

  handleHide() {
    this.show = false
  }

  addImage(image) {
    if (this.images && this.imagesUri.includes(image.imageUri)) {
      return null;
    } else {
      this.images.push(image);
      this.imagesUri.push(image.imageUri);

    }

  }

  deleteImage(index) {
    let newArray = JSON.parse(JSON.stringify(this.images));
    newArray.splice(index, 1);
    this.images = newArray;
    this.key++;
  }

  getObjectType(image) {
    return image.type.split(":")[1];
  }

}
</script>


<style scoped>
div >>> .carousel-control-prev-icon,
div >>> .carousel-control-next-icon {
  width: 80px;
  height: 80px;
}

div >>> .modal-body {
  padding: 0 !important;
}

.carousel-inner img {
  margin: auto;
}

@media (min-width: 576px) {
  .modal-dialog-centered {
    min-height: calc(100% - 3.5rem);
  }
}

@media (min-width: 576px) {
  div >>> .modal-sm {
    max-height: 500px;
    max-width: 500px;
  }
}

.carousel-item:after {
  content: "";
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 40%;
  display: inline-block;
  background: -moz-linear-gradient(
      top,
      rgba(0, 0, 0, 0) 0%,
      rgba(0, 0, 0, 0.65) 100%
  ); /* FF3.6+ */
  background: -webkit-gradient(
      linear,
      left top,
      left bottom,
      color-stop(0%, rgba(0, 0, 0, 0.65)),
      color-stop(100%, rgba(0, 0, 0, 0))
  ); /* Chrome,Safari4+ */
  background: -webkit-linear-gradient(
      top,
      rgba(0, 0, 0, 0) 0%,
      rgba(0, 0, 0, 0.65) 100%
  ); /* Chrome10+,Safari5.1+ */
  background: -o-linear-gradient(
      top,
      rgba(0, 0, 0, 0) 0%,
      rgba(0, 0, 0, 0.65) 100%
  ); /* Opera 11.10+ */
  background: -ms-linear-gradient(
      top,
      rgba(0, 0, 0, 0) 0%,
      rgba(0, 0, 0, 0.65) 100%
  ); /* IE10+ */
  background: linear-gradient(
      to bottom,
      rgba(0, 0, 0, 0) 0%,
      rgba(0, 0, 0, 0.65) 100%
  ); /* W3C */
  filter: progid:DXImageTransform.Microsoft.gradient(startColorstr='#a6000000', endColorstr='#00000000', GradientType=0); /* IE6-9 */
}

p {
  margin-bottom: 0px;
}

.row {
  margin-left: 0px;
}

.images {
  margin: 0;
  padding: 0;
  white-space: nowrap;
  width: 100%;

  overflow-x: auto;
}
</style>
