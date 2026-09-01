import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-nav-card',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './nav-card.html',
  styleUrl: './nav-card.css',
})
export class NavCardComponent {
  @Input() icon = '📦';
  @Input({ required: true }) title!: string;
  @Input() description = '';
  @Input() link: string | null = null;   // si viene, se renderiza como <a>
  @Input() disabled = false;
  @Output() cardClick = new EventEmitter<void>();

  protected onClick(): void {
    if (this.disabled || this.link) return;
    this.cardClick.emit();
  }
}