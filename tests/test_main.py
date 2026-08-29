import importlib.util
import pathlib
import sys
import unittest


MODULE_PATH = pathlib.Path(__file__).parents[1] / "src" / "main.py"
SPEC = importlib.util.spec_from_file_location("assistant_main", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class KeywordExtractionTest(unittest.TestCase):
    def test_removes_stop_words_and_duplicates(self):
        result = MODULE.extract_keywords("Hogyan kell auth tokent kérni auth requestVersion értékkel?")
        self.assertEqual(["auth", "tokent", "kérni", "requestversion", "értékkel"], result)

    def test_has_fallback(self):
        self.assertEqual(["documentation"], MODULE.extract_keywords("hogy kell ezt meg"))


class EmailTest(unittest.TestCase):
    def test_escapes_untrusted_html(self):
        question = MODULE.Question(
            kind="issue",
            repository="nav-gov-hu/test",
            number="1",
            title="<script>alert(1)</script>",
            body="body",
            url="https://github.com/nav-gov-hu/test/issues/1",
            author="user",
        )
        subject, _, html_body = MODULE.build_email(question, "<b>draft</b>")
        self.assertNotIn("\n", subject)
        self.assertNotIn("<script>", html_body)
        self.assertIn("&lt;b&gt;draft&lt;/b&gt;", html_body)


if __name__ == "__main__":
    unittest.main()
