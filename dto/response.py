from typing import Optional

from dto.user import User


class Response:
    __addresser: User
    __content: Optional[str]
    __csv_path: Optional[str]

    def __init__(self, addresser: User, csv_path: str = None, content: str = None):
        self.__addresser = addresser
        self.__csv_path = csv_path
        self.__content = content

    def content(self):
        return self.__content

    def from_csv(self):
        with open(self.__csv_path, 'r') as f:
            self.__content = '\n'.join(f.readline())

        self.__csv_path = None

    def to_csv(self, path: str):
        self.__csv_path = path

        with open(self.__csv_path, 'w') as f:
            f.writelines(self.__content.split('\n'))

        self.__content = None
