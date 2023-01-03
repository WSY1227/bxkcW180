package com.bidizhaobiao.data.Crawl.service.impl.XX2989;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
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
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 程序员：许广衡 日期：2021-12-08
 * 原网站：http://ggzy.jz.gov.cn/jyxx/077002/077002001/listMore.html
 * 主页：http://ggzy.jz.gov.cn/
 **/

@Service
public class XX2989_ZhongbXxService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://caigou.peizheng.edu.cn/cat.php?id=4&page=0";
    public String baseUrl = "http://caigou.peizheng.edu.cn";
    public Pattern p = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");
    // 网站编号
    public String sourceNum = "XX2989";
    // 网站名称
    public String sourceName = "广东培正学院";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华南";
    // 设置省份
    public String province = "广东";
    // 设置城市
    public String city = "广州市";
    // 设置县
    public String district = "花都区";
    public String createBy = "许广衡";
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
        return this.site.addHeader("Referer", "http://caigou.peizheng.edu.cn/cat.php?id=4")
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36");
    }

    public void startCrawl(int ThreadNum, int crawlType) {
        int date = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (date > 8 && date < 23) {
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
    }

    public void process(Page page) {
        String url = page.getUrl().toString();
        try {
            List<BranchNew> detailList = new ArrayList<BranchNew>();
            Thread.sleep(1000);
            if (url.contains("page=")) {
                Document doc = page.getHtml().getDocument();
                Elements list = doc.select("div[class=list_main_content] ul li:has(a)");
                if (list.size() > 0) {
                    for (int i = 0; i < list.size(); i++) {
                        Element li = list.get(i);
                        Element a = li.select("a").first();
                        String id = a.attr("href");
                        String link = url.substring(0, url.lastIndexOf("/") + 1) + id;
                        if (id.startsWith("http")) {
                            link = id;
                        }
                        id = id.substring(id.indexOf("?") + 1);
                        String title = a.attr("title").trim();
                        if (title == null || title.length() < 2) {
                            title = a.text().trim();
                        }
                        Matcher m = p.matcher(li.text());
                        String date = "";
                        if (m.find()) {
                            String month = m.group(3).length() == 2 ? m.group(3) : "0" + m.group(3);
                            String day = m.group(5).length() == 2 ? m.group(5) : "0" + m.group(5);
                            date = m.group(1) + "-" + month + "-" + day;
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
                // 翻页连接
                Element ele = doc.select("a:contains(下一页)").first();
                if (ele != null && ele.attr("href").contains("page=") && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    String nextPage = url.substring(0, url.lastIndexOf("/") + 1) + ele.attr("href");
                    page.addTargetRequest(nextPage);
                }
            } else {
                String detailHtml = page.getHtml().toString();
                String Content = "";
                BranchNew bn = map.get(url);
                serviceContext.setCurrentRecord(bn.getId());
                if (bn != null) {
                    String Title = bn.getTitle();
                    String date = bn.getDate();
                    Document doc = Jsoup.parse(page.getRawText());
                    Element div = doc.select("div[class=detail_content]").first();
                    if (div != null) {
                        Element tit = doc.select("div[class=detail_main_content] h3").first();
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
                        } else if (page.getRawText().contains("您已选择离开本网站")) {
                            Content = "<div>更多详细内容：<a href='" + url + "'>" + url + "</a></div>";
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
}
