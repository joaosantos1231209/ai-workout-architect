import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthRequest, RegisterRequest, UserProfile } from '../models/workout.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private readonly STORAGE_KEY = 'ai_workout_user';
  currentUser = signal<UserProfile | null>(this.loadUserFromStorage());

  constructor() {
    this.pingServer();
  }

  pingServer(): void {
    this.http.get(`${this.baseUrl}/ping`, { responseType: 'text' }).subscribe({
      next: () => {},
      error: () => {}
    });
  }

  login(credentials: AuthRequest): Observable<UserProfile> {
    return this.http.post<UserProfile>(`${this.baseUrl}/login`, credentials).pipe(
      tap((user) => this.setCurrentUser(user))
    );
  }

  register(data: RegisterRequest): Observable<UserProfile> {
    return this.http.post<UserProfile>(`${this.baseUrl}/register`, data).pipe(
      tap((user) => this.setCurrentUser(user))
    );
  }

  logout(): void {
    localStorage.removeItem(this.STORAGE_KEY);
    this.currentUser.set(null);
  }

  private setCurrentUser(user: UserProfile): void {
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(user));
    this.currentUser.set(user);
  }

  private loadUserFromStorage(): UserProfile | null {
    try {
      const data = localStorage.getItem(this.STORAGE_KEY);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  }
}
