import { useCallback, useState } from "react";

/** Minimal controlled-form helper: const [values, bind, setValues] = useForm({name: ""}). */
export function useForm(initial) {
  const [values, setValues] = useState(initial);
  const bind = useCallback(
    (name) => ({
      name,
      value: values[name] ?? "",
      onChange: (e) => {
        const target = e.target;
        const value = target.type === "checkbox" ? target.checked : target.value;
        setValues((v) => ({ ...v, [name]: value }));
      },
    }),
    [values]
  );
  return [values, bind, setValues];
}
