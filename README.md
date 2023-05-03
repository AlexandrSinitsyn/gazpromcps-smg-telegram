# Daily activity tracker

---

### Deploying

Add `config.properties` in `./src/main/resources` with several environment variables:
* bot.name
* bot.token
* bot.master.id

Use `docker-compose up -d` to start

---

### Grafana setup

[Grafana (http://localhost:3000)](http://localhost:3000) &rarr; Data source &rarr; Add new data source &rarr;
Loki &rarr; Url: http://loki:3100 &rarr; Explore &rarr; Log browser: `{job="varlogs"} |= "ERROR"`
