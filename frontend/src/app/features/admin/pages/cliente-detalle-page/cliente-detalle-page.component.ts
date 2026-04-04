import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { UserManagementService } from '../../../../core/services/user-management.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-cliente-detalle-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PageHeaderComponent],
  template: `
    <section class="page-grid">
      <app-page-header title="Detalle de cliente" subtitle="Actualizar estado y datos principales"></app-page-header>
      <article class="card" style="padding: 1rem; max-width: 760px;">
        <form class="form-grid" [formGroup]="form" (ngSubmit)="onSubmit()">
          <label><span>Nombre completo</span><input class="input-field" formControlName="fullName" /></label>
          <label><span>Correo</span><input class="input-field" type="email" formControlName="email" /></label>
          <label><span>Telefono</span><input class="input-field" formControlName="phone" /></label>
          <label>
            <span>Estado</span>
            <select class="input-field" formControlName="status">
              <option value="ACTIVE">Activo</option>
              <option value="INACTIVE">Inactivo</option>
            </select>
          </label>
          <button class="btn-primary" type="submit">Guardar cliente</button>
          <p *ngIf="saved()" style="margin:0; color: var(--success);">Cliente actualizado (simulado).</p>
        </form>
      </article>
    </section>
  `
})
export class ClienteDetallePageComponent implements OnInit {
  readonly saved = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    status: ['ACTIVE' as 'ACTIVE' | 'INACTIVE', [Validators.required]]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly userManagementService: UserManagementService,
    private readonly activatedRoute: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const userId = this.activatedRoute.snapshot.paramMap.get('id');
    this.userManagementService.listClientes().subscribe((users) => {
      const selected = users.find((user) => user.id === userId) ?? users[0];
      if (selected) {
        this.form.patchValue({
          fullName: selected.fullName,
          email: selected.email,
          phone: selected.phone ?? '',
          status: selected.status
        });
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saved.set(true);
  }
}
