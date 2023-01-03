package com.bidizhaobiao.data.Crawl.service.impl.GJ_07280;

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
import us.codecraft.webmagic.selector.Html;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 程序员：邱文杰
 * 日期：2021-04-16
 * 原网站：http://www.bjztc.com/notice/clarification
 * 主页：http://www.bjztc.com/
 */

@Service("GJ_07280_1_GonggBgService")
public class GJ_07280_1_GonggBgService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://www.bjztc.com/notice/clarification?page=0";

    // 网站名
    public Pattern p = Pattern.compile("(\\d{4})(年|/|-)(\\d{1,2})(月|/|-)(\\d{1,2})");
    public Pattern p_p = Pattern.compile("view\\('(.*?)','(.*?)','(.*?)'\\)");
    public Pattern p_t = Pattern.compile("共有(\\d+)条信息");
    // 网站编号
    public String sourceNum = "07280-1";
    // 网站名称
    public String sourceName = "中交咨询";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "全国";
    // 设置省份
    public String province;
    // 设置城市
    public String city;
    // 设置县
    public String district;
    // 设置县
    public String createBy = "邱文杰";
    //站源类型
    public String taskType;
    //站源名称
    public String taskName;
    public double priod = 4;
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20).setCharset("UTF-8");
    //是否需要入广联达
    public boolean isNeedInsertGonggxinxi = false;

    public Site getSite() {
        return this.site.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
    }

    public void startCrawl(int ThreadNum, int crawlType) {
        // 赋值
        serviceContextEvaluation();
        // 保存日志
        saveCrawlLog(serviceContext);
        serviceContext.setCrawlType(crawlType);
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
        Html html = page.getHtml();
        String url = page.getUrl().toString();
        try {
            // HtmlPage htmlPage = client.getPage(url);
            List<BranchNew> detailList = new ArrayList<BranchNew>();
            Thread.sleep(1000);
            // 判断是否是翻页连接
            if (url.contains("page=")) {
                Document document = Jsoup.parse(page.getRawText().replace("&nbsp;", " ").replace("&amp;", "&").replace("&ensp;", ""));
                Elements lis = document.select("div.clearnfix.page-news-list li");
                if (lis.size() > 0) {
                    for (Element li : lis) {
                        Element a = li.select("a").first();
                        String title = a.text().trim();
                        String href = a.attr("href");
                        if (href.contains("http")) {
                            continue;
                        }
                        String id = href;
                        String link = "http://www.bjztc.com" + id;
                        String detailLink = link;
                        String date = li.select("span").last().text().trim();
                        BranchNew branch = new BranchNew();
                        branch.setId(id);
                        branch.setLink(link);
                        branch.setDetailLink(detailLink);
                        branch.setTitle(title);
                        branch.setDate(date);
                        detailList.add(branch);
                    }
                    // 校验数据List<BranchNew> detailList,int pageNum,String
                    // sourceNum
                    List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                    for (BranchNew branch : needCrawlList) {
                        map.put(branch.getLink(), branch);
                        page.addTargetRequest(branch.getLink());
                    }

                } else {
                    if (document.select("div.node-content").first() == null) {
                        dealWithNullListPage(serviceContext);
                    }
                }
            } else {
                String baseUrl = "http://www.bjztc.com";
                BranchNew branchNew = map.get(url);
                if (branchNew == null) {
                    return;
                }
                String title = branchNew.getTitle();
                String id = branchNew.getId();
                String date = branchNew.getDate();
                String detailLink = branchNew.getDetailLink();
                String detailTitle = "";
                String content = "";
                String detailContent = html.toString();
                Document document = Jsoup.parse(page.getRawText().replace("&nbsp;", " ").replace("&amp;", "&"));
                if (document.select("span.field.field--name-title.field--type-string.field--label-hidden").first() == null) {
                    return;
                }
                detailTitle = document.select("span.field.field--name-title.field--type-string.field--label-hidden").first().text().trim().replace(" ", "").replace("...", "");
                if (detailTitle.equals("")) {
                    detailTitle = "公示公告";
                }
                Element contentE = document.select("div.node-content").first();
                if (contentE == null) {
                    return;
                }
                setUrl(contentE, baseUrl, url);
                content = contentE.outerHtml();
                detailContent = detailContent != null ? detailContent : Jsoup.parse(content).html();
                RecordVO recordVO = new RecordVO();
                recordVO.setId(id);
                recordVO.setListTitle(title);
                recordVO.setDate(date);
                recordVO.setContent(content);
                recordVO.setTitle(detailTitle);//详情页标题
                recordVO.setDdid(SpecialUtil.stringMd5(detailContent));//详情页md5
                recordVO.setDetailLink(detailLink);//详情页链接
                recordVO.setDetailHtml(detailContent);
                dataStorage(serviceContext, recordVO, branchNew.getType());
            }
        } catch (Exception e) {
//            e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }

    public void setUrl(Element conE, String baseUrl, String url) {
        Element contentE = conE;
        contentE.removeAttr("style");
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
                String href = a.attr("href");
                if (!href.contains("@") && !"".equals(href) && !href.contains("javascript") && !href.contains("http") && !href.contains("#")) {
                    if (href.contains("../")) {
                        href = baseUrl + "/" + href.replace("../", "");
                        a.attr("href", href);
                    } else {
                        href = baseUrl + href;
                        a.attr("href", href);
                        a.removeAttr("rel");
                    }
                }
            }
        }
        if (contentE.select("img").first() != null) {
            Elements imgs = contentE.select("img");
            for (Element img : imgs) {
                String src = img.attr("src");
                if (!src.contains("javascript") && !"".equals(src) && !src.contains("http")) {
                    if (src.contains("../")) {
                        src = baseUrl + "/" + src.replace("../", "");
                        img.attr("src", src);
                    } else {
                        src = baseUrl + src;
                        img.attr("src", src);
                    }
                }
            }
        }
    }
}
