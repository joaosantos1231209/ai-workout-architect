export interface UserProfile {
  id?: number;
  name: string;
  email: string;
  age: number;
  fitnessLevel: string;
  primaryGoal: string;
}

export interface AuthRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  age: number;
  fitnessLevel: string;
  primaryGoal: string;
}

export interface ExerciseItem {
  id: number;
  name: string;
  sets: number;
  reps: string;
  equipmentNeeded: string;
  instructions: string;
  completed: boolean;
}

export interface WorkoutPlanResponse {
  id: number;
  title: string;
  focusArea: string;
  warmupRoutine?: string;
  cooldownRoutine?: string;
  explanation: string;
  workoutDate: string; // ISO date 'YYYY-MM-DD'
  completed: boolean;
  perceivedDifficulty?: string;
  exercises: ExerciseItem[];
}

export interface UserStatsResponse {
  streakDays: number;
  totalCompletedWorkouts: number;
  unlockedBadges: string[];
}

export interface AiWorkoutSuggestion {
  focusArea: string;
  fitnessLevel: string;
  reason: string;
  highlightBadge: string;
  icon: string;
}


