# Final Project FastAPI Server

## Run

```bash
cd fastapi_server
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

Android emulator uses `http://10.0.2.2:8001`.
For a real Android device, change `ApiConfig.BASE_URL` to your computer or EC2 public IP.

## Data

```text
places.json   place metadata
photos/       place photos named with y+x prefix, for example 3757016611269990691_1.jpg
```

The server matches photos with:

```text
serial = place["y"] + place["x"]
```

## API

```text
GET /health
GET /places
GET /places?q=강남
GET /places?city=종로구
POST /courses/recommend
```
