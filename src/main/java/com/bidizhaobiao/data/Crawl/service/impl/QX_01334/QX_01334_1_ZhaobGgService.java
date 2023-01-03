package com.bidizhaobiao.data.Crawl.service.impl.QX_01334;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
import com.bidizhaobiao.data.Crawl.utils.CheckProclamationUtil;
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
 * 程序员：郭建婷  日期：2022-03-15
 * 原网站：http://www.dongtou.gov.cn/col/col1229482967/index.html
 * 主页：http://www.dongtou.gov.cn
 **/
@Service
public class QX_01334_1_ZhaobGgService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://www.dongtou.gov.cn/module/jpage/dataproxy.jsp?startrecord=1&endrecord=60&perpage=20";
    public String homeUrl = "http://www.dongtou.gov.cn";
    public int maxNum = 120;

    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(3).setTimeOut(30000).setSleepTime(20);
    // 网站编号
    public String sourceNum = "01334-1";
    // 网站名称
    public String sourceName = "洞头区政府";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华东";
    // 设置省份
    public String province = "浙江";
    // 设置城市
    public String city = "温州市";
    // 设置县
    public String district = "洞头区";
    // 设置CreateBy
    public String createBy = "潘嘉明";
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
                .setDownloader(new MyDownloader(serviceContext, true, listUrl));
        Request request = getListRequest(1);
        spider.addRequest(request);
        serviceContext.setSpider(spider);
        spider.run();
        // 爬虫状态监控部分
        saveCrawlResult(serviceContext);
    }

    public Request getListRequest(int pageNum) {
        int start = (pageNum - 1) * 60 + 1;
        int end = pageNum * 60;
        String path = listUrl.replace("startrecord=1", "startrecord=" + start).replace("endrecord=60", "endrecord=" + end);
        Request request = new Request(path);
        try {
            request.setMethod(HttpConstant.Method.POST);
            request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("col", "1");
            params.put("appid", "1");
            params.put("webid", "1832");
            params.put("path", "/");
            params.put("columnid", "1229482967");
            params.put("sourceContentType", "1");
            params.put("unitid", "6749220");
            params.put("webname", "洞头区政府");
            params.put("permissiontype", "0");
            request.setRequestBody(HttpRequestBody.form(params, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return request;
    }

    public void process(Page page) {
        String url = page.getUrl().toString();
        try {
            List<BranchNew> detailList = new ArrayList<BranchNew>();
            Thread.sleep(2000);
            // 判断是否是翻页连接
            if (url.contains("startrecord=")) {
                String keys = "征集、购买、咨询、询标、交易、机构、需求、废旧、废置、处置、报废、供应商、承销商、服务商、调研、优选、择选、择优、选取、公选、选定、摇选、摇号、摇珠、抽选、定选、定点、招标、采购、询价、询比、竞标、竞价、竞谈、竞拍、竞卖、竞买、竞投、竞租、比选、比价、竞争性、谈判、磋商、投标、邀标、议标、议价、单一来源、标段、明标、明投、出让、转让、拍卖、招租、出租、预审、发包、承包、分包、外包、开标、遴选、答疑、补遗、澄清、延期、挂牌、变更、预公告、监理、改造工程、报价、小额、零星、自采、商谈";
                String[] rules = keys.split("、");
                Document document = Jsoup.parse(page.getRawText().replace("&nbsp;", "").replace("&amp;", "&").replace("&ensp;", "").replace("<![CDATA[", "").replace("]]>", "").replace("&lt;", "<").replace("&gt;", ">"));
                Elements records = document.select("record:has(a)");
                if (records.size() > 0) {
                    for (Element record : records) {
                        Element a = record.select("a").first();
                        String title = "";
                        if (a.hasAttr("title")) {
                            title = a.attr("title").trim();
                            if (title.equals("")) {
                                title = a.text().trim();
                            }
                        } else {
                            title = a.text().trim();
                        }
                        title = title.replace("...", "").replace(" ", "").trim();
                        if (!CheckProclamationUtil.isProclamationValuable(title, rules)) {
                            continue;
                        }
                        String href = a.attr("href");
                        String id = href;
                        String link = homeUrl + id;
                        String detailLink = link;
                        String date = record.select("span").last().text().trim();
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
                    logger.info("请重试...");
                }
                if (serviceContext.getPageNum() == 1 && document.select("totalrecord").first() != null) {
                    String pageCount = document.select("totalrecord").text().trim();
                    int total = Integer.valueOf(pageCount);
                    int maxPage = total / 60;
                    if (total % 60 != 0) {
                        maxPage++;
                    }
                    serviceContext.setMaxPage(maxPage);
                }
                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    int nowPage = serviceContext.getPageNum();
                    Request request = getListRequest(nowPage);
                    page.addTargetRequest(request);
                }
            } else {
                // 列表页请求
                BranchNew branchNew = map.get(url);
                if (branchNew == null) {
                    return;
                }
                String title = branchNew.getTitle();
                String id = branchNew.getId();
                serviceContext.setCurrentRecord(id);
                String date = branchNew.getDate();
                String detailLink = branchNew.getDetailLink();
                String content = "";
                Document document = Jsoup.parse(page.getRawText().replace("&nbsp;", "").replace("&amp;", "&").replace("&ensp;", "").replace("<![CDATA[", "").replace("]]>", "").replace("&lt;", "<").replace("&gt;", ">"));
                Element contentE = document.select("div.main_show_cont").first();
                if (contentE != null) {
                    saveFile(contentE, url, date);
                    Elements elements = contentE.getAllElements();
                    for (Element element : elements) {
                        element.removeAttr("style");
                    }
                    contentE.select("*[style~=^.*display\\s*:\\s*none\\s*(;\\s*[0-9A-Za-z]+|;\\s*)?$]").remove();
                    contentE.select("script").remove();
                    contentE.select("style").remove();
                    content = "<div>" + title + "</div>" + contentE.outerHtml();
                } else if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip") || url.contains(".xls") || url.contains(".xlsx")) {
                    content = "<div>附件下载：<a href='" + url + "'>" + branchNew.getTitle() + "</a></div>";
                }
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

    public void saveFile(Element contentElement, String url, String date) {
        Elements aList = contentElement.select("a");
        for (Element a : aList) {
            String href = a.attr("href");
            a.attr("rel", "noreferrer");
            if (href.startsWith("mailto")) {
                continue;
            }
            if (a.hasAttr("data-download")) {
                href = a.attr("data-download");
                a.attr("href", href);
                a.removeAttr("data-download");
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
                    href = homeUrl.substring(0, homeUrl.indexOf(":") + 1) + href;
                    a.attr("href", href);
                } else if (href.indexOf("../") == 0) {
                    href = href.replace("../", "");
                    href = homeUrl + "/" + href;
                    a.attr("href", href);
                } else if (href.startsWith("/")) {
                    href = homeUrl + href;
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
                    src = homeUrl + "/" + src;
                    img.attr("src", src);
                } else if (src.indexOf("./") == 0) {
                    src = url.substring(0, url.lastIndexOf("/")) + src.substring(src.lastIndexOf("./") + 1);
                    img.attr("src", src);
                } else if (src.startsWith("//")) {
                    src = homeUrl.substring(0, homeUrl.indexOf(":") + 1) + src;
                    img.attr("src", src);
                } else if (src.indexOf("/") == 0) {
                    src = homeUrl + src;
                    img.attr("src", src);
                } else {
                    src = url.substring(0, url.lastIndexOf("/") + 1) + src;
                    img.attr("src", src);
                }
            }
        }
    }

}
