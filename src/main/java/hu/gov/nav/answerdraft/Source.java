package hu.gov.nav.answerdraft;

record Source(String title,String url,String content,int score,SourceType type) {
    Source(String title,String url,String content,int score){this(title,url,content,score,SourceType.PUBLIC);}
    boolean privateKnowledge(){return type==SourceType.APPROVED_KNOWLEDGE;}
}
