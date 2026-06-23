import oracledb
import os
from dotenv import load_dotenv

load_dotenv()

_pool = None

def get_pool():
    global _pool
    if _pool is None:
        _pool = oracledb.create_pool(
            user=os.getenv("DB_USER"),
            password=os.getenv("DB_PASSWORD"),
            dsn=os.getenv("DB_DSN"),
            config_dir=os.getenv("DB_WALLET_PATH"),
            wallet_location=os.getenv("DB_WALLET_PATH"),
            wallet_password=os.getenv("DB_WALLET_PASSWORD"),
            min=2,       # 최소 유지 연결 수
            max=10,      # 최대 연결 수
            increment=1, # 부족할 때 늘리는 단위
            ping_interval=60,  # 60초마다 연결 상태 체크 → 끊긴 연결 자동 교체
        )
    return _pool

def get_connection():
    return get_pool().acquire()