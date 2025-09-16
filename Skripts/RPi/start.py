import time
import requests
import board, digitalio
from PIL import Image, ImageDraw, ImageFont
import adafruit_ssd1306

RESET_PIN = digitalio.DigitalInOut(board.D4)
i2c = board.I2C()
oled = adafruit_ssd1306.SSD1306_I2C(128, 64, i2c, addr=0x3C, reset=RESET_PIN)

# Fonts
font_small = ImageFont.truetype(
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 12
)
font_normal = ImageFont.truetype(
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14
)

SPEEDTEST_API_URL = "http://192.168.20.10:8080/api/v1/results/latest"
SPEEDTEST_API_TOKEN = "Yhnyke53IT8tZhkSnZJIEGMu2wHV4p9ltGo7tjHta22dde24"
SPEEDTEST_HEADERS = {"Authorization": f"Bearer {SPEEDTEST_API_TOKEN}"}

ROUTER_BASE = "https://192.168.253.1/api"
ROUTER_LOGIN_URL = f"{ROUTER_BASE}/login"
ROUTER_WG_URL = f"{ROUTER_BASE}/wireguard/config/Hetzner"
ROUTER_DATA_URL = f"{ROUTER_BASE}/data_to_server/collections/config/1"

ROUTER_USER = "admin"
ROUTER_PASS = "******"

def router_enable_services():
    try:
        login_resp = requests.post(
            ROUTER_LOGIN_URL,
            json={"username": ROUTER_USER, "password": ROUTER_PASS},
            verify=False,
            timeout=5,
        )
        login_resp.raise_for_status()
        login_data = login_resp.json()
        token = login_data["data"]["token"]

        headers = {"Authorization": f"Bearer {token}"}

        wg_resp = requests.put(
            ROUTER_WG_URL,
            headers=headers,
            json={"data": {"enabled": "1"}},
            verify=False,
            timeout=5,
        )
        wg_resp.raise_for_status()
        print("WireGuard Hetzner erfolgreich aktiviert.")

        data_resp = requests.put(
            ROUTER_DATA_URL,
            headers=headers,
            json={"data": {"enabled": "1"}},
            verify=False,
            timeout=5,
        )
        data_resp.raise_for_status()
        print("Data-to-Server erfolgreich aktiviert.")

    except Exception as e:
        print("Fehler bei Router-API:", e)

def fetch_speedtest_data():
    try:
        r = requests.get(SPEEDTEST_API_URL, headers=SPEEDTEST_HEADERS, timeout=5)
        r.raise_for_status()
        data = r.json().get("data", {})
        created_at = str(data.get("created_at", "N/A"))
        time_only = created_at.split(" ")[1] if " " in created_at else created_at
        ping = str(data.get("ping", "N/A"))
        download = str(data.get("download_bits_human", "N/A"))
        upload = str(data.get("upload_bits_human", "N/A"))
        return time_only, ping, download, upload
    except Exception as e:
        return "Error", "-", "-", "-"

def display_on_oled(time_only, ping, download, upload):
    oled.fill(0)
    oled.show()

    image = Image.new("1", (oled.width, oled.height))
    draw = ImageDraw.Draw(image)

    draw.text((0, 0), time_only, font=font_small, fill=255)

    draw.text((0, 16), f"Ping: {ping}", font=font_normal, fill=255)
    draw.text((0, 32), f"Down: {download}", font=font_normal, fill=255)
    draw.text((0, 48), f"Up:   {upload}", font=font_normal, fill=255)

    oled.image(image)
    oled.show()

if __name__ == "__main__":
    router_enable_services()

    while True:
        time_only, ping, download, upload = fetch_speedtest_data()
        display_on_oled(time_only, ping, download, upload)
        time.sleep(60)