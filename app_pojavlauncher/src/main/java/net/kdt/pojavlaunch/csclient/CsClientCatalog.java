package net.kdt.pojavlaunch.csclient;

import net.kdt.pojavlaunch.utils.DownloadUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Modrinth-backed catalog for Blaze Client and optional Fabric content. */
public final class CsClientCatalog {
    public static final String BLAZE_PROJECT = "xakOmmYI";
    public static final String FABRIC_API_PROJECT = "P7dR8mSH";
    private static final String API = "https://api.modrinth.com/v2/";

    public static final class VersionEntry implements Serializable {
        public String minecraft, versionId, build, name, fileUrl, fileName, sha1;
    }
    public static final class ProjectEntry implements Serializable {
        public String id, title, description, iconUrl;
        public boolean modpack;
        public ProjectEntry(String id, String title, String description, String iconUrl, boolean modpack) {
            this.id=id;this.title=title;this.description=description;this.iconUrl=iconUrl;this.modpack=modpack;
        }
    }
    public static final class FileEntry {
        public String url, name, sha1;
        public FileEntry(String u,String n,String s){url=u;name=n;sha1=s;}
    }

    private CsClientCatalog() {}

    public static List<VersionEntry> fetchLatestByMinecraft() throws Exception {
        JSONArray versions = new JSONArray(DownloadUtils.downloadString(API+"project/"+BLAZE_PROJECT+"/version"));
        LinkedHashMap<String,VersionEntry> grouped = new LinkedHashMap<>();
        for(int i=0;i<versions.length();i++){
            JSONObject v=versions.getJSONObject(i); JSONArray games=v.getJSONArray("game_versions");
            JSONObject file=primary(v.getJSONArray("files")); if(file==null)continue;
            for(int g=0;g<games.length();g++){
                String mc=games.getString(g); if(grouped.containsKey(mc))continue;
                VersionEntry e=new VersionEntry();e.minecraft=mc;e.versionId=v.getString("id");
                e.build=v.optString("version_number","");e.name=v.optString("name",e.build);
                e.fileUrl=file.getString("url");e.fileName=file.getString("filename");
                e.sha1=file.optJSONObject("hashes")!=null?file.getJSONObject("hashes").optString("sha1",null):null;
                grouped.put(mc,e);
            }
        }
        List<VersionEntry> out=new ArrayList<>(grouped.values());
        out.sort((a,b)->compareVersions(b.minecraft,a.minecraft)); return out;
    }

    public static List<ProjectEntry> search(String query,String mc,boolean packs) throws Exception {
        String facets="[[\"project_type:"+(packs?"modpack":"mod")+"\"],[\"versions:"+mc+"\"],[\"categories:fabric\"]]";
        String url=API+"search?limit=40&index=downloads&query="+URLEncoder.encode(query==null?"":query,"UTF-8")+"&facets="+URLEncoder.encode(facets,"UTF-8");
        JSONObject root=new JSONObject(DownloadUtils.downloadString(url));JSONArray hits=root.getJSONArray("hits");
        List<ProjectEntry> out=new ArrayList<>();
        for(int i=0;i<hits.length();i++){JSONObject h=hits.getJSONObject(i);out.add(new ProjectEntry(
                h.getString("project_id"),h.getString("title"),h.optString("description",""),h.optString("icon_url",null),packs));}
        return out;
    }

    public static FileEntry latestFile(String project,String mc,boolean modpack) throws Exception {
        JSONArray arr=new JSONArray(DownloadUtils.downloadString(API+"project/"+project+"/version"));
        for(int i=0;i<arr.length();i++){
            JSONObject v=arr.getJSONObject(i);if(!contains(v.optJSONArray("game_versions"),mc))continue;
            if(!modpack&&!containsIgnoreCase(v.optJSONArray("loaders"),"fabric"))continue;
            JSONObject f=primary(v.optJSONArray("files"));if(f==null)continue;
            JSONObject hashes=f.optJSONObject("hashes");return new FileEntry(f.getString("url"),f.getString("filename"),hashes==null?null:hashes.optString("sha1",null));
        }
        throw new IllegalStateException("No compatible "+(modpack?"modpack":"mod")+" version for Minecraft "+mc);
    }

    private static JSONObject primary(JSONArray a){if(a==null)return null;for(int i=0;i<a.length();i++){JSONObject f=a.optJSONObject(i);if(f!=null&&f.optBoolean("primary",false))return f;}return a.optJSONObject(0);}
    private static boolean contains(JSONArray a,String value){if(a==null)return false;for(int i=0;i<a.length();i++)if(value.equals(a.optString(i)))return true;return false;}
    private static boolean containsIgnoreCase(JSONArray a,String value){if(a==null)return false;for(int i=0;i<a.length();i++)if(value.equalsIgnoreCase(a.optString(i)))return true;return false;}
    private static int compareVersions(String a,String b){String[]x=a.split("\\."),y=b.split("\\.");for(int i=0;i<Math.max(x.length,y.length);i++){int xi=i<x.length?num(x[i]):0,yi=i<y.length?num(y[i]):0;if(xi!=yi)return Integer.compare(xi,yi);}return a.compareTo(b);}
    private static int num(String s){try{return Integer.parseInt(s.replaceAll("[^0-9].*",""));}catch(Exception e){return 0;}}
}