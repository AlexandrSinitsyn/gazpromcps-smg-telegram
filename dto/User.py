from dataclasses import dataclass
from typing import List


@dataclass
class User:
    id: int
    name: str
    groups: List[int]
    admin_in: List[int]
