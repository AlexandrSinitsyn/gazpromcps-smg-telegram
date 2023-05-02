from repository.user_repository import *
from exceptions.exceptions import RequestError


class UserService:
    @staticmethod
    def get_all() -> List[User]:
        return find_all()

    @staticmethod
    def get_by_id(user_id: int) -> Optional[Union[User, Superuser]]:
        return find_by_id(user_id)

    @staticmethod
    def get_by_chat(chat_id: int) -> List[User]:
        return find_all_in_group(chat_id)

    @staticmethod
    def get_admin_by_chat(chat_id: int) -> List[User]:
        return [u for u in find_all_admins() if chat_id in u.admin_in]

    @staticmethod
    def get_superusers() -> List[Superuser]:
        return find_all_superusers()

    @staticmethod
    def register(user: User):
        return save_user(user)

    @staticmethod
    def make_admin(sys: Union[User, Superuser], chat_id: int, user: User):
        if isinstance(sys, Superuser) or len(sys.admin_in) != 0:
            return make_admin(chat_id, user)
        else:
            raise RequestError('not-allowed')
