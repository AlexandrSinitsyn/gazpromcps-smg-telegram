from dataclasses import dataclass

from dto.User import User


@dataclass
class Superuser(User):
    admin_in = None
