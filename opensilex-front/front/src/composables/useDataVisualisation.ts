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
    NamedResourceDTO,
    OntologyService,
    ProvEntityModel,
    VariableDetailsDTO,
    VariablesService
} from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import type {
    DataVisuEvent,
    DataVisuPoint,
    DataVisuPointImage,
    DataVisuSerie,
    VisuImage
} from "@/components/visualization/DataVisuTypes";

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
 * Points of a series having the same target, see splitSeriesByTarget()
 */
export interface TargetSerie {
    /** Index, in the split series, of the series these points come from */
    sourceIndex: number
    /** URI of the target of the points, undefined for the data without target */
    target?: string
    /** Name of the target, its URI if its name isn't known */
    targetName?: string
    points: DataVisuPoint[]
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

/**
 * Colors of the graphics, given in this order to the elements that need their own color (e.g. one per scientific
 * object in an experiment)
 */
export const GRAPHIC_COLOR_PALETTE: string[] = [
    "#ca6434", "#427775", "#f2dc7c", "#0f839c", "#a45354",
    "#d3b0ae", "#774e42", "#776942", "#5c4277", "#34a0ca",
    "#9334ca", "#caaf34", "#4F72D6", "#DB78E1", "#97CA68",
];

/**
 * @returns the color of the palette at this index, starting again from the first color after the last one
 */
export function getGraphicColor(index: number): string {
    return GRAPHIC_COLOR_PALETTE[index % GRAPHIC_COLOR_PALETTE.length];
}

/**
 * Symbols of the graphics, given in this order to the elements that need their own symbol (e.g. one per variable in
 * an experiment). The "pin" symbol isn't in the list, as it is used by the event and image pins.
 */
export const GRAPHIC_SYMBOLS: string[] = ["circle", "rect", "triangle", "diamond", "roundRect", "arrow"];

/**
 * @returns the symbol at this index, starting again from the first symbol after the last one
 */
export function getGraphicSymbol(index: number): string {
    return GRAPHIC_SYMBOLS[index % GRAPHIC_SYMBOLS.length];
}

/**
 * @returns the default start date of the graphics filters: 15 days ago, at midnight, as an ISO string
 */
export function getLastFifteenDaysStartDate(): string {
    const lastFifteenDays: Date = new Date();
    lastFifteenDays.setDate(lastFifteenDays.getDate() - 15);
    lastFifteenDays.setHours(0, 0, 0, 0);
    return lastFifteenDays.toISOString();
}

/**
 * Names and styles the series of each (target, device) pair, after splitting the series of each device by target
 * (see splitSeriesByTarget): one color per target, one symbol per device.
 *
 * The name is the target, followed by the device if there are several devices. The data without target is named
 * after its device, and gets a color of its own for each device.
 *
 * @param deviceSeries the series of each device, before being split. Their names are the names of the devices
 * @param targetSeries the series of each device, split by target
 */
export function styleDeviceTargetSeries(deviceSeries: DataVisuSerie[], targetSeries: TargetSerie[]): DataVisuSerie[] {
    /** What gets its own color: the target, or the device for the data without target */
    const colorKey = (targetSerie: TargetSerie): string => targetSerie.target ?? `device:${targetSerie.sourceIndex}`;
    const colorKeys: string[] = [...new Set(targetSeries.map(colorKey))];
    const multipleDevices: boolean = deviceSeries.length > 1;

    return targetSeries.map((targetSerie: TargetSerie): DataVisuSerie => {
        const deviceName: string = deviceSeries[targetSerie.sourceIndex].name;
        let name: string = deviceName;
        if (targetSerie.target) {
            name = multipleDevices ? `${targetSerie.targetName} / ${deviceName}` : targetSerie.targetName;
        }
        return {
            name,
            points: targetSerie.points,
            color: getGraphicColor(colorKeys.indexOf(colorKey(targetSerie))),
            symbol: getGraphicSymbol(targetSerie.sourceIndex)
        };
    });
}

/**
 * @returns the events with one color per event type, e.g. when all the events are about the same element
 */
export function colorEventsByType(events: DataVisuEvent[]): DataVisuEvent[] {
    const eventTypes: string[] = [...new Set(events.map((event: DataVisuEvent) => event.event.rdf_type))];
    return events.map((event: DataVisuEvent): DataVisuEvent => ({
        ...event,
        color: getGraphicColor(eventTypes.indexOf(event.event.rdf_type))
    }));
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
    const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
    const {t} = useI18n();
    const store = useStore();

    const dataService: DataService = $opensilex.getService<DataService>("opensilex.DataService");
    const variablesService: VariablesService = $opensilex.getService<VariablesService>("opensilex.VariablesService");
    const eventsService: EventsService = $opensilex.getService<EventsService>("opensilex.EventsService");
    const annotationsService: AnnotationsService = $opensilex.getService<AnnotationsService>("opensilex.AnnotationsService");
    const ontologyService: OntologyService = $opensilex.getService<OntologyService>("opensilex.OntologyService");
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
            $opensilex.showInfoToast(t("DataVisualisation.datatypeMessage", {
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
            $opensilex.showInfoToast(t("component.common.search.noDataFound"));
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
            src: await $opensilex.viewImageFromGetService(thumbnailPath),
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
            $opensilex.showWarningToast(t("DataVisualisation.dataLimit", {
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

    /**
     * Splits each series by the target of its points (e.g. the scientific object the data was measured on), when a
     * series mixes the data of several targets (e.g. all the data of a device). The names of the targets are loaded in
     * a single request. The points of each target stay sorted by date.
     *
     * @returns the points of each (series, target) pair, in the order of the series then of the first point of each
     * target. The views give them their names and styles.
     */
    async function splitSeriesByTarget(series: DataVisuSerie[]): Promise<TargetSerie[]> {
        const targets: string[] = [...new Set(series
            .flatMap((serie: DataVisuSerie) => serie.points)
            .map((point: DataVisuPoint) => point.data?.target)
            .filter((target: string | undefined) => target))];
        const targetNames: Map<string, string> = await getTargetNames(targets);

        return series.flatMap((serie: DataVisuSerie, sourceIndex: number) => {
            const pointsByTarget: Map<string | undefined, DataVisuPoint[]> = new Map<string | undefined, DataVisuPoint[]>();
            for (const point of serie.points) {
                const target: string | undefined = point.data?.target || undefined;
                pointsByTarget.set(target, [...(pointsByTarget.get(target) ?? []), point]);
            }
            return Array.from(pointsByTarget, ([target, points]: [string | undefined, DataVisuPoint[]]): TargetSerie => ({
                sourceIndex,
                target,
                targetName: target ? targetNames.get(target) ?? target : undefined,
                points
            }));
        });
    }

    /**
     * @returns the name of each target by URI, whatever its type (scientific object, facility...)
     */
    async function getTargetNames(targets: string[]): Promise<Map<string, string>> {
        if (targets.length === 0) {
            return new Map<string, string>();
        }
        const resources: NamedResourceDTO[] = (await ontologyService.getURILabelsList(targets)).response.result;
        return new Map(resources.map((resource: NamedResourceDTO) => [resource.uri, resource.name]));
    }

    return {
        getNumericVariables,
        loadDataSeries,
        loadEvents,
        countEvents,
        loadImage,
        splitSeriesByTarget
    };
}
