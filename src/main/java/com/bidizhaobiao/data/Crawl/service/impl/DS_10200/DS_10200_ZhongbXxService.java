package com.bidizhaobiao.data.Crawl.service.impl.DS_10200;import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
import com.bidizhaobiao.data.Crawl.utils.SpecialUtil;
import org.json.JSONArray;
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
import java.util.List;
import java.util.regex.Pattern;

/**
 * 程序员：梁伟雄
 * 日期：2019-09-29
 * 原网站：http://www.yunjizhaobiao.com/pro.php?keyword=&lm=3
 * 主页：http://www.yunjizhaobiao.com/
 **/
@Service
public class DS_10200_ZhongbXxService extends SpiderService implements PageProcessor {

    public  Spider spider = null;

    public  String listUrl = "http://www.yunjizhaobiao.com/result.php?page=0&lm=3";
    public  String homeUrl = "http://www.yunjizhaobiao.com/";

    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    public  Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);
    public  Pattern p = Pattern.compile("(?<year>\\d{2})-(?<month>\\d{1,2})-(?<day>\\d{1,2})");
    // 网站编号
    public String sourceNum = "10200";
    // 网站名称
    public String sourceName = "深圳市云集招标有限公司";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华南";
    // 设置省份
    public String province = "广东";
    // 设置城市
    public String city = "深圳市";
    // 设置县
    public String district;
    // 设置县
    public String createBy = "梁伟雄";
    //站源类型
    public String taskType;
    //站源名称
    public String taskName;
    //是否需要入广联达
    public  boolean isNeedInsertGonggxinxi = false;

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
        spider = Spider.create(this).thread(ThreadNum).setDownloader(new MyDownloader(serviceContext, false, listUrl));
        spider.addRequest(new Request(listUrl));
        serviceContext.setPageNum(0);
        serviceContext.setSpider(spider);
        spider.run();
        // 爬虫状态监控部分
        saveCrawlResult(serviceContext);
    }

    public void process(Page page) {
        Html html = page.getHtml();
        String url = page.getUrl().toString();
        List<BranchNew> detailList = new ArrayList<>();
        Document doc = page.getHtml().getDocument();
        try {
            Thread.sleep(500);
            String rawText = page.getRawText();
            // 判断是否是翻页连接
            if (url.contains("page=")) {
                if (rawText.indexOf("[") == 0) {
                    JSONArray jsonArray = new JSONArray(rawText);
                    if (jsonArray.length() > 0) {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject json = jsonArray.getJSONObject(i);
                            String title = json.getString("title");
                            String id = json.getString("id");
                            String href = homeUrl + "pro_show.php?id=" + id;
                            String listTime = json.getString("wtime");
                            BranchNew branch = new BranchNew();
                            branch.setId(id);
                            branch.setTitle(title);
                            branch.setLink(href);
                            branch.setDate(listTime);
                            detailList.add(branch);
                        }
                        // 校验数据,判断是否需要继续触发爬虫
                        List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                        for (BranchNew branch : needCrawlList) {
                            map.put(branch.getLink(), branch);
                            page.addTargetRequest(branch.getLink());
                        }
                        //无页码，经查看，当前仅仅有7页
                        serviceContext.setMaxPage(7);
                        if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                            serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                            String nextPage = listUrl.replaceAll("page=(\\d+)", "page=" + serviceContext.getPageNum());
                            page.addTargetRequest(nextPage);
                        }
                    } else {
                        dealWithNullListPage(serviceContext);
                    }
                }
            } else {
                BranchNew branch = map.get(url);
                if (branch != null) {
                    Elements content = doc.select(".detail_z_body");
                    String detailTitle = null;
                    String detailContent = null;
                    if (content.size() > 0) {
                        //补全附件链接
                        Elements as = content.select("a");
                        for (Element a : as) {
                            //当href出现 / ../  ../../
                            String link = a.attr("href");
                            if ("".equals(link) || link == null
                                    || link.indexOf("#") == 0
                                    || link.contains("javascript:")) {
                                a.removeAttr("href");
                                continue;
                            }
                            if (link.indexOf("http") != 0) {
                                if (link.indexOf("/") == 0) {
                                    link = homeUrl + link.substring(link.indexOf("/") + 1);
                                } else if (link.indexOf("../") == 0) {
                                    link = link.replace("../", "");
                                    link = homeUrl + link;//禁止盗链， 请从本网站上下载!
                                } else if (link.indexOf("./") == 0) {
                                    link = homeUrl + link.replace("./", "");
                                } else {// 否则 直接文件名
                                    if (!"".equals(link)) {
                                        link = homeUrl + link;
                                    }
                                }
                                a.attr("href", link);
                            }
                        }
                        //补全图片链接
                        Elements imgs = content.select("img");
                        for (Element img : imgs) {
                            //当src出现 / ../  ../../
                            String src = img.attr("src");
                            if (src.indexOf("http") != 0) {
                                if (src.indexOf("/") == 0) {
                                    src = homeUrl + src.substring(src.indexOf("/") + 1);
                                } else if (src.indexOf("../") == 0) {
                                    src = src.replace("../", "");
                                    src = homeUrl + src;//禁止盗链， 请从本网站上下载!
                                } else if (src.indexOf("./") == 0) {
                                    src = homeUrl + src.replace("./", "");
                                } else {// 否则 直接文件名
                                    if (!"".equals(src)) {
                                        src = homeUrl + src;
                                    }
                                }
                                img.attr("src", src);
                            }
                        }
                        /*body里有一下东西需要清除**/
                        content.select("script").remove();
                        content.select("style").remove();
                        detailTitle = doc.select(".detail_title").text();
                        detailContent = detailTitle + content.html();
                    } else {
                        if (url.contains(".pdf") || url.contains(".doc") || url.contains(".zip")) {
                            detailTitle = branch.getTitle();
                            detailContent = "<a href='" + branch.getLink() + "'>" + detailTitle + "</a>";
                        }
                        //其他类型文件暂时先不写，或者样式不同，先报错，再修改
                    }
                    RecordVO recordVO = new RecordVO();
                    recordVO.setId(branch.getId());
                    recordVO.setTitle(detailTitle);
                    recordVO.setListTitle(branch.getTitle());
                    recordVO.setDetailLink(url);
                    recordVO.setDate(branch.getDate());
                    recordVO.setContent(detailContent);
                    recordVO.setDetailHtml(html != null ? html.toString() : Jsoup.parse(detailContent).html());
                    recordVO.setDdid(SpecialUtil.stringMd5(html != null ? html.toString() : Jsoup.parse(content.html()).html()));
                    dataStorage(serviceContext, recordVO, branch.getType());//入库操作（包括数据校验和入库）
                }
            }
        } catch (Exception e) {
            dealWithError(url, serviceContext, e);
        }
    }
}
