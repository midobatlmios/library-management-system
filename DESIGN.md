---
name: Library
description: A local library's website signed like a city in cobalt-and-white enamel street plaques.
colors:
  enamel-cobalt: "#1b3c9e"
  enamel-cobalt-deep: "#132c76"
  enamel-white: "#f6f7f3"
  enamel-ink: "#12151c"
  page-ground: "#e8ebf1"
  steel: "#9aa1ad"
  enamel-ochre: "#b67d06"
  enamel-green: "#1d6b49"
  enamel-red: "#a8261f"
  on-shelf-ink: "#16673e"
  genre-bottle-green: "#1d6b49"
  genre-signal-red: "#a8261f"
  genre-ochre: "#8a5c00"
  genre-black: "#1b1d23"
  genre-teal: "#0d6672"
  genre-plum: "#5b2a6e"
  genre-deep-cobalt: "#132c76"
  genre-brick: "#7a3412"
  night-ground: "#0d1120"
  night-paper: "#161c2e"
  night-cobalt: "#2448b8"
typography:
  display:
    fontFamily: "Overpass Variable, system-ui, sans-serif"
    fontSize: "clamp(2.1rem, 6.4vw, 4.6rem)"
    fontWeight: 800
    lineHeight: 0.95
    letterSpacing: "0.01em"
  sign:
    fontFamily: "Overpass Variable, system-ui, sans-serif"
    fontSize: "15px"
    fontWeight: 800
    lineHeight: 1.2
    letterSpacing: "0.06em"
  heading:
    fontFamily: "Overpass Variable, system-ui, sans-serif"
    fontSize: "2.125rem"
    fontWeight: 800
    lineHeight: 1.2
    letterSpacing: "-0.005em"
  body:
    fontFamily: "Overpass Variable, system-ui, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: "0em"
  label:
    fontFamily: "Overpass Variable, system-ui, sans-serif"
    fontSize: "11px"
    fontWeight: 800
    lineHeight: 1
    letterSpacing: "0.06em"
rounded:
  inlay: "6px"
  button: "9px"
  plate-sm: "9px"
  field: "10px"
  plate: "14px"
spacing:
  xs: "8px"
  sm: "12px"
  md: "18px"
  lg: "28px"
  section: "56px"
  section-lg: "64px"
components:
  plaque:
    backgroundColor: "{colors.enamel-cobalt}"
    textColor: "{colors.enamel-white}"
    rounded: "{rounded.plate}"
    padding: "40px 48px"
  plaque-sm:
    backgroundColor: "{colors.enamel-cobalt}"
    textColor: "{colors.enamel-white}"
    rounded: "{rounded.plate-sm}"
    padding: "8px 32px"
  plaque-white:
    backgroundColor: "{colors.enamel-white}"
    textColor: "{colors.enamel-ink}"
    rounded: "{rounded.plate}"
    padding: "20px 28px"
  button-primary:
    backgroundColor: "{colors.enamel-cobalt}"
    textColor: "{colors.enamel-white}"
    rounded: "{rounded.button}"
    padding: "6px 18px"
  button-search:
    backgroundColor: "{colors.enamel-ochre}"
    textColor: "{colors.enamel-ink}"
    rounded: "{rounded.field}"
    padding: "16px 32px"
  input-search:
    backgroundColor: "{colors.enamel-white}"
    textColor: "{colors.enamel-ink}"
    rounded: "{rounded.field}"
    padding: "16px 16px 16px 52px"
  inlay:
    backgroundColor: "{colors.enamel-white}"
    textColor: "{colors.enamel-cobalt}"
    rounded: "{rounded.inlay}"
    padding: "4px 8px"
---

# Design System: Library

## Overview

**Enamel Street Plaques.** Finding a book is wayfinding, so the library's website is signed like a Moroccan city: cobalt enamel plates with a white rim set in from a rounded edge, domed steel rivets, and white enamel inlays for numbers. Books have no cover images, so every book is its own enamel plate in its genre's colour, carrying the genre (its plate of origin), title, author and a white strip with the live shelf count. The world is a material, not a picture: plates are the containers, headings and navigation of the site.

Light mode is a street in daylight: a pale steel ground with white plates. Dark mode is the same street at night: a night-blue ground where the enamel keeps its colour and the inlays stay white.

## Colors

### Primary
- **Enamel Cobalt** (#1b3c9e): the site's own signs: the navbar, the search plaque, section headings, selected items, primary buttons. Never used for a genre. In dark mode the plates use Night Cobalt (#2448b8) and text links use a lighter blue (#7f9cff) to keep contrast.

### Secondary
- **Enamel Ochre** (#b67d06): the Search button and the focus ring. Ink lettering on it, never white.

### Genre enamels
Eight enamel colours (bottle green, signal red, ochre #8a5c00, black, teal, plum, deep cobalt, brick), each carrying white lettering at 4.5:1 or better. A genre's colour comes from its id, so it never changes.

### Neutral
- **Enamel White** (#f6f7f3): white plates and inlays. Inlays stay this white in dark mode, like real enamel.
- **Enamel Ink** (#12151c): lettering on white plates.
- **Page Ground** (#e8ebf1) / **Night Ground** (#0d1120): the wall the signs hang on.
- **Steel** (#9aa1ad): rivets and scrollbars.

### Named Rules
**The Cobalt Belongs To The City Rule.** Cobalt marks the site's own wayfinding. Genres take the other enamels.
**The Inlay Stays White Rule.** Count tiles and shelf strips are white enamel in both modes, with fixed inks: green #16673e for "on the shelf", red #a8261f for "all out".

## Typography

One signage family, **Overpass** (variable, self-hosted from `@fontsource-variable/overpass`), descended from road-sign lettering.

### Hierarchy
- **Display** (800, caps, `clamp(2.1rem, 6.4vw, 4.6rem)`, line-height 0.95): the search plaque's heading only.
- **Sign** (800, caps, 0.06em tracking, 13–17px): everything set on a plate: section headings, genre names, step names, buttons on plaques.
- **Heading** (800, sentence case): page and book titles outside plates.
- **Body** (400, 15–16px, line-height 1.6): reading text, in sentence case.
- **Label** (800, caps, 11px): detail labels and inlays. Numbers use tabular figures.

## Layout

Content sits in a 1200px container with 16px gutters on phones. Home page sections are separated by 56px (64px from 600px up). Shelves are single rows that scroll sideways and snap plate by plate (columns of 150–180px). The catalog grid shows 2 / 3 / 4 / 5 plates per row at phone / 600px / 900px / 1200px. Below 600px the navbar collapses into a menu drawer and the genre plates on the search plaque become one scrolling row.

## Elevation & Depth

Plates hang on the wall with a short hard edge and a soft drop: `0 1px 0` plus `0 6px 14px -6px`. Pointing at a plate lifts it 4px and deepens the drop (`0 14px 26px -10px`), with an exponential ease-out of 220ms; the lift is off under reduced motion. A faint top-down glaze (white at 10% to 0 at 45%) is the enamel's sheen, not decoration.

## Shapes

Rounded rectangles only: plates 14px, small plates and buttons 9px, fields 10px, inlays 6px. Every plate has its white rim as an inset outline (2px, 7px in from the edge; 1.5px, 4px in on small plates), which follows the corners. Rivets are 8–12px domes: two at the ends of a horizontal plate, two in the top corners of a book plate, four in the corners of a large plaque.

## Components

### Buttons
Primary buttons are small cobalt plates with an inset white rim. The Search button is an ochre plate with ink lettering. On green or cobalt plaques, secondary actions are white plates or rim-only outlines.

### Book plate (signature)
A 2:3 enamel plate in the genre's colour: genre name in sign caps, a short white rule, the title (900 weight, sized from the plate's width and stepped down for long titles or long words), the author at the foot, and a white inlay strip with the shelf count. A cover image, when a book has one, sits inside the same riveted frame.

### Genre sign
A street-name plaque in cobalt with rivets, the genre in sign caps and its book count on a white inlay. The selected genre is struck forward: raised shadow and an ochre rim. All genres stay visible.

### Section heading
The heading itself is a small cobalt plaque with rivets; a "See all" link with an arrow icon sits at the right.

### Page title
Every page title outside the home page is a small cobalt plaque with rivets (`PageTitle`), with an optional short status beside it and actions on the right.

### Cards
MUI cards are white enamel plates: 14px corners, a 1px divider-coloured rim set 6px in from the edge, and the plate shadow.

### Charts (admin dashboard)
Plain SVG, built to the data-viz method: 2px lines, columns at most 24px wide with a 4px rounded top and a square base, hairline solid grid, a legend for two or more series and end labels only when they do not collide, a crosshair tooltip that also follows the arrow keys, and a Chart / Table switch on every chart. Series colours are tokens validated with the dataviz validator against the card surface in each mode: `--series-1` cobalt (#2b52c4 light, #5b7cf0 dark) and `--series-2` ochre (#b67d06 light, #c98500 dark). Labels and values wear text colours, never the series colour. Axis ticks use one style per axis (compact once it reaches 10,000).

### Inputs / Fields
The home search is a white enamel field set into the cobalt plaque with an inset shadow. Other fields are MUI outlined inputs with 9px corners.

### Navigation
The navbar is the top edge of a sign: cobalt with a white rim line along its foot. The library name sits on a small white plate with a cobalt rim. Selected items in side lists are cobalt plates with a white rim.

## Do's and Don'ts

### Do:
- Do set new containers as plates (cobalt, white or genre enamel) with the inset rim, not as grey cards.
- Do state availability plainly on a white inlay: "2 of 3 on the shelf", "All out · reserve".
- Do keep the theme's tokens (`--enamel-*`, MUI palette) so light and dark mode follow.
- Do use a real icon (MUI icons) for arrows and symbols.

### Don't:
- Don't use cobalt for a genre plate.
- Don't put a label above a heading; the plate is the heading.
- Don't invent the library's name, logo, address, opening hours or testimonials; they don't exist yet.
- Don't use text characters (→, ★) in place of icons.
