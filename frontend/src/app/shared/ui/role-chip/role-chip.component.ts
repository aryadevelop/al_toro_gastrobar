import { Component, Input } from '@angular/core';
import { Role } from '../../../core/models/domain.models';

@Component({
  selector: 'app-role-chip',
  standalone: true,
  template: `<span class="chip">{{ role }}</span>`,
  styles: [
    `
      .chip {
        display: inline-block;
        font-size: 0.75rem;
        font-weight: 700;
        border-radius: 999px;
        padding: 0.2rem 0.55rem;
        background: #edf3ef;
        color: #0b3f2f;
      }
    `
  ]
})
export class RoleChipComponent {
  @Input() role: Role = 'CLIENTE';
}