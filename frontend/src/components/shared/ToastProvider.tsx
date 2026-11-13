import React, { createContext, useCallback, useContext, useState } from "react";
import { AlertCircle, CheckCircle2, Info, AlertTriangle, X } from "lucide-react";
import { ApiError } from "@/lib/api";

export type ToastVariant = "success" | "error" | "info" | "warning";

export interface ToastItem {
  id: string;
  title: string;
  description?: string;
  fieldErrors?: Record<string, string>;
  variant: ToastVariant;
}

interface ToastContextValue {
  toast: (options: Omit<ToastItem, "id">) => void;
  success: (title: string, description?: string) => void;
  info: (title: string, description?: string) => void;
  warning: (title: string, description?: string) => void;
  error: (title: string, description?: string, fieldErrors?: Record<string, string>) => void;
  showApiError: (err: unknown, fallbackTitle?: string) => void;
  dismiss: (id: string) => void;
}

const ToastContext = createContext<ToastContextValue | undefined>(undefined);

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const dismiss = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const toast = useCallback(
    (options: Omit<ToastItem, "id">) => {
      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
      setToasts((prev) => [...prev, { ...options, id }]);
      setTimeout(() => {
        dismiss(id);
      }, 5000);
    },
    [dismiss]
  );

  const success = useCallback(
    (title: string, description?: string) => {
      toast({ title, description, variant: "success" });
    },
    [toast]
  );

  const info = useCallback(
    (title: string, description?: string) => {
      toast({ title, description, variant: "info" });
    },
    [toast]
  );

  const warning = useCallback(
    (title: string, description?: string) => {
      toast({ title, description, variant: "warning" });
    },
    [toast]
  );

  const error = useCallback(
    (title: string, description?: string, fieldErrors?: Record<string, string>) => {
      toast({ title, description, fieldErrors, variant: "error" });
    },
    [toast]
  );

  const showApiError = useCallback(
    (err: unknown, fallbackTitle = "Erreur") => {
      if (err instanceof ApiError) {
        toast({
          title: err.problem?.title || fallbackTitle,
          description: err.problem?.detail || err.message,
          fieldErrors: err.fieldErrors,
          variant: "error",
        });
      } else if (err instanceof Error) {
        toast({
          title: fallbackTitle,
          description: err.message,
          variant: "error",
        });
      } else {
        toast({
          title: fallbackTitle,
          description: "Une erreur inattendue est survenue.",
          variant: "error",
        });
      }
    },
    [toast]
  );

  return (
    <ToastContext.Provider value={{ toast, success, info, warning, error, showApiError, dismiss }}>
      {children}
      <div
        aria-live="polite"
        className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 w-full max-w-sm pointer-events-none px-4 sm:px-0"
      >
        {toasts.map((item) => {
          const styles: Record<ToastVariant, string> = {
            success:
              "bg-card border-green-500/40 text-foreground shadow-lg ring-1 ring-green-500/20",
            error:
              "bg-card border-destructive/40 text-foreground shadow-lg ring-1 ring-destructive/20",
            warning:
              "bg-card border-amber-500/40 text-foreground shadow-lg ring-1 ring-amber-500/20",
            info: "bg-card border-border text-foreground shadow-lg ring-1 ring-foreground/10",
          };

          const icons: Record<ToastVariant, React.ReactNode> = {
            success: <CheckCircle2 className="w-4 h-4 text-green-600 shrink-0 mt-0.5" />,
            error: <AlertCircle className="w-4 h-4 text-destructive shrink-0 mt-0.5" />,
            warning: <AlertTriangle className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />,
            info: <Info className="w-4 h-4 text-primary shrink-0 mt-0.5" />,
          };

          return (
            <div
              key={item.id}
              role="status"
              className={`pointer-events-auto flex items-start gap-3 rounded-xl border p-3.5 text-left text-sm transition-all ${styles[item.variant]}`}
            >
              {icons[item.variant]}
              <div className="flex-1 min-w-0 space-y-1">
                <div className="font-semibold leading-tight">{item.title}</div>
                {item.description && (
                  <div className="text-xs text-muted-foreground leading-snug">
                    {item.description}
                  </div>
                )}
                {item.fieldErrors && Object.keys(item.fieldErrors).length > 0 && (
                  <ul className="text-xs text-destructive list-disc list-inside pt-1 space-y-0.5">
                    {Object.entries(item.fieldErrors).map(([field, msg]) => (
                      <li key={field}>
                        <span className="font-medium">{field}</span> : {msg}
                      </li>
                    ))}
                  </ul>
                )}
              </div>
              <button
                type="button"
                onClick={() => dismiss(item.id)}
                className="text-muted-foreground hover:text-foreground cursor-pointer p-0.5"
                aria-label="Fermer la notification"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error("useToast doit être utilisé dans un ToastProvider");
  }
  return ctx;
}
