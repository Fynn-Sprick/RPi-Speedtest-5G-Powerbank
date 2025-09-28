# RPi-Speedtest-5G-Powerbank

## Übersicht
Dieses Projekt nutzt einen **Raspberry Pi** mit 5G-Anbindung und einer Powerbank, um regelmäßige Speedtests durchzuführen.  
Die Messergebnisse werden mit [Speedtest Tracker](https://github.com/alexjustesen/speedtest-tracker) erfasst, in **InfluxDB** gespeichert und über **Grafana** visualisiert.  
Zusätzlich existiert eine **Android-App**, die eine mobile Anzeige der Ergebnisse ermöglicht.

---

## Projektstruktur
```
RPi-Speedtest-5G-Powerbank/
├── App-Android/                # Native Android App (Kotlin)
│   ├── app/                    
│   └── gradle/                 
├── Configs/                    # Konfigurationsdateien & Screenshots
│   ├── Grafana Dashboard.json  # Exportiertes Dashboard
│   ├── Influxdb Buckets.png    # Übersicht über Buckets
│   └── Speedtest Tracker InfluxDB Config.png
├── Speedtest-Tracker/          # Docker Setup für Speedtest Tracker
│   └── docker-compose.yml
├── Skripts/                    
│   └── Server/                 
│       └── data_to_server_web.py # Python Skript zur Weitergabe der Daten
```

---

## Komponenten

### 🔹 Raspberry Pi
- Dient als Hardware-Basis
- 5G-Modem/Router für Internetanbindung
- Powerbank für mobilen Betrieb

### 🔹 Speedtest Tracker
- Containerisierte Lösung für automatisierte Speedtests
- Ergebnisse werden an InfluxDB übermittelt

### 🔹 InfluxDB
- Speichert alle Speedtest-Messwerte (Download, Upload, Ping)
- Struktur siehe `Influxdb Buckets.png`

### 🔹 Grafana
- Visualisierung der Ergebnisse
- Beispiel-Dashboard liegt in `Configs/Grafana Dashboard.json`

### 🔹 Android-App
- Im Ordner `App-Android`
- Native App in Kotlin, um Speedtest-Daten mobil darzustellen

### 🔹 Python-Skript
- `data_to_server_web.py`
- Schnittstelle/Helper zur Datenweitergabe von Pi → InfluxDB

---

## Beispiel-Dashboard
Comming - Soon

---

## Erweiterungen
- Benachrichtigungen bei schlechten Speedtest-Werten
- Optimierung der Android-App (z. B. Push-Updates statt Polling)

---

## Lizenz
*(Hier gewünschte Lizenz eintragen, z. B. MIT oder GPL)*
