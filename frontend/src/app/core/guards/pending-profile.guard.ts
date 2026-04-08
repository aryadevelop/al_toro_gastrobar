import { CanDeactivateFn } from '@angular/router';

export interface CanExitWithPendingChanges {
  canDeactivate: () => boolean;
}

export const pendingProfileGuard: CanDeactivateFn<CanExitWithPendingChanges> = (component) => {
  return component.canDeactivate();
};
