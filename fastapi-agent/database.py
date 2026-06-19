import oracledb
import os
from dotenv import load_dotenv

load_dotenv()

def get_connection():
    conn = oracledb.connect(
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        dsn=os.getenv("DB_DSN"),
        config_dir=os.getenv("DB_WALLET_PATH"),
        wallet_location=os.getenv("DB_WALLET_PATH"),
        wallet_password=os.getenv("DB_WALLET_PASSWORD")
    )
    return conn