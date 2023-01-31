package com.bidizhaobiao.data.Crawl.service.impl.QX_24613;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
import com.bidizhaobiao.data.Crawl.utils.SpecialUtil;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 程序员：徐文帅 日期：2023-01-31
 * 原网站：http://ec.hbncp.com.cn/#/home/candidates
 * 主页：http://ec.hbncp.com.cn/#/home
 **/
@Service
public class QX_24613_2_ZhongbXxService extends SpiderService implements PageProcessor {
    public Spider spider = null;

    public String listUrl = "https://ebs.hbncp.com.cn/ebsapi/project/result/resultnote/public/send/lists";
    public String baseUrl = "http://ec.hbncp.com.cn/#/home/candidates";
    public Pattern datePat = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");

    // 网站编号
    public String sourceNum = "24613-2";
    // 网站名称
    public String sourceName = "鄂州市鄂城区政府采购电子交易平台";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华中";
    // 设置省份
    public String province = "湖北";
    // 设置城市
    public String city = "鄂州";
    // 设置县
    public String district = "鄂城";
    public String createBy = "徐文帅";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);

    public Site getSite() {
        return this.site;
    }

    public Request getListRequest(int pageNumber) {
        Request request = new Request(listUrl);
        request.setMethod(HttpConstant.Method.POST);
        String json = "{\n" +
                "  \"pageIndex\": " + pageNumber + ",\n" +
                "  \"pageSize\": 10,\n" +
                "  \"query\": {\n" +
                "    \"area\": [\n" +
                "      \"420704\"\n" +
                "    ],\n" +
                "    \"projectType\": 0,\n" +
                "    \"keyword_LIKE\": \"\"\n" +
                "  }\n" +
                "}";
        request.setRequestBody(HttpRequestBody.json(json, "UTF-8"));
        return request;
    }

    public void startCrawl(int ThreadNum, int crawlType) {
        // 赋值
        serviceContextEvaluation();
        // 保存日志
        serviceContext.setCrawlType(crawlType);
        saveCrawlLog(serviceContext);
        // 启动爬虫
        spider = Spider.create(this).thread(ThreadNum)
                .setDownloader(new MyDownloader(serviceContext, false, listUrl));
        spider.addRequest(getListRequest(1));
        serviceContext.setSpider(spider);
        spider.run();
        // 爬虫状态监控部分
        saveCrawlResult(serviceContext);
    }

    public void process(Page page) {
        String url = page.getUrl().toString();
        try {
            List<BranchNew> detailList = new ArrayList<BranchNew>();
            Thread.sleep(500);
            if (url.equals(listUrl)) {
                JSONObject dataJSON = JSONObject.parseObject(page.getRawText()).getJSONObject("data");
                JSONArray rows = dataJSON.getJSONArray("rows");
                if (rows.size() > 0) {
                    for (int i = 0; i < rows.size(); i++) {
                        JSONObject row = rows.getJSONObject(i);
                        String id = row.getString("id");
                        String link = "https://ebs.hbncp.com.cn/ebsapi/project/result/resultnote/public/send/get/" + id;
                        String detailLink = "http://ec.hbncp.com.cn/#/home/candidates/detail?id=" + id;

                        String date = "";
                        if (row.getString("proPublicBeginTime") != null) {
                            date = row.getString("proPublicBeginTime");
                            Matcher dateMat = datePat.matcher(date);
                            if (dateMat.find()) {
                                date = dateMat.group(1);
                                date += dateMat.group(3).length() == 2 ? "-" + dateMat.group(3) : "-0" + dateMat.group(3);
                                date += dateMat.group(5).length() == 2 ? "-" + dateMat.group(5) : "-0" + dateMat.group(5);
                            }
                        }
                        String title = row.getString("candTitle").trim();
                        BranchNew branch = new BranchNew();
                        branch.setId(id);
                        serviceContext.setCurrentRecord(branch.getId());
                        branch.setLink(link);
                        branch.setDetailLink(detailLink);
                        branch.setDate(date);
                        branch.setTitle(title);
                        detailList.add(branch);
                    }
                    List<BranchNew> branchNewList = checkData(detailList, serviceContext);
                    for (BranchNew branch : branchNewList) {
                        map.put(branch.getLink(), branch);
                        page.addTargetRequest(new Request("https://www.baidu.com?wd=" + branch.getLink()));
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
                if (serviceContext.getPageNum() == 1) {
                    int count = dataJSON.getInteger("total");
                    serviceContext.setMaxPage(count % 10 == 0 ? count / 10 : count / 10 + 1);
                }
                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    page.addTargetRequest(getListRequest(serviceContext.getPageNum()));
                }
            } else {
                url = url.substring(url.indexOf("=") + 1);
                String detailHtml = getContent(url);
                int count = 2;
                while ("".equals(detailHtml) && count > 0) {
                    detailHtml = getContent(url);
                    count--;
                }
                BranchNew branch = map.get(url);
                if (branch != null) {
                    map.remove(url);
                    serviceContext.setCurrentRecord(branch.getId());
                    JSONObject dataJSON = JSONObject.parseObject(detailHtml).getJSONObject("data");
                    String html = dataJSON.getString("proPublicContent");
                    Document doc = Jsoup.parse(html);
                    String title = branch.getTitle().replace("...", "");
                    String date = branch.getDate();
                    String content = "";
                    Element contentElement = doc.select("body").first();
                    if (contentElement != null) {
                        if (date.isEmpty()) {
                            Matcher dateMat = datePat.matcher(html);
                            if (dateMat.find()) {
                                date = dateMat.group(1);
                                date += dateMat.group(3).length() == 2 ? "-" + dateMat.group(3) : "-0" + dateMat.group(3);
                                date += dateMat.group(5).length() == 2 ? "-" + dateMat.group(5) : "-0" + dateMat.group(5);
                            }
                            if (date.isEmpty()) {
                                date = SpecialUtil.date2Str(new Date());
                            }
                        }
                        contentElement.tagName("div");
                        Elements aList = contentElement.select("a");
                        for (Element a : aList) {
                            String href = a.attr("href");
                            a.attr("rel", "noreferrer");
                            if (href.startsWith("mailto")) {
                                continue;
                            }
                            if (href.contains("javascript") || href.equals("#")) {
                                if (a.attr("onclick").contains("window.open('http")) {
                                    String onclick = a.attr("onclick");
                                    onclick = onclick.substring(onclick.indexOf("'") + 1, onclick.lastIndexOf("'"));
                                    a.attr("href", onclick);
                                    a.removeAttr("onclick");
                                } else {
                                    a.removeAttr("href");
                                    a.removeAttr("onclick");
                                }
                                continue;
                            }
                            if (href.length() > 10 && href.indexOf("http") != 0) {
                                if (href.indexOf("//www") == 0) {
                                    href = baseUrl.substring(0, baseUrl.indexOf(":") + 1) + href;
                                    a.attr("href", href);
                                } else if (href.indexOf("../") == 0) {
                                    href = href.replace("../", "");
                                    href = baseUrl + "/" + href;
                                    a.attr("href", href);
                                } else if (href.startsWith("/")) {
                                    href = baseUrl + href;
                                    a.attr("href", href);
                                } else if (href.indexOf("./") == 0) {
                                    href = url.substring(0, url.lastIndexOf("/")) + href.substring(href.lastIndexOf("./") + 1);
                                    a.attr("href", href);
                                } else {
                                    href = url.substring(0, url.lastIndexOf("/") + 1) + href;
                                    a.attr("href", href);
                                }
                            }
                        }
                        Elements imgList = contentElement.select("IMG");
                        for (Element img : imgList) {
                            String src = img.attr("src");
                            if (src.startsWith("file://")) {
                                img.remove();
                                continue;
                            }
                            if (src.contains("data:image")) {
                                continue;
                            }
                            if (src.length() > 10 && src.indexOf("http") != 0) {
                                if (src.indexOf("../") == 0) {
                                    src = src.replace("../", "");
                                    src = baseUrl + "/" + src;
                                    img.attr("src", src);
                                } else if (src.indexOf("./") == 0) {
                                    src = url.substring(0, url.lastIndexOf("/")) + src.substring(src.lastIndexOf("./") + 1);
                                    img.attr("src", src);
                                } else if (src.startsWith("//")) {
                                    src = baseUrl.substring(0, baseUrl.indexOf(":") + 1) + src;
                                    img.attr("src", src);
                                } else if (src.indexOf("/") == 0) {
                                    src = baseUrl + src;
                                    img.attr("src", src);
                                } else {
                                    src = url.substring(0, url.lastIndexOf("/") + 1) + src;
                                    img.attr("src", src);
                                }
                            }
                        }
                        title = dataJSON.getString("candTitle");
                        contentElement.select("script").remove();
                        contentElement.select("style").remove();
                        JSONArray filesProving = dataJSON.getJSONArray("filesProving");
                        if (filesProving != null) {
                            JSONArray dataArr = JSONObject.parseObject(getContent("https://ebs.hbncp.com.cn/ebsapi/filesengine/oss/fileinfobyids/list", filesProving.toString())).getJSONArray("data");
                            for (int i = 0; i < dataArr.size(); i++) {
                                JSONObject fileJSON = dataArr.getJSONObject(i);
                                String proFileName = fileJSON.getString("proFileName");
                                String filePath = fileJSON.getString("objectPath");
                                contentElement.append("<div>附件下载：<a href='" + filePath + "'>" + proFileName + "</a></div>");
                            }

                        }
                        content = contentElement.outerHtml();
                    } else if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip") || url.contains(".xls")) {
                        content = "<div>附件下载：<a href='" + url + "'>" + branch.getTitle() + "</a></div>";
                        detailHtml = Jsoup.parse(content).toString();
                    }
                    RecordVO recordVO = new RecordVO();
                    recordVO.setId(branch.getId());
                    recordVO.setListTitle(branch.getTitle());
                    recordVO.setTitle(title);
                    recordVO.setDetailLink(branch.getDetailLink());
                    recordVO.setDetailHtml(detailHtml);
                    recordVO.setDdid(SpecialUtil.stringMd5(detailHtml));
                    recordVO.setDate(date);
                    recordVO.setContent(content.replaceAll("\\ufeff|\\u2002|\\u200b|\\u2003", ""));
                    dataStorage(serviceContext, recordVO, branch.getType());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }


    public String getContent(String path) {
        String result = "";
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = getHttpClient(true, false);
            HttpGet httpGet = new HttpGet(path);
            httpGet.addHeader("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:108.0) Gecko/20100101 Firefox/108.0");
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(20 * 1000)
                    .setSocketTimeout(30 * 1000).setRedirectsEnabled(false).build();
            httpGet.setConfig(requestConfig);
            response = client.execute(httpGet);
            response.addHeader("Connection", "close");
            if (response.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                if (client != null) {
                    client.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public String getContent(String path, String json) {
        String result = "";
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = getHttpClient(true, false);
            HttpPost httpPost = new HttpPost(path);
            httpPost.addHeader("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:108.0) Gecko/20100101 Firefox/108.0");
            httpPost.addHeader("content-type", "application/json");
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(20 * 1000)
                    .setSocketTimeout(30 * 1000).setRedirectsEnabled(false).build();
            httpPost.setConfig(requestConfig);
            StringEntity params = new StringEntity(json);
            httpPost.setEntity(params);
            response = client.execute(httpPost);
            response.addHeader("Connection", "close");
            if (response.getStatusLine().getStatusCode() == 200) {
                result = EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                if (client != null) {
                    client.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

}
