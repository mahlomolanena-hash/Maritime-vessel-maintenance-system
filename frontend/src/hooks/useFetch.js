import { useCallback, useEffect, useRef, useState } from "react";

/**
 * Runs an async loader and tracks loading/error state. The loader is re-run when `key` changes
 * (pass any value that identifies the request, e.g. the serialised filters) or when reload() is
 * called. Previous data stays visible while a new request is in flight, and responses that arrive
 * for an outdated request are ignored.
 */
export function useFetch(loader, key = "") {
  const [tick, setTick] = useState(0);
  const [state, setState] = useState({ token: null, data: null, error: null });
  const loaderRef = useRef(loader);
  const token = `${key}#${tick}`;

  useEffect(() => {
    loaderRef.current = loader;
  });

  useEffect(() => {
    let cancelled = false;
    loaderRef
      .current()
      .then((data) => !cancelled && setState({ token, data, error: null }))
      .catch((error) => !cancelled && setState((s) => ({ token, data: s.data, error })));
    return () => {
      cancelled = true;
    };
  }, [token]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  const loading = state.token !== token;
  return { data: state.data, error: loading ? null : state.error, loading, reload };
}
