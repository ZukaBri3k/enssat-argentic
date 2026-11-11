import { AlertCircle } from "lucide-react";
import { ApiError } from "@/lib/api";

interface ErrorAlertProps {
  error: unknown;
  title?: string;
}

export function ErrorAlert({ error, title }: ErrorAlertProps) {
  if (!error) return null;

  let displayTitle = title;
  let detailMessage = "";
  let fieldErrors: Record<string, string> | undefined;

  if (error instanceof ApiError) {
    displayTitle = displayTitle || error.problem?.title || `Erreur (${error.status})`;
    detailMessage = error.problem?.detail || error.message;
    fieldErrors = error.fieldErrors;
  } else if (error instanceof Error) {
    detailMessage = error.message;
  } else if (typeof error === "string") {
    detailMessage = error;
  } else {
    detailMessage = "Une erreur inattendue est survenue.";
  }

  return (
    <div
      role="alert"
      className="flex items-start gap-2.5 p-3.5 rounded-xl bg-destructive/10 border border-destructive/25 text-destructive text-sm text-left"
    >
      <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
      <div className="space-y-1 flex-1">
        {displayTitle && <div className="font-semibold leading-tight">{displayTitle}</div>}
        {detailMessage && <div className="leading-snug">{detailMessage}</div>}
        {fieldErrors && Object.keys(fieldErrors).length > 0 && (
          <ul className="list-disc list-inside text-xs space-y-0.5 pt-1">
            {Object.entries(fieldErrors).map(([field, msg]) => (
              <li key={field}>
                <span className="font-semibold">{field}</span> : {msg}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
