<template>
  <b-modal
      v-model="isVisible"
      :title="$t('Annotation.details')"
      hide-footer
      no-close-on-backdrop
      no-close-on-esc
      centered
      @close="close()"
      @keydown.enter.native.stop="close"
  >
    <div class="details-container">
      <p><strong>URI :</strong>
        <opensilex-UriLink
            :uri="annotationDetails.uri"
            :value="annotationDetails.uri"
        />
      </p>
      <p><strong>{{ $t('Annotation.motivation') }}  :</strong> {{ annotationDetails.motivation && annotationDetails.motivation.name || $t('component.common.not-specified') }}</p>
      <p><strong>{{ $t('Annotation.publisher') }}  :</strong> {{ annotationDetails.publisher || $t('component.common.not-specified') }}</p>
      <p><strong>{{ $t('Annotation.published') }}  :</strong> {{ formatDate(annotationDetails.published) }}</p>
      <p><strong>{{ $t('Annotation.description') }}  :</strong> {{ annotationDetails.description || $t('component.common.not-specified') }}</p>
    </div>
    <div class="text-right">
      <b-button variant="primary" class="helpButton" @click="close">OK</b-button>
    </div>
  </b-modal>
</template>

<script lang="ts">
import { Component, Vue, Prop, PropSync } from "vue-property-decorator";

@Component
export default class AnnotationDetails extends Vue {
  @Prop({ default: () => ({}) }) annotationDetails!: {
    uri?: string;
    motivation?: { name?: string };
    publisher?:string;
    published?:string;
    description?:string
  };
  @PropSync("value",{ default: false }) isVisible!: boolean;

  close() {
    this.$emit("input", false);
    this.$emit("close");
  }

  formatDate(dateStr: string): string {
    const localeString = new Date(dateStr).toLocaleString();

    return localeString.replace(/\//g, '-');
  }

}
</script>

<style scoped>

.details-container {
  text-align: left;
  padding: 10px 20px;
}

.text-right {
  margin-top: 15px;
}

.helpButton {
  margin-left: 25px;
  color: #f1f1f1;
  background-color: #00A28C;
  border: none;
}

/* Hover effect */
.helpButton:hover {
  background-color: #00A2B0;
  color: #f1f1f1;
  border: none;
}

</style>
<i18n>
en:
  Annotation:
    details: Details annotation
    motivation: Motivation
    publisher: Publisher
    published: Date
    description: Description
fr:
  Annotation:
    details: Détails de l'annotation
    motivation: Motivation
    publisher: Publieur
    published: Date
    description: Description
</i18n>

