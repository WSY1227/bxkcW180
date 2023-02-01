package com.bidizhaobiao.data.Crawl.service.impl.DS_24845;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
import com.bidizhaobiao.data.Crawl.utils.CheckProclamationUtil;
import com.bidizhaobiao.data.Crawl.utils.SpecialUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 程序员：徐文帅 日期：2023-02-01
 * 原网站：http://xzzf.sanya.gov.cn/zhxzjsite/zfxxgk/newxxgk_rigth_list.shtml?channelCode=zfcgxx&cn=%E6%94%BF%E5%BA%9C%E9%87%87%E8%B4%AD
 * 主页：http://xzzf.sanya.gov.cn
 **/
@Service
public class DS_24845_ZhaobGgService extends SpiderService implements PageProcessor {
    public Spider spider = null;

    public String listUrl = "http://xzzf.sanya.gov.cn/u/search/topxxgk/zhxzjsite?PageSize=15&curPage=1&channelCode=zfcgxx&type=&title=&JsonpCallBack=ztzpCallBack&_=1675214925664";
    public String baseUrl = "http://xzzf.sanya.gov.cn";
    public Pattern datePat = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");

    // 网站编号
    public String sourceNum = "24845";
    // 网站名称
    public String sourceName = "三亚市综合行政执法局";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华南";
    // 设置省份
    public String province = "海南";
    // 设置城市
    public String city = "三亚";
    // 设置县
    public String district;
    public String createBy = "徐文帅";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);

    public Site getSite() {
        return this.site;
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
        spider.addRequest(new Request(listUrl));
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
                String json = page.getRawText().replace("ztzpCallBack(", "");
                json = json.substring(0, json.length() - 1);
                JSONArray rows = JSONObject.parseObject(json).getJSONArray("list");
                if (rows.size() > 0) {
                    for (int i = 0; i < rows.size(); i++) {
                        JSONObject row = rows.getJSONObject(i);
                        String title = row.getString("title").trim();
                        if (title.contains("目录")) {
                            continue;
                        }
                        String link = row.getString("url");
                        String id = link.substring(link.lastIndexOf("/") + 1);
                        String detailLink = link;
                        String date = "";
                        Matcher dateMat = datePat.matcher(row.getString("published_time"));
                        if (dateMat.find()) {
                            date = dateMat.group(1);
                            date += dateMat.group(3).length() == 2 ? "-" + dateMat.group(3) : "-0" + dateMat.group(3);
                            date += dateMat.group(5).length() == 2 ? "-" + dateMat.group(5) : "-0" + dateMat.group(5);
                        }
                        if (!CheckProclamationUtil.isProclamationValuable(title, null)) {
                            continue;
                        }
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
                        page.addTargetRequest(new Request(branch.getLink()));
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
            } else {
                BranchNew branch = map.get(url);
                if (branch != null) {
                    map.remove(url);
                    serviceContext.setCurrentRecord(branch.getId());
                    String detailHtml = page.getRawText();
                    Document doc = Jsoup.parse(detailHtml);
                    String title = branch.getTitle().replace("...", "");
                    String date = branch.getDate();
                    String content = "";
                    Element contentElement = doc.select("#news_content").first();
                    if (contentElement != null) {
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
                        Element titleElement = doc.select("div.tit h2").first();
                        if (titleElement != null) {
                            title = titleElement.text().trim();
                        }
                        contentElement.select("script").remove();
                        contentElement.select("style").remove();
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


}
