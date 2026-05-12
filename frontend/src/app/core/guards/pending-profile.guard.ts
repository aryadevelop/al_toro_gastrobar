import { CanDeactivateFn } from '@angular/router';
import { Observable } from 'rxjs';

export interface CanExitWithPendingChanges {
  canDeactivate: () => boolean | Observable<boolean>;
}

export const pendingProfileGuard: CanDeactivateFn<CanExitWithPendingChanges> = (component) => {
  return component.canDeactivate();
};
