from dto.User import User


class Response:
    __addresser = None  # type: User|None
    __content = None  # type: str|None
    __csv_path = None  # type: str|None

    def __init__(self, addresser: User, csv_path: str = None, content: str = None):
        self.__addresser = addresser
        self.__csv_path = csv_path
        self.__content = content

    def from_csv(self):
        with open(self.__csv_path, 'r') as f:
            self.__content = '\n'.join(f.readline())

        self.__csv_path = None

    def to_csv(self, path: str):
        self.__csv_path = path

        with open(self.__csv_path, 'w') as f:
            f.writelines(self.__content.split('\n'))

        self.__content = None
