import {inject} from "vue";
import {useI18n} from "vue-i18n";
import {useStore} from "vuex";
import {
    DataGetSearchDTO,
    DataService,
    EventGetDTO,
    EventsService,
    VariableDetailsDTO,
    VariablesService
} from "opensilex-core/index";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import type {DataVisuEvent, DataVisuPoint, DataVisuSerie} from "@/components/visualization/DataVisuGraphic.vue";

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
    async function loadDataSeries(requests: DataSeriesRequest[]): Promise<DataVisuSerie[]> {
        const series: DataVisuSerie[] = await Promise.all(requests.map(loadDataSerie));

        if (series.every((s: DataVisuSerie) => s.points.length === 0)) {
            opensilex.showInfoToast(t("component.common.search.noDataFound"));
        }
        return series;
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
        countEvents
    };
}
