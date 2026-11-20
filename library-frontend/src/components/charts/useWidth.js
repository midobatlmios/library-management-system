import { useEffect, useRef, useState } from 'react'

/** Measures an element's width and follows resizes (charts redraw to fit their card). */
export function useWidth(initial = 600) {
  const ref = useRef(null)
  const [width, setWidth] = useState(initial)
  useEffect(() => {
    if (!ref.current) return
    const ro = new ResizeObserver(([entry]) => setWidth(Math.max(240, Math.floor(entry.contentRect.width))))
    ro.observe(ref.current)
    return () => ro.disconnect()
  }, [])
  return [ref, width]
}

/**
 * A round axis maximum just above the data, with 3 to 5 clean steps:
 * max 21 -> 0, 5, 10, 15, 20, 25 (not 0..40). Picks the smallest top that fits.
 */
export function niceScale(max) {
  if (!(max > 0)) return { top: 4, ticks: [0, 1, 2, 3, 4] }
  let best = null
  for (const count of [4, 5, 3]) {
    const rough = max / count
    const pow = 10 ** Math.floor(Math.log10(rough))
    const step = [1, 2, 2.5, 5, 10].map((m) => m * pow).find((s) => s * count >= max)
    if (step && (!best || step * count < best.step * best.count)) best = { step, count }
  }
  const ticks = Array.from({ length: best.count + 1 }, (_, i) => +(i * best.step).toFixed(6))
  return { top: ticks[ticks.length - 1], ticks }
}

/** 1284 -> "1,284"; 12900 -> "12.9K" (compact only from 10,000 up). */
export function compact(n) {
  const v = Number(n)
  if (Math.abs(v) >= 10000) return new Intl.NumberFormat('en', { notation: 'compact', maximumFractionDigits: 1 }).format(v)
  return new Intl.NumberFormat('en', { maximumFractionDigits: 2 }).format(v)
}

/** Axis tick text, one style per axis: all compact (0 / 10K / 20K) once the axis reaches 10,000. */
export function axisLabel(value, top) {
  if (top >= 10000) return new Intl.NumberFormat('en', { notation: 'compact', maximumFractionDigits: 1 }).format(value)
  return new Intl.NumberFormat('en', { maximumFractionDigits: 2 }).format(value)
}

/** "2026-11-20" -> "20 Nov" (local date, no timezone shift). */
export function shortDate(iso) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d).toLocaleDateString('en-GB', { day: 'numeric', month: 'short' })
}
