import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { GlobalLoaderComponent } from './shared/components/global-loader/global-loader.component';
import { GlobalToastComponent } from './shared/components/global-toast/global-toast.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavbarComponent, GlobalLoaderComponent, GlobalToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('greensure-frontend');
}
