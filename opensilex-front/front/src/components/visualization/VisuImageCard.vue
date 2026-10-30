<template>
  <n-card
      class="visuImageCard"
      :class="{highlighted}"
      size="small"
      content-style="padding: 4px 8px;"
      @mouseenter="emit('hover', image.uri)"
      @mouseleave="emit('hover', undefined)"
  >
    <template #cover>
      <div class="thumbnailContainer">
        <img :src="image.src" class="thumbnail" :alt="image.typeName" @click="emit('open', image)"/>
        <span v-if="image.annotated" class="annotatedBadge" :title="t('VisuImageCard.annotated')"></span>
        <n-dropdown trigger="click" placement="bottom-end" :options="menuOptions" @select="onMenuSelect">
          <n-button size="tiny" circle secondary class="menuButton" :title="t('VisuImageCard.menu')">
            <i class="bi bi-three-dots-vertical"></i>
          </n-button>
        </n-dropdown>
      </div>
    </template>
    <div class="imageInfo" :title="image.typeName">{{ image.typeName }}</div>
    <div class="imageInfo" :title="image.target">{{ image.target }}</div>
    <div class="imageInfo">{{ formattedDate }}</div>
  </n-card>
</template>

<script setup lang="ts">
import {computed, ComputedRef, inject} from "vue";
import {useI18n} from "vue-i18n";
import {DropdownOption, NButton, NCard, NDropdown} from "naive-ui";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import type {VisuImage} from "@/components/visualization/VisuImagePanel.vue";

/** Keys of the entries of the card menu */
type MenuKey = "details" | "annotationDetails" | "annotate" | "hide";

/**
 * One image of the VisuImagePanel: its thumbnail, type, target and date, with a red dot if it is annotated.
 */

//#region Public
const emit = defineEmits<{
  /** The user wants to remove this image from the panel */
  hide: [imageUri: string]
  /** The mouse entered (image URI) or left (undefined) the card */
  hover: [imageUri: string | undefined]
  /** The thumbnail was clicked, to display the image in full size */
  open: [image: VisuImage]
  /** "Details" was selected in the menu, to display the image description and provenance */
  details: [imageUri: string]
  /** "Annotation details" was selected in the menu, to display the annotations of the image */
  annotationDetails: [imageUri: string]
  /** "Annotate" was selected in the menu, to create an annotation on the image */
  annotate: [imageUri: string]
}>();

const props = defineProps<{
  image: VisuImage
  /** true to outline the card, e.g. when the pin of this image is hovered on the graphic */
  highlighted?: boolean
}>();
//#endregion

//#region Private
//#region Plugins & services
const opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex");
const {t} = useI18n();
//#endregion

//#region Data & Computed
const menuOptions: ComputedRef<DropdownOption[]> = computed<DropdownOption[]>(() => [
  {key: "details", label: t("VisuImageCard.details")},
  {key: "annotationDetails", label: t("VisuImageCard.annotationDetails")},
  {key: "annotate", label: t("VisuImageCard.annotate")},
  {key: "hide", label: t("VisuImageCard.hide")}
]);

const formattedDate: ComputedRef<string> = computed<string>(() =>
    props.image.date ? opensilex.$dateTimeFormatter.formatLocaleDateTime(props.image.date) : ""
);
//#endregion

//#region Event handlers
function onMenuSelect(key: MenuKey): void {
  switch (key) {
    case "details":
      emit("details", props.image.uri);
      break;
    case "annotationDetails":
      emit("annotationDetails", props.image.uri);
      break;
    case "annotate":
      emit("annotate", props.image.uri);
      break;
    case "hide":
      emit("hide", props.image.uri);
      break;
  }
}
//#endregion
//#endregion
</script>

<style scoped lang="scss">
.visuImageCard {
  width: 232px;
  flex-shrink: 0;
  border: 2px solid transparent;
}

.visuImageCard.highlighted {
  border-color: #FF0000;
}

/*
 * Empty strips on each side of the image, where the red dot (left) and the menu button (right) are placed, so they
 * are always visible, whatever the colors of the image
 */
.thumbnailContainer {
  position: relative;
  padding: 0 26px;
}

.thumbnail {
  width: 100%;
  height: 100px;
  object-fit: cover;
  cursor: zoom-in;
}

.annotatedBadge {
  position: absolute;
  top: 6px;
  left: 8px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #FF0000;
}

.menuButton {
  position: absolute;
  top: 2px;
  right: 2px;
}

.imageInfo {
  font-size: 0.8rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
