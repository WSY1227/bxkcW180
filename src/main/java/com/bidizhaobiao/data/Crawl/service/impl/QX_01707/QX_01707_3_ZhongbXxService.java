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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 程序员：杨业深  日期：2021-11-11
 * 原网站：http://ggzy.huangzhou.gov.cn/ceinwz/WebInfo_List.aspx?newsid=0&jsgc=&zfcg=0000010&tdjy=&cqjy=&qtjy=&PubDateSort=1&ShowPre=1&CbsZgys=0&zbfs=&qxxx=0&showqxname=0&NewsShowPre=1&wsjj=0&showCgr=0&ShowOverDate=0&showdate=1&FromUrl=nourl
 * 主页：http://ggzy.huangzhou.gov.cn
 **/

@Service("QX_01707_3_ZhongbXxService")
public class QX_01707_3_ZhongbXxService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://ggzy.huangzhou.gov.cn/ceinwz/WebInfo_List.aspx?newsid=0&jsgc=&zfcg=0000010&tdjy=&cqjy=&qtjy=&PubDateSort=1&ShowPre=1&CbsZgys=0&zbfs=&qxxx=0&showqxname=0&NewsShowPre=1&wsjj=0&showCgr=0&ShowOverDate=0&showdate=1&FromUrl=nourl&page=1";

    public String baseUrl = "http://ggzy.huangzhou.gov.cn";

    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(3).setTimeOut(30000).setSleepTime(20);
    // 网站编号
    public String sourceNum = "01707-3";
    // 网站名称
    public String sourceName = "黄州区公共资源交易网";
    // 信息源
    public String infoSource = "政府采购";
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
                        title = title.replace("[查看结果]", "").replace("...", "").replace(" ", "").trim();
                        String href = a.attr("href");
                        if (!href.contains("hyzbjggszfcg")) {
                            continue;
                        }
                        String id = href.substring(href.indexOf("=") + 1).trim();
                        String link = "http://ggzy.huangzhou.gov.cn/ceinwz/hyzq/hyzbjggszfcg.aspx?sgzbbm=" + id;
                        String detailLink = link;
                        String date = tr.select("span").last().previousElementSibling().text().replace("发布", "").trim();
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
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String path = "http://ggzy.huangzhou.gov.cn/temphtml";
                Document xml = Jsoup.parse(page.getRawText());
                xml.outputSettings().prettyPrint(true);

                Element conTag = xml.select("table[class=yygl]").last();

                if (xml.select("iframe[id=frmBestwordHtml]").first() != null) {
                    String appendUrl = xml.select("iframe[id=frmBestwordHtml]").first().attr("src").trim();
                    if (!appendUrl.startsWith("http")) {
                        appendUrl = "http://ggzy.huangzhou.gov.cn" + appendUrl;
                    }
                    Document appendDoc = Jsoup.connect(appendUrl).get();
                    if (appendDoc.select("script").first() == null) {
                        return;
                    }
                    String appendStr = appendDoc.select("script").first().data();
                    if (!appendStr.contains("location.href")) {
                        return;
                    }
                    appendStr = appendStr.substring(appendStr.indexOf("'") + 1, appendStr.lastIndexOf("'"));
                    appendStr = appendStr.replace("..", "");
                    if (appendStr.startsWith("/")) {
                        appendStr = "http://ggzy.huangzhou.gov.cn" + appendStr;
                    }
                    appendDoc = Jsoup.connect(appendStr).get();
                    appendStr = appendDoc.body().toString();
                    conTag.append(appendStr);
                }
                conTag.select("*[style~=^.*display\\s*:\\s*none\\s*(;\\s*[0-9A-Za-z]+|;\\s*)?$]").remove();
                Elements scripts = conTag.select("script");
                for (Element script : scripts) {
                    script.remove();
                }
                Elements metas = conTag.select("meta");
                for (Element metae : metas) {
                    metae.remove();
                }

                // 补全附件的链接
                Elements as = conTag.select("a");
                for (Element ae : as) {
                    String href = ae.attr("href");

                    if (!"#".equals(href) && !href.contains("http") && href.length() > 0 && !href.contains("HTTP")) {
                        if (href.startsWith("/")) {
                            href = path + href;
                        } else {
                            href = url.substring(0, url.lastIndexOf("/")) + "/" + href;
                        }
                        ae.attr("href", href);
                    }
                }

                Elements imgs = conTag.select("img");
                for (Element imge : imgs) {
                    String src = imge.attr("src");
                    if (!src.contains("http") && !src.contains("HTTP") && !src.startsWith("data")) {
                        if (src.startsWith("/")) {
                            src = path + src;
                        } else {
                            src = path + "/" + src;
                        }
                        imge.attr("src", src);
                    }
                }

                conTag.select("iframe").remove();
                content = conTag.outerHtml();
                content = content.replace("word文档修改中，请勿退出或刷新本页面", "");
                String str = conTag.text().replaceAll("[\u00a0\u1680\u180e\u2000-\u200a\u2028\u2029\u202f\u205f\u3000\ufeff\\s+]+", "");
                str = str.replaceAll("[.|/|年|月]", "-");
                Matcher m = p.matcher(str);
                if (m.find()) {
                    date = sdf.format(sdf.parse(m.group()));
                } else {
                    date = sdf.format(new Date());
                }
                if (content.replace(" ", "").equals("")) {
                    return;
                }
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
        map.put("__EVENTTARGET", "ctl00$ContentPlaceHolder1$myGV$ctl23$LinkButtonNextPage");
        map.put("__EVENTARGUMENT", "");
        map.put("__LASTFOCUS", "");
        map.put("__VIEWSTATE", __VIEWSTATE);
        map.put("__VIEWSTATEENCRYPTED", "");
        map.put("__EVENTVALIDATION", __EVENTVALIDATION);
        map.put("ctl00$ContentPlaceHolder1$txtGcmc", "");
        map.put("ctl00$ContentPlaceHolder1$DDLPageSize", "20");
        request.setRequestBody(HttpRequestBody.form(map, "UTF-8"));
        return request;
    }


}
