import random
import unittest

from dto.job import *
from service.excel_service import ExcelService
from service.job_service import JobService


class JobServiceTestCase(unittest.TestCase):
    job_service = JobService()
    excel_service = ExcelService()

    def test_emptiness(self):
        self.assertEqual(len(self.job_service.get_all()), 0)

    def test_absense(self):
        self.assertEqual(self.job_service.get_by_id(2), None)

    def test_presence(self):
        test_job = Job(1, 'test master', 'test title', 'test measurement', datetime.now())
        self.excel_service.import_data([test_job])

        found = self.job_service.get_by_params('test master', 'test title')
        self.assertEqual(found.title, 'test title')

        self.excel_service.delete_all()

    def test_found_list(self):
        test_jobs = [Job(i, str(i), str(i), str(i), datetime.now()) for i in range(1, 10)]
        self.excel_service.import_data(test_jobs)

        found = self.job_service.get_all()
        self.assertEqual(len(found), 9)

        print(found)

        for j in found:
            self.assertEqual(str(j.master), str(j.title))

        self.excel_service.delete_all()


class ExcelServiceTestCase(unittest.TestCase):
    job_service = JobService()
    excel_service = ExcelService()

    def test_import(self):
        test_jobs = [Job(i, str(i), str(i), str(i), datetime.now()) for i in range(1, 10)]

        self.excel_service.import_data(test_jobs)

        found = self.job_service.get_all()

        self.assertEqual(len(test_jobs), len(found))

        for (exp, act) in zip(test_jobs, found):
            self.assertEqual(exp.title, act.title)

        self.excel_service.delete_all()

    def test_export(self):
        test_jobs = [Job(i, str(i), str(i), str(i), datetime.now()) for i in range(1, 10)]

        self.excel_service.import_data(test_jobs)

        completed = []
        for i in range(10):
            idx = test_jobs[random.randint(0, len(test_jobs) - 1)]

            job = self.job_service.get_by_params(idx.master, idx.title)

            completed.append(CompletedJob(i, job, random.randint(1, 100), datetime.now()))

        for cj in completed:
            self.job_service.day_activity(cj)

        expected_csv = '\n'.join([CompletedJob.csv_title()] + [str(cj) for cj in completed])
        actual_csv = self.excel_service.export_csv(datetime(1, 1, 1))

        self.assertEqual(len(expected_csv), len(actual_csv))

        print(actual_csv)

        for cj in completed:
            self.assertTrue(actual_csv.__contains__(f'{cj.job},"{cj.count}"'))

        self.excel_service.delete_all()


if __name__ == '__main__':
    unittest.main()
