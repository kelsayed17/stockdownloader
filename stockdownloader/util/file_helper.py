"""File system operations."""

from pathlib import Path


def read_file(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write_file(path: str, content: str) -> None:
    Path(path).write_text(content, encoding="utf-8")


def file_exists(path: str) -> bool:
    return Path(path).exists()


def ensure_directory(path: str) -> None:
    Path(path).mkdir(parents=True, exist_ok=True)
