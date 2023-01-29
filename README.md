# Gazpromcps telegram bot

Telegram bot that tracks daily activity.

---

Requests have format of `type <count> comment`.

---

CSV table is arranged as:

| Section number | job title       | measurement | code      | ... |
|----------------|-----------------|-------------|-----------|-----|
| 1              | Bridges         | piece       | 1234.5678 | ... |
| 2              | Concrete works  | m3          | 4321.5678 | ... |
| 2.1            | Concrete fences | m3          | 4321.5679 | ... |
| 2.2            | Groundwork      | m3          | 4321.5680 | ... |
