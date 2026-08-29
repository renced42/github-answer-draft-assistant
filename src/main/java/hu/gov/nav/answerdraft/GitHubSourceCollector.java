package hu.gov.nav.answerdraft;

import java.net.URI; import java.nio.charset.StandardCharsets; import java.util.*; import java.util.regex.*;

final class GitHubSourceCollector {
    private final WebClient http; private final Config config; private final Map<String,String> headers;
    GitHubSourceCollector(WebClient http,Config config){this.http=http;this.config=config;this.headers=config.githubToken().isBlank()?Map.of("Accept","application/vnd.github+json"):Map.of("Accept","application/vnd.github+json","Authorization","Bearer "+config.githubToken(),"X-GitHub-Api-Version","2022-11-28");}

    List<Source> collect(Question question){
        ensureQuestionRepositoryPublic();
        List<String> keys=KeywordExtractor.extract(question); List<Source> found=new ArrayList<>();
        List<String> searches=queries(keys);
        for(String terms:searches){ found.addAll(searchCode(terms)); found.addAll(searchIssues(terms)); }
        found.addAll(searchDiscussions(searches));
        found=dedupe(found); found.sort(Comparator.comparingInt(Source::score).reversed());
        List<Source> selected=new ArrayList<>(found.stream().limit(9).toList());
        followSchemaImports(selected,found);
        return dedupe(selected).stream().sorted(Comparator.comparingInt(Source::score).reversed()).limit(12).toList();
    }

    private void ensureQuestionRepositoryPublic(){
        Map<String,Object> repo=Json.object(Json.parse(http.get("https://api.github.com/repos/"+config.repository(),headers).body()));
        if(Boolean.TRUE.equals(repo.get("private"))) throw new IllegalStateException("A kérdést tartalmazó repository privát. Ez a csomag csak nyilvános forrásokkal működik.");
    }
    private List<String> queries(List<String> k){
        LinkedHashSet<String> q=new LinkedHashSet<>();
        if(!k.isEmpty())q.add(quote(k.get(0))+" "+(k.size()>1?quote(k.get(1)):""));
        if(k.contains("VatAnalytics"))q.add("VatAnalytics earData");
        if(k.contains("tokenExchange"))q.add("tokenExchange requestVersion");
        if(k.contains("receiptData"))q.add("receiptData xsd");
        if(k.contains("hash"))q.add("hash upload");
        if(q.size()<3&&k.size()>3)q.add(quote(k.get(2))+" "+quote(k.get(3)));
        return q.stream().filter(s->!s.isBlank()).limit(3).toList();
    }
    private String quote(String s){return s.matches("[A-Za-z0-9_-]+")?s:"\""+s.replace("\"","")+"\"";}
    private List<Source> searchCode(String terms){
        String q=terms+" org:"+config.organization();
        Object raw=Json.parse(http.get("https://api.github.com/search/code?q="+WebClient.query(q)+"&per_page=10",headers).body());
        List<Source> out=new ArrayList<>(); for(Object item:Json.array(Json.object(raw).get("items"))){Map<String,Object> i=Json.object(item);String url=Json.string(i.get("url"));
            try{Map<String,Object> file=Json.object(Json.parse(http.get(url,headers).body()));String content=decode(file);String html=Json.string(file.get("html_url"));String path=Json.string(file.get("path"));out.add(new Source(Json.string(Json.object(i.get("repository")).get("full_name"))+"/"+path,html,WebClient.shorten(content,12000),scorePath(path)));}catch(Exception x){System.out.println("Figyelmeztetés: GitHub-fájl kihagyva: "+x.getMessage());}}
        return out;
    }
    private List<Source> searchIssues(String terms){
        String q=terms+" org:"+config.organization()+" is:issue"; Object raw=Json.parse(http.get("https://api.github.com/search/issues?q="+WebClient.query(q)+"&per_page=6",headers).body());
        List<Source> out=new ArrayList<>();for(Object item:Json.array(Json.object(raw).get("items"))){Map<String,Object> i=Json.object(item);String content=Json.string(i.get("title"))+"\n\n"+Json.string(i.get("body"));String comments=Json.string(i.get("comments_url"));
            if(!comments.isBlank())try{for(Object c:Json.array(Json.parse(http.get(comments+"?per_page=20",headers).body())))content+="\n\nHOZZÁSZÓLÁS:\n"+Json.string(Json.object(c).get("body"));}catch(Exception ignored){}
            out.add(new Source("GitHub Issue: "+Json.string(i.get("title")),Json.string(i.get("html_url")),WebClient.shorten(content,10000),45));}
        return out;
    }
    private List<Source> searchDiscussions(List<String> searches){
        if(config.githubToken().isBlank())return List.of(); List<Source> out=new ArrayList<>();
        String query="query($q:String!){search(query:$q,type:DISCUSSION,first:8){nodes{... on Discussion{title body url comments(first:15){nodes{body}}}}}}";
        for(String terms:searches)try{Map<String,Object> request=Map.of("query",query,"variables",Map.of("q",terms+" org:"+config.organization()));Map<String,Object> root=Json.object(Json.parse(http.postJson("https://api.github.com/graphql",request,headers).body()));
            Object data=root.get("data");if(data==null)continue;Map<String,Object> search=Json.object(Json.object(data).get("search"));for(Object node:Json.array(search.get("nodes"))){Map<String,Object> d=Json.object(node);String content=Json.string(d.get("body"));Object comments=d.get("comments");if(comments!=null)for(Object c:Json.array(Json.object(comments).get("nodes")))content+="\n\nHOZZÁSZÓLÁS:\n"+Json.string(Json.object(c).get("body"));out.add(new Source("GitHub Discussion: "+Json.string(d.get("title")),Json.string(d.get("url")),WebClient.shorten(content,10000),50));}}
        catch(Exception x){System.out.println("Figyelmeztetés: Discussion keresés sikertelen: "+x.getMessage());}
        return out;
    }
    private void followSchemaImports(List<Source> selected,List<Source> all){
        Pattern p=Pattern.compile("schemaLocation\\s*=\\s*[\"']([^\"']+)[\"']"); List<Source> extra=new ArrayList<>();
        for(Source s:selected)if(s.url().contains("github.com/")&&s.title().toLowerCase().endsWith(".xsd")){Matcher m=p.matcher(s.content());while(m.find()&&extra.size()<4){String location=m.group(1);if(location.startsWith("http"))continue;try{
            URI blob=URI.create(s.url());String[] parts=blob.getPath().split("/",6);if(parts.length<6||!"blob".equals(parts[3]))continue;
            String owner=parts[1],repo=parts[2],branch=parts[4],filePath=parts[5];String parent=filePath.contains("/")?filePath.substring(0,filePath.lastIndexOf('/')+1):"";
            String resolvedPath=URI.create("https://x/"+parent).resolve(location).getPath().substring(1);String rawUrl="https://raw.githubusercontent.com/"+owner+"/"+repo+"/"+branch+"/"+resolvedPath;
            if(!owner.equalsIgnoreCase(config.organization()))continue;String body=http.get(rawUrl,Map.of()).body();extra.add(new Source("XSD include/import: "+location,rawUrl,WebClient.shorten(body,10000),90));
        }catch(Exception ignored){}}}
        selected.addAll(extra);
    }
    private String decode(Map<String,Object> file){String c=Json.string(file.get("content")).replaceAll("\\s","");return "base64".equals(Json.string(file.get("encoding")))?new String(Base64.getDecoder().decode(c),StandardCharsets.UTF_8):c;}
    private int scorePath(String path){String p=path.toLowerCase(Locale.ROOT);int s=p.endsWith(".xsd")?100:p.endsWith(".xml")?95:p.endsWith(".md")?70:55;if(p.contains("sample")||p.contains("example")||p.contains("minta"))s+=30;if(p.contains("doc")||p.contains("spec"))s+=15;return s;}
    private List<Source> dedupe(List<Source> sources){LinkedHashMap<String,Source> d=new LinkedHashMap<>();for(Source s:sources)if(!s.url().isBlank()&&!s.content().isBlank())d.merge(s.url(),s,(a,b)->a.score()>=b.score()?a:b);return new ArrayList<>(d.values());}
}
