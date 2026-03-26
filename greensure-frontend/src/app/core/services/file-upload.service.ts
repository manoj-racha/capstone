import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, forkJoin } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { ToastService } from './toast.service';

export interface UploadedFile {
  fileUrl: string;
  originalFileName: string;
  mimeType: string;
  fileSizeBytes: number;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class FileUploadService {
  private http = inject(HttpClient);
  private toastService = inject(ToastService);

  private readonly ALLOWED_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];
  private readonly MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

  uploadFile(file: File, category: string = 'general'): Observable<UploadedFile> {
    // 1. Validate file client-side before sending
    if (!this.ALLOWED_TYPES.includes(file.type)) {
      return throwError(() => new Error('Invalid file type. Only PDF, JPG, PNG files are allowed.'));
    }

    if (file.size > this.MAX_SIZE_BYTES) {
      return throwError(() => new Error('File too large. Maximum size is 10MB.'));
    }

    // 2. Build FormData correctly
    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append('category', category);

    // 3. Send WITHOUT Content-Type header (let browser set multipart boundary)
    return this.http.post<ApiResponse<UploadedFile>>(`${environment.apiUrl}/uploads/file`, formData).pipe(
      map(response => {
        if (!response.success || !response.data?.fileUrl) {
          throw new Error(response.message || 'Upload failed');
        }
        
        const uploaded = response.data;
        // If backend returned a relative URL (starting with /), prepend the API base URL
        if (uploaded.fileUrl.startsWith('/')) {
          uploaded.fileUrl = `${environment.apiUrl}${uploaded.fileUrl}`;
        }
        
        return uploaded;
      }),
      catchError(error => {
        const message = error.error?.message || error.message || 'File upload failed. Please try again.';
        return throwError(() => new Error(message));
      })
    );
  }

  uploadMultipleFiles(files: File[], category: string): Observable<UploadedFile[]> {
    return forkJoin(files.map(f => this.uploadFile(f, category)));
  }

  getFilePreviewType(mimeType: string): 'image' | 'pdf' | 'other' {
    if (mimeType.startsWith('image/')) {
      return 'image';
    }
    if (mimeType === 'application/pdf') {
      return 'pdf';
    }
    return 'other';
  }
}
