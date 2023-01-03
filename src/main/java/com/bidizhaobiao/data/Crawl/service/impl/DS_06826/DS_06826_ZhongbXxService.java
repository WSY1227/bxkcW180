package com.bidizhaobiao.data.Crawl.service.impl.DS_06826;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
import com.bidizhaobiao.data.Crawl.utils.CheckProclamationUtil;
import com.bidizhaobiao.data.Crawl.utils.SpecialUtil;
import org.json.JSONObject;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 程序员：白嘉全  日期：2021-09-07
 * 原网站：http://gbdsj.hunan.gov.cn/xxgk/tzgg/
 * 主页：http://gbdsj.hunan.gov.cn
 **/

@Service("DS_06826_ZhongbXxService")
public class DS_06826_ZhongbXxService extends SpiderService implements PageProcessor {
    public Spider spider = null;
    public String listUrl = "http://gbdsj.hunan.gov.cn/xxgk/tzgg/index.html";
    public Map<String, BranchNew> map = new HashMap<>();
    // 网站编号
    public String sourceNum = "06826";
    // 网站名称
    public String sourceName = "湖南省广播电视局";
    // 设置地区
    public String area = "华中";
    // 设置省份
    public String province = "湖南";
    // 设置城市
    public String city = "长沙市";
    // 设置县
    public String district;
    // 设置县
    public String createBy = "白嘉全";
    // 信息源
    public String infoSource = "政府采购";
    //站源类型
    public String taskType;
    //站源名称
    public String taskName;
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);
    public Pattern pattern_page = Pattern.compile("createPageHTML\\('paging',(\\d+)");
    public Pattern pattern_date = Pattern.compile("20\\d{2}-\\d{1,2}-\\d{1,2}");
    //是否需要入广联达
    public boolean isNeedInsertGonggxinxi = false;

    public Site getSite() {
        return this.site;
    }

    public void startCrawl(int ThreadNum, int crawlType) {
        // 赋值
        serviceContextEvaluation();
        // 保存日志
        saveCrawlLog(serviceContext);
        serviceContext.setCrawlType(crawlType);
        // 启动爬虫
        spider = Spider.create(this).thread(ThreadNum).setDownloader(new MyDownloader(serviceContext, true, listUrl));
        spider.addRequest(new Request(listUrl));
        serviceContext.setSpider(spider);
        spider.run();
        // 爬虫状态监控部分
        saveCrawlResult(serviceContext);
    }

    public void process(Page page) {
        Html html = page.getHtml();
        String url = page.getUrl().toString();
        try {
            Thread.sleep(500);
            if (url.contains("index")) {
                Matcher matcher;
                if (serviceContext.getPageNum() == 1) {
                    matcher = pattern_page.matcher(page.getRawText());
                    if (matcher.find()) {
                        String total = matcher.group(1);
                        int maxPage = Integer.parseInt(total);
                        serviceContext.setMaxPage(maxPage);
                    }
                }
                List<String> links = html.xpath("//table[@class=table]/tbody/tr").links().all();
                List<String> titles = html.xpath("//table[@class=table]/tbody/tr//a/@title").all();
                List<String> dates = html.xpath("//table[@class=table]/tbody/tr").all();
                List<BranchNew> detailList = new ArrayList<>();
                if (links.size() > 0) {
                    for (int i = 0; i < links.size(); i++) {
                        BranchNew branchNew = new BranchNew();
                        String link = links.get(i);
                        String id = link.substring(link.lastIndexOf("/") + 1);
                        String title = titles.get(i);
                        if (!CheckProclamationUtil.isProclamationValuable(title)) {
                            continue;
                        }
                        title = title.replace("...", "");
                        title = title.replace("..", "");
                        String date = dates.get(i);
                        date = date.replaceAll("[./年月]", "-");
                        matcher = pattern_date.matcher(date);
                        if (matcher.find()) {
                            date = matcher.group();
                            date = sdf.get().format(sdf.get().parse(date));
                            branchNew.setDate(date);
                        }
                        dealWithNullTitleOrNullId(serviceContext, title, id);
                        branchNew.setId(id);
                        serviceContext.setCurrentRecord(branchNew.getId());
                        branchNew.setLink(link);
                        branchNew.setTitle(title);
                        branchNew.setDetailLink(link);
                        detailList.add(branchNew);
                    }
                    List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                    for (BranchNew branch : needCrawlList) {
                        map.put(branch.getLink(), branch);
                        page.addTargetRequest(branch.getLink());
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
                // 翻页连接
                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    page.addTargetRequest(listUrl.replace("index", "index_" + (serviceContext.getPageNum())));
                }
            } else {
                BranchNew item = map.get(url);
                if (item != null) {
                    map.remove(url);
                    String content = "";
                    String detailTitle = "";
                    String id = item.getId();
                    serviceContext.setCurrentRecord(id);
                    Document doc = html.getDocument();
                    url = url.substring(0, url.lastIndexOf("/") + 1);
                    doc.setBaseUri(url);
                    Element contEle = doc.select("div.main_content").first();
                    if (contEle != null) {
                        contEle.select("div.main_con_ftit").remove();
                        contEle.select("div.mian_con_foot").remove();
                        attachLinks(contEle);
                        content = contEle.outerHtml();

                        RecordVO recordVO = new RecordVO();
                        recordVO.setId(item.getId());
                        recordVO.setTitle(item.getTitle());
                        recordVO.setDate(item.getDate());
                        recordVO.setContent(content);
                        recordVO.setListTitle(item.getTitle());//列表页的标题
                        recordVO.setDetailHtml(html.toString());//详情页源码
                        recordVO.setDdid(SpecialUtil.stringMd5(html.toString()));//详情页md5
                        recordVO.setDetailLink(item.getDetailLink()); //详情页链接
                        dataStorage(serviceContext, recordVO, item.getType());
                    }

                }
            }
        } catch (Exception e) {
            dealWithError(url, serviceContext, e);
        }
    }

    public void attachLinks(Element contELe) {
        if (contELe.select("a").first() != null) {
            Elements aList = contELe.select("a");
            for (Element a : aList) {
                String href = a.attr("abs:href");
                a.attr("href", href);
            }
        }
        if (contELe.select("img").first() != null) {
            Elements imgList = contELe.select("img");
            for (Element img : imgList) {
                String src = img.attr("abs:src");
                img.attr("src", src);
            }
        }
    }

}
