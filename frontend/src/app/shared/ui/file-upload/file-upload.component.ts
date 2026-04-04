import { Component, EventEmitter, Output } from '@angular/core';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  template: `
    <label class="card file-upload">
      <span>Subir archivo</span>
      <input type="file" (change)="onFileSelect($event)" />
    </label>
  `,
  styles: [
    `
      .file-upload {
        display: grid;
        gap: 0.5rem;
        padding: 1rem;
        border: 2px dashed rgba(242, 53, 53, 0.55);
      }
    `
  ]
})
export class FileUploadComponent {
  @Output() readonly fileSelected = new EventEmitter<File>();

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.fileSelected.emit(file);
    }
  }
}