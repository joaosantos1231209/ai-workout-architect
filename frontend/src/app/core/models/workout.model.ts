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
  explanation: string;
  workoutDate: string; // ISO date 'YYYY-MM-DD'
  completed: boolean;
  exercises: ExerciseItem[];
}
