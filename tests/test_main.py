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

    def test_prioritizes_hungarian_domain_terms(self):
        result = MODULE.extract_keywords(
            "Hogyan tudok az eÁfa rendszerben analitikát feltölteni? Tudnál példát adni rá?"
        )
        self.assertEqual(["eáfa", "analitika", "feltöltés", "példa"], result)

    def test_excerpt_uses_keyword_context(self):
        content = "x" * 6_000 + "analitika feltöltés példa" + "y" * 6_000
        excerpt = MODULE.relevant_excerpt(content, ["analitika"])
        self.assertIn("analitika feltöltés példa", excerpt)
        self.assertLessEqual(len(excerpt), MODULE.MAX_SOURCE_CHARACTERS)


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


class NavGovHuTest(unittest.TestCase):
    def test_parser_uses_main_content_and_collects_links(self):
        parser = MODULE.NavHtmlParser()
        parser.feed("""<html><head><title>eÁFA információk</title></head><body>
            <nav>általános menü</nav><main><h1>Analitika feltöltése</h1>
            <a href="/ado/eafa/informaciok/pelda">Példa</a></main></body></html>""")
        self.assertEqual("eÁFA információk", parser.title)
        self.assertIn("Analitika feltöltése", parser.content)
        self.assertNotIn("általános menü", parser.content)
        self.assertEqual(("/ado/eafa/informaciok/pelda", "Példa"), parser.links[0])

    def test_only_allows_nav_topic_html_pages(self):
        allowed = MODULE.NavGovHuClient._allowed_url(
            "https://nav.gov.hu/ado/eafa/informaciok/pelda#resz"
        )
        self.assertEqual("https://nav.gov.hu/ado/eafa/informaciok/pelda", allowed)
        self.assertIsNone(MODULE.NavGovHuClient._allowed_url("https://example.com/ado/eafa"))
        self.assertIsNone(
            MODULE.NavGovHuClient._allowed_url("https://nav.gov.hu/ado/eafa/leiras.pdf")
        )


if __name__ == "__main__":
    unittest.main()
