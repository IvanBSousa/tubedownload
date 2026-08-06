package com.tubedownload.javatube;

import com.tubedownload.javatube.InnerTube;
import com.tubedownload.javatube.exceptions.RegexMatchError;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Playlist {
    private final String url;
    protected String html = null;
    protected JSONObject json = null;
    protected String continuationToken = null;
    protected String visitorData = null;
    InnerTube innerTube;

    public Playlist(String InputUrl) throws JSONException {
        url = InputUrl;
        innerTube = new InnerTube("WEB");
    }

    @Override
    public String toString(){
        try {
            return "<com.tubedownload.javatube.Playlist object: playlistId=" + getPlaylistId() + ">";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getPlaylistId() throws Exception {
        Pattern pattern = Pattern.compile("list=([a-zA-Z0-9_\\-]*)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()){
            return matcher.group(1);
        }else {
            throw new RegexMatchError("getPlaylistId: Unable to find match on: " + url);
        }
    }

    private String getPlaylistUrl() throws Exception {
        return "https://www.youtube.com/playlist?list=" + getPlaylistId();
    }

    protected String setHtml() throws Exception {
        return Request.get(getPlaylistUrl(), null, innerTube.getClientHeaders()).toString();
    }
    protected String getHtml() throws Exception {
        if(html == null){
            html = setHtml();
        }
        return html;
    }

    protected JSONObject setJson() throws Exception {
        String page = getHtml();
        Pattern[] patterns = new Pattern[] {
                Pattern.compile("var ytInitialData\\s*=\\s*(\\{.*?\\});</script>", Pattern.DOTALL),
                Pattern.compile("ytInitialData\\s*=\\s*(\\{.*?\\});</script>", Pattern.DOTALL),
                Pattern.compile("ytInitialData\\s*=\\s*(\\{.*?\\});", Pattern.DOTALL)
        };
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(page);
            if (matcher.find()) {
                return new JSONObject(matcher.group(1));
            }
        }
        throw new RegexMatchError("setJson: ytInitialData not found");
    }

    protected JSONObject getJson() throws Exception {
        if(json == null){
            json = setJson();
        }
        return json;
    }

    protected void setContinuationToken(JSONArray importantContent) throws JSONException {
        JSONObject continuationEndpoint = importantContent.getJSONObject(importantContent.length() - 1)
                .getJSONObject("continuationItemRenderer")
                .getJSONObject("continuationEndpoint");

        if (continuationEndpoint.has("continuationCommand")){
            continuationToken = continuationEndpoint
                    .getJSONObject("continuationCommand")
                    .getString("token");
            
        }else if (continuationEndpoint.has("commandExecutorCommand")){
            continuationToken = continuationEndpoint
                    .getJSONObject("commandExecutorCommand")
                    .getJSONArray("commands")
                    .getJSONObject(1)
                    .getJSONObject("continuationCommand")
                    .getString("token");
        }
    }

    protected JSONArray extractContinuationItems(JSONArray importantContent) throws Exception {
        JSONArray swap = new JSONArray();

        JSONArray continuationEnd = buildContinuationUrl(continuationToken);

        for(int i = 0; i < importantContent.length(); i++){
            swap.put(importantContent.get(i));
        }

        for(int i = 0; i < continuationEnd.length(); i++){
            swap.put(continuationEnd.get(i));
        }
        return swap;
    }

    private JSONArray extractContentsFromContinuation(JSONObject rawJson) throws JSONException {
        if (rawJson.has("continuationContents")) {
            JSONObject continuationContents = rawJson.getJSONObject("continuationContents");
            if (continuationContents.has("playlistVideoListContinuation")) {
                return continuationContents
                        .getJSONObject("playlistVideoListContinuation")
                        .getJSONArray("contents");
            }
            if (continuationContents.has("richGridContinuation")) {
                return continuationContents
                        .getJSONObject("richGridContinuation")
                        .getJSONArray("contents");
            }
        }

        if (rawJson.has("onResponseReceivedActions")) {
            JSONArray actions = rawJson.getJSONArray("onResponseReceivedActions");
            for (int i = 0; i < actions.length(); i++) {
                JSONObject action = actions.getJSONObject(i);
                if (action.has("appendContinuationItemsAction")) {
                    return action.getJSONObject("appendContinuationItemsAction")
                            .getJSONArray("continuationItems");
                }
            }
        }

        return new JSONArray();
    }

    private JSONArray extractContentsFromTabs(JSONObject rawJson) throws JSONException {
        JSONObject browseResults = rawJson.getJSONObject("contents")
                .getJSONObject("twoColumnBrowseResultsRenderer");

        JSONArray tabs = browseResults.getJSONArray("tabs");
        JSONObject selectedTab = null;
        for (int i = 0; i < tabs.length(); i++) {
            JSONObject tab = tabs.getJSONObject(i).getJSONObject("tabRenderer");
            if (tab.optBoolean("selected", false)) {
                selectedTab = tab;
                break;
            }
            if (selectedTab == null) {
                selectedTab = tab;
            }
        }

        if (selectedTab == null || !selectedTab.has("content")) {
            return new JSONArray();
        }

        JSONObject content = selectedTab.getJSONObject("content");
        if (content.has("sectionListRenderer")) {
            JSONArray sections = content.getJSONObject("sectionListRenderer").getJSONArray("contents");
            for (int j = 0; j < sections.length(); j++) {
                JSONObject section = sections.getJSONObject(j).getJSONObject("itemSectionRenderer");
                JSONArray sectionContents = section.getJSONArray("contents");
                for (int k = 0; k < sectionContents.length(); k++) {
                    JSONObject renderer = sectionContents.getJSONObject(k);
                    if (renderer.has("playlistVideoListRenderer")) {
                        return renderer.getJSONObject("playlistVideoListRenderer").getJSONArray("contents");
                    }
                    if (renderer.has("richGridRenderer")) {
                        return renderer.getJSONObject("richGridRenderer").getJSONArray("contents");
                    }
                }
            }
        }

        if (content.has("richGridRenderer")) {
            return content.getJSONObject("richGridRenderer").getJSONArray("contents");
        }

        return new JSONArray();
    }

    protected JSONArray buildContinuationUrl(String continuation) throws Exception {
        String data = "{" +
                        "\"continuation\": \"" + continuation + "\"," +
                        "\"context\": {" +
                            "\"client\": {" +
                                "\"visitorData\": \"" + visitorData + "\"" +
                            "}" +
                        "}" +
                    "}";
        return extractVideos(innerTube.browse(new JSONObject(data)));
    }

    protected JSONArray extractVideos(JSONObject rawJson) {
        JSONArray swap = new JSONArray();
        try {
            JSONArray importantContent = new JSONArray();
            if (rawJson.has("contents")) {
                JSONObject responseContext = rawJson.optJSONObject("responseContext");
                if (responseContext != null) {
                    JSONObject extensionData = responseContext.optJSONObject("webResponseContextExtensionData");
                    if (extensionData != null) {
                        JSONObject ytConfigData = extensionData.optJSONObject("ytConfigData");
                        if (ytConfigData != null) {
                            visitorData = ytConfigData.optString("visitorData", visitorData);
                        }
                    }
                }
                importantContent = extractContentsFromTabs(rawJson);
            } else if (rawJson.has("continuationContents") || rawJson.has("onResponseReceivedActions")) {
                importantContent = extractContentsFromContinuation(rawJson);
            }

            if (importantContent.isEmpty()) {
                importantContent = extractContentsFromContinuation(rawJson);
            }

            if (importantContent.length() > 0
                    && importantContent.getJSONObject(importantContent.length() - 1).has("continuationItemRenderer")) {
                setContinuationToken(importantContent);
                swap = extractContinuationItems(importantContent);
            } else {
                for(int i = 0; i < importantContent.length(); i++){
                    swap.put(importantContent.get(i));
                }
            }

        }catch (Exception ignored){
        }
        if (swap.isEmpty()) {
            swap = collectItemsRecursively(rawJson);
        }
        return swap;
    }

    private JSONArray collectItemsRecursively(Object node) {
        JSONArray items = new JSONArray();
        collectItemsRecursively(node, items);
        return items;
    }

    private void collectItemsRecursively(Object node, JSONArray items) {
            if (node instanceof JSONObject object) {
            if (object.has("playlistVideoRenderer")
                    || object.has("gridVideoRenderer")
                    || object.has("videoRenderer")
                    || object.has("richItemRenderer")
                    || object.has("shortsLockupViewModel")
                    || object.has("playlistRenderer")
                    || object.has("playlistVideoListRenderer")) {
                items.put(object);
            }
            for (String key : object.keySet()) {
                collectItemsRecursively(object.get(key), items);
            }
        } else if (node instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                collectItemsRecursively(array.get(i), items);
            }
        }
    }

    protected ArrayList<String> unify(ArrayList<String> list){
        LinkedHashSet<String> unifiedList = new LinkedHashSet<>(list);
        list.clear();
        list.addAll(unifiedList);
        return list;
    }

    public ArrayList<String> getVideos() throws Exception {
        JSONArray video = extractVideos(getJson());
        ArrayList<String> videosId = new ArrayList<>();
        try {
            for(int i = 0; i < video.length(); i++){
                try{
                    if (video.getJSONObject(i).has("richItemRenderer")){
                        JSONObject content = video.getJSONObject(i)
                                .getJSONObject("richItemRenderer")
                                .getJSONObject("content");
                        if (content.has("shortsLockupViewModel")) {
                            videosId.add("https://www.youtube.com/watch?v=" + content
                                    .getJSONObject("shortsLockupViewModel")
                                    .getJSONObject("onTap")
                                    .getJSONObject("innertubeCommand")
                                    .getJSONObject("reelWatchEndpoint")
                                    .getString("videoId"));
                        } else if (content.has("videoRenderer")) {
                            videosId.add("https://www.youtube.com/watch?v=" + content
                                    .getJSONObject("videoRenderer")
                                    .getString("videoId"));
                        } else if (content.has("playlistRenderer")) {
                            videosId.add("https://www.youtube.com/playlist?list=" + content
                                    .getJSONObject("playlistRenderer")
                                    .getString("playlistId"));
                        }
                    } else if (video.getJSONObject(i).has("gridVideoRenderer")) {
                        videosId.add("https://www.youtube.com/watch?v=" + video.getJSONObject(i)
                                .getJSONObject("gridVideoRenderer")
                                .getString("videoId"));
                    } else if (video.getJSONObject(i).has("playlistVideoRenderer")) {
                        videosId.add("https://www.youtube.com/watch?v=" + video.getJSONObject(i)
                                .getJSONObject("playlistVideoRenderer")
                                .getString("videoId"));
                    } else if (video.getJSONObject(i).has("playlistVideoListRenderer")) {
                        JSONArray contents = video.getJSONObject(i)
                                .getJSONObject("playlistVideoListRenderer")
                                .getJSONArray("contents");
                        for (int j = 0; j < contents.length(); j++) {
                            JSONObject item = contents.getJSONObject(j);
                            if (item.has("playlistVideoRenderer")) {
                                videosId.add("https://www.youtube.com/watch?v=" + item
                                        .getJSONObject("playlistVideoRenderer")
                                        .getString("videoId"));
                            }
                        }
                    } else if (video.getJSONObject(i).has("videoRenderer")) {
                        videosId.add("https://www.youtube.com/watch?v=" + video.getJSONObject(i)
                                .getJSONObject("videoRenderer")
                                .getString("videoId"));
                    }else {
                        continue;
                    }
                }catch (Exception ignored){
                }
            }
            return unify(videosId);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private JSONObject getSidebarInfo(Integer i) throws Exception {
        return getJson().getJSONObject("sidebar")
                .getJSONObject("playlistSidebarRenderer")
                .getJSONArray("items")
                .getJSONObject(i);
    }

    public String getUrl() throws Exception {
        return getPlaylistUrl();
    }

    public String getTitle() throws Exception {
        return getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
                .getJSONObject("title")
                .getJSONArray("runs")
                .getJSONObject(0)
                .getString("text");
    }

    public String getDescription() throws Exception {
        try {
            try {
                return getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
                        .getJSONObject("description")
                        .getString("simpleText");
            }catch (JSONException e) {
                return getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
                        .getJSONObject("description")
                        .getJSONArray("runs")
                        .getJSONObject(0)
                        .getString("text");
            }
        }catch (Exception e){
            return null;
        }
    }

    public String getViews() throws Exception {
        return getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
                .getJSONArray("stats")
                .getJSONObject(1)
                .getString("simpleText");
    }

    public String getLastUpdated() throws Exception {
        try {
            return getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
                    .getJSONArray("stats").getJSONObject(2)
                    .getJSONArray("runs").getJSONObject(1)
                    .getString("text");
        }catch (JSONException e){
            return getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
                    .getJSONArray("stats")
                    .getJSONObject(2)
                    .getJSONArray("runs")
                    .getJSONObject(0)
                    .getString("text");
        }
    }

    public String getOwner() throws Exception {
        return getSidebarInfo(1).getJSONObject("playlistSidebarSecondaryInfoRenderer")
                .getJSONObject("videoOwner")
                .getJSONObject("videoOwnerRenderer")
                .getJSONObject("title")
                .getJSONArray("runs")
                .getJSONObject(0)
                .getString("text");
    }

    public String getOwnerId() throws Exception {
        return getSidebarInfo(1).getJSONObject("playlistSidebarSecondaryInfoRenderer")
                .getJSONObject("videoOwner")
                .getJSONObject("videoOwnerRenderer")
                .getJSONObject("title")
                .getJSONArray("runs")
                .getJSONObject(0)
                .getJSONObject("navigationEndpoint")
                .getJSONObject("browseEndpoint")
                .getString("browseId");
    }

    public String getOwnerUrl() throws Exception {
        return "https://www.youtube.com/channel/" + getOwnerId();
    }

    public String length() throws Exception {
        return getSidebarInfo(0).getJSONObject("playlistSidebarPrimaryInfoRenderer")
                .getJSONArray("stats")
                .getJSONObject(0)
                .getJSONArray("runs")
                .getJSONObject(0)
                .getString("text");
    }

}
