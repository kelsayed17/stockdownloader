"""Date manipulation utilities."""

from datetime import datetime, timedelta


def parse_date(date_str: str) -> datetime:
    """Parse date string in common formats."""
    for fmt in ("%Y-%m-%d", "%m/%d/%Y", "%Y/%m/%d"):
        try:
            return datetime.strptime(date_str, fmt)
        except ValueError:
            continue
    raise ValueError(f"Unable to parse date: {date_str}")


def format_date(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%d")


def add_days(date_str: str, days: int) -> str:
    dt = parse_date(date_str)
    return format_date(dt + timedelta(days=days))


def days_between(start: str, end: str) -> int:
    return (parse_date(end) - parse_date(start)).days
