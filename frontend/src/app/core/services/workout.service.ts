import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { WorkoutPlanResponse } from '../models/workout.model';

@Injectable({
  providedIn: 'root'
})
export class WorkoutService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/workouts`;

  /**
   * Gera um treino em casa com IA para um utilizador numa data específica,
   * com nível e foco personalizados para esse dia.
   */
  generateWorkout(
    userId: number,
    date: string,
    fitnessLevel?: string,
    focusArea?: string
  ): Observable<WorkoutPlanResponse> {
    let url = `${this.baseUrl}/generate?userId=${userId}&date=${date}`;
    if (fitnessLevel) {
      url += `&fitnessLevel=${encodeURIComponent(fitnessLevel)}`;
    }
    if (focusArea) {
      url += `&focusArea=${encodeURIComponent(focusArea)}`;
    }
    return this.http.post<WorkoutPlanResponse>(url, {});
  }

  /**
   * Obtém todos os treinos do utilizador com as respetivas datas e status.
   */
  getUserWorkouts(userId: number): Observable<WorkoutPlanResponse[]> {
    return this.http.get<WorkoutPlanResponse[]>(`${this.baseUrl}/user/${userId}`);
  }

  /**
   * Obtém o treino de uma data específica para o utilizador.
   */
  getWorkoutByDate(userId: number, date: string): Observable<WorkoutPlanResponse> {
    return this.http.get<WorkoutPlanResponse>(`${this.baseUrl}/user/${userId}/date/${date}`);
  }

  /**
   * Assinala um exercício como feito ou pendente (toggle).
   */
  toggleExercise(workoutId: number, exerciseId: number): Observable<WorkoutPlanResponse> {
    return this.http.patch<WorkoutPlanResponse>(
      `${this.baseUrl}/${workoutId}/exercises/${exerciseId}/toggle`,
      {}
    );
  }
}
