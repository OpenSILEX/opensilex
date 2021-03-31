<template>
  <b-modal
    ref="modal"
    :title="$t('ImageModal.title')"
    ok-only
    size="lg"
  >
    <template v-slot:modal-header>
      <b-row class="mt-1" style="width: 100%">
        <b-col cols="10">
          <i>
            <h4>
              <opensilex-Icon icon="fa#image" />
              {{ $t("ImageModal.title") }}
            </h4>
          </i>
        </b-col>
      </b-row>
    </template>
    <b-img ref='image' :src="url" fluid center></b-img>
  </b-modal>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Ref, PropSync} from "vue-property-decorator";

@Component
export default class ImageModal extends Vue {
    $opensilex: any;
    $t: any;
    
    @PropSync("fileUrl")
    url;

    @Ref("modal") readonly modal!: any;
    @Ref("image") readonly image!: any;

    show() {      
      this.modal.show();
    }

    hide() {
      this.modal.hide();
    }

}
</script>

<style>
.validation-confirm-container {
  color: rgb(40, 167, 69);
  font-weight: bold;
}
</style>
<i18n>
  en: 
    ImageModal:
      title : Image
  
  fr: 
    ImageModal:
      title : Image

</i18n>