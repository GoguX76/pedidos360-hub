import { Directive, ElementRef, HostListener, output } from '@angular/core';

@Directive({
  selector: '[clickOutside]',
  standalone: true,
})
export class ClickOutsideDirective {
  clickOutside = output<void>();

  constructor(private elementRef: ElementRef) {}

  @HostListener('document:click', ['$event.target'])
  onClick(target: HTMLElement): void {
    if (!this.elementRef.nativeElement.contains(target)) {
      this.clickOutside.emit();
    }
  }
}
