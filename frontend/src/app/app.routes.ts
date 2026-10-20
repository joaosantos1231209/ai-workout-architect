import { Routes } from '@angular/router';
import { WorkoutFormComponent } from './features/workout-form/workout-form.component';

export const routes: Routes = [
  { path: '', component: WorkoutFormComponent },
  { path: '**', redirectTo: '' }
];
