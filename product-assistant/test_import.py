import sys
sys.path.insert(0, '.')

try:
    from api_server import app
    print("API Server module loaded successfully!")
    print(f"FastAPI app: {app.title}")
except Exception as e:
    print(f"Error: {e}")
    import traceback
    traceback.print_exc()