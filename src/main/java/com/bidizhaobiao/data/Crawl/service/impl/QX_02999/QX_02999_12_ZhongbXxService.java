package com.bidizhaobiao.data.Crawl.service.impl.QX_02999;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.utils.HttpConstant;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 程序员：余林锐  日期：2022-05-23
 * 原网站：http://ggzyjy.weihai.cn/rongcheng/jyxx/003001/003001008/transInfo.html
 * 主页：http://ggzyjy.weihai.cn
 **/

@Service("QX_02999_12_ZhongbXxService")
public class QX_02999_12_ZhongbXxService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://ggzyjy.weihai.cn/EpointWebBuilder/rest/frontAppCustomAction/getPageInfoListNew?page=1";

    public String baseUrl = "http://ggzyjy.weihai.cn";

    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(3).setTimeOut(30000).setSleepTime(20);
    // 网站编号
    public String sourceNum = "02999-12";
    // 网站名称
    public String sourceName = "荣成公共资源交易网";
    // 信息源
    public String infoSource = "工程建设";
    // 设置地区
    public String area = "华东";
    // 设置省份
    public String province = "山东";
    // 设置城市
    public String city = "威海市";
    // 设置县
    public String district = "荣成市";
    // 设置CreateBy
    public String createBy = "余林锐";

    public Pattern p = Pattern.compile("(\\d{4})(年|/|-)(\\d{1,2})(月|/|-)(\\d{1,2})");

    public Pattern p_p = Pattern.compile("view\\('(.*?)','(.*?)','(.*?)'\\)");

    public Site getSite() {
        String result = getContent("http://ggzyjy.weihai.cn/EpointWebBuilder/rest/getOauthInfoAction/getNoUserAccessToken");
        String authorization ="";
        try{
            if(result.contains("操作成功")){
                result = result.substring(result.indexOf("{")).trim();
                JSONObject jsonObject = new JSONObject(result);
                if(jsonObject.has("custom")){
                    JSONObject custom = jsonObject.getJSONObject("custom");
                    String access_token = custom.getString("access_token").trim();
                    authorization = "Bearer " + access_token;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return this.site.addHeader("Authorization", authorization).setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
    }

    public void startCrawl(int ThreadNum, int crawlType) {
        // 赋值
        serviceContextEvaluation();
        serviceContext.setCrawlType(crawlType);
        // 保存日志
        saveCrawlLog(serviceContext);
        // 启动爬虫
        spider = Spider.create(this).thread(ThreadNum)
                .setDownloader(new MyDownloader(serviceContext, false, listUrl));
        Request request = getRequest(listUrl, 1);
        spider.addRequest(request);
        serviceContext.setSpider(spider);
        spider.run();
        // 爬虫状态监控部分
        saveCrawlResult(serviceContext);
    }

    public void process(Page page) {
        String url = page.getUrl().toString();
        try {
            List<BranchNew> detailList = new ArrayList<BranchNew>();
            Thread.sleep(2000);
            // 判断是否是翻页连接
            if (url.contains("page=")) {
                String result = page.getRawText().replace("&nbsp;", "").replace("&amp;", "&").replace("&ensp;", "").replace("<![CDATA[", "").replace("]]>", "").replace("&lt;", "<").replace("&gt;", ">");
                result = result.substring(result.indexOf("{")).trim();
                JSONObject jsonObject = new JSONObject(result);
                JSONObject custom = jsonObject.getJSONObject("custom");
                if (serviceContext.getPageNum() == 1 && custom.has("count")) {
                    int total = custom.getInt("count");
                    int maxPage = total / 12;
                    if (total % 12 != 0) {
                        maxPage++;
                    }
                    serviceContext.setMaxPage(maxPage);
                }
                JSONArray infodata = custom.getJSONArray("infodata");
                if (infodata.length() > 0) {
                    for (int i = 0; i < infodata.length(); i++) {
                        JSONObject object = infodata.getJSONObject(i);
                        String title = object.getString("title").trim();
                        title = title.replace("...", "").replace(" ", "").trim();
                        String id = object.getString("infourl").trim();
                        String link = baseUrl + id;
                        String detailLink = link;
                        String date = object.getString("infodate").trim();
                        dealWithNullTitleOrNullId(serviceContext, title, id);
                        BranchNew branch = new BranchNew();
                        branch.setId(id);
                        serviceContext.setCurrentRecord(id);
                        branch.setDetailLink(detailLink);
                        branch.setLink(link);
                        branch.setTitle(title);
                        branch.setDate(date);
                        detailList.add(branch);
                    }
                    // 校验数据,判断是否需要继续触发爬虫
                    List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                    for (BranchNew branch : needCrawlList) {
                        map.put(branch.getLink(), branch);
                        page.addTargetRequest(branch.getLink());
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    Request request = getRequest(listUrl.replace("page=1", "page=" + serviceContext.getPageNum()), serviceContext.getPageNum());
                    page.addTargetRequest(request);
                }
            } else {
                // 列表页请求
                BranchNew branchNew = map.get(url);
                if (branchNew == null) {
                    return;
                }
                String homeUrl = baseUrl;
                String title = branchNew.getTitle();
                String id = branchNew.getId();
                serviceContext.setCurrentRecord(id);
                String date = branchNew.getDate();
                String detailLink = branchNew.getDetailLink();
                String content = "";
                Document document = Jsoup.parse(page.getRawText().replace("&nbsp;", "").replace("&amp;", "&").replace("&ensp;", "").replace("<![CDATA[", "").replace("]]>", "").replace("&lt;", "<").replace("&gt;", ">"));
                Element contentE = document.select("div.content-article").first();
                Elements elements = contentE.getAllElements();
                for (Element element : elements) {
                    element.removeAttr("style");
                }
                contentE.select("div#share").remove();
                contentE.select("iframe").remove();
                contentE.select("meta").remove();
                contentE.select("link").remove();
                contentE.select("button").remove();
                contentE.select("input").remove();
                contentE.select("*[style~=^.*display\\s*:\\s*none\\s*(;\\s*[0-9A-Za-z]+|;\\s*)?$]").remove();
                contentE.select("script").remove();
                contentE.select("style").remove();
                if (contentE.select("a") != null) {
                    Elements as = contentE.select("a");
                    for (Element a : as) {
                        a.attr("rel", "noreferrer");
                        String href = a.attr("href");
                        if (!href.contains("@") && !"".equals(href) && !href.contains("javascript") && !href.contains("http") && !href.contains("#")) {
                            if (href.contains("../")) {
                                href = homeUrl + "/" + href.replace("../", "");
                                a.attr("href", href);
                            } else if (href.startsWith("/")) {
                                href = homeUrl + href;
                                a.attr("href", href);
                            } else if (href.startsWith("./")) {
                                href = url.substring(0, url.lastIndexOf("/") + 1) + href.replace("./", "");
                                a.attr("href", href);
                            } else if (href.startsWith(" ")) {
                                href = url.substring(0, url.lastIndexOf("/")) + href.replace(" ", "");
                                a.attr("href", href);
                            } else {
                                href = homeUrl + "/" + href;
                                a.attr("href", href);
                            }
                        }
                        if (a.attr("onclick").contains("openinfo")) {
                            String onclick = a.attr("onclick").trim();
                            onclick = onclick.substring(onclick.indexOf("'") + 1, onclick.lastIndexOf("'")).trim();
                            href = baseUrl + onclick;
                            a.removeAttr("onclick");
                            a.attr("href", href);
                        }
                        if (a.attr("href").equals("")) {
                            a.removeAttr("href");
                        }
                    }
                }
                if (contentE.select("img").first() != null) {
                    Elements imgs = contentE.select("img");
                    for (Element img : imgs) {
                        String src = img.attr("src");
                        if (!src.contains("javascript") && !"".equals(src) && !src.contains("http") && !src.contains("data:image")) {
                            if (src.contains("../")) {
                                src = homeUrl + "/" + src.replace("../", "");
                                img.attr("src", src);
                            } else if (src.startsWith("/")) {
                                src = homeUrl + src;
                                img.attr("src", src);
                            } else if (src.startsWith("./")) {
                                src = url.substring(0, url.lastIndexOf("/") + 1) + src.replace("./", "");
                                img.attr("src", src);
                            } else if (src.startsWith(" ")) {
                                src = url.substring(0, url.lastIndexOf("/")) + src.replace(" ", "");
                                img.attr("src", src);
                            } else {
                                src = homeUrl + "/" + src;
                                img.attr("src", src);
                            }
                        }
                        if (img.attr("src").equals("")) {
                            img.removeAttr("src");
                        }
                    }
                }
                if (contentE.select("a[href*=javascript]").first() != null) {
                    Elements as = contentE.select("a[href*=javascript]");
                    for (Element a : as) {
                        a.removeAttr("href");
                    }
                }
                if (contentE.select("a[href*=#]").first() != null) {
                    Elements as = contentE.select("a[href*=#]");
                    for (Element a : as) {
                        a.removeAttr("href");
                    }
                }
                content = "<div>" + title + "</div>" + contentE.outerHtml();
                RecordVO recordVO = new RecordVO();
                recordVO.setId(id);
                recordVO.setDate(date);
                recordVO.setContent(content);
                recordVO.setTitle(title.replaceAll("\\<.*?\\>", ""));//详情页标题
                recordVO.setDetailLink(detailLink);//详情页链接
                dataStorage(serviceContext, recordVO, branchNew.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }

    public Request getRequest(String link, int pageIndex) {
        Request request = new Request(link);
        request.setMethod(HttpConstant.Method.POST);
        Map<String, Object> map = new HashMap<>();
        map.put("params", "{\"siteGuid\":\"3f183aed-2171-4692-8cfc-ea14fe07cc96\",\"categoryNum\":\"003001008\",\"kw\":\"\",\"startDate\":\"\",\"endDate\":\"\",\"pageIndex\":" + (pageIndex - 1) + ",\"pageSize\":12,\"area\":\"\"}");
        request.setRequestBody(HttpRequestBody.form(map, "UTF-8"));
        return request;
    }

    public String getContent(String url) {
        String result = "";
        CloseableHttpClient httpClient = getHttpClient(false, false);
        CloseableHttpResponse httpResponse = null;
        HttpPost httpPost = new HttpPost(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10 * 1000).setSocketTimeout(60 * 1000).build();
        httpPost.setConfig(requestConfig);
        httpPost.setHeader("Connection", "close");
        try {
            httpResponse = httpClient.execute(httpPost);
            httpResponse.setHeader("Connection", "close");
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity httpEntity = httpResponse.getEntity();
                InputStream ins = httpEntity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(ins, "UTF-8"));
                StringBuilder stringBuilder = new StringBuilder();
                while ((result = reader.readLine()) != null) {
                    stringBuilder.append(result);
                }
                result = stringBuilder.toString();
                if (ins != null) {
                    ins.close();
                }
                httpPost.abort();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }


}
