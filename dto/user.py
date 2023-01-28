from dataclasses import dataclass
from datetime import datetime
from typing import List


@dataclass
class User:
    id: int
    name: str
    groups: List[int]
    admin_in: List[int]
    timestamp: datetime


@dataclass
class Superuser(User):
    admin_in = None
