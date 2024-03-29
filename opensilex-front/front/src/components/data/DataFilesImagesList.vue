<template>
  <div>
    <div v-if="this.images.length === 0">{{ $t("DataFilesImagesList.no-file") }}</div>

    <button 
      v-if="showGoBackToTopButton"
      @click="goBackToTop()"
      id="goBackToTop"
      class="greenThemeColor ik ik-chevrons-up"
      icon="ik ik-chevrons-up"
      v-b-tooltip.hover.top="$t('DataFilesImagesList.goToTopButton')"
    >    
    </button>

    <!-- Images list -->
    <opensilex-ImageGrid
        :images="images"
        @click="onImageClicked"
        @annotate="onImageAnnotate"
    ></opensilex-ImageGrid>

    <!-- Enlarged image view -->
    <opensilex-ImageLightBox
        :key="key"
        ref="ImageLightBox"
        :images="images"
    ></opensilex-ImageLightBox>

    <strong v-if="this.images.length >= 35 && !showScrollSpinner"
    >{{ $t("DataFilesImagesList.scroll-to-display") }}</strong
    >

    <opensilex-AnnotationModalForm
        ref="annotationModalForm"
    ></opensilex-AnnotationModalForm>

    <div v-if="showScrollSpinner" class="d-flex align-items-center">
      <strong>{{ $t("component.common.loading") }}</strong>
      <div
          class="spinner-border ml-auto"
          role="status"
          aria-hidden="true"
      ></div>
    </div>
  </div>
</template>
<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {DataFileGetDTO} from "../../../../../opensilex-core/front/src/lib";
import HttpResponse from "../../lib/HttpResponse";
import {OpenSilexResponse} from "../../../../../opensilex-core/front/src/lib/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {DataService} from "opensilex-core/api/data.service";
import {DataFileImageDTO} from "./DataFileImageDTO";

@Component
export default class DataFilesImagesList extends Vue {
  $opensilex: OpenSilexVuePlugin;
  service: DataService;

  currentPage: number = 0;
  pageSize = 30;

  // true if sum of browser window + current scroll position >= total height of document body - 1px
  bottomOfWindow: boolean = window.innerHeight + window.scrollY >= document.body.offsetHeight - 1;

  images: Array<DataFileImageDTO> = [];
  imagesURL = [];

  showScrollSpinner: boolean = false;
  showGoBackToTopButton : boolean = false;
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

    if (this.filter.start_date === ""){
      this.filter.start_date = undefined;
    }
    if (this.filter.end_date === ""){
      this.filter.end_date = undefined;
    }
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
    this.toggleButtonDisplay()
    if (this.bottomOfWindow && !this.paused) {
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
        if (this.filter.start_date === ""){
          this.filter.start_date = undefined;
        }
        if (this.filter.end_date === ""){
          this.filter.end_date = undefined;
        }
    this.images = [];
    this.showScrollSpinner = true;
    this.currentPage = 0;
    this.loadImages();
  }

  reload() {
        if (this.filter.start_date === ""){
          this.filter.start_date = undefined;
        }
        if (this.filter.end_date === ""){
          this.filter.end_date = undefined;
        }
    this.showScrollSpinner = true;
    this.currentPage++;
    this.loadImages();
  }

  @Prop({default: 1250})
  scaledWidth: number;

  @Prop({default: 640})
  scaledHeight: number;

  /**
   * Call the thumbnail datafile service and fetch the file which has dto.uri as URI
   * @param dto description of a DataFile
   * @param maxLength maximum number of file, if reached, then the component is re-render
   * @param i current index of image
   * @param reject error callback used in case of error when fetching image from API
   */
  loadImage(dto: DataFileGetDTO, maxLength : number, i: number, reject) {

    if (!dto.archive) {
      let path =
          "/core/datafiles/" +
          encodeURIComponent(dto.uri) +
          "/thumbnail?scaled_width=" + this.scaledWidth + "&scaled_height=" + this.scaledHeight;
      let promise = this.$opensilex.viewImageFromGetService(path);
      promise.then((url) => {

        const image: DataFileImageDTO = {
          url: url,
          uri: dto.uri,
          rdf_type: dto.rdf_type,
          target: dto.target,
          date: dto.date,
          provenance: dto.provenance,
          filename: dto.filename,
          publisher: dto.publisher,
          issued: dto.issued
        };
        this.images.push(image);
        i++;

        if (i === maxLength) {
          this.paused = false;
          this.showScrollSpinner = false;
        }

        this.key++;

      }).catch((error) => {
        i++;
        if (i === maxLength) {
          this.paused = false;
          this.showScrollSpinner = false;
          this.key++;
        }
        reject(error);
      });

    }
  }

  /**
   * Call the {@link DataService#getDataFileDescriptionsByTargets} in order to retrieve each file description corresponding to filter.
   * Then for each file description, fetch the corresponding picture thumbnail and load these pictures into ImageGrid component
   *
   * @see loadImage
   */
  loadImages() {
    return new Promise((resolve, reject) => {
      this.service
          .getDataFileDescriptionsByTargets(
              this.filter.rdf_type
                  ? this.filter.rdf_type
                  : this.$opensilex.Oeso.IMAGE_TYPE_URI,
              this.filter.start_date,
              this.filter.end_date,
              undefined,
              this.filter.experiments,
              this.filter.devices,
              this.filter.provenance ? [this.filter.provenance] : undefined,
              undefined,
              ["date=desc"],
              this.currentPage,
              this.pageSize,
              this.filter.scientificObjects
          )
          .then(
              (http: HttpResponse<OpenSilexResponse<Array<DataFileGetDTO>>>) => {
                const result = http.response.result as Array<DataFileGetDTO>;
                let i = 0;

                if (this.showScrollSpinner) {
                  this.showScrollSpinner = false;
                }
                result.forEach((element) => {
                  this.loadImage(element, result.length, i++, reject);
                });

                if (!result || result.length === 0) {
                  this.showScrollSpinner = false;
                }

                resolve(result);
              }
          )
          .catch((error) => {
            this.$opensilex.errorHandler(error);
            this.canReload = false;
            this.showScrollSpinner = false;
            reject(error);
          });
    });
  }

  toggleButtonDisplay() {
    if (this.bottomOfWindow) {
      this.showGoBackToTopButton = true;
    } else {
      this.showGoBackToTopButton = false;
    }
  }

  goBackToTop() {
    document.body.scrollTop = 0; // For Safari
    document.documentElement.scrollTop = 0;
  }
}
</script>

<style scoped lang="scss">

#goBackToTop {
  position: fixed;
  bottom: 20px;
  right: 30px;
  z-index: 99;
  border: none;
  outline: none;
  cursor: pointer;
  padding: 10px;
  border-radius: 10px;
  font-size: 18px;
}
</style>

<i18n>
en:
  DataFilesImagesList:
    no-file: No file to display
    scroll-to-display: Scroll to see more images
    goToTopButton: "Go back to top"
fr:
  DataFilesImagesList:
    no-file: "Aucun fichier à afficher"
    scroll-to-display: "Faire défiler pour afficher plus d'images"
    goToTopButton: "Retour au début"



</i18n>