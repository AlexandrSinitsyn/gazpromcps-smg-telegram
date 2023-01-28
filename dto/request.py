from dto.job import *
from dto.response import Response
from dto.user import User
from service.JobService import JobService
from service.UserService import UserService


class Request:
    __sender: User
    __job: Job
    __count: int

    def __init__(self, sender: User, job: Job, count: int):
        self.__sender = sender
        self.__job = job
        self.__count = count

    @staticmethod
    def generate(user_service: UserService, user_id: int):
        def generate_inner(job_service: JobService, section_number: float, message: str, measurement: str):
            def generate_iinner(count: int):
                return Request(user_service.get_by_id(user_id),
                               job_service.get_by_params(section_number, message, measurement),
                               count)
            return generate_iinner
        return generate_inner

    def response(self, csv_path: str = None, content: str = None) -> Response:
        return Response(self.__sender, csv_path, content)

    def get_job(self) -> CompletedJob:
        return CompletedJob(-1, self.__job, self.__count, datetime.now())
