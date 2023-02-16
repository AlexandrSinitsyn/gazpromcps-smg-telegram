from typing import List, Optional

from dto.user import *
from repository.database import *


# row := (id, name, superuser, groups, admin_in, timestamp)

def to_user(row) -> User:
    return User(row[0], row[1], row[3], row[4], row[5])


def to_superuser(row) -> Superuser:
    return Superuser(row[0], row[1], row[3], row[5])


def find_all() -> List[User]:
    return run_query('SELECT * FROM users ;') \
        (lambda rows: [to_user(row) for row in rows])


def find_by_id(user_id: int) -> Optional[User]:
    def find(rows):
        for row in rows:
            return to_user(row)
        return None

    return run_query('SELECT * FROM users where id=%(ui)s ;', ui=user_id)(find)


def find_all_in_group(group_id: int) -> List[User]:
    return run_query(f'SELECT * FROM users WHERE %(gid)s=ANY(groups) ;', gid=group_id) \
        (lambda rows: [to_user(row) for row in rows])


def find_all_superusers() -> List[Superuser]:
    return run_query(f"SELECT * FROM users where superuser='1' ;") \
        (lambda rows: [to_superuser(row) for row in rows])


def find_all_admins() -> List[User]:
    return run_query(f'SELECT * FROM users WHERE cardinality(admin_in)!=0 ;') \
        (lambda rows: [to_user(row) for row in rows])


def save_user(user: User):
    run_query(f'INSERT INTO users (id, name, superuser, groups, admin_in)'
              f"VALUES (%(ui)s, %(n)s, '0', %(gids)s, '{{}}') ;", ui=user.id, n=user.name,
              gids=str(user.groups).replace('[', '\'{').replace(']', '}\''))(id)


def make_admin(chat_id: int, user: User):
    run_query(f"UPDATE users SET admin_in=admin_in || %(chat_id)s WHERE id=%(ui)s ;",
              chat_id=chat_id, ui=user.id)(id)
