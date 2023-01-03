package com.bidizhaobiao.data.Crawl.service.impl.QX_01707;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 程序员：杨业深  日期：2021-11-11
 * 原网站：http://ggzy.huangzhou.gov.cn/ceinwz/WebInfo_List.aspx?newsid=8002&jsgc=0000000&zfcg=&tdjy=&cqjy=&qtjy=&PubDateSort=0&ShowPre=0&CbsZgys=0&zbfs=&qxxx=0&showqxname=0&NewsShowPre=1&wsjj=0&showCgr=0&ShowOverDate=0&showdate=1&FromUrl=qtggzygs
 * 主页：http://ggzy.huangzhou.gov.cn
 **/

@Service("QX_01707_2_ZhongbXxService")
public class QX_01707_2_ZhongbXxService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://ggzy.huangzhou.gov.cn/ceinwz/WebInfo_List.aspx?newsid=8002&jsgc=0000000&zfcg=&tdjy=&cqjy=&qtjy=&PubDateSort=0&ShowPre=0&CbsZgys=0&zbfs=&qxxx=0&showqxname=0&NewsShowPre=1&wsjj=0&showCgr=0&ShowOverDate=0&showdate=1&FromUrl=qtggzygs&page=1";

    public String baseUrl = "http://ggzy.huangzhou.gov.cn";

    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(3).setTimeOut(30000).setSleepTime(20);
    // 网站编号
    public String sourceNum = "01707-2";
    // 网站名称
    public String sourceName = "黄州区公共资源交易网";
    // 信息源
    public String infoSource = "工程建设";
    // 设置地区
    public String area = "华中";
    // 设置省份
    public String province = "湖北";
    // 设置城市
    public String city = "黄冈市";
    // 设置县
    public String district = "黄州区";
    // 设置CreateBy
    public String createBy = "杨业深";
    //附件
    // 

    public Pattern p = Pattern.compile("(\\d{4})(年|/|-)(\\d{1,2})(月|/|-)(\\d{1,2})");

    public Pattern p_p = Pattern.compile("view\\('(.*?)','(.*?)','(.*?)'\\)");

    public Site getSite() {
        return this.site.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
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
        Request request = new Request(listUrl);
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
                Document document = Jsoup.parse(page.getRawText().replace("&nbsp;", "").replace("&amp;", "&").replace("&ensp;", "").replace("<![CDATA[", "").replace("]]>", "").replace("&lt;", "<").replace("&gt;", ">"));
                if (serviceContext.getPageNum() == 1 && document.select("span#ctl00_ContentPlaceHolder1_myGV_ctl23_LabelPageCount").first() != null) {
                    String pageCount = document.select("span#ctl00_ContentPlaceHolder1_myGV_ctl23_LabelPageCount").first().text().trim();
                    int maxPage = Integer.valueOf(pageCount);
                    serviceContext.setMaxPage(maxPage);
                }
                Elements trs = document.select("table.myGVClass  > tbody > tr:has(a)");
                if (trs.size() > 1) {
                    for (int i = 0; i < trs.size() - 1; i++) {
                        Element tr = trs.get(i);
                        Element a = tr.select("a").first();
                        String title = "";
                        if (a.hasAttr("title")) {
                            title = a.attr("title").trim();
                            if (title.equals("")) {
                                title = a.text().trim();
                            }
                        } else {
                            title = a.text().trim();
                        }
                        title = title.replace("[查看公告]", "").replace("...", "").replace(" ", "").trim();
                        String href = a.attr("href");
                        if (!href.contains("admin_show")) {
                            continue;
                        }
                        String id = href.substring(href.indexOf("?") + 1);
                        String link = "http://ggzy.huangzhou.gov.cn/ceinwz/admin_show.aspx?" + id;
                        String detailLink = link;
                        String date = tr.select("span").last().previousElementSibling().text().trim().replace("发布", "").trim();
                        dealWithNullTitleOrNullId(serviceContext, title, id);
                        BranchNew branch = new BranchNew();
                        branch.setTitle(title);
                        branch.setId(id);
                        serviceContext.setCurrentRecord(id);
                        branch.setDetailLink(detailLink);
                        branch.setLink(link);
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
                    String __VIEWSTATE = document.getElementById("__VIEWSTATE").attr("value");
                    String __EVENTVALIDATION = document.getElementById("__EVENTVALIDATION").attr("value");
                    Request request = getRequest(listUrl.replace("page=1", "page=" + serviceContext.getPageNum()), __VIEWSTATE, __EVENTVALIDATION);
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
                Element contentE = document.select("span#ctl00_ContentPlaceHolder1_BodyLabel").first();
                Elements elements = contentE.getAllElements();
                for (Element element : elements) {
                    element.removeAttr("style");
                }
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
                            } else {
                                href = homeUrl + "/" + href;
                                a.attr("href", href);
                            }
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
                content = "<div>" + title + "</div>" + contentE.outerHtml() + "<div>更多资讯报价请点击<a href=\"" + url + "\" rel=\"noreferrer\">" + url + "</a></div>";
                RecordVO recordVO = new RecordVO();
                recordVO.setId(id);
                recordVO.setDate(date);
                recordVO.setContent(content.replace("&amp;", "&"));
                recordVO.setTitle(title.replaceAll("\\<.*?\\>", ""));//详情页标题
                recordVO.setDetailLink(detailLink);//详情页链接
                dataStorage(serviceContext, recordVO, branchNew.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }

    public Request getRequest(String link, String __VIEWSTATE, String __EVENTVALIDATION) {
        Request request = new Request(link);
        request.setMethod(HttpConstant.Method.POST);
        Map<String, Object> map = new HashMap<>();
        map.put("ctl00_myTreeView_ExpandState", "n");
        map.put("ctl00_myTreeView_SelectedNode", "");
        map.put("__EVENTTARGET", "ctl00$ContentPlaceHolder1$BestNewsListALL$myGV$ctl13$lbnNext");
        map.put("__EVENTARGUMENT", "");
        map.put("ctl00_myTreeView_PopulateLog", "");
        map.put("__VIEWSTATE", __VIEWSTATE);
        map.put("__VIEWSTATEENCRYPTED", "");
        map.put("__EVENTVALIDATION", __EVENTVALIDATION);
        map.put("ctl00$ContentPlaceHolder1$txtGcmc", "");
        map.put("ctl00$ContentPlaceHolder1$DDLPageSize", "20");
        request.setRequestBody(HttpRequestBody.form(map, "UTF-8"));
        return request;
    }


}
