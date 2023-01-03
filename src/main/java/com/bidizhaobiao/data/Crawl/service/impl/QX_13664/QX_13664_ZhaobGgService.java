package com.bidizhaobiao.data.Crawl.service.impl.QX_13664;

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
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 程序员：董文锋
 * 日期：2020-07-30
 * 原网站：http://newwcwy.ahxf.gov.cn/Skin4/List/?page=1&id=124696&villageid=5571
 **/
@Service
public class QX_13664_ZhaobGgService extends SpiderService implements PageProcessor {
    public Spider spider = null;
    public String[] keys = "招标、采购、询价、竞标、竞价、竞谈、竞拍、竞卖、竞买、竞投、竞租、比选、比价、竞争性、谈判、磋商、投标、邀标、议标、议价、单一来源、遴选、标段、明标、明投、出让、转让、拍卖、招租、预审、发包、开标、答疑、补遗、澄清、挂牌".split("、");
    public String listUrl = "http://newwcwy.ahxf.gov.cn/Skin4/List/?page=1&id=124696&villageid=5571";
    public String baseUrl = "http://newwcwy.ahxf.gov.cn";
    // 网站编号
    public String sourceNum = "13664";
    // 网站名称
    public String sourceName = "全椒县襄河镇先锋网";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华东";
    // 设置省份
    public String province = "安徽";
    // 设置城市
    public String city = "滁州市";
    // 设置县
    public String district = "全椒县";
    // 设置县
    public String createBy = "董文锋";
    public Pattern p = Pattern.compile("20\\d{2}-\\d{1,2}-\\d{1,2}");
    //站源类型
    public String taskType;
    //站源名称
    public String taskName;
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);

    public Site getSite() {
        return this.site.setCharset("UTF-8");
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
            Html html = page.getHtml();
            Thread.sleep(2000);
            if (url.contains("page=")) {
                List<Selectable> nodes = html.xpath("//div[@class='list_news']/ul/li").nodes();
                if (nodes.size() > 0) {
                    List<BranchNew> branchNewList = new ArrayList<BranchNew>();
                    for (Selectable node : nodes) {
                        String title = node.xpath("//a/text()").get();
                        if (!CheckProclamationUtil.isProclamationValuable(title, keys)) {
                            continue;
                        }
                        String link = node.xpath("//a").links().get();
                        String dateStr = node.xpath("//div[@class='fr']/text()").get();
                        Matcher m = p.matcher(dateStr.replaceAll("[./年月]", "-"));
                        String date = "";
                        if (m.find()) {
                            date = sdf.get().format(sdf.get().parse(m.group()));
                        }
                        String id = node.xpath("//a/@href").get();
                        BranchNew branchNew = new BranchNew();
                        branchNew.setId(id);
                        branchNew.setTitle(title);
                        branchNew.setDate(date);
                        branchNew.setLink(link);
                        branchNewList.add(branchNew);
                    }
                    // 检查数据
                    List<BranchNew> needCrawlList = checkData(branchNewList, serviceContext);
                    for (BranchNew branch : needCrawlList) {
                        map.put(branch.getLink(), branch);
                        page.addTargetRequest(branch.getLink());
                    }
                    // 翻页
                    if (serviceContext.isNeedCrawl()) {
                        Element nextUrlEle = html.getDocument().select("a:contains(下一页)").first();
                        if (nextUrlEle != null) {
                            String nextUrl = nextUrlEle.attr("abs:href");
                            serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                            page.addTargetRequest(nextUrl);
                        }
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
            } else {
                BranchNew branchNew = map.get(url);
                map.remove(url);
                String detailHtml = page.getHtml().toString();
                if (branchNew != null) {
                    Document doc = Jsoup.parse(page.getRawText());
                    doc.setBaseUri(baseUrl);
                    String title = branchNew.getTitle().replace(".", "");
                    Element titleEle = doc.select("div.tit").first();
                    if (titleEle != null) {
                        title = titleEle.text();
                    }

                    Element contentDiv = doc.select("div#BodyLabel").first();
                    if (contentDiv == null) {
                        return;
                    }
                    contentDiv.select("script").remove();
                    contentDiv.select("style").remove();

                    Elements aList = contentDiv.select("a");
                    for (Element a : aList) {
                        String href = a.attr("href");
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
                                href = baseUrl + "/" + href;
                                a.attr("href", href);
                            }
                        }
                    }
                    Elements imgList = contentDiv.select("IMG");
                    for (Element img : imgList) {
                        String href = img.attr("src");
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
                                href = baseUrl + "/" + href;
                                img.attr("src", href);
                            }
                        }
                    }
                    String content = title + contentDiv.outerHtml().replace("amp;", "");
                    RecordVO recordVO = new RecordVO();
                    recordVO.setId(branchNew.getId());
                    recordVO.setListTitle(branchNew.getTitle());
                    recordVO.setTitle(title);
                    recordVO.setDetailLink(url);
                    recordVO.setDetailHtml(detailHtml);
                    recordVO.setDdid(SpecialUtil.stringMd5(detailHtml));
                    recordVO.setDate(branchNew.getDate());
                    recordVO.setContent(content);
                    dataStorage(serviceContext, recordVO, branchNew.getType());
                }
            }
        } catch (Exception e) {
            dealWithError(url, serviceContext, e);
        }
    }
}
