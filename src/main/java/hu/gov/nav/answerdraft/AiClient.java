package hu.gov.nav.answerdraft;

import java.util.List;

interface AiClient {
    record Generation(String content,List<String> warnings){}
    Generation generate(Prompt prompt);
}
