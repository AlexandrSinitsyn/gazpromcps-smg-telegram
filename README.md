# Daily activity tracker

---

### Functionality

- `/start` - starting the bot
- `/register [registration token]` - registration with a token (usage example: `/register A0123456789`)
- `/makereport` - new report
- `/updatereport` - update one of 5 last committed reports
- `/exportxlsx` - provides `xlsx`-file with all tracked data
- `/exportcsv` - provides `csv`-file with all tracked data
- `/exporttext` - shows all tracked data in a message
- `/exportmedia` - provides all the media that has been uploaded for all jobs in a `zip`-archive
- `/importxlsx` - upload new job list
- `/promote [user id]` - promote to a specific role (usage example: `/promote 123`)
- `/users` - list  all the users
- `/token` - generate registration token
- `/en` - switch to english
- `/ru` - switch to russian
- `/lang [country code]` - change the language to any other https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2 (usage example: `/lang ja`)
- `/help` - use for help

---

### Role-based bot

- `ANONYMOUS` - unregistered users
- `USER` - base users who make daily reports
- `MANAGER` - those who collect data and manage reports
- `SERVICE` - same as MANAGER but have rights for registering new users
- `ADMIN` - have both rights to make and collect reports; also can change manage roles
- `SUPERUSER` - the headmaster

---

### Deploying

Add `config.properties` in `./src/main/resources` with several environment variables:
- `bot.name` - name of your bot (ex: `@test_bot`)
- `bot.token` - bot token
- `bot.superuser` - admin id. As soon as you introduce yourself to bot with `/start`, you will get `SUPERUSER` role
- `token.salt` - salt for generating registration tokens

Use `docker-compose up -d` to start

* _If docker fails use `docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions`_

#### Containers:

| Container | Port      | Purpose                          |
|-----------|-----------|----------------------------------|
| backend   | 8080:8080 | server; bot controller           |
| postgres  | 5432:5432 | database                         |
| promtail  |           | logs collector                   |
| loki      | 3100:3100 | handles promtail logs database   |
| grafana   | 3000:3000 | for bot metrics (logs as one of) |

---

### Grafana setup

* [Grafana (http://localhost:3000)](http://localhost:3000) &rarr; Dashboards &rarr; General &rarr; telegram-smg-gazpromcps
* [Real time logging] Exprore &rarr; loki &rarr; `job="telegram-smg-logs"` &rarr; _Run query_