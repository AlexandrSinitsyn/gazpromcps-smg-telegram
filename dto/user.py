from dataclasses import dataclass
from datetime import datetime
from typing import List, Union


@dataclass
class User:
    id: int
    name: str
    groups: List[int]
    admin_in: Union[List[int], None]
    timestamp: datetime


@dataclass
class Superuser(User):
    # noinspection PyShadowingBuiltins
    def __init__(self, id: int, name: str, groups: List[int], timestamp: datetime):
        self.id = id
        self.name = name
        self.groups = groups
        self.admin_in = None
        self.timestamp = timestamp
