import type {DataGetSearchDTO, EventGetDTO} from "opensilex-core/index";

/*
 * Types of the graphics: what DataVisuGraphic draws, and what VisuImagePanel displays.
 */

/**
 * One point of a series
 */
export interface DataVisuPoint {
  /** Date of the point, in milliseconds */
  timestamp: number
  value: number
  /** The Data this point was made from, kept for the interactions on the point (e.g. displaying its provenance) */
  data?: DataGetSearchDTO
  /** The image the value was measured from (its provenance "prov_used"), if any. Drawn as a pin on the point */
  image?: DataVisuPointImage
}

/**
 * Image a point was measured from
 */
export interface DataVisuPointImage {
  /** URI of the image data file */
  uri: string
  /** true if the image has annotations, its pin is then red */
  annotated: boolean
}

/**
 * Type to represent one series drawn on the graphic, e.g. the data of one (ScientificObject, Variable) pair.
 */
export interface DataVisuSerie {
  name: string
  /** Points sorted by date */
  points: DataVisuPoint[]
  color?: string
  /** ECharts symbol name (circle, rect, triangle, diamond...) */
  symbol?: string
  /** Index in the yAxisNames prop of the axis this series is drawn on */
  yAxisIndex?: number
  /** true to hide the series at first, it can be displayed by clicking its name in the legend */
  hidden?: boolean
}

/**
 * One event, drawn as a pin at the bottom of the graphic
 */
export interface DataVisuEvent {
  /** Date where the pin is drawn, in milliseconds */
  timestamp: number
  /** Text written in the pin, e.g. the first letter of the event type */
  label: string
  /**
   * Name of the legend entry grouping this event with others, e.g. "Events -> plot 1". Events with the same
   * seriesName can be hidden together by clicking the legend.
   */
  seriesName: string
  color?: string
  event: EventGetDTO
}

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
