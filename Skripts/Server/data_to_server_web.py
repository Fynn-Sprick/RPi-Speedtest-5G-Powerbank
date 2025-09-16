from flask import Flask, request
import json
from influxdb_client import InfluxDBClient, Point, WriteOptions

# --- InfluxDB Einstellungen ---
INFLUX_URL = "http://192.168.10.22:8086"
INFLUX_TOKEN = "JbHRkBye1at-gUjC1rvYGAGvhC9KczBmzZmNoCP9aZbxcXtqIKZ1YfclhR7PppIY4GNLfRraGs7H1Dh2aVk-pg=="
INFLUX_ORG = "influxdb"
INFLUX_BUCKET = "teltonika"

client = InfluxDBClient(url=INFLUX_URL, token=INFLUX_TOKEN, org=INFLUX_ORG)
write_api = client.write_api(write_options=WriteOptions(batch_size=1))

app = Flask(__name__)

@app.route("/data", methods=["POST"])
def receive():
    parsed = None

    # --- JSON parsen ---
    try:
        parsed = request.get_json(force=True)
    except:
        # fallback: falls Daten als form-data kommen
        if request.form:
            for k, v in request.form.items():
                try:
                    parsed = json.loads(k)
                except:
                    try:
                        parsed = json.loads(v)
                    except:
                        pass

    if not parsed:
        return "Invalid JSON", 400

    # Tags vorbereiten
    tags = {
        "network": "",
        "conntype": "",
        "cellid": ""
    }

    # --- Modem-Daten ---
    if "Modem" in parsed:
        m = parsed["Modem"]
        tags["network"] = str(m.get("network", ""))
        tags["conntype"] = str(m.get("conntype", ""))
        tags["cellid"] = str(m.get("cellid", ""))

        point_modem = (
            Point("modem_status")
            .tag("network", tags["network"])
            .tag("conntype", tags["conntype"])
            .tag("cellid", tags["cellid"])
            .field("rssi", int(m.get("rssi", 0)))
            .field("rsrp", int(m.get("rsrp", 0)))
            .field("rsrq", int(m.get("rsrq", 0)))
            .field("sinr", int(m.get("sinr", 0)))
        )
        write_api.write(bucket=INFLUX_BUCKET, record=point_modem)

    # --- Data Usage ---
    if "Data_Usage" in parsed:
        d = parsed["Data_Usage"]

        point_usage = (
            Point("data_usage")
            .tag("network", tags["network"])
            .tag("conntype", tags["conntype"])
            .tag("cellid", tags["cellid"])
            .field("tx_bytes", int(d.get("tx", 0)))
            .field("rx_bytes", int(d.get("rx", 0)))
        )
        write_api.write(bucket=INFLUX_BUCKET, record=point_usage)

    return "OK", 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
