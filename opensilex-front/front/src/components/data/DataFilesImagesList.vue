<template>
  <div>
    <div v-if="showedImages === 0">No images to dispay</div>

    <opensilex-ImageGrid
      :images="images"
      @click="onImageClicked"
      @annotate="onImageAnnotate"
    ></opensilex-ImageGrid>

    <opensilex-ImageLightBox
      :key="key"
      ref="ImageLightBox"
      :images="images"
    ></opensilex-ImageLightBox>

    <strong v-if="showedImages >= 50 && !showScrollSpinner"
      >Scroll to see more images</strong
    >

    <opensilex-AnnotationModalForm
      ref="annotationModalForm"
    ></opensilex-AnnotationModalForm>

    <div v-if="showScrollSpinner" class="d-flex align-items-center">
      <strong>Loading...</strong>
      <div
        class="spinner-border ml-auto"
        role="status"
        aria-hidden="true"
      ></div>
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import { DataFileGetDTO } from "../../../../../opensilex-core/front/src/lib";
import HttpResponse from "../../lib/HttpResponse";
import { OpenSilexResponse } from "../../../../../opensilex-core/front/src/lib/HttpResponse";

@Component
export default class DataFilesImagesList extends Vue {
  $opensilex: any;
  service: any;

  currentPage: number = 0;
  pageSize = 30;

  images = [];
  imagesURL = [];

  showedImages: number = 0;

  showScrollSpinner: boolean = false;
  canReload: boolean = true;
  paused = false;
  key = 0; // fix VisuImageGrid reload

  @Ref("ImageLightBox") readonly imageLightBox!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;

  @Prop({
    default: () => {
      return {
        start_date: undefined,
        end_date: undefined,
        rdf_type: undefined,
        provenance: undefined,
        experiments: [],
        scientificObjects: [],
      };
    },
  })
  filter: any;

  created() {
    this.initScroll();
    this.service = this.$opensilex.getService("opensilex.DataService");
    this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter);
    this.loadImages();
  }

  onImageClicked(index) {
    this.imageLightBox.openOnIndex(index);
  }

  onImageAnnotate(target) {
    this.annotationModalForm.showCreateForm([target]);
  }

  initScroll() {
    window.addEventListener("scroll", this.pageScroll);
  }

  pageScroll(event) {
    let bottomOfWindow =
      window.innerHeight + window.scrollY >= document.body.offsetHeight - 1;
    if (bottomOfWindow && !this.paused) {
      if (this.canReload) {
        this.reload();
      }
      this.paused = true;
    }
  }

  destroyed() {
    window.removeEventListener("scroll", this.pageScroll);
  }

  refresh() {
    this.showScrollSpinner = true;
    this.currentPage = 0;
    this.loadImages();
  }

  reload() {
    this.showScrollSpinner = true;
    this.currentPage++;
    this.loadImages();
  }

  loadImages() {
    return new Promise((resolve, reject) => {
      this.service
        .getDataFileDescriptionsByTargets(
          this.filter.rdf_type
            ? this.filter.rdf_type
            : this.$opensilex.Oeso.IMAGE_TYPE_URI,
          this.filter.start_date, // start_date
          this.filter.end_date, // end_date
          undefined, // timezone,
          this.filter.experiments, // experiments
          this.filter.devices, //devices
          this.filter.provenance ? [this.filter.provenance] : undefined, // provenanceprovenances
          undefined, // metadata
          ["date=desc"], // order_by
          this.currentPage,
          this.pageSize,
          this.filter.scientificObjects // scientific_object
        )
        .then(
          (http: HttpResponse<OpenSilexResponse<Array<DataFileGetDTO>>>) => {
            const result = http.response.result as Array<DataFileGetDTO>;
            let i = 0;
            result.forEach((element) => {
              if (this.showScrollSpinner) {
                this.showScrollSpinner = false;
              }
              if(!element.archive){
                 let path =
                "/core/datafiles/" +
                encodeURIComponent(element.uri) +
                "/thumbnail?scaled_width=1250&scaled_height=640";
              let promise = this.$opensilex.viewImageFromGetService(path);
              promise
                .then((url) => {
                 
                  const image = {
                    url: url,
                    uri: element.uri,
                    type: element.rdf_type,
                    target: element.target,
                    date: element.date,
                    provenance: element.provenance, 
                    filename: element.filename

                  };
                  this.images.push(image);
                  this.showedImages = this.images.length;
                  i++;

                  if (i === result.length) {
                    this.paused = false;
                    this.showScrollSpinner = false;
                  }

                  this.key++;
                
                })
                .catch((error) => {
                  console.log("error");
                  i++;
                  if (i === result.length) {
                    this.paused = false;
                    this.showScrollSpinner = false;
                    this.key++;
                  }
                  reject(error);
                });

              }

             
            });

            if (!result || result.length == 0) {
              this.showScrollSpinner = false;
            }

            resolve(result);
          }
        )
        .catch((error) => {
          console.log(error);
          this.canReload = false;
          this.showScrollSpinner = false;
          reject(error);
        });
    });
  }
}
</script>







<style scoped lang="scss">
</style>