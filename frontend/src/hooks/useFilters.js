import { useSearchParams } from "react-router-dom";

/** Keeps list filters and the page number in the URL so views can be bookmarked and linked to. */
export function useFilters() {
  const [params, setParams] = useSearchParams();

  const get = (name) => params.get(name) || "";
  const set = (name, value) => {
    const next = new URLSearchParams(params);
    if (value === "" || value === false || value === null || value === undefined) next.delete(name);
    else next.set(name, value === true ? "1" : value);
    if (name !== "page") next.delete("page");
    setParams(next, { replace: true });
  };
  const page = Number(params.get("page") || 0);

  return { get, set, page, key: params.toString() };
}
