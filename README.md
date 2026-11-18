# CareerFit AI

CareerFit AI is a simple full-stack portfolio project that compares resume text against a job description and returns an AI-assisted match report. It uses React, Spring Boot, MySQL, JPA/Hibernate, REST APIs, and the Google Gemini REST API.

## Features

- Paste resume text and job description into a polished React dashboard.
- Analyze fit with Gemini and receive structured JSON.
- View match score, matching skills, missing skills, suggestions, ATS keywords, and a skill-gap roadmap.
- Save each analysis to MySQL.
- Browse previous analyses ordered newest first.
- Open a saved analysis to view the complete result.

## Tech Stack

- Backend: Java 17+, Spring Boot 3.x, Maven, Spring Web, Spring Data JPA, Bean Validation, Lombok, MySQL Driver
- Frontend: React, Vite, JavaScript, Axios, CSS, lucide-react
- Database: MySQL
- AI: Google Gemini API via Spring `RestClient`

## Architecture

```text
frontend React app
  -> Axios REST calls
backend Spring Boot API
  -> controller / service / repository / entity / dto / config / exception
  -> Gemini REST API
  -> MySQL analyses table
```

## Database Schema

Create the database before running the backend:

```sql
CREATE DATABASE careerfit;
```

Hibernate creates or updates the `analyses` table from the JPA entity:

```text
id
resume_text
job_description
match_score
matching_skills
missing_skills
suggestions
recommended_keywords
skill_gap_roadmap
created_at
```

## API Endpoints

### POST `/api/analyses`

Request:

```json
{
  "resumeText": "Resume text...",
  "jobDescription": "Job description..."
}
```

Response:

```json
{
  "id": 1,
  "matchScore": 82,
  "matchingSkills": ["Java", "Spring Boot", "MySQL"],
  "missingSkills": ["Docker", "AWS"],
  "suggestions": ["Highlight measurable achievements in your experience."],
  "recommendedKeywords": ["Spring Boot", "REST API", "Docker"],
  "skillGapRoadmap": [
    {
      "skill": "Docker",
      "priority": "HIGH",
      "reason": "The job description lists Docker as a required skill."
    }
  ],
  "createdAt": "2026-09-06T12:00:00Z"
}
```

### GET `/api/analyses`

Returns previous analyses ordered newest first.

### GET `/api/analyses/{id}`

Returns one complete analysis.

## Gemini Integration

The backend uses Google Gemini's `generateContent` REST endpoint with the API key passed through the `x-goog-api-key` header. The prompt instructs Gemini to return only concise JSON matching the required schema. The backend validates the match score, strips accidental code fences, and returns clean error responses for API failures or malformed JSON.

## Environment Variables

Copy `.env.example` values into your shell or IDE run configuration. Do not commit real credentials.

```text
DB_URL=jdbc:mysql://localhost:3306/careerfit
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password
GEMINI_API_KEY=your_gemini_api_key
GEMINI_MODEL=gemini-2.0-flash
FRONTEND_ORIGIN=http://localhost:5173
```

For the frontend, `frontend/.env.example` shows:

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

## Run Backend

From the project root:

```bash
cd backend
mvn spring-boot:run
```

PowerShell example:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/careerfit"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:GEMINI_API_KEY="your_key"
cd backend
mvn spring-boot:run
```

## Run Frontend

From a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

## Example Workflow

1. Start MySQL and create the `careerfit` database.
2. Start the Spring Boot backend with the required environment variables.
3. Start the React frontend.
4. Paste a resume and a job description.
5. Click **Analyze Resume**.
6. Review the generated match report and saved analysis history.

## Future Improvements

- Add optional PDF/DOCX resume parsing.
- Add charts for match score trends.
- Add export to PDF.
- Add configurable analysis templates for different roles.
