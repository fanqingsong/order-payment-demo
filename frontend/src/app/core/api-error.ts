import { HttpErrorResponse } from '@angular/common/http';

interface ApiErrorBody {
  message?: string;
  validationErrors?: Record<string, string>;
}

export function apiErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'An unexpected error occurred.';
  }

  const body = error.error as ApiErrorBody | undefined;
  const validationMessage = body?.validationErrors
    ? Object.values(body.validationErrors).join(' ')
    : undefined;

  return validationMessage ?? body?.message ?? 'The request could not be completed.';
}
