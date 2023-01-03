package com.bidizhaobiao.data.Crawl.service.impl.Y03074;

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
 * 程序员：陆浩锐 日期：2020-11-18
 * 原网站：http://www.jxxrmyy.cn/list.php?fid=72
 * 主页：http://www.jxxrmyy.cn
 **/

@Service("Y03074_ZhaobGgService")
public class Y03074_ZhaobGgService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://www.jxxrmyy.cn/list.php?fid=72&page=1";
    public String baseUrl = "http://www.jxxrmyy.cn";
    public Pattern datePat = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");

    // 网站编号
    public String sourceNum = "Y03074";
    // 网站名称
    public String sourceName = "靖西市人民医院";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华南";
    // 设置省份
    public String province = "广西";
    // 设置城市
    public String city = "百色市";
    // 设置县
    public String district = "靖西市";
    public String createBy = "陆浩锐";
    // 站源类型
    public String taskType = "";
    // 站源名称
    public String taskName = "";
    // 是否需要入广联达
    public boolean isNeedInsertGonggxinxi = false;
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);
    // 信息源

    public Site getSite() {
        return this.site
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
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
            if (url.contains("page")) {
                Document doc = Jsoup.parse(page.getRawText());
                Elements listElement = doc.select("table#list_article table:has(a)");
                if (listElement.size() > 0) {
                    for (Element element : listElement) {
                        Element a = element.select("a").first();
                        String link = a.attr("href");
                        String id = link.substring(link.lastIndexOf("?") + 1);
                        link = url.substring(0, url.lastIndexOf("/") + 1) + link;
                        String date = "";
                        Matcher dateMat = datePat.matcher(element.text());
                        if (dateMat.find()) {
                            date = dateMat.group(1);
                            date += dateMat.group(3).length() == 2 ? "-" + dateMat.group(3) : "-0" + dateMat.group(3);
                            date += dateMat.group(5).length() == 2 ? "-" + dateMat.group(5) : "-0" + dateMat.group(5);
                        }
                        String title = a.attr("title").trim();
                        if (title.length() < 2) title = a.text().trim();
                        if (!CheckProclamationUtil.isProclamationValuable(title, null)) {
                            continue;
                        }
                        BranchNew branch = new BranchNew();
                        branch.setId(id);
                        branch.setLink(link);
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
                Element nextPage = doc.select("a:contains(下一页)").first();
                if (nextPage != null && serviceContext.isNeedCrawl()) {
                    String href = nextPage.attr("href").trim();
                    if (url.contains("page") && !url.contains(href)) {
                        href = url.substring(0, url.lastIndexOf("/") + 1) + href;
                        serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                        page.addTargetRequest(href);
                    }
                }
            } else {
                BranchNew branch = map.get(url);
                if (branch != null) {
                    map.remove(url);
                    String detailHtml = page.getRawText();
                    Document doc = Jsoup.parse(detailHtml);
                    String title = branch.getTitle().replace("...", "");
                    String date = branch.getDate();
                    String content = "";
                    Element contentElement = doc.select("#viewarticle td.middle").first();
                    if (contentElement != null) {
                        Elements aList = contentElement.select("a");
                        for (Element a : aList) {
                            String href = a.attr("href");
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
                                    href = url.substring(0, url.lastIndexOf("/") + 1) + href.substring(href.lastIndexOf("./") + 1);
                                    a.attr("href", href);
                                } else {
                                    href = url.substring(0, url.lastIndexOf("/") + 1) + href;
                                    a.attr("href", href);
                                }
                            }
                        }
                        Elements imgList = contentElement.select("IMG");
                        for (Element img : imgList) {
                            String href = img.attr("src");
                            if (href.startsWith("file://")) {
                                img.remove();
                                continue;
                            }
                            if (href.length() > 10 && href.indexOf("http") != 0) {
                                if (href.indexOf("../") == 0) {
                                    href = href.replace("../", "");
                                    href = baseUrl + "/" + href;
                                    img.attr("src", href);
                                } else if (href.indexOf("./") == 0) {
                                    href = url.substring(0, url.lastIndexOf("/") + 1) + href.substring(href.lastIndexOf("./") + 1);
                                    img.attr("src", href);
                                } else if (href.startsWith("//www")) {
                                    href = baseUrl.substring(0, baseUrl.indexOf(":") + 1) + href;
                                    img.attr("src", href);
                                } else if (href.indexOf("/") == 0) {
                                    href = baseUrl + href;
                                    img.attr("src", href);
                                } else {
                                    href = url.substring(0, url.lastIndexOf("/") + 1) + href;
                                    img.attr("src", href);
                                }
                            }
                        }
                        Element titleElement = contentElement.select(" td[style*=font-weight:bold;font-size:22px;]").first();
                        if (titleElement != null) {
                            title = titleElement.text().trim();
                        }
                        contentElement.select("table[width=100%]:has(font#hits)").remove();
                        contentElement.select("table[style=TABLE-LAYOUT: fixed;WORD-WRAP: break-word]").remove();
                        contentElement.select("script").remove();
                        contentElement.select("style").remove();
                        content = contentElement.outerHtml();
                    } else {
                        contentElement = doc.select("table.content").first();
                        if (contentElement != null) {
                            Element titleElement = doc.select(" div.main_title").first();
                            if (titleElement != null) {
                                title = titleElement.text().trim();
                            }
                            contentElement.select("script").remove();
                            contentElement.select("style").remove();
                            content = title + contentElement.outerHtml();
                        }
                    }
                    if (contentElement == null && doc.select("table#MsgTable:contains(已超过了这个期限)").first() != null) {
                        return;
                    }
                    if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip") || url.contains(".xls")) {
                        content = "<div>附件下载：<a href='" + url + "'>" + branch.getTitle() + "</a></div>";
                        detailHtml = Jsoup.parse(content).toString();
                    }
                    RecordVO recordVO = new RecordVO();
                    recordVO.setId(branch.getId());
                    recordVO.setListTitle(branch.getTitle());
                    recordVO.setTitle(title);
                    recordVO.setDetailLink(url);
                    recordVO.setDetailHtml(detailHtml);
                    recordVO.setDdid(SpecialUtil.stringMd5(detailHtml));
                    recordVO.setDate(date);
                    recordVO.setContent(content);
                    dataStorage(serviceContext, recordVO, branch.getType());
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }

}
