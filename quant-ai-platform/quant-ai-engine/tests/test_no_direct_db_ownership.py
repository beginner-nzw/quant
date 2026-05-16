import re
import unittest
from pathlib import Path


class NoDirectDbOwnershipTests(unittest.TestCase):
    APP_ROOT = Path(__file__).resolve().parents[1] / "app"
    SKIPPED_DIRS = {"__pycache__", ".pytest_cache", ".mypy_cache", ".ruff_cache"}
    SOURCE_SUFFIXES = {".py", ".json", ".yaml", ".yml", ".toml", ".env", ".ini"}
    FORBIDDEN_PATTERNS = {
        "sqlalchemy": re.compile(r"\bsqlalchemy\b", re.IGNORECASE),
        "mysql_client": re.compile(r"\b(pymysql|aiomysql|asyncmy|mysqlclient|MySQLdb)\b", re.IGNORECASE),
        "orm_factory": re.compile(r"\b(create_engine|sessionmaker|declarative_base)\b"),
        "jdbc_mysql": re.compile(r"jdbc:mysql", re.IGNORECASE),
        "db_url": re.compile(r"\b(DATABASE_URL|DB_URL|MYSQL_[A-Z0-9_]*|mysql\+)\b", re.IGNORECASE),
        "raw_sql_execute": re.compile(r"\.execute(?:many)?\s*\(", re.IGNORECASE),
        "raw_select": re.compile(r"\bselect\b[\s\S]{0,160}\bfrom\b", re.IGNORECASE),
        "raw_insert": re.compile(r"\binsert\b[\s\S]{0,160}\binto\b", re.IGNORECASE),
        "raw_update": re.compile(r"\bupdate\b[\s\S]{0,160}\bset\b", re.IGNORECASE),
        "raw_delete": re.compile(r"\bdelete\b[\s\S]{0,160}\bfrom\b", re.IGNORECASE),
    }

    def test_engine_app_has_no_direct_db_ownership_signals(self):
        violations = []

        for source_file in self.source_files():
            text = source_file.read_text(encoding="utf-8")
            relative_path = source_file.relative_to(self.APP_ROOT.parent)
            for signal_name, pattern in self.FORBIDDEN_PATTERNS.items():
                if pattern.search(text):
                    violations.append(f"{relative_path}: {signal_name}")

        self.assertEqual([], violations)

    def source_files(self):
        for path in self.APP_ROOT.rglob("*"):
            if any(part in self.SKIPPED_DIRS for part in path.parts):
                continue
            if path.is_file() and path.suffix in self.SOURCE_SUFFIXES:
                yield path
