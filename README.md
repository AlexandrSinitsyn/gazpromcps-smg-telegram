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

## Functionality
This bot is for tracking daily activity. For help use /help

- /start - starting the bot
- /help - use for help
- /reload - reload your role
- /promote - [Expects user id] promote user to master role
- /en - switch to english
- /ru - switch to russian
- /lang - [Expects country code] Changes language to a different one. You can search for yours one here: https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
- /make_report - new report
- /export_text - shows all tracked data in message
- /export_csv - provides `.csv`-file with all tracked data

There are two ways to report:
- /make_report, then peek the write one of sections and print the number you want
- Put &lt;section number&gt;, &lt;full section name&gt;, &lt;count&gt;, &lt;measurement&gt; separated by comma (as they are given in the main document)
