import { useCallback, useEffect, useState } from 'react'

/**
 * Loads data with an async function and keeps track of loading and errors.
 *
 *   const load = useCallback(() => getMyLoans({ status, page }), [status, page])
 *   const { data, error, loading, reload } = useAsync(load)
 *
 * - `load` must come from useCallback: when its inputs change, a new
 *   function is made and the data is loaded again.
 * - While new data loads, the previous data stays (no flashing empty lists).
 * - reload() loads again with the same inputs (e.g. after an action).
 */
export function useAsync(load) {
  const [version, setVersion] = useState(0)
  // The answer remembers which load/version it belongs to,
  // so "loading" is simply: the newest answer has not arrived yet.
  const [state, setState] = useState(null)

  useEffect(() => {
    let cancelled = false
    load()
      .then((data) => !cancelled && setState({ load, version, data }))
      .catch((e) => !cancelled && setState((prev) => ({ load, version, data: prev?.data, error: e.message })))
    return () => {
      cancelled = true
    }
  }, [load, version])

  const current = state?.load === load && state?.version === version
  const reload = useCallback(() => setVersion((v) => v + 1), [])

  return {
    data: state?.data ?? null,
    error: current ? (state.error ?? null) : null,
    loading: !current,
    reload,
  }
}
