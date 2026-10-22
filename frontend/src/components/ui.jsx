import { dueProgress, dueText, label, toneOf } from "../utils/format";

/** Status or category shown as a coloured dot plus text. */
export function Tag({ value, text }) {
  return (
    <span className={`tag tone-${toneOf(value)}`}>
      <span className="tag-dot" aria-hidden="true" />
      {text ?? label(value)}
    </span>
  );
}

/** How much of a maintenance interval has been used, with the remaining time in words. */
export function DueGauge({ schedule }) {
  const tone = toneOf(schedule.dueStatus);
  const pct = Math.round(dueProgress(schedule) * 100);
  return (
    <div className="gauge" title={`${pct}% of the interval used`}>
      <div className="gauge-track">
        <div className={`gauge-fill tone-${tone}`} style={{ width: `${Math.max(pct, 4)}%` }} />
      </div>
      <span className={`gauge-text tone-${tone}`}>{dueText(schedule)}</span>
    </div>
  );
}

export function Alert({ error, children, tone = "danger" }) {
  const text = children ?? error?.message;
  if (!text) return null;
  return (
    <div className={`alert alert-${tone}`} role={tone === "danger" ? "alert" : "status"}>
      {text}
    </div>
  );
}

export function Loading({ what = "Loading" }) {
  return <p className="muted">{what}…</p>;
}

export function Empty({ children }) {
  return <p className="empty">{children}</p>;
}

export function PageHeader({ title, subtitle, children }) {
  return (
    <header className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {children && <div className="page-actions">{children}</div>}
    </header>
  );
}

export function Pagination({ page, onPage }) {
  if (!page || page.totalPages <= 1) return null;
  return (
    <div className="pagination">
      <button className="btn btn-quiet" disabled={page.pageNumber === 0} onClick={() => onPage(page.pageNumber - 1)}>
        Previous
      </button>
      <span className="muted">
        Page {page.pageNumber + 1} of {page.totalPages} ({page.totalElements} total)
      </span>
      <button className="btn btn-quiet" disabled={page.last} onClick={() => onPage(page.pageNumber + 1)}>
        Next
      </button>
    </div>
  );
}

export function Field({ id, label: text, error, children, hint }) {
  return (
    <div className="field">
      <label htmlFor={id}>{text}</label>
      {children}
      {hint && <span className="hint">{hint}</span>}
      {error && <span className="field-error">{error}</span>}
    </div>
  );
}
