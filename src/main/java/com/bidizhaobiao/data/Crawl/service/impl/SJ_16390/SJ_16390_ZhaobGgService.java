package com.bidizhaobiao.data.Crawl.service.impl.SJ_16390;

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
 * 程序员： 陈广艺
 * 日期：2021-06-10
 * 原网站：http://ynrf.yn.gov.cn/html/tongzhigonggao/index.html
 * 主页：http://ynrf.yn.gov.cn/
 **/

@Service("SJ_16390_ZhaobGgService")
public class SJ_16390_ZhaobGgService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://ynrf.yn.gov.cn/html/tongzhigonggao/index.html";
    public String baseUrl = "http://ynrf.yn.gov.cn/";
    public Pattern dateRex = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");

    // 网站编号
    public String sourceNum = "16390";
    // 网站名称
    public String sourceName = "云南省人民防空办公室";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "西南";
    // 设置省份
    public String province = "云南";
    // 设置城市
    public String city;
    // 设置县
    public String district;
    public String createBy = "陈广艺";
    // 站源类型
    public String taskType = "";
    // 站源名称
    public String taskName = "";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);
    // 是否需要入广联达
    public boolean isNeedInsertGonggxinxi = false;
    // 信息源

    public Site getSite() {
        return this.site
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.72 Safari/537.36");
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
        String detailHtml = page.getHtml().toString();
        Document doc = Jsoup.parse(detailHtml);
        try {
            List<BranchNew> detailList = new ArrayList<BranchNew>();
            Thread.sleep(1000);
            if (url.contains("/tongzhigonggao/")) {
                Elements listElement = doc.select("div.lc li");
                if (listElement.size() > 0) {
                    for (Element element : listElement) {
                        Element a = element.select("a").first();
                        String link = baseUrl + a.attr("href").trim();
                        String id = link.substring(link.lastIndexOf("/") + 1);
                        String title = a.text().trim();
                        String word = "招标、采购、询价、询比、竞标、竞价、竞谈、竞拍、竞卖、竞买、竞投、竞租、比选、比价、竞争性、谈判、磋商、投标、邀标、议标、议价、单一来源、遴选、标段、明标、明投、出让、转让、拍卖、招租、预审、发包、开标、答疑、补遗、澄清、挂牌";
                        String[] split = word.split("、");
                        if (!CheckProclamationUtil.isProclamationValuable(title, split)) {
                        	continue;
                        }
                        String date = "";
                        Matcher dateMat = dateRex.matcher(element.select("span.fr").text());
                        if (dateMat.find()) {
                            date = dateMat.group(1);
                            date += dateMat.group(3).length() == 2 ? "-" + dateMat.group(3) : "-0" + dateMat.group(3);
                            date += dateMat.group(5).length() == 2 ? "-" + dateMat.group(5) : "-0" + dateMat.group(5);
                        }
                        BranchNew branch = new BranchNew();
                        branch.setId(id);
                        serviceContext.setCurrentRecord(branch.getId());
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
                //翻页
                if (listElement.size() == 15 && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    page.addTargetRequest(listUrl.replace("/tongzhigonggao/index", "/tongzhigonggao/"+ serviceContext.getPageNum()));
                }
            } else {
                BranchNew branch = map.get(url);
                if (branch != null) {
                    map.remove(url);
                    serviceContext.setCurrentRecord(branch.getId());
                    String title = branch.getTitle().replace("...", "");
                    String date = branch.getDate();
                    String content = "";
                    Elements aList = doc.select("a");
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
                                href = url.substring(0, url.lastIndexOf("/")) + href.substring(href.lastIndexOf("./") + 1);
                                a.attr("href", href);
                            } else {
                                href = url.substring(0, url.lastIndexOf("/") + 1) + href;
                                a.attr("href", href);
                            }
                        }
                    }
                    Elements imgList = doc.select("IMG");
                    for (Element img : imgList) {
                        String href = img.attr("src");
                        if (href.contains("dkxx.png")) {
                            img.remove();
                            continue;
                        }
                        if (href.length() > 10 && href.indexOf("http") != 0) {
                            if (href.indexOf("../") == 0) {
                                href = href.replace("../", "");
                                href = baseUrl + "/" + href;
                                img.attr("src", href);
                            } else if (href.indexOf("./") == 0) {
                                href = url.substring(0, url.lastIndexOf("/")) + href.substring(href.lastIndexOf("./") + 1);
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
                    Element contentElement = doc.select("div.lc").first();
                    if (contentElement != null) {
                        Element titleElement = contentElement.select("h3").first();
                        if (titleElement != null) {
                            title = titleElement.text().trim();
                        }

                        contentElement.select("div.space01").remove();
                        contentElement.select("div.datetime").remove();
                        contentElement.select("script").remove();
                        contentElement.select("style").remove();
                        content = contentElement.outerHtml();
                        content = content.replaceAll("\\ufeff|\\u2002|\\u200b|\\u2003|\\uff1f", "");
                    } else if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip") || url.contains(".xls")) {
                        content = "<div>附件下载：<a href='" + url + "'>" + title + "</a></div>";
                        detailHtml = Jsoup.parse(content).toString();
                    } else if (url.contains(".jpg") || url.contains(".jpeg") || url.contains(".png")) {
                        content = "<div>查看图片：<img src='" + url + "'></img></div>";
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
