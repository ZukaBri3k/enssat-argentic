import React, { useEffect } from "react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { AlertTriangle, Loader2 } from "lucide-react";

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  children?: React.ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: "default" | "destructive";
  isLoading?: boolean;
  onConfirm: () => void | Promise<void>;
}

export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  children,
  confirmLabel = "Confirmer",
  cancelLabel = "Annuler",
  variant = "default",
  isLoading = false,
  onConfirm,
}: ConfirmDialogProps) {
  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !isLoading) {
        onOpenChange(false);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [open, isLoading, onOpenChange]);

  if (!open) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-dialog-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4"
      onClick={() => {
        if (!isLoading) onOpenChange(false);
      }}
    >
      <div
        className="w-full max-w-md text-left"
        onClick={(e) => e.stopPropagation()}
      >
        <Card className="shadow-2xl border-border">
          <CardHeader className="space-y-1.5">
            <div className="flex items-center gap-2.5">
              {variant === "destructive" && (
                <div className="p-2 rounded-lg bg-destructive/15 text-destructive shrink-0">
                  <AlertTriangle className="w-5 h-5" />
                </div>
              )}
              <CardTitle id="confirm-dialog-title" className="text-lg">
                {title}
              </CardTitle>
            </div>
            {description && (
              <CardDescription className="text-sm text-muted-foreground leading-relaxed">
                {description}
              </CardDescription>
            )}
          </CardHeader>

          {children && <CardContent className="text-sm">{children}</CardContent>}

          <CardFooter className="flex items-center justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isLoading}
              className="cursor-pointer"
            >
              {cancelLabel}
            </Button>
            <Button
              type="button"
              variant={variant === "destructive" ? "destructive" : "default"}
              onClick={onConfirm}
              disabled={isLoading}
              className="cursor-pointer flex items-center gap-1.5"
            >
              {isLoading && <Loader2 className="w-4 h-4 animate-spin" />}
              <span>{confirmLabel}</span>
            </Button>
          </CardFooter>
        </Card>
      </div>
    </div>
  );
}
