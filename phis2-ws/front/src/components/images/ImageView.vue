<template >
  <div>
    <b-button
      :class="showSearchComponent ? null : 'collapsed'"
      :aria-expanded="showSearchComponent ? 'true' : 'false'"
      aria-controls="collapse-1"
      @click="onSearchButtonClick"
      variant="phis"
    >
      Filter's parameters
      <font-awesome-icon v-if="!showSearchComponent" icon="chevron-down" size="sm" />
      <font-awesome-icon v-if="showSearchComponent" icon="chevron-up" size="sm" />
    </b-button>

    <b-collapse id="collapse-1" v-model="showSearchComponent" class="mt-2">
      <phis2ws-ImageSearch @onSearchFormSubmit="onSearchFormSubmit"></phis2ws-ImageSearch>
    </b-collapse>

    <b-collapse id="collapse-2" v-model="showImageComponent" class="mt-2">
      <div v-if="totalImages>0">
        <p>
          Showed images: {{showedImages}}/{{totalImages}} (
          <b>Scroll to see more images</b>)
        </p>
        <phis2ws-ImageList :images="images"></phis2ws-ImageList>

        <div v-if="showScrollSpinner" class="d-flex align-items-center">
          <strong>Loading...</strong>
          <div class="spinner-border ml-auto" role="status" aria-hidden="true"></div>
        </div>
      </div>
      <div v-else>No images to dispay</div>
    </b-collapse>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { DataService } from "../../lib/api/data.service";
import { FileDescriptionDTO } from "../../lib/model/fileDescriptionDTO";
import { Image } from "./image";

@Component
export default class ImageView extends Vue {
  $opensilex: any;
  $store: any;
  get user() {
    return this.$store.state.user;
  }
  dataService: DataService = this.$opensilex.getService(
    "opensilex.DataService"
  );
  images = [];

  showSearchComponent: boolean = true;
  showImageComponent: boolean = true;
  showScrollSpinner: boolean = false;
  canReload: boolean = true;

  currentPage: number = 1;
  pageSize = 50;
  totalImages: number = 0;
  showedImages: number = 0;

  private searchImagesFields: any = {
    rdfType: undefined,
    startDate: undefined,
    endDate: undefined,
    provenance: undefined,
    jsonValueFilter: undefined,
    dateSortAsc: false,
    concernedItemsValue: [],
    objectType: null,
    experiment: null
  };

  created() {
    this.initScroll();
  }

  showImage() {
    this.showSearchComponent = false;
    this.showImageComponent = true;
  }

  onSearchFormSubmit(form) {
    if (form.rdfType !== null) {
      this.currentPage = 1;
      this.searchImagesFields.experiment = form.experiment;
      this.searchImagesFields.objectType = form.objectType;
      this.searchImagesFields.rdfType = form.rdfType;
      this.searchImagesFields.startDate = form.startDate;
      this.searchImagesFields.endDate = form.endDate;
      this.searchImagesFields.concernedItemsValue = [];
      if (form.objectList) {
        this.searchImagesFields.concernedItemsValue=form.objectList;
      }
      this.loadData();
    }
  }

  initScroll() {
    window.onscroll = () => {
      let bottomOfWindow =
        window.innerHeight + window.scrollY >= document.body.offsetHeight - 1;
      if (bottomOfWindow) {
        console.log("reload");
        if (this.canReload) {
          this.reload();
        }
      }
    };
  }

  reload() {
    this.showScrollSpinner = true;
    this.currentPage++;
    this.getData();
  }

  loadData() {
    this.images = [];
    this.totalImages = 0;
    this.showedImages = 0;
    this.canReload = true;
    this.getData();
  }

  getData() {
    this.dataService
      .getDataFileDescriptionsBySearch(
        this.user.getAuthorizationHeader(),
        this.searchImagesFields.rdfType,
        this.searchImagesFields.startDate,
        this.searchImagesFields.endDate,
        this.searchImagesFields.provenance,
        Object.keys(this.searchImagesFields.concernedItemsValue),
        this.searchImagesFields.jsonValueFilter,
        this.searchImagesFields.dateSortAsc,
        this.pageSize,
        this.currentPage - 1
      )
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<FileDescriptionDTO>>>) => {
          this.totalImages = http.response.metadata.pagination.totalCount;
          const res = http.response.result as any;
          const data = res.data as Array<FileDescriptionDTO>;
          console.log("data");
          console.log(data);
          this.imagesFilter(data);
         
        }
      )
      .catch(error => {
        console.log(error);
        this.canReload = false;
        this.showScrollSpinner = false;
      });
  }

  imagesFilter(data: Array<FileDescriptionDTO>) {
    // pour chaque concerneditem une nouvelle image ..
    if (this.searchImagesFields.objectType !== null) {
      data.forEach(element => {
        element.concernedItems.forEach(concernedItem => {
          if (this.searchImagesFields.objectType === concernedItem.typeURI) {
            const image: Image = {
              objectType: concernedItem.typeURI,
              uri:
                this.$opensilex.getBaseAPI() +
                "/data/file/" +
                encodeURIComponent(element.uri),
              type: element.rdfType,
              objectUri: concernedItem.uri,
              date: element.date,
              provenanceUri: element.provenanceUri,
              objectAlias:this.searchImagesFields.concernedItemsValue[concernedItem.uri]
            };
           this.images.push(image);
          }
        });
      });
    } else {
      data.forEach(element => {
        element.concernedItems.forEach(concernedItem => {
          const image: Image = {
            objectType: concernedItem.typeURI,
            uri:
              this.$opensilex.getBaseAPI() +
              "/data/file/" +
              encodeURIComponent(element.uri),
            type: element.rdfType,
            objectUri: concernedItem.uri,
            date: element.date,
            provenanceUri: element.provenanceUri,
            objectAlias:this.searchImagesFields.concernedItemsValue[concernedItem.uri]
          };
          this.images.push(image);
        });
      });
    }
    this.showedImages = this.images.length;
    this.showScrollSpinner = false;
    //this.showImage();
  }

  format(date) {
    let d = new Date(date),
      month = "" + (d.getMonth() + 1),
      day = "" + d.getDate(),
      year = d.getFullYear();
    if (month.length < 2) month = "0" + month;
    if (day.length < 2) day = "0" + day;
    return [year, month, day].join("-");
  }

  onSearchButtonClick() {
    this.showSearchComponent = !this.showSearchComponent;
  }
}
</script>

<style scoped lang="scss">
.btn-phis {
  background-color: #00a38d;
  border: 1px solid #00a38d;
  color: #ffffff !important;
}
.btn-phis:hover,
.btn-phis:focus,
.btn-phis.active {
  background-color: #00a38d;
  border: 1px solid #00a38d;
  color: #ffffff !important;
}
.btn-phis:focus {
  outline: 0;
  -webkit-box-shadow: none;
  box-shadow: none;
}
</style>

