package com.bidizhaobiao.data.Crawl.service.impl.Y03134;

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
import java.util.regex.Pattern;

/**
 * 程序员：邱文杰 日期：2021-03-16
 * 原网站：http://www.ssxzyy.cn/newlist.php?aid=7&bid=29
 * 主页：http://www.ssxzyy.cn
 **/

@Service("Y03134_ZhongbXxService")
public class Y03134_ZhongbXxService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "https://www.ssxzyy.cn/newlist.php?aid=7&bid=29&page=1";
    public String baseUrl = "http://www.ssxzyy.cn";
    // 网站编号
    public String sourceNum = "Y03134";
    // 网站名称
    public String sourceName = "宿松县中医院";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华东";
    // 设置省份
    public String province = "安徽";
    // 设置城市
    public String city = "安庆市";
    // 设置县
    public String district = "宿松县";
    public String createBy = "邱文杰";
    public Pattern pDate = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");
    public Pattern p = Pattern.compile("countPage = (\\d+)");
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
            if (url.contains("list")) {
                Elements list = doc.select("table[width=98%]");
                if (list.size() > 0) {
                    for (int i = 0; i < list.size(); i++) {
                        Element a = list.get(i).select("a").first();
                        String title = a.text();
                        String href = baseUrl + "/" + a.attr("href");
                        String id = href.substring(href.lastIndexOf("?") + 1);
                        String date = list.get(i).select("td.table_list_time").text();
                        date = date.replace("/", "-");
                        if (!CheckProclamationUtil.isProclamationValuable(title)) {
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
                Element ele = doc.select("a:contains(下一页)").first();
                if (ele != null && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    page.addTargetRequest(baseUrl + ele.attr("href"));
                }
            } else {
                String detailHtml = page.getHtml().toString();
                String content = "";
                BranchNew bn = map.get(url);
                if (bn != null) {
                    String title = bn.getTitle();
                    String date = bn.getDate();
                    Element subject = doc.select("table[cellspacing=10]").first();
                    if (subject != null) {
                        subject.select("tr").first().remove();
                        Elements aList = subject.select("a");
                        for (Element a : aList) {
                            String href = a.attr("href");
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
                                    href = url.substring(0, url.lastIndexOf("/") + 1) + href;
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
                            if (href.startsWith("file://")) {
                                img.remove();
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
                        content = title + "<br>" + subject.html();
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
