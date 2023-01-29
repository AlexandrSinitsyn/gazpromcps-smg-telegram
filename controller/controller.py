from dto.request import Request
from dto.response import Response
from service.excel_service import ExcelService
from service.job_service import JobService

job_service = JobService()
excel_service = ExcelService()


def process(request: Request):
    job_service.day_activity(request.get_job())


def answer(request: Request, store: bool = False) -> Response:
    csv = excel_service.export_csv()

    if store:
        excel_service.save()

    return request.response(content=csv)
