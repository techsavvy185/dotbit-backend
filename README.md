# Dotbit Backend

Standalone Ktor service for constrained correction of uncertain Braille OCR text. It is intentionally independent from the Dotbit app repository and contains no image upload endpoint.

## Run locally

Requires JDK 21.

```bash
export DOTBIT_BACKEND_TOKEN="replace-with-a-long-random-token"
export GEMINI_API_KEY="your-google-ai-studio-key"
./gradlew run
```

When `GEMINI_API_KEY` is present, the service uses Gemini 3.8 Flash through `generateContent` with low thinking effort and structured JSON output. Deprecated Gemini 3 sampling controls are intentionally omitted. Without the key, development mode uses a deterministic provider that chooses only from submitted OCR alternatives. Production mode fails fast when the key is missing. Run the tests with:

```bash
./gradlew test
```

## API

- `GET /health`
- `POST /v1/corrections`
- Optional bearer authentication through `DOTBIT_BACKEND_TOKEN`

With Gemini configured, the health response identifies the active provider and model:

```json
{
  "status": "ok",
  "correctionProvider": "gemini-constrained",
  "correctionModel": "gemini-3.8-flash"
}
```

The correction request accepts only recognized text, uncertain spans, confidence values, and candidate alternatives. Unknown JSON fields are rejected; page images and raw photos are never accepted.

```json
{
  "recognizedText": "The boy plahed outside.",
  "uncertainSpans": [
    {
      "startIndex": 11,
      "endIndex": 12,
      "original": "h",
      "confidence": 0.52,
      "alternatives": [{ "text": "y", "confidence": 0.44 }]
    }
  ]
}
```

## Production configuration

Copy `.env.example` into your deployment provider's environment settings. Production mode requires both `DOTBIT_BACKEND_TOKEN` and `GEMINI_API_KEY`.

- `PORT`: server port; defaults to `8080`.
- `ENVIRONMENT`: set to `production` to enforce production secrets.
- `DOTBIT_BACKEND_TOKEN`: bearer token expected from the app.
- `GEMINI_API_KEY`: Gemini API key created in Google AI Studio and held only by this service.
- `GEMINI_BASE_URL`: Gemini REST API base URL; defaults to Google's `v1beta` endpoint.
- `GEMINI_MODEL`: defaults to `gemini-3.8-flash`.
- `ALLOWED_ORIGINS`: optional comma-separated browser origins.

Build and run the container with:

```bash
docker build -t dotbit-backend .
docker run --rm -p 8080:8080 \
  -e ENVIRONMENT=production \
  -e DOTBIT_BACKEND_TOKEN="..." \
  -e GEMINI_API_KEY="..." \
  dotbit-backend
```

This folder is already initialized as an independent local Git repository on the `main` branch. Add only the new remote you create for this service, and do not commit `.env` or provider credentials.

## Deploy to Render for testing

This repository includes a root `render.yaml` Blueprint and a Dockerfile. Push this folder to its own Git repository, then create a Render Blueprint from that repository. Render will ask for `DOTBIT_BACKEND_TOKEN`; use the same long random value in the app build configuration.

The Blueprint runs in production mode and is configured for Gemini 3.8 Flash. Before redeploying the existing Render service, add these values under **Environment**:

1. Add `GEMINI_API_KEY` as a secret environment variable. Paste the API key itself as the value.
2. Keep the existing `DOTBIT_BACKEND_TOKEN`; it is a separate app-to-backend credential.
3. Set `ENVIRONMENT=production` and `GEMINI_MODEL=gemini-3.8-flash`.
4. Redeploy and verify `GET /health` reports `gemini-constrained` and `gemini-3.8-flash`.

Never put `GEMINI_API_KEY` in the mobile app, `local.properties`, source control, or the public request payload. Only Render receives it. The backend sends Gemini only recognized text, uncertain spans, confidence values, and alternatives. It has no image upload field or image-forwarding path.

After Render assigns the service URL, configure the app's untracked `local.properties`:

```properties
DOTBIT_BACKEND_URL=https://your-service-name.onrender.com/
DOTBIT_BACKEND_TOKEN=the-same-token-entered-in-render
```

Rebuild the app after changing these compile-time values.
