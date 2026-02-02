"""CSV parsing utility."""

import csv
from io import StringIO


def parse_csv(content: str) -> list[list[str]]:
    """Parse CSV content into a list of rows."""
    reader = csv.reader(StringIO(content))
    return [row for row in reader]


def parse_csv_with_header(content: str) -> list[dict[str, str]]:
    """Parse CSV content with header row into list of dicts."""
    reader = csv.DictReader(StringIO(content))
    return [row for row in reader]
