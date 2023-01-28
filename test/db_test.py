import unittest

from dto.job import *
from service.job_service import JobService


class JobServiceTestCase(unittest.TestCase):
    job_service = JobService()

    def test_absense(self):
        self.assertEqual(self.job_service.get_by_id(2), None)

    def test_presence(self):
        test_cjob = CompletedJob(2, Job(2, 1.1, 'test message', 'test measurement', datetime.now()), 5, datetime.now())
        self.job_service.day_activity(test_cjob)

        found = self.job_service.get_by_id(2)
        self.assertEqual(found.message, 'test message')

    def test_found_list(self):
        test_cjobs = [CompletedJob(i, Job(i, float(i), str(i), str(i), datetime.now()), i, datetime.now()) for i in range(1, 10)]
        for cjob in test_cjobs:
            self.job_service.day_activity(cjob)

        found = self.job_service.get_all()
        self.assertEqual(len(found), 9)

        for j in found:
            self.assertEqual(int(j.id), int(j.section_number))


if __name__ == '__main__':
    unittest.main()
