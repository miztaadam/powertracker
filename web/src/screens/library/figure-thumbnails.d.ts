/**
 * The Exercises tab's thumbnails, worked out when the app is built (plugins/figureThumbnails.ts): for each catalogue
 * exercise with a figure, by its catalogue name, its view box (width, height) and then its thumbnail pose's shapes,
 * written as thumbnailFormat.ts describes.
 */
declare module 'virtual:figure-thumbnails' {
  const thumbnails: Readonly<Record<string, readonly number[]>>
  export default thumbnails
}
