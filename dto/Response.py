from dto.User import User


class Response:
    __addresser: User
    __content: str
    __csv_path: str

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
