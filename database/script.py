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
            data.append(Job(-1, row[1], row[2], row[3], capitalize_first(row[0]), row[4], True, datetime.now()))

        excel_service.import_data(data)


def upload(file_name: str):
    reader = ExcelReader(file_name)

    table = reader.table()

    current_master = ''

    data = []

    for sheet_name, sheet in table.items():
        current_gen_plan = 'Другие'

        wait = True
        for i, row in enumerate(sheet):
            if row[0] == '№ п/п':
                wait = False
                continue

            if wait:
                continue

            if row[0] == 'Потребность в людских и технических ресурсах':
                break

            # if reader.is_blue(sheet_name, i, col=1):
            #     if row[1] is None:
            #         current_gen_plan = sheet[i - 1][1]
            #     else:
            #         current_gen_plan = row[1]
            #     continue
            if isinstance(row[0], str) and '*' in row[0]:
                current_gen_plan = row[1].strip()
                continue

            if row[2] is not None and row[2].strip():
                m = re.search(r"[^(\n]*(\(([^)]*)\))?(\n(.*))?", row[2].strip())
                current_master = next(item for item in [m.group(i) for i in range(4, -1, -1)] if item is not None)
                current_master = re.sub(r"\s+", " ", current_master).strip()

            if row[3] is not None and row[3].strip():
                data.append(Job(-1, sheet_name, current_gen_plan, current_master.strip(), row[1].strip(), row[3].strip(),
                                True, datetime.now()))

    return data
