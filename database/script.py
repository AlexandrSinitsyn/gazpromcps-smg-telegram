import csv
import logging

import pyexcel
from datetime import datetime

from typing import List

from dto.job import Job
from service.excel_service import ExcelService


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
    with open(file_name, 'r', encoding="utf8") as f:
        # table = pyexcel.get_array(file_name=file_name)

        current_master = ''

        data = []

        wait = True
        for row in csv.reader(f, delimiter=','):
            if row[0] == '№ п/п':
                wait = False
                continue

            if wait:
                continue

            if row[0] == 'Потребность в людских и технических ресурсах':
                break

            logging.info(str(row[0:5]))

            if row[2].strip():
                current_master = row[2]

            if row[3].strip():
                data.append(Job(-1, current_master, row[1], row[3], True, datetime.now()))

        return data
