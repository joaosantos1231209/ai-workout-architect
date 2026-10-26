import { Component, inject, signal, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { WorkoutService } from '../../core/services/workout.service';
import { ReminderService } from '../../core/services/reminder.service';
import { UserProfile, WorkoutPlanResponse, AuthRequest, RegisterRequest, UserStatsResponse, AiWorkoutSuggestion } from '../../core/models/workout.model';
import { environment } from '../../../environments/environment';

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

  // Gestão de Abas da Aplicação ('workouts' | 'streaks' | 'profile')
  activeTab = signal<'workouts' | 'streaks' | 'profile'>('workouts');

  // Versão Dinâmica da Aplicação
  readonly appVersion = environment.version || 'v1.6.0';

  // Estados de Aquecimento Pré-Treino & Volta à Calma Pós-Treino
  warmupCompleted = signal<boolean>(false);
  cooldownCompleted = signal<boolean>(false);

  // Sugestão Inteligente da IA
  aiSuggestion = signal<AiWorkoutSuggestion | null>(null);
  suggestionApplied = signal<boolean>(false);

  // Estados de Gamificação e Estatísticas
  userStats = signal<UserStatsResponse | null>(null);
  allUserWorkouts = signal<WorkoutPlanResponse[]>([]);
  showBadgesModal = signal<boolean>(false);

  readonly allBadges = [
    {
      id: 'first_step',
      icon: '🥉',
      title: 'Primeiro Passo',
      desc: 'Completaste o teu 1º treino em casa com sucesso',
      target: 1,
      type: 'workouts'
    },
    {
      id: 'flame',
      icon: '🔥',
      title: 'Chama Acesa',
      desc: '3 dias consecutivos de treino sem falhar',
      target: 3,
      type: 'streak'
    },
    {
      id: 'home_warrior',
      icon: '💪',
      title: 'Guerreiro do Lar',
      desc: '5 treinos em casa completados',
      target: 5,
      type: 'workouts'
    },
    {
      id: 'iron_week',
      icon: '⚡',
      title: 'Semana de Ferro',
      desc: '7 dias seguidos de foco total',
      target: 7,
      type: 'streak'
    },
    {
      id: 'master',
      icon: '🏆',
      title: 'Mestre da Consistência',
      desc: '10 treinos no teu historial',
      target: 10,
      type: 'workouts'
    },
    {
      id: 'legend',
      icon: '👑',
      title: 'Lenda do Treino',
      desc: '30 treinos registados e cumpridos',
      target: 30,
      type: 'workouts'
    }
  ];

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
    const user = this.authService.currentUser();
    if (user?.id) {
      this.loadCachedData(user.id);
      this.loadUserWorkouts();
    }
  }

  private loadCachedData(userId: number): void {
    try {
      const cachedStats = localStorage.getItem(`ai_workout_stats_${userId}`);
      if (cachedStats) {
        this.userStats.set(JSON.parse(cachedStats));
      }
      const cachedWorkouts = localStorage.getItem(`ai_workout_workouts_${userId}`);
      if (cachedWorkouts) {
        const workouts: WorkoutPlanResponse[] = JSON.parse(cachedWorkouts);
        this.allUserWorkouts.set(workouts);
        const days = this.weekDays().map((day) => {
          const match = workouts.find((w) => w.workoutDate === day.date);
          return { ...day, workout: match };
        });
        this.weekDays.set(days);
        const selected = days.find((d) => d.date === this.selectedDate());
        if (selected?.workout) {
          this.currentWorkout.set(selected.workout);
          this.warmupCompleted.set(selected.workout.completed);
          this.cooldownCompleted.set(selected.workout.completed);
        }
        this.calculateAiSuggestion();
      }
      this.cdr.detectChanges();
    } catch (e) {
      console.warn('Erro ao carregar dados do cache local:', e);
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
      next: (user) => {
        this.authLoading.set(false);
        if (user?.id) {
          this.loadCachedData(user.id);
        }
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
      next: (user) => {
        this.authLoading.set(false);
        if (user?.id) {
          this.loadCachedData(user.id);
        }
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
    const user = this.authService.currentUser();
    if (user?.id) {
      localStorage.removeItem(`ai_workout_stats_${user.id}`);
      localStorage.removeItem(`ai_workout_workouts_${user.id}`);
    }
    this.authService.logout();
    this.currentWorkout.set(null);
    this.allUserWorkouts.set([]);
    this.userStats.set(null);
    this.activeTab.set('workouts');
    this.buildWeekSchedule();
    this.cdr.detectChanges();
  }

  setActiveTab(tab: 'workouts' | 'streaks' | 'profile'): void {
    this.activeTab.set(tab);
    window.scrollTo({ top: 0, behavior: 'smooth' });
    this.cdr.detectChanges();
  }

  isBadgeUnlocked(badge: any): boolean {
    const stats = this.userStats();
    if (!stats) return false;
    if (badge.type === 'streak') {
      return (stats.streakDays || 0) >= badge.target;
    }
    return (stats.totalCompletedWorkouts || 0) >= badge.target;
  }

  getBadgeProgress(badge: any): number {
    const stats = this.userStats();
    if (!stats) return 0;
    const current = badge.type === 'streak' ? (stats.streakDays || 0) : (stats.totalCompletedWorkouts || 0);
    return Math.min(100, Math.round((current / badge.target) * 100));
  }

  sendTestNotification(): void {
    this.reminderService.triggerNotification(
      'AI Workout Architect 💪',
      'O teu sistema de lembretes está a funcionar perfeitamente! Lembra-te: o teu treino espera por ti às 09:30.'
    );
  }

  toggleWarmup(): void {
    this.warmupCompleted.update((val) => !val);
  }

  toggleCooldown(): void {
    this.cooldownCompleted.update((val) => !val);
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

  getTodayDate(): string {
    return this.formatDate(new Date());
  }

  getSelectedDaySchedule(): DaySchedule | undefined {
    return this.weekDays().find((d) => d.date === this.selectedDate());
  }

  getYesterdaySchedule(): DaySchedule | undefined {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yStr = this.formatDate(yesterday);
    return this.weekDays().find((d) => d.date === yStr);
  }

  selectToday(): void {
    const today = this.weekDays().find((d) => d.isToday);
    if (today) {
      this.selectDay(today);
    }
  }

  selectYesterday(): void {
    const yesterday = this.getYesterdaySchedule();
    if (yesterday) {
      this.selectDay(yesterday);
    }
  }

  getSelectedDayFormatted(): string {
    const schedule = this.getSelectedDaySchedule();
    if (!schedule) return this.selectedDate();
    const parts = schedule.date.split('-');
    const day = parts[2];
    const monthNames = [
      'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
      'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
    ];
    const month = monthNames[parseInt(parts[1], 10) - 1];
    const label = `${schedule.dayName}, ${day} de ${month}`;
    return schedule.isToday ? `${label} (Hoje)` : label;
  }

  loadUserWorkouts(): void {
    const user = this.authService.currentUser();
    if (!user?.id) return;

    this.loadUserStats();

    this.workoutService.getUserWorkouts(user.id).subscribe({
      next: (workouts) => {
        this.allUserWorkouts.set(workouts);
        try {
          localStorage.setItem(`ai_workout_workouts_${user.id}`, JSON.stringify(workouts));
        } catch {}
        const days = this.weekDays().map((day) => {
          const match = workouts.find((w) => w.workoutDate === day.date);
          return { ...day, workout: match };
        });
        this.weekDays.set(days);

        // Selecionar o treino do dia atual
        const selected = days.find((d) => d.date === this.selectedDate());
        this.currentWorkout.set(selected?.workout || null);
        this.calculateAiSuggestion();
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
        try {
          localStorage.setItem(`ai_workout_stats_${user.id}`, JSON.stringify(stats));
        } catch {}
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
    this.warmupCompleted.set(day.workout?.completed ? true : false);
    this.cooldownCompleted.set(day.workout?.completed ? true : false);
    this.showRegenerateOptions.set(false);
    this.errorMessage.set(null);

    const user = this.authService.currentUser();
    if (day.workout) {
      if (day.workout.focusArea) {
        this.dailyFocusArea.set(day.workout.focusArea);
      }
    } else {
      // Calcula a sugestão inteligente da IA com base no histórico
      this.calculateAiSuggestion();
      const suggestion = this.aiSuggestion();
      if (suggestion) {
        this.dailyFitnessLevel.set(suggestion.fitnessLevel);
        this.dailyFocusArea.set(suggestion.focusArea);
      } else if (user?.fitnessLevel) {
        this.dailyFitnessLevel.set(user.fitnessLevel);
      }
    }
    this.cdr.detectChanges();
  }

  calculateAiSuggestion(): void {
    const user = this.authService.currentUser();
    const workouts = this.allUserWorkouts();

    // Filtra treinos concluídos ou anteriores à data selecionada, ordenados pelo mais recente
    const pastWorkouts = [...workouts]
      .filter((w) => w.completed || w.workoutDate < this.selectedDate())
      .sort((a, b) => b.workoutDate.localeCompare(a.workoutDate));

    let focusArea = 'Corpo Inteiro (Full Body)';
    let fitnessLevel = user?.fitnessLevel || 'Intermédio';
    let reason = '';
    let highlightBadge = 'Equilíbrio Semanal';
    let icon = '⚡';

    if (pastWorkouts.length === 0) {
      focusArea = 'Corpo Inteiro (Full Body)';
      fitnessLevel = user?.fitnessLevel || 'Iniciante';
      highlightBadge = 'Primeiro Treino';
      icon = '🏠';
      reason = 'Como estás a começar a tua jornada, a IA sugere um treino de Corpo Inteiro funcional para ativares os músculos em casa de forma progressiva e segura.';
    } else {
      const last = pastWorkouts[0];
      const lastFocus = (last.focusArea || '').toLowerCase();
      const lastDate = last.workoutDate;

      // Calibração de intensidade com base no feedback percebido
      if (last.perceivedDifficulty === 'Muito Fácil') {
        if (fitnessLevel === 'Iniciante') fitnessLevel = 'Intermédio';
        else if (fitnessLevel === 'Intermédio') fitnessLevel = 'Avançado';
        else if (fitnessLevel === 'Avançado') fitnessLevel = 'Atleta';
        highlightBadge = 'Progressão de Carga';
      } else if (last.perceivedDifficulty === 'Muito Difícil') {
        if (fitnessLevel === 'Atleta') fitnessLevel = 'Avançado';
        else if (fitnessLevel === 'Avançado') fitnessLevel = 'Intermédio';
        else if (fitnessLevel === 'Intermédio') fitnessLevel = 'Iniciante';
        highlightBadge = 'Prevenção de Fadiga';
      }

      // Rotação muscular inteligente baseada no último foco
      if (lastFocus.includes('superior') || lastFocus.includes('peito') || lastFocus.includes('braço') || lastFocus.includes('costa')) {
        focusArea = 'Membros Inferiores';
        icon = '🦵';
        if (highlightBadge !== 'Prevenção de Fadiga') highlightBadge = 'Equilíbrio Muscular';
        reason = `No teu último treino (${lastDate}) focaste em Membros Superiores. A IA recomenda hoje Membros Inferiores para descansares peito, ombros e braços enquanto estimulas pernas e glúteos.`;
      } else if (lastFocus.includes('inferior') || lastFocus.includes('perna') || lastFocus.includes('glúteo')) {
        focusArea = 'Membros Superiores';
        icon = '💪';
        if (highlightBadge !== 'Prevenção de Fadiga') highlightBadge = 'Equilíbrio Muscular';
        reason = `Como no teu último treino (${lastDate}) treinaste Pernas e Glúteos, hoje deves focar nos Membros Superiores para permitir a regeneração muscular inferior.`;
      } else if (lastFocus.includes('core') || lastFocus.includes('abdominal')) {
        focusArea = 'Cardio HIIT';
        icon = '🔥';
        highlightBadge = 'Queima Metabólica';
        reason = `Depois de fortalecer a estabilidade do Core no último treino (${lastDate}), um circuito Cardio HIIT irá acelerar o metabolismo e o consumo calórico.`;
      } else if (lastFocus.includes('cardio') || lastFocus.includes('hiit')) {
        focusArea = 'Corpo Inteiro (Full Body)';
        icon = '⚡';
        highlightBadge = 'Força Funcional';
        reason = `Após uma sessão intensa de Cardio (${lastDate}), a IA sugere um treino de Corpo Inteiro com foco em força e tonificação com calistenia caseira.`;
      } else if (lastFocus.includes('mobilidade') || lastFocus.includes('alongamento')) {
        focusArea = 'Membros Superiores';
        icon = '💪';
        highlightBadge = 'Articulações Prontas';
        reason = `Após o treino regenerativo de mobilidade (${lastDate}), as tuas articulações estão frescas e prontas para uma sessão de força nos Membros Superiores.`;
      } else {
        focusArea = 'Core & Abdominais';
        icon = '🛡️';
        highlightBadge = 'Estabilidade Central';
        reason = `A seguir a um treino geral de Corpo Inteiro (${lastDate}), focar no Core e na postura fortalece o tronco sem sobrecarregar a musculatura periférica.`;
      }

      if (last.perceivedDifficulty === 'Muito Difícil') {
        reason += ' A intensidade foi adaptada para garantir uma recuperação saudável.';
      } else if (last.perceivedDifficulty === 'Muito Fácil') {
        reason += ' A intensidade foi elevada para continuar a desafiar os teus limites.';
      }
    }

    this.aiSuggestion.set({
      focusArea,
      fitnessLevel,
      reason,
      highlightBadge,
      icon
    });
  }

  applyAiSuggestion(): void {
    const suggestion = this.aiSuggestion();
    if (!suggestion) return;

    this.dailyFitnessLevel.set(suggestion.fitnessLevel);
    this.dailyFocusArea.set(suggestion.focusArea);
    this.suggestionApplied.set(true);
    setTimeout(() => {
      this.suggestionApplied.set(false);
      this.cdr.detectChanges();
    }, 2500);
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

    // 1. ATUALIZAÇÃO OTIMISTA IMEDIATA (0ms de latência no clique)
    const originalWorkout = workout;
    const targetExercise = workout.exercises.find((e) => e.id === exerciseId);
    if (!targetExercise) return;

    const newCompleted = !targetExercise.completed;
    const updatedExercises = workout.exercises.map((e) =>
      e.id === exerciseId ? { ...e, completed: newCompleted } : e
    );

    const allCompleted = updatedExercises.length > 0 && updatedExercises.every((e) => e.completed);
    const updatedWorkout: WorkoutPlanResponse = {
      ...workout,
      completed: allCompleted,
      exercises: updatedExercises
    };

    // Atualiza o treino corrente de imediato
    this.currentWorkout.set(updatedWorkout);

    // Se concluiu todos os exercícios, assinala também o aquecimento e relaxamento
    if (allCompleted) {
      this.warmupCompleted.set(true);
      this.cooldownCompleted.set(true);
    }

    // Atualiza o calendário de imediato
    const updatedDays = this.weekDays().map((d) =>
      d.date === updatedWorkout.workoutDate ? { ...d, workout: updatedWorkout } : d
    );
    this.weekDays.set(updatedDays);

    // Guarda de imediato em cache local para persistência instantânea
    const user = this.authService.currentUser();
    if (user?.id) {
      try {
        const workouts = this.allUserWorkouts().map((w) =>
          w.id === updatedWorkout.id ? updatedWorkout : w
        );
        this.allUserWorkouts.set(workouts);
        localStorage.setItem(`ai_workout_workouts_${user.id}`, JSON.stringify(workouts));
      } catch {}
    }

    this.cdr.detectChanges();

    // 2. SINCRONIZAÇÃO EM SEGUNDO PLANO COM O BACKEND
    this.workoutService.toggleExercise(workout.id, exerciseId).subscribe({
      next: (serverPlan) => {
        this.currentWorkout.set(serverPlan);
        const days = this.weekDays().map((d) =>
          d.date === serverPlan.workoutDate ? { ...d, workout: serverPlan } : d
        );
        this.weekDays.set(days);
        this.loadUserStats();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erro ao sincronizar alternância de exercício:', err);
        // Em caso de falha de ligação, reverte para o estado original
        this.currentWorkout.set(originalWorkout);
        const revertedDays = this.weekDays().map((d) =>
          d.date === originalWorkout.workoutDate ? { ...d, workout: originalWorkout } : d
        );
        this.weekDays.set(revertedDays);
        this.cdr.detectChanges();
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
