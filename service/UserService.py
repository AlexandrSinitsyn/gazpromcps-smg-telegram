from typing import List

from dto.Superuser import Superuser
from dto.User import User


class UserService:
    def __init__(self):
        pass

    def get_all(self) -> List[User]:
        pass

    def get_by_id(self, user_id: int) -> User:
        pass

    def get_by_chat(self, chat_id: int) -> List[User]:
        pass

    def get_admin_by_chat(self, chat_id: int) -> List[User]:
        pass

    def get_superusers(self) -> List[Superuser]:
        pass

    def add_allowed_chat(self, new_user: User):
        pass
