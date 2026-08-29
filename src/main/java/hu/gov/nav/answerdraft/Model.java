package hu.gov.nav.answerdraft;

record Question(String kind, String repository, int number, String title, String body, String url, String author) {}
record Source(String title, String url, String content, int score) {}
