package com.bidizhaobiao.data.Crawl.service.impl.DS_05887;

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
 * @Author: 史炜立
 * @DateTime: 2021-02-1
 * @Description: 原网站：http://gjj.hangzhou.gov.cn/col/col1229287673/index.html
 * 主页：http://gjj.hangzhou.gov.cn/
 */
@Service
public class DS_05887_ZhongbXxService extends SpiderService implements PageProcessor {
    public Spider spider = null;

    public String listUrl = "http://gjj.hangzhou.gov.cn/module/jpage/dataproxy.jsp?startrecord=1&endrecord=100&perpage=100&col=1&appid=1&webid=3149&path=%2F&columnid=1229287673&sourceContentType=1&unitid=6323904&webname=%E6%9D%AD%E5%B7%9E%E4%BD%8F%E6%88%BF%E5%85%AC%E7%A7%AF%E9%87%91%E7%BD%91&permissiontype=0";
    public String baseUrl = "http://gjj.hangzhou.gov.cn";
    public Pattern datePat = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");
    public Pattern pagePat = Pattern.compile("(?<=共).*?(?=页)");
    // 网站编号
    public String sourceNum = "05887";
    // 网站名称
    public String sourceName = "杭州住房公积金网";
    // 设置地区
    public String area = "华东";
    // 设置省份
    public String province = "浙江";
    // 设置城市
    public String city = "杭州市";
    // 设置县
    public String district;
    // 设置县
    public String createBy = "史炜立";
    // 信息源
    public String infoSource = "政府采购";

    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);
    // 是否需要入广联达
    public boolean isNeedInsertGonggxinxi = false;

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
            Thread.sleep(500);
            String detailHtml = page.getHtml().toString();
            Document doc = Jsoup.parse(detailHtml);
            List<BranchNew> detailList = new ArrayList<BranchNew>();
            if (url.contains("dataproxy.jsp")) {
                String Context = page.getRawText();
                if (Context != null) {
                    Context = Context.replace("&nbsp;", " ")
                            .replace("&amp;", "&")
                            .replace("<![CDATA[", "")
                            .replace("]]>", "")
                            .replace("&lt;\\/", "<")
                            .replace("&gt;", ">")
                            .replace("\\/", "/")
                            .replace("\\\"", "\"")
                            .replace("\\t", "");
                }
                doc = Jsoup.parse(Context);
                if (doc != null) {
                    Elements listElement = doc.select("record");//详情列表
                    if (listElement.size() > 0) {
                        for (Element element : listElement) {
                            Element a = element.select("a").first();
                            String href = a.attr("href");//链接
                            String id = href.substring(href.indexOf(".cn/") + 3 , href.length());
                            String title = "";
                            if (a.hasAttr("title")) {//title
                                title = a.attr("title").trim();
                            } else {
                                title = element.select("a").text().trim().replace("...", "");
                            }
                            String date = element.outerHtml();
                            Matcher m = datePat.matcher(date);
                            if (m.find()) {
                                String month = m.group(3).length() == 2 ? m.group(3) : "0" + m.group(3);
                                String day = m.group(5).length() == 2 ? m.group(5) : "0" + m.group(5);
                                date = m.group(1) + "-" + month + "-" + day;
                            }
                            if(!href.contains(baseUrl)){
                                continue;
                            }
                            if (!CheckProclamationUtil.isProclamationValuable(title)) {//招标
                                continue;
                            }
                            BranchNew branch = new BranchNew();
                            branch.setId(id);
                            branch.setLink(href);
                            branch.setDate(date);
                            branch.setTitle(title);
                            detailList.add(branch);
                        }
                    }
                    List<BranchNew> branchNewList = checkData(detailList, serviceContext);
                    for (BranchNew branch : branchNewList) {
                        map.put(branch.getLink(), branch);
                        page.addTargetRequest(branch.getLink());
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
            } else {
                BranchNew branch = map.get(url);
                if (branch != null) {
                    map.remove(url);
                    String title = branch.getTitle().replace("...", "");
                    String date = branch.getDate();
                    String content = "";
                    Element contentElement = doc.select("div#zoom").first();//详情页content
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
                                if (href.indexOf("/") == 0) {
                                    href = baseUrl + href;
                                    a.attr("href", href);
                                } else if (href.indexOf("../") == 0) {
                                    href = href.replace("../", "");
                                    href = baseUrl + "/" + href;
                                    a.attr("href", href);
                                } else if (href.indexOf("./") == 0) {
                                    href = url.substring(0, url.lastIndexOf("/") + 1) + href.substring(2);
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
                            if (href.length() > 10 && href.indexOf("http") != 0) {
                                if (href.indexOf("../") == 0) {
                                    href = href.replace("../", "");
                                    href = baseUrl + "/" + href;
                                    img.attr("src", href);
                                } else if (href.indexOf("./") == 0) {
                                    href = url.substring(0, url.lastIndexOf("/") + 1) + href.substring(2);
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
                        Element titleElement = doc.select("div.ptitle").first();//详情title
                        if (titleElement != null) {
                            title = titleElement.text().trim();
                        }
                        contentElement.select("script").remove();
                        contentElement.select("style").remove();
                        content = title + contentElement.outerHtml();
                    } else if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip") || url.contains(".xls")) {
                        content = "<div>附件下载：<a href='" + url + "'>" + branch.getTitle() + "</a></div>";
                        detailHtml = Jsoup.parse(content).toString();
                    }
                    RecordVO recordVO = new RecordVO();
                    recordVO.setId(branch.getId());
                    recordVO.setListTitle(title);
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



