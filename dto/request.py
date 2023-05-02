from dto.job import *
from dto.response import Response
from dto.user import User
from exceptions.exceptions import RequestError
from service.job_service import JobService
from service.user_service import UserService


class Request:
    __sender: User
    __job: Job
    __count: float

    def __init__(self, sender: User, job: Job, count: float):
        self.__sender = sender
        self.__job = job
        self.__count = count

    @staticmethod
    def generate(user_service: UserService, user_id: int):
        def generate_inner(job_service: JobService, stage: str, gen_plan: str, master: str, title: str):
            def generate_iinner(count: float):
                return Request(user_service.get_by_id(user_id),
                               job_service.get_by_params(stage, gen_plan, master, title),
                               count)
            return generate_iinner
        return generate_inner

    def response(self, csv_path: str = None, content: str = None) -> Response:
        return Response(self.__sender, csv_path, content)

    def get_job(self) -> CompletedJob:
        if self.__count <= 0:
            raise RequestError('invalid-count')

        if self.__count > 2_147_483_647:
            raise RequestError('to-large')

        if self.__job is None:
            raise RequestError('job-no-found')

        return CompletedJob(-1, self.__job, self.__count, datetime.now())
