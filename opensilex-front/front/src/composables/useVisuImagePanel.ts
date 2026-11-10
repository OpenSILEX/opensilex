import {inject, nextTick, onBeforeUnmount, ref, Ref, ShallowRef} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import type {DataVisuPoint, VisuImage} from "@/components/visualization/DataVisuTypes";
import type VisuImagePanel from "@/components/visualization/VisuImagePanel.vue";
import useDataVisualisation from "@/composables/useDataVisualisation";

/**
 * State and event handlers linking a DataVisuGraphic to a VisuImagePanel, for the views displaying images
 * (experiment, scientific object). In the view:
 *
 * - the panel receives `images` and `highlightedImageUri`, and its events are handled by `onImagePanelHide`,
 *   `onImagePanelAnnotationCreated`, and `@hover="highlightedImageUri = $event"`
 * - the graphic receives `highlightedImageUri`, and its events are handled by `onGraphicImageClick` and
 *   `@imageHover="highlightedImageUri = $event"`
 * - `clearImagePanel()` is called on each new search
 *
 * The thumbnails of the images are released when they are removed from the panel, and when the view is unmounted.
 *
 * @param imagePanel template ref of the VisuImagePanel, to scroll it to an image
 */
export default function useVisuImagePanel(imagePanel: Readonly<ShallowRef<InstanceType<typeof VisuImagePanel> | null>>) {
    //#region Plugins & services
    const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
    const {loadImage} = useDataVisualisation();
    //#endregion

    //#region Data
    /** Images added to the image panel by clicking their pin on the graphic */
    const panelImages: Ref<VisuImage[]> = ref<VisuImage[]>([]);
    /** Image hovered in the panel or on the graphic, highlighted in both */
    const highlightedImageUri: Ref<string | undefined> = ref<string | undefined>(undefined);
    //#endregion

    //#region Hooks & watchers
    onBeforeUnmount(() => {
        clearImagePanel();
    });
    //#endregion

    //#region Event handlers
    /**
     * Adds the image of the clicked pin to the image panel, if it isn't there yet, and scrolls the panel to it.
     */
    async function onGraphicImageClick(point: DataVisuPoint): Promise<void> {
        const imageUri: string = point.image.uri;
        try {
            if (!panelImages.value.some((image: VisuImage) => image.uri === imageUri)) {
                panelImages.value.push(await loadImage(point.image));
            }
            highlightedImageUri.value = imageUri;
            // The panel (or the card) may have just been created, the scroll waits for its render
            await nextTick();
            imagePanel.value?.scrollToImage(imageUri);
        } catch (error) {
            $opensilex.errorHandler(error);
        }
    }

    /**
     * Displays the red dot of the card. The pins of the graphic are updated by the next search.
     */
    function onImagePanelAnnotationCreated(imageUri: string): void {
        const image: VisuImage | undefined = panelImages.value.find((i: VisuImage) => i.uri === imageUri);
        if (image) {
            image.annotated = true;
        }
    }

    function onImagePanelHide(imageUri: string): void {
        const image: VisuImage | undefined = panelImages.value.find((i: VisuImage) => i.uri === imageUri);
        if (image) {
            URL.revokeObjectURL(image.src);
            panelImages.value = panelImages.value.filter((i: VisuImage) => i !== image);
        }
        highlightedImageUri.value = undefined;
    }
    //#endregion

    //#region Methods
    /**
     * Removes every image of the panel, releasing their thumbnails
     */
    function clearImagePanel(): void {
        panelImages.value.forEach((image: VisuImage) => URL.revokeObjectURL(image.src));
        panelImages.value = [];
        highlightedImageUri.value = undefined;
    }
    //#endregion

    return {
        panelImages,
        highlightedImageUri,
        onGraphicImageClick,
        onImagePanelAnnotationCreated,
        onImagePanelHide,
        clearImagePanel
    };
}
