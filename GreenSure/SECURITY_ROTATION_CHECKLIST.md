# Security Rotation Checklist

Use this after secrets were exposed in committed files or screenshots.

1. Rotate Gemini API key in Google AI Studio and update GEMINI_API_KEY in .env.
2. Rotate Gmail OAuth credentials:
   - Regenerate client secret in Google Cloud Console.
   - Revoke and recreate refresh token.
   - Update GMAIL_CLIENT_SECRET and GMAIL_REFRESH_TOKEN in .env.
3. Rotate JWT secret used by backend and update JWT_SECRET in .env.
4. Revoke any old app passwords if they were shared or committed.
5. Restart backend after updating .env:
   - ./mvnw spring-boot:run
6. Verify flows:
   - Login/token generation works (JWT).
   - Gemini features work.
   - OTP/forgot-password emails work.
7. Check git status before commit and ensure .env is not staged.

Quick verification commands:
- git -C /c/Hartford/capstone status --short
- git -C /c/Hartford/capstone check-ignore -v GreenSure/.env
