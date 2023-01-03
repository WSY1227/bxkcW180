package com.bidizhaobiao.data.Crawl.service.impl.QX_12459;

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
 * 程序员：余林锐
 * 日期：2021-10-09
 * 原网站：http://ygxfy.jxfy.gov.cn/article/index/id/MygrNzAwNjAwNCACAAA.shtml
 * 主页：http://ygxfy.jxfy.gov.cn/
 **/
@Service
public class QX_12459_ZhongbXxService extends SpiderService implements PageProcessor {
    public  Spider spider = null;

    public  String listUrl = "http://ygxfy.jxfy.gov.cn/article/index/id/MygrNzAwNjAwNCACAAA.shtml";
    public  String baseUrl = "http://ygxfy.jxfy.gov.cn";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
     Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);

    // 网站编号
    public String sourceNum = "12459";
    // 网站名称
    public String sourceName = "江西省余干县人民法院";
    // 信息源
    public  String infoSource = "政府采购";

    // 设置地区
    public String area = "华东";
    // 设置省份
    public String province = "江西";
    // 设置城市
    public String city="上饶市";
    // 设置县
    public String district="余干县";
    // 设置县
    public String createBy = "余林锐";

    public  Pattern pattern = Pattern.compile("createPageHTML\\('paging',(\\d+),");
    public  Pattern p = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");
    //是否需要入广联达
    public  boolean isNeedInsertGonggxinxi = false;
    //站源类型
    public  String taskType;
    //站源名称
    public  String taskName;


    public Site getSite() {
          return this.site.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36");
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
            if (url.contains(listUrl)) {
                Document doc = page.getHtml().getDocument();
                Element conTag = doc.select("div[class=font14 dian_a] ul").first();
                Elements eachTags = conTag.select("li:has(a)");
                List<BranchNew> detailList = new ArrayList<>();
                if (eachTags.size() > 0) {
                    for (Element eachTag : eachTags) {
                        if (eachTag.select("a").first() == null) continue;
                        String id = eachTag.select("a").first().attr("href");
                        String link = baseUrl + id;
                        String title = eachTag.select("a").first().attr("title").trim();
                        if (title.length() < 2) {
                            title = eachTag.select("a").first().text().trim();
                        }
                        String date = "";
                        Matcher m = p.matcher(eachTag.outerHtml());
                        if (m.find()) {
                            String month = m.group(3).length() == 2 ? m.group(3) : "0" + m.group(3);
                            String day = m.group(5).length() == 2 ? m.group(5) : "0" + m.group(5);
                            date = m.group(1) + "-" + month + "-" + day;
                            int year = Integer.parseInt(m.group(1));
                            if (year < 2016) {
                                continue;
                            }
                        }
                        if (!CheckProclamationUtil.isProclamationValuable(title)) {
                            continue;
                        }
                        BranchNew bn = new BranchNew();
                        bn.setTitle(title);
                        bn.setId(id);
                        bn.setDate(date);
                        bn.setLink(link);
                        detailList.add(bn);
                    }
                    // 校验数据List<BranchNew> detailList,int pageNum,String
                    List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                    for (BranchNew branch : needCrawlList) {
                        map.put(branch.getLink(), branch);
                        page.addTargetRequest(branch.getLink());
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
            } else {
                if (page.getStatusCode() == 404) return;
                BranchNew bn = map.get(url);
                if (bn == null) {
                    return;
                }
                String detailHtml = page.getHtml().toString();
                String Content = "";
                if (bn != null) {
                    String Title = bn.getTitle();
                    String date = bn.getDate();
                    Document doc = Jsoup.parse(page.getRawText());
                    Element div = doc.select("div[class=detail]").first();
                    if (div != null) {
                        Element titleText = div.select("div[class=b_title]").first();
                        if (titleText != null) {
                            Title = titleText.text().trim();
                        }
                        div.select("script").remove();
                        div.select("style").remove();
                        div.select("div[class=detail_location]").remove();
                        div.select("div[class=sth_a]").remove();
                        Elements aList = div.select("a");
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
                        Content += div.outerHtml().replace("display:none", "");

                    }
                    if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip") || url.contains(".xls")) {
                        Content = "<div>附件下载：<a href='" + url + "'>" + Title + "</a></div>";
                        detailHtml = Jsoup.parse(Content).toString();
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
            dealWithError(url, serviceContext, e);
        }
    }


}
