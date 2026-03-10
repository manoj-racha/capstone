export interface ApiResponse<T> {
  success: boolean;    // true or false
  message: string;     // "Login successful", "Registration successful"
  data: T;             // the actual payload — different for every endpoint
  error: string;       // error message if success is false
}


