import logging
import re

from service.excel_service import *


def run():
    excel_service = ExcelService()

    excel_service.delete_all()

    # s/"([^"]*)","([^\|"]+)(\|([^"]+))?"\n/"$1","$2"\n"$1","$4"/gmi
    # s/"[^"]+",""\n//gmi
    with open('./database/full_table.csv', 'r', encoding="utf8") as f:
        data = []

        def capitalize_first(text: str):
            return text[0].upper() + text[1:]

        for row in csv.reader(f, delimiter=','):
            # row := "title","master","measurement"
            data.append(Job(-1, row[1], capitalize_first(row[0]), row[2], True, datetime.now()))

        excel_service.import_data(data)


def upload(file_name: str) -> List[Job]:
    table = read_xlsx(file_name)

    current_master = ''

    data = []

    for sheet in table:
        wait = True
        for row in sheet:
            if row[0] == '№ п/п':
                wait = False
                continue

            if wait:
                continue

            if row[0] == 'Потребность в людских и технических ресурсах':
                break

            logging.info(str(row[0:5]))

            if row[2] is not None and row[2].strip():
                m = re.search(r"[^(\n]*(\(([^)]*)\))?(\n(.*))?", row[2].strip())
                current_master = next(item for item in [m.group(i) for i in range(4, -1, -1)] if item is not None)
                current_master = re.sub(r"\s+", " ", current_master).strip()

            if row[3] is not None and row[3].strip():
                data.append(Job(-1, current_master.strip(), row[1].strip(), row[3].strip(), True, datetime.now()))

    return data
