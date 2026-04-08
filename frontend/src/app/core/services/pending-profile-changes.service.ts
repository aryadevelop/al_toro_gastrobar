import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class PendingProfileChangesService {
  private readonly hasUnsavedChangesState = signal(false);
  private readonly skipNextPromptState = signal(false);

  hasUnsavedChanges(): boolean {
    return this.hasUnsavedChangesState();
  }

  setHasUnsavedChanges(value: boolean): void {
    this.hasUnsavedChangesState.set(value);
  }

  skipNextPrompt(): void {
    this.skipNextPromptState.set(true);
  }

  consumeSkipNextPrompt(): boolean {
    const shouldSkip = this.skipNextPromptState();
    if (shouldSkip) {
      this.skipNextPromptState.set(false);
    }
    return shouldSkip;
  }
}
