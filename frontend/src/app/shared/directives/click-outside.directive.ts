import { Directive, ElementRef, HostListener, output } from '@angular/core';

@Directive({
  selector: '[clickOutside]',
  standalone: true,
})
export class ClickOutsideDirective {
  clickOutside = output<void>();

  constructor(private elementRef: ElementRef) {}

  @HostListener('document:click', ['$event.target'])
  onClick(target: EventTarget | null): void {
    const host = this.elementRef.nativeElement as Node;
    if (target instanceof Node && !host.contains(target)) {
      this.clickOutside.emit();
    }
  }
}
