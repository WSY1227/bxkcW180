package com.bidizhaobiao.data.Crawl.service.impl.Y01124;

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
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 程序员：潘嘉明 日期：2021-12-08
 * 原网站：http://www.tdhsqwsfwzx.cn/zxyd.php
 * 主页：http://www.tdhsqwsfwzx.cn/
 **/

@Service("Y01124_ZhaobGgService")
public class Y01124_ZhaobGgService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://www.tdhsqwsfwzx.cn/zxyd.php?page=1";
    public String baseUrl = "http://www.tdhsqwsfwzx.cn";
    // 网站编号
    public String sourceNum = "Y01124";
    // 网站名称
    public String sourceName = "四川省成都市成华区跳蹬河社区卫生服务中心";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "西南";
    // 设置省份
    public String province = "四川";
    // 设置城市
    public String city = "成都市";
    // 设置县
    public String district = "成华区";
    public String createBy = "潘嘉明";
    public Pattern pMaxPage = Pattern.compile("共 (\\d+)页");
    // 是否需要入广联达
    public boolean isNeedInsertGonggxinxi = false;
    // 站源类型
    public String taskType = "";
    // 站源名称
    public String taskName = "";
    // 抓取网站的相关配置，包括：编码","抓取间隔","重试次数等
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);

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
        Document doc = Jsoup.parse(page.getRawText());
        try {
            List<BranchNew> detailList = new ArrayList<BranchNew>();
            Thread.sleep(500);
            if (url.contains("page")) {
                Element div = doc.select("div[class=art_list]").first();
                if (div != null) {
                    Elements h3s = div.select("h3");
                    Elements times = div.select("div.author");
                    if (h3s.size() > 0) {
                        for (int i = 0; i < h3s.size(); i++) {
                            Element a = h3s.get(i).select("a").first();
                            String href = a.attr("href").trim();
                            href = baseUrl + "/" + href;
                            String id = href;
                            String title = a.text().trim();
                            String date = times.get(i).text().trim();
                            String keys = "租赁、服务、改造、购买、邀请、设计、询标、交易、机构、需求、废旧、废置、处置、报废、供应商、承销商、服务商、调研、优选、择选、择优、选取、公选、选定、摇选、摇号、摇珠、抽选、定选、定点、招标、采购、询价、询比、竞标、竞价、竞谈、竞拍、竞卖、竞买、竞投、竞租、比选、比价、竞争性、谈判、磋商、投标、邀标、议标、议价、单一来源、标段、明标、明投、出让、转让、拍卖、招租、出租、预审、发包、承包、分包、外包、开标、遴选、答疑、补遗、澄清、延期、挂牌、变更、预公告、监理、改造工程、报价、小额、零星、自采、商谈";
                            String[] titleKeyWords = keys.split("、");
                            if (!CheckProclamationUtil.isProclamationValuable(title, titleKeyWords)) {
                                continue;
                            }
                            dealWithNullTitleOrNullId(serviceContext, title, id);
                            BranchNew branch = new BranchNew();
                            branch.setId(id);
                            branch.setLink(href);
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
                } else {
                    dealWithNullListPage(serviceContext);
                }
                if (serviceContext.getPageNum() == 1) {
                    int maxPage = 0;
                    Matcher m = pMaxPage.matcher(doc.text());
                    if (m.find()) {
                        maxPage = Integer.parseInt(m.group(1));
                        serviceContext.setMaxPage(maxPage);
                    }
                }
                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    page.addTargetRequest(listUrl.replace("page=1", "page=" + serviceContext.getPageNum()));
                }
            } else {
                String detailHtml = page.getHtml().toString();
                String content = "";
                BranchNew bn = map.get(url);
                if (bn != null) {
                    String title = bn.getTitle();
                    String date = bn.getDate();
                    Element tit = doc.select("div.article h1").first();
                    if (tit != null) {
                        title = tit.text().trim();
                        content = tit.outerHtml();
                    }
                    Element subject = doc.select("div[class=art_con]").first();
                    if (subject != null) {
                        Elements aList = subject.select("a");
                        for (Element a : aList) {
                            String href = a.attr("href");
                            if (href.startsWith("mailto")) {
                                continue;
                            }
                            if (href.startsWith("javascript")) {
                                a.remove();
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
                                if (href.indexOf("//") == 0) {
                                    href = "http:" + href;
                                    a.attr("href", href);
                                } else if (href.indexOf("/") == 0) {
                                    href = baseUrl + href;
                                    a.attr("href", href);
                                } else if (href.indexOf("../") == 0) {
                                    href = href.replace("../", "");
                                    href = "http://www.cqyc.gov.cn/zwgk_204/" + href;
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
                            if (!href.contains("/")) {
                                href = "./" + img.attr("OLDSRC");
                            }
                            if (href.contains("/qrcode/")) {
                                img.remove();
                                continue;
                            }
                            if (href.length() > 10 && href.indexOf("http") != 0) {
                                if (href.indexOf("//") == 0) {
                                    href = "http:" + href;
                                    img.attr("src", href);
                                } else if (href.indexOf("../") == 0) {
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
                        content += subject.html();
                    }
                    RecordVO recordVO = new RecordVO();
                    recordVO.setId(bn.getId());
                    recordVO.setListTitle(bn.getTitle());
                    recordVO.setTitle(title);
                    recordVO.setDetailLink(url);
                    recordVO.setDetailHtml(detailHtml);
                    recordVO.setDate(date);
                    recordVO.setContent(content);
                    dataStorage(serviceContext, recordVO, bn.getType());
                }
            }
        } catch (Exception e) {
            // e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }
}
