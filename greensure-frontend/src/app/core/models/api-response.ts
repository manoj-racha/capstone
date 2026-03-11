export interface ApiResponse<T> {
  success: boolean;    // true or false
  message: string;     // "Login successful", "Registration successful"
  data: T;             // the actual payload — different for every endpoint
  error: string;       // error message if success is false
}

// Spring Boot Page response structure
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}


