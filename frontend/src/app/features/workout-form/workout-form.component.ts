import { Component, inject, signal, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { WorkoutService } from '../../core/services/workout.service';
import { ReminderService } from '../../core/services/reminder.service';
import { UserProfile, WorkoutPlanResponse, AuthRequest, RegisterRequest, UserStatsResponse } from '../../core/models/workout.model';

export interface DaySchedule {
  date: string; // 'YYYY-MM-DD'
  dayName: string;
  isToday: boolean;
  workout?: WorkoutPlanResponse;
}

@Component({
  selector: 'app-workout-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './workout-form.component.html',
  styleUrls: ['./workout-form.component.css']
})
export class WorkoutFormComponent implements OnInit, OnDestroy {
  readonly authService = inject(AuthService);
  readonly reminderService = inject(ReminderService);
  private readonly workoutService = inject(WorkoutService);
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);

  // Estados de Gamificação e Estatísticas
  userStats = signal<UserStatsResponse | null>(null);
  showBadgesModal = signal<boolean>(false);

  // Estados de IA & Feedback
  replacingExerciseId = signal<number | null>(null);
  submittingFeedback = signal<boolean>(false);

  // Estados de Autenticação
  authTab = signal<'login' | 'register'>('login');
  authError = signal<string | null>(null);
  authLoading = signal(false);

  loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4)]]
  });

  registerForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4)]],
    age: [25, [Validators.required, Validators.min(14), Validators.max(100)]],
    fitnessLevel: ['Intermédio', [Validators.required]],
    primaryGoal: ['Hipertrofia & Ganho Muscular', [Validators.required]]
  });

  // Estados do Treino e Calendário
  selectedDate = signal<string>(this.formatDate(new Date()));
  weekDays = signal<DaySchedule[]>([]);
  currentWorkout = signal<WorkoutPlanResponse | null>(null);

  // Escolha personalizada do dia (Nível e Foco)
  dailyFitnessLevel = signal<string>('Intermédio');
  dailyFocusArea = signal<string>('Corpo Inteiro (Full Body)');
  showRegenerateOptions = signal<boolean>(false);

  dailyFocusOptions = [
    { id: 'Membros Superiores', label: 'Membros Superiores', desc: 'Peito, Costas, Ombros e Braços', icon: '💪' },
    { id: 'Membros Inferiores', label: 'Membros Inferiores', desc: 'Pernas, Glúteos e Gémeos', icon: '🦵' },
    { id: 'Core & Abdominais', label: 'Core & Abdominais', desc: 'Tronco, Abdómen e Postura', icon: '🛡️' },
    { id: 'Corpo Inteiro (Full Body)', label: 'Corpo Inteiro', desc: 'Full Body Funcional', icon: '⚡' },
    { id: 'Cardio HIIT', label: 'Cardio HIIT', desc: 'Queima de Calorias & Ritmo', icon: '🔥' },
    { id: 'Mobilidade & Alongamento', label: 'Mobilidade & Alongamento', desc: 'Recuperação Articular', icon: '🧘' }
  ];

  isGenerating = signal(false);
  loadingStep = signal('A ligar ao motor de IA...');
  private loadingInterval: any;

  errorMessage = signal<string | null>(null);

  fitnessLevels = ['Iniciante', 'Intermédio', 'Avançado', 'Atleta'];

  primaryGoals = [
    'Hipertrofia & Ganho Muscular',
    'Força & Resistência',
    'Perda de Gordura & Tonificação',
    'Mobilidade & Condicionamento',
    'Saúde & Longevidade'
  ];

  private readonly steps = [
    '📊 A analisar o nível e o foco escolhido para este dia...',
    '🧠 A desenhar a rotina ideal com Google Gemini...',
    '⚖️ A calibrar volume e exercícios com peso corporal e objetos de casa...',
    '💾 A guardar o teu treino na base de dados...'
  ];

  ngOnInit(): void {
    this.buildWeekSchedule();
    if (this.authService.currentUser()) {
      this.loadUserWorkouts();
    }
  }

  // --- MÉTODOS DE AUTENTICAÇÃO ---

  onLogin(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.authLoading.set(true);
    this.authError.set(null);

    const credentials: AuthRequest = this.loginForm.value;
    this.authService.login(credentials).subscribe({
      next: () => {
        this.authLoading.set(false);
        this.loadUserWorkouts();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.authLoading.set(false);
        this.authError.set(err?.error?.message || 'Email ou palavra-passe incorretos.');
        this.cdr.detectChanges();
      }
    });
  }

  onRegister(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.authLoading.set(true);
    this.authError.set(null);

    const data: RegisterRequest = this.registerForm.value;
    this.authService.register(data).subscribe({
      next: () => {
        this.authLoading.set(false);
        this.loadUserWorkouts();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.authLoading.set(false);
        this.authError.set(err?.error?.message || 'Erro ao criar conta. Verifica os dados.');
        this.cdr.detectChanges();
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.currentWorkout.set(null);
    this.buildWeekSchedule();
    this.cdr.detectChanges();
  }

  // --- GESTÃO SEMANAL E TREINOS ---

  buildWeekSchedule(): void {
    const days: DaySchedule[] = [];
    const now = new Date();
    const currentDayOfWeek = (now.getDay() + 6) % 7; // Segunda = 0, Domingo = 6
    const monday = new Date(now);
    monday.setDate(now.getDate() - currentDayOfWeek);

    const dayNames = ['Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado', 'Domingo'];

    for (let i = 0; i < 7; i++) {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      const dateStr = this.formatDate(d);
      days.push({
        date: dateStr,
        dayName: dayNames[i],
        isToday: dateStr === this.formatDate(now)
      });
    }

    this.weekDays.set(days);
  }

  loadUserWorkouts(): void {
    const user = this.authService.currentUser();
    if (!user?.id) return;

    this.loadUserStats();

    this.workoutService.getUserWorkouts(user.id).subscribe({
      next: (workouts) => {
        const days = this.weekDays().map((day) => {
          const match = workouts.find((w) => w.workoutDate === day.date);
          return { ...day, workout: match };
        });
        this.weekDays.set(days);

        // Selecionar o treino do dia atual
        const selected = days.find((d) => d.date === this.selectedDate());
        this.currentWorkout.set(selected?.workout || null);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao carregar treinos:', err);
      }
    });
  }

  loadUserStats(): void {
    const user = this.authService.currentUser();
    if (!user?.id) return;

    this.workoutService.getUserStats(user.id).subscribe({
      next: (stats) => {
        this.userStats.set(stats);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao carregar estatísticas:', err);
      }
    });
  }

  selectDay(day: DaySchedule): void {
    this.selectedDate.set(day.date);
    this.currentWorkout.set(day.workout || null);
    this.showRegenerateOptions.set(false);
    this.errorMessage.set(null);

    const user = this.authService.currentUser();
    if (day.workout) {
      if (day.workout.focusArea) {
        this.dailyFocusArea.set(day.workout.focusArea);
      }
    } else {
      // Pre-preencher com o nível atual do utilizador
      if (user?.fitnessLevel) {
        this.dailyFitnessLevel.set(user.fitnessLevel);
      }
      // Sugestão de foco dinâmica para novos treinos
      const dayOfWeek = new Date(day.date + 'T00:00:00').getDay();
      switch (dayOfWeek) {
        case 1: // Segunda
          this.dailyFocusArea.set('Membros Superiores');
          break;
        case 2: // Terça
          this.dailyFocusArea.set('Membros Inferiores');
          break;
        case 3: // Quarta
          this.dailyFocusArea.set('Core & Abdominais');
          break;
        case 4: // Quinta
          this.dailyFocusArea.set('Cardio HIIT');
          break;
        case 5: // Sexta
          this.dailyFocusArea.set('Corpo Inteiro (Full Body)');
          break;
        case 6: // Sábado
          this.dailyFocusArea.set('Mobilidade & Alongamento');
          break;
        default: // Domingo
          this.dailyFocusArea.set('Corpo Inteiro (Full Body)');
          break;
      }
    }
    this.cdr.detectChanges();
  }

  setDailyFitnessLevel(level: string): void {
    this.dailyFitnessLevel.set(level);
    this.cdr.detectChanges();
  }

  setDailyFocusArea(focusId: string): void {
    this.dailyFocusArea.set(focusId);
    this.cdr.detectChanges();
  }

  toggleRegenerateOptions(): void {
    this.showRegenerateOptions.set(!this.showRegenerateOptions());
    this.cdr.detectChanges();
  }

  generateForSelectedDay(): void {
    const user = this.authService.currentUser();
    if (!user?.id) return;

    this.isGenerating.set(true);
    this.errorMessage.set(null);

    let stepIndex = 0;
    this.loadingStep.set(this.steps[0]);
    this.loadingInterval = setInterval(() => {
      stepIndex = (stepIndex + 1) % this.steps.length;
      this.loadingStep.set(this.steps[stepIndex]);
      this.cdr.detectChanges();
    }, 3200);

    this.workoutService.generateWorkout(
      user.id,
      this.selectedDate(),
      this.dailyFitnessLevel(),
      this.dailyFocusArea()
    ).subscribe({
      next: (plan) => {
        this.currentWorkout.set(plan);
        this.showRegenerateOptions.set(false);
        this.isGenerating.set(false);
        if (this.loadingInterval) clearInterval(this.loadingInterval);

        // Atualizar a lista de dias
        const updatedDays = this.weekDays().map((d) =>
          d.date === this.selectedDate() ? { ...d, workout: plan } : d
        );
        this.weekDays.set(updatedDays);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao gerar treino:', err);
        this.errorMessage.set(
          err?.error?.message ||
          'Não foi possível gerar o treino. Verifica se o backend está ativo.'
        );
        this.isGenerating.set(false);
        if (this.loadingInterval) clearInterval(this.loadingInterval);
        this.cdr.detectChanges();
      }
    });
  }

  toggleExercise(exerciseId: number): void {
    const workout = this.currentWorkout();
    if (!workout) return;

    this.workoutService.toggleExercise(workout.id, exerciseId).subscribe({
      next: (updatedPlan) => {
        this.currentWorkout.set(updatedPlan);

        // Atualizar estado no calendário
        const updatedDays = this.weekDays().map((d) =>
          d.date === updatedPlan.workoutDate ? { ...d, workout: updatedPlan } : d
        );
        this.weekDays.set(updatedDays);

        // Se o treino ficou concluído ou alterou estado, recarrega as estatísticas e streak
        this.loadUserStats();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao alternar exercício:', err);
      }
    });
  }

  replaceExercise(event: Event, exerciseId: number): void {
    event.stopPropagation(); // Não marcar a checkbox
    const workout = this.currentWorkout();
    if (!workout || this.replacingExerciseId() !== null) return;

    this.replacingExerciseId.set(exerciseId);

    this.workoutService.replaceExercise(workout.id, exerciseId).subscribe({
      next: (updatedPlan) => {
        this.currentWorkout.set(updatedPlan);

        const updatedDays = this.weekDays().map((d) =>
          d.date === updatedPlan.workoutDate ? { ...d, workout: updatedPlan } : d
        );
        this.weekDays.set(updatedDays);

        this.replacingExerciseId.set(null);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao substituir exercício com IA:', err);
        this.replacingExerciseId.set(null);
        alert('Não foi possível substituir o exercício de momento. Tenta novamente.');
        this.cdr.detectChanges();
      }
    });
  }

  submitFeedback(difficulty: string): void {
    const workout = this.currentWorkout();
    if (!workout) return;

    this.submittingFeedback.set(true);

    this.workoutService.submitFeedback(workout.id, difficulty).subscribe({
      next: (updatedPlan) => {
        this.currentWorkout.set(updatedPlan);
        this.submittingFeedback.set(false);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao submeter feedback:', err);
        this.submittingFeedback.set(false);
      }
    });
  }

  toggleReminder(): void {
    this.reminderService.toggleReminder();
  }

  getCompletedCount(): number {
    const workout = this.currentWorkout();
    if (!workout?.exercises) return 0;
    return workout.exercises.filter((e) => e.completed).length;
  }

  getProgressPercentage(): number {
    const workout = this.currentWorkout();
    if (!workout?.exercises?.length) return 0;
    return Math.round((this.getCompletedCount() / workout.exercises.length) * 100);
  }

  private formatDate(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  ngOnDestroy(): void {
    if (this.loadingInterval) {
      clearInterval(this.loadingInterval);
    }
  }
}
