# Dotbit Backend

Standalone Ktor service for constrained correction of uncertain Braille OCR text. It is intentionally independent from the Dotbit app repository and contains no image upload endpoint.

## Run locally

Requires JDK 21.

```bash
export DOTBIT_BACKEND_TOKEN="replace-with-a-long-random-token"
./gradlew run
```

Without `LLM_API_KEY`, development mode uses a deterministic provider that chooses only from submitted OCR alternatives. Run the tests with:

```bash
./gradlew test
```

## API

- `GET /health`
- `POST /v1/corrections`
- Optional bearer authentication through `DOTBIT_BACKEND_TOKEN`

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

Copy `.env.example` into your deployment provider's environment settings. Production mode requires both `DOTBIT_BACKEND_TOKEN` and `LLM_API_KEY`.

- `PORT`: server port; defaults to `8080`.
- `ENVIRONMENT`: set to `production` to enforce production secrets.
- `DOTBIT_BACKEND_TOKEN`: bearer token expected from the app.
- `LLM_API_KEY`: provider API key, held only by this service.
- `LLM_BASE_URL`: OpenAI-compatible API base URL.
- `LLM_MODEL`: model identifier.
- `ALLOWED_ORIGINS`: optional comma-separated browser origins.

Build and run the container with:

```bash
docker build -t dotbit-backend .
docker run --rm -p 8080:8080 \
  -e ENVIRONMENT=production \
  -e DOTBIT_BACKEND_TOKEN="..." \
  -e LLM_API_KEY="..." \
  dotbit-backend
```

This folder is already initialized as an independent local Git repository on the `main` branch. Add only the new remote you create for this service, and do not commit `.env` or provider credentials.

## Deploy to Render for testing

This repository includes a root `render.yaml` Blueprint and a Dockerfile. Push this folder to its own Git repository, then create a Render Blueprint from that repository. Render will ask for `DOTBIT_BACKEND_TOKEN`; use the same long random value in the app build configuration.

The Blueprint initially uses `ENVIRONMENT=development`, so it can exercise the deterministic correction provider without an LLM key. To enable the external provider in Render:

1. Add `LLM_API_KEY` as a secret environment variable.
2. Change `ENVIRONMENT` to `production`.
3. Redeploy and verify `GET /health` reports `llm-constrained`.

After Render assigns the service URL, configure the app's untracked `local.properties`:

```properties
DOTBIT_BACKEND_URL=https://your-service-name.onrender.com/
DOTBIT_BACKEND_TOKEN=the-same-token-entered-in-render
```

Rebuild the app after changing these compile-time values.
