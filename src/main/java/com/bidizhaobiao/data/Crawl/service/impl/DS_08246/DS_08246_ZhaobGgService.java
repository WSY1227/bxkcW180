package com.bidizhaobiao.data.Crawl.service.impl.DS_08246;

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
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.utils.HttpConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 程序员：白嘉全 日期：2019-06-18
 * 原网站：http://ggzy.jz.gov.cn/jyxx/077002/077002001/listMore.html
 * 主页：http://ggzy.jz.gov.cn/
 **/

@Service
public class DS_08246_ZhaobGgService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://ly.1203.org/1090/index.jhtml";
    public String baseUrl = "http://ly.1203.org";
    public Pattern p = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");
    // 网站编号
    public String sourceNum = "08246";
    // 网站名称
    public String sourceName = "龙岩市残疾人联合会";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华东";
    // 设置省份
    public String province = "福建";
    // 设置城市
    public String city = "龙岩市";
    // 设置县
    public String district;
    public String createBy = "白嘉全";
    // 站源类型
    public String taskType = "";
    // 站源名称
    public String taskName = "";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);
    // 是否需要入广联达
    public boolean isNeedInsertGonggxinxi = false;
    // 信息源

    public Site getSite() {
        return this.site.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36");
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
            Thread.sleep(1000);
            if (url.contains("/index")) {
                Document doc = page.getHtml().getDocument();
                Elements list = doc.select("div[class=listr right] ul li:has(a)");
                if (list.size() > 0) {
                    for (int i = 0; i < list.size(); i++) {
                        Element li = list.get(i);
                        Element a = li.select("a").first();
                        String id = a.attr("href");
                        String link = id.replace(":80", "");
                        id = id.substring(id.lastIndexOf("/") + 1);
                        Matcher m = p.matcher(li.text());
                        String date = "";
                        if (m.find()) {
                            String month = m.group(3).length() == 2 ? m.group(3) : "0" + m.group(3);
                            String day = m.group(5).length() == 2 ? m.group(5) : "0" + m.group(5);
                            date = m.group(1) + "-" + month + "-" + day;
                            int year = Integer.parseInt(m.group(1));
                            if (year < 2016) {
                                continue;
                            }
                        }
                        String title = a.attr("title").trim();
                        if (title == null || title.length() < 2) {
                            title = a.text().trim();
                        }
                        //分解
                        String key = "评估机构、招标、采购、询价、询比、竞标、竞价、竞谈、竞拍、竞卖、竞买、竞投、竞租、比选、比价、竞争性、谈判、磋商、投标、邀标、议标、议价、单一来源、遴选、标段、明标、明投、出让、转让、拍卖、招租、预审、发包、开标、答疑、补遗、澄清、挂牌";
                        String[] keys = key.split("、");
                        if (!CheckProclamationUtil.isProclamationValuable(title, keys)) {
                            continue;
                        }
                        BranchNew branch = new BranchNew();
                        branch.setId(id);
                        serviceContext.setCurrentRecord(branch.getId());
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
                Element ele = doc.select("a:contains(下一页)").first();
                if (ele != null && ele.attr("href").contains("fjsclGoPage") && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    String href = ele.attr("href");
                    href = href.substring(href.indexOf("'") + 1, href.lastIndexOf("'"));
                    String nextPage = url.substring(0, url.lastIndexOf("/") + 1) + href;
                    page.addTargetRequest(nextPage);
                }
            } else {
                String detailHtml = page.getHtml().toString();
                String Content = "";
                BranchNew bn = map.get(url);
                if (bn != null) {
                    String Title = bn.getTitle();
                    String date = bn.getDate();
                    String id = bn.getId();
                    serviceContext.setCurrentRecord(id);
                    Document doc = Jsoup.parse(page.getRawText());
                    Element div = doc.select("div[id=fontzoom]").first();
                    if (div != null) {
                        Element tit = doc.select("p[class=a_title]").first();
                        if (tit != null) {
                            Content = tit.outerHtml();
                            Title = tit.text().trim();
                        }
                        div.select("script").remove();
                        div.select("style").remove();
                        Elements aList = div.select("a");
                        for (Element a : aList) {
                            String href = a.attr("href");
                            if (href.startsWith("mailto")) {
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
                        Elements imgList = div.select("IMG");
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
                                    href = url.substring(0, url.lastIndexOf("/") + 1) + href.substring(2);
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
                        Content += div.outerHtml();

                    } else {
                        if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip")) {
                            Content = "<div>附件下载：<a href='" + url + "'>" + Title + "</a></div>";
                            detailHtml = Jsoup.parse(Content).toString();
                        } else if (url.contains("zwfw.hubei.gov.cn")) {
                            Content = doc.select("body").html();
                        }
                    }
                    RecordVO recordVO = new RecordVO();
                    recordVO.setId(bn.getId());
                    recordVO.setListTitle(bn.getTitle());
                    recordVO.setTitle(Title);
                    recordVO.setDetailLink(url);
                    recordVO.setDetailHtml(detailHtml);
                    recordVO.setDdid(SpecialUtil.stringMd5(detailHtml));
                    recordVO.setDate(date);
                    recordVO.setContent(Content);
                    dataStorage(serviceContext, recordVO, bn.getType());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }

    public Request getListRequest(int pageNo, String __VIEWSTATE, String __EVENTVALIDATION) {
        Request request = new Request(listUrl);
        request.setMethod(HttpConstant.Method.POST);
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("__VIEWSTATE", __VIEWSTATE);
            params.put("__VIEWSTATEGENERATOR", "CA8C29DA");
            params.put("__EVENTTARGET", "AspNetPager1");
            params.put("__EVENTARGUMENT", pageNo);
            params.put("__VIEWSTATEENCRYPTED", "");
            params.put("__EVENTVALIDATION", __EVENTVALIDATION);
            params.put("top1$tbKeyWord", "");
            request.setRequestBody(HttpRequestBody.form(params, "UTF-8"));
        } catch (Exception e) {
            // TODO: handle exception
        }
        return request;
    }
}
