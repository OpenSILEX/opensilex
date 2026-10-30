import {inject} from "vue";
import {useI18n} from "vue-i18n";
import {useStore} from "vuex";
import {
    AnnotationsService,
    DataFileGetDTO,
    DataGetSearchDTO,
    DataService,
    EventGetDTO,
    EventsService,
    ProvEntityModel,
    VariableDetailsDTO,
    VariablesService
} from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import type {
    DataVisuEvent,
    DataVisuPoint,
    DataVisuPointImage,
    DataVisuSerie
} from "@/components/visualization/DataVisuGraphic.vue";
import type {VisuImage} from "@/components/visualization/VisuImagePanel.vue";

/**
 * Filters of DataService.searchDataList used by the graphics. Undefined filters are not applied.
 */
export interface DataSearchFilter {
    start_date?: string
    end_date?: string
    experiments?: string[]
    targets?: string[]
    variables?: string[]
    devices?: string[]
    provenances?: string[]
}

/**
 * Describes one series to load: the data matching the filter, and how to display it.
 */
export interface DataSeriesRequest {
    /** Name of the series, displayed in the legend and the tooltip */
    name: string
    filter: DataSearchFilter
    color?: string
    symbol?: string
    yAxisIndex?: number
}

/**
 * Describes the events to load for one target, and how to display them.
 */
export interface EventsRequest {
    /** URI of the element the events are about, e.g. a scientific object */
    target: string
    start_date?: string
    end_date?: string
    /** Name of the legend entry grouping these events, e.g. "Events -> plot 1" */
    seriesName: string
    color?: string
}

/** Size of the image thumbnails displayed in the image panel, in pixels */
const THUMBNAIL_WIDTH: number = 640;
const THUMBNAIL_HEIGHT: number = 320;

/** Datatypes (local names of the xsd URIs) that can be drawn on a graphic */
const NUMERIC_DATATYPES: string[] = ["decimal", "integer"];

/**
 * Loads what the DataVisuGraphic component displays: data series and events.
 *
 * Every location that draws a graphic (experiment, scientific object, device...) describes the series it wants with
 * its own filters, and this composable fetches them, with the same rules everywhere: data ordered by date, limited to
 * the graphDataLimit of the store, with a toast when the limit is reached.
 */
export default function useDataVisualisation() {
    //#region Plugins & services
    const opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex");
    const {t} = useI18n();
    const store = useStore();

    const dataService: DataService = opensilex.getService<DataService>("opensilex.DataService");
    const variablesService: VariablesService = opensilex.getService<VariablesService>("opensilex.VariablesService");
    const eventsService: EventsService = opensilex.getService<EventsService>("opensilex.EventsService");
    const annotationsService: AnnotationsService = opensilex.getService<AnnotationsService>("opensilex.AnnotationsService");
    //#endregion

    //#region Methods
    /**
     * Loads the details of the variables, and checks that they can be drawn (numeric datatype).
     *
     * @returns the variables details, or undefined if one of them isn't numeric (an info toast is then displayed)
     */
    async function getNumericVariables(variableUris: string[]): Promise<VariableDetailsDTO[] | undefined> {
        const variables: VariableDetailsDTO[] = (await variablesService.getVariablesByURIs(variableUris)).response.result;

        const nonNumericVariable: VariableDetailsDTO | undefined = variables.find(
            (v: VariableDetailsDTO) => !NUMERIC_DATATYPES.includes(getDatatypeName(v))
        );
        if (nonNumericVariable) {
            opensilex.showInfoToast(t("DataVisualisation.datatypeMessage", {
                variable: nonNumericVariable.name,
                datatype: getDatatypeName(nonNumericVariable)
            }));
            return undefined;
        }
        return variables;
    }

    /**
     * Loads every requested series, with one call per series. Displays a toast if none of them has data.
     */
    async function loadDataSeries(requests: DataSeriesRequest[], options?: { withImages?: boolean }): Promise<DataVisuSerie[]> {
        const series: DataVisuSerie[] = await Promise.all(requests.map(loadDataSerie));
        if (options?.withImages) {
            await addImages(series);
        }

        if (series.every((s: DataVisuSerie) => s.points.length === 0)) {
            opensilex.showInfoToast(t("component.common.search.noDataFound"));
        }
        return series;
    }

    /**
     * Sets the image of the points measured from an image (the image is the first "prov_used" of the data
     * provenance), and checks which images are annotated. The same image can be used by several points (e.g. one
     * CSV row with several variables), so each image is only checked once.
     */
    async function addImages(series: DataVisuSerie[]): Promise<void> {
        const pointsWithImage: Array<{ point: DataVisuPoint, imageUri: string }> = series
            .flatMap((serie: DataVisuSerie) => serie.points)
            .map((point: DataVisuPoint) => ({point, imageUri: getImageUri(point)}))
            .filter(({imageUri}) => imageUri);

        const imageUris: string[] = [...new Set(pointsWithImage.map(({imageUri}) => imageUri))];
        // One [uri, image] pair per image, each built next to its own annotation count
        const entries: Array<[string, DataVisuPointImage]> = await Promise.all(imageUris.map(async (uri: string) => {
            const annotationCount: number = (await annotationsService.countAnnotations(uri)).response.result;
            return [uri, {uri, annotated: annotationCount > 0}] as [string, DataVisuPointImage];
        }));
        const images: Map<string, DataVisuPointImage> = new Map(entries);

        for (const {point, imageUri} of pointsWithImage) {
            point.image = images.get(imageUri);
        }
    }

    /**
     * Loads what the image panel displays about an image: its description and its thumbnail.
     *
     * The thumbnail is an object URL (blob:...), which should be released with URL.revokeObjectURL() when the image
     * isn't displayed anymore.
     */
    async function loadImage(image: DataVisuPointImage): Promise<VisuImage> {
        const description: DataFileGetDTO = (await dataService.getDataFileDescription(image.uri)).response.result;
        const thumbnailPath: string = `/core/datafiles/${encodeURIComponent(image.uri)}/thumbnail`
            + `?scaled_width=${THUMBNAIL_WIDTH}&scaled_height=${THUMBNAIL_HEIGHT}`;
        return {
            uri: image.uri,
            src: await opensilex.viewImageFromGetService(thumbnailPath),
            // The local name of the type, e.g. "RGBImage" for "vocabulary:RGBImage"
            typeName: description.rdf_type.split(/[#:]/).pop(),
            target: description.target,
            date: description.date,
            annotated: image.annotated
        };
    }

    function getImageUri(point: DataVisuPoint): string | undefined {
        const provUsed: ProvEntityModel[] | undefined = point.data?.provenance?.prov_used;
        return provUsed?.[0]?.uri;
    }

    /**
     * Loads the events of every requested target, with one call per target.
     */
    async function loadEvents(requests: EventsRequest[]): Promise<DataVisuEvent[]> {
        const eventsPerRequest: DataVisuEvent[][] = await Promise.all(requests.map(async (request: EventsRequest) => {
            const events: EventGetDTO[] = (await searchEvents(request.target, request.start_date, request.end_date, 0)).response.result;
            return events.map((event: EventGetDTO) => toDataVisuEvent(event, request));
        }));
        return eventsPerRequest.flat();
    }

    /**
     * @returns the total number of events of the targets between the dates, e.g. to tell the user how many events
     * would be displayed. Only the counts are loaded, not the events.
     */
    async function countEvents(targets: string[], startDate?: string, endDate?: string): Promise<number> {
        const counts: number[] = await Promise.all(targets.map(async (target: string) => {
            const http = await searchEvents(target, startDate, endDate, 1);
            return http.response.metadata.pagination.totalCount;
        }));
        return counts.reduce((total: number, count: number) => total + count, 0);
    }

    /**
     * @returns the local name of the variable datatype, e.g. "decimal" for "xsd:decimal"
     */
    function getDatatypeName(variable: VariableDetailsDTO): string {
        return variable.datatype?.split("#")[1] ?? "";
    }

    /**
     * Loads the data of one series, warning the user if the data limit is reached.
     */
    async function loadDataSerie(request: DataSeriesRequest): Promise<DataVisuSerie> {
        const limit: number = store.state.graphDataLimit;
        const f: DataSearchFilter = request.filter;
        const http = await dataService.searchDataList(
            f.start_date || undefined,
            f.end_date || undefined,
            undefined,
            f.experiments,
            f.targets,
            f.variables,
            f.devices,
            undefined,
            undefined,
            f.provenances,
            undefined,
            undefined,
            ["date=asc"],
            0,
            limit
        );
        const data: DataGetSearchDTO[] = http.response.result;

        if (data.length >= limit) {
            opensilex.showWarningToast(t("DataVisualisation.dataLimit", {
                total: http.response.metadata?.pagination?.totalCount ?? data.length,
                serie: request.name,
                limit
            }));
        }

        return {
            name: request.name,
            points: data.map((d: DataGetSearchDTO): DataVisuPoint => ({
                timestamp: new Date(d.date).getTime(),
                value: Number(d.value),
                data: d
            })),
            color: request.color,
            symbol: request.symbol,
            yAxisIndex: request.yAxisIndex
        };
    }

    /**
     * @param pageSize 0 to load every event
     */
    function searchEvents(target: string, startDate: string | undefined, endDate: string | undefined, pageSize: number) {
        return eventsService.searchEvents(
            undefined,
            startDate || undefined,
            endDate || undefined,
            target,
            undefined,
            undefined,
            0,
            pageSize
        );
    }

    /**
     * Places the event at its start, or at its end for an instant event (which only has an end), and labels it with
     * the first letter of its type.
     */
    function toDataVisuEvent(event: EventGetDTO, request: EventsRequest): DataVisuEvent {
        const typeName: string = event.rdf_type_name ?? event.rdf_type ?? "";
        return {
            timestamp: new Date(event.start ?? event.end).getTime(),
            label: typeName.charAt(0).toUpperCase(),
            seriesName: request.seriesName,
            color: request.color,
            event
        };
    }
    //#endregion

    return {
        getNumericVariables,
        loadDataSeries,
        loadEvents,
        countEvents,
        loadImage
    };
}
