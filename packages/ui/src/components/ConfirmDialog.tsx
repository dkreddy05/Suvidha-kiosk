import * as React from "react";
import * as AlertDialogPrimitive from "@radix-ui/react-alert-dialog";
import { cn } from "../lib/utils";

const ConfirmDialog = AlertDialogPrimitive.Root;
const ConfirmDialogTrigger = AlertDialogPrimitive.Trigger;

const ConfirmDialogPortal = AlertDialogPrimitive.Portal;

const ConfirmDialogOverlay = React.forwardRef<
  React.ElementRef<typeof AlertDialogPrimitive.Overlay>,
  React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Overlay>
>(({ className, ...props }, ref) => (
  <AlertDialogPrimitive.Overlay
    className={cn(
      "fixed inset-0 z-50 bg-black/60 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0",
      className
    )}
    {...props}
    ref={ref}
  />
));
ConfirmDialogOverlay.displayName = AlertDialogPrimitive.Overlay.displayName;

const ConfirmDialogContent = React.forwardRef<
  React.ElementRef<typeof AlertDialogPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Content>
>(({ className, ...props }, ref) => (
  <ConfirmDialogPortal>
    <ConfirmDialogOverlay />
    <AlertDialogPrimitive.Content
      ref={ref}
      className={cn(
        "fixed left-[50%] top-[50%] z-50 grid w-full max-w-md translate-x-[-50%] translate-y-[-50%] gap-4 border border-border bg-white dark:bg-surface-dark p-6 shadow-lg duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[state=closed]:slide-out-to-left-1/2 data-[state=closed]:slide-out-to-top-[48%] data-[state=open]:slide-in-from-left-1/2 data-[state=open]:slide-in-from-top-[48%] sm:rounded-xl",
        className
      )}
      {...props}
    />
  </ConfirmDialogPortal>
));
ConfirmDialogContent.displayName = AlertDialogPrimitive.Content.displayName;

const ConfirmDialogHeader = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn("flex flex-col space-y-1.5 text-center sm:text-left", className)} {...props} />
);
ConfirmDialogHeader.displayName = "ConfirmDialogHeader";

const ConfirmDialogFooter = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn("flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2", className)} {...props} />
);
ConfirmDialogFooter.displayName = "ConfirmDialogFooter";

const ConfirmDialogTitle = React.forwardRef<
  React.ElementRef<typeof AlertDialogPrimitive.Title>,
  React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Title>
>(({ className, ...props }, ref) => (
  <AlertDialogPrimitive.Title
    ref={ref}
    className={cn("text-lg font-semibold font-display leading-none tracking-tight", className)}
    {...props}
  />
));
ConfirmDialogTitle.displayName = AlertDialogPrimitive.Title.displayName;

const ConfirmDialogDescription = React.forwardRef<
  React.ElementRef<typeof AlertDialogPrimitive.Description>,
  React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Description>
>(({ className, ...props }, ref) => (
  <AlertDialogPrimitive.Description
    ref={ref}
    className={cn("text-sm text-muted dark:text-muted-dark", className)}
    {...props}
  />
));
ConfirmDialogDescription.displayName = AlertDialogPrimitive.Description.displayName;

const ConfirmDialogAction = React.forwardRef<
  React.ElementRef<typeof AlertDialogPrimitive.Action>,
  React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Action>
>(({ className, ...props }, ref) => (
  <AlertDialogPrimitive.Action
    ref={ref}
    className={cn(
      "inline-flex h-10 items-center justify-center rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50",
      className
    )}
    {...props}
  />
));
ConfirmDialogAction.displayName = AlertDialogPrimitive.Action.displayName;

const ConfirmDialogCancel = React.forwardRef<
  React.ElementRef<typeof AlertDialogPrimitive.Cancel>,
  React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Cancel>
>(({ className, ...props }, ref) => (
  <AlertDialogPrimitive.Cancel
    ref={ref}
    className={cn(
      "inline-flex h-10 items-center justify-center rounded-lg border border-border bg-transparent px-4 py-2 text-sm font-medium hover:bg-gray-50 dark:hover:bg-gray-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 mt-2 sm:mt-0",
      className
    )}
    {...props}
  />
));
ConfirmDialogCancel.displayName = AlertDialogPrimitive.Cancel.displayName;

export {
  ConfirmDialog,
  ConfirmDialogPortal,
  ConfirmDialogOverlay,
  ConfirmDialogTrigger,
  ConfirmDialogContent,
  ConfirmDialogHeader,
  ConfirmDialogFooter,
  ConfirmDialogTitle,
  ConfirmDialogDescription,
  ConfirmDialogAction,
  ConfirmDialogCancel,
};
