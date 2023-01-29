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
        test_job = Job(1, 1.1, 'test title', 'test measurement', datetime.now())
        self.excel_service.import_data([test_job])

        found = self.job_service.get_by_params(1.1, 'test title', 'test measurement')
        self.assertEqual(found.title, 'test title')

        self.excel_service.delete_all()

    def test_found_list(self):
        test_jobs = [Job(i, float(i), str(i), str(i), datetime.now()) for i in range(1, 10)]
        self.excel_service.import_data(test_jobs)

        found = self.job_service.get_all()
        self.assertEqual(len(found), 9)

        print(found)

        for j in found:
            self.assertEqual(str(int(j.section_number)), str(j.title))

        self.excel_service.delete_all()


if __name__ == '__main__':
    unittest.main()
