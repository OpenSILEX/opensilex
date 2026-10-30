<template>
  <div ref="panel" class="visuImagePanel">
    <VisuImageCard
        v-for="image in images"
        :key="image.uri"
        :data-image-uri="image.uri"
        :image="image"
        :highlighted="image.uri === highlightedImageUri"
        @hide="emit('hide', $event)"
        @hover="emit('hover', $event)"
        @open="onCardOpen"
        @details="onCardDetails"
        @annotationDetails="onCardAnnotationDetails"
        @annotate="onCardAnnotate"
    ></VisuImageCard>

    <ImageModal ref="imageModal" v-model:fileUrl="openedImageSrc"></ImageModal>

    <DataProvenanceModalView ref="provenanceModal" :datafile="true"></DataProvenanceModalView>

    <AnnotationsModal ref="annotationsModal" :title="t('VisuImagePanel.annotationsTitle')"></AnnotationsModal>

    <AnnotationModalForm ref="annotationForm" @onCreate="onAnnotationCreate"></AnnotationModalForm>
  </div>
</template>

<script lang="ts">
/**
 * An image displayed in the VisuImagePanel
 */
export interface VisuImage {
  /** URI of the image data file */
  uri: string
  /** Thumbnail of the image, as an object URL (blob:...) */
  src: string
  /** Local name of the image type, e.g. "RGBImage" */
  typeName: string
  /** URI of the element the image is about, e.g. a scientific object */
  target?: string
  date?: string
  annotated: boolean
}
</script>

<script setup lang="ts">
import {inject, ref, Ref, useTemplateRef} from "vue";
import {useI18n} from "vue-i18n";
import {DataFileGetDTO, DataService, ProvenanceGetDTO} from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import VisuImageCard from "@/components/visualization/VisuImageCard.vue";
import ImageModal from "@/components/data/ImageModal.vue";
import DataProvenanceModalView from "@/components/data/DataProvenanceModalView.vue";
import AnnotationsModal from "@/components/annotations/list/AnnotationsModal.vue";
import AnnotationModalForm from "@/components/annotations/list/form/AnnotationModalForm.vue";

/**
 * Row of images displayed above a graphic, added by clicking the image pins of the DataVisuGraphic. Clicking a
 * thumbnail displays the image in a bigger size. The menu of each card displays the image details (description
 * and provenance) and annotations, and allows to annotate it.
 */

//#region Public
const emit = defineEmits<{
  /** The user wants to remove this image from the panel */
  hide: [imageUri: string]
  /** The mouse entered (image URI) or left (undefined) the card of an image */
  hover: [imageUri: string | undefined]
  /** An annotation was created on this image from its card */
  annotationCreated: [imageUri: string]
}>();

defineProps<{
  images: VisuImage[]
  /** URI of the image whose card is outlined, e.g. because its pin is hovered on the graphic */
  highlightedImageUri?: string
}>();
//#endregion

//#region Private
//#region Plugins & services
const opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex");
const {t} = useI18n();
const dataService: DataService = opensilex.getService<DataService>("opensilex.DataService");
//#endregion

//#region Template refs
const panel = useTemplateRef<HTMLDivElement>("panel");
const imageModal = useTemplateRef<InstanceType<typeof ImageModal>>("imageModal");
const provenanceModal = useTemplateRef<InstanceType<typeof DataProvenanceModalView>>("provenanceModal");
const annotationsModal = useTemplateRef<InstanceType<typeof AnnotationsModal>>("annotationsModal");
const annotationForm = useTemplateRef<InstanceType<typeof AnnotationModalForm>>("annotationForm");
//#endregion

//#region Data & Computed
/** Thumbnail displayed in the modal */
const openedImageSrc: Ref<string | undefined> = ref<string | undefined>(undefined);
/** Image being annotated in the annotation form */
const annotatedImageUri: Ref<string | undefined> = ref<string | undefined>(undefined);
//#endregion

//#region Event handlers
function onCardOpen(image: VisuImage): void {
  openedImageSrc.value = image.src;
  imageModal.value?.show();
}

/**
 * Displays the description of the image data file and its provenance
 */
async function onCardDetails(imageUri: string): Promise<void> {
  try {
    const description: DataFileGetDTO = (await dataService.getDataFileDescription(imageUri)).response.result;
    const provenance: ProvenanceGetDTO = (await dataService.getProvenance(description.provenance.uri)).response.result;
    provenanceModal.value?.setProvenanceAndBatch({data: description, provenance});
    provenanceModal.value?.show();
  } catch (error) {
    opensilex.errorHandler(error);
  }
}

function onCardAnnotationDetails(imageUri: string): void {
  annotationsModal.value?.show(imageUri);
}

function onCardAnnotate(imageUri: string): void {
  annotatedImageUri.value = imageUri;
  annotationForm.value?.showCreateForm([imageUri]);
}

function onAnnotationCreate(): void {
  emit("annotationCreated", annotatedImageUri.value);
}
//#endregion

//#region Methods
/**
 * Scrolls the panel to the card of the image, e.g. when its pin is clicked on the graphic.
 */
function scrollToImage(imageUri: string): void {
  const card: HTMLElement | null = panel.value?.querySelector(`[data-image-uri="${CSS.escape(imageUri)}"]`);
  card?.scrollIntoView({behavior: "smooth", block: "nearest", inline: "nearest"});
}
//#endregion
//#endregion

defineExpose({
  scrollToImage
});
</script>

<style scoped lang="scss">
.visuImagePanel {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 8px;
  margin-bottom: 12px;
}
</style>
