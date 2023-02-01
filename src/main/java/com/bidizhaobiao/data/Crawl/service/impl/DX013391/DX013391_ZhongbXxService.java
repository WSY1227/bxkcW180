package com.bidizhaobiao.data.Crawl.service.impl.DX013391;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
 * 程序员：徐文帅 日期：2023-02-01
 * 原网站：首页/公示公告/查看更多
 * 主页：http://www.gaii.org.cn
 **/
@Service
public class DX013391_ZhongbXxService extends SpiderService implements PageProcessor {
    public Spider spider = null;

    public String listUrl = "http://www.gaii.org.cn/gdhx/message/getList?cols=27&pageNumber=1&pageSize=9";
    public String baseUrl = "http://www.gaii.org.cn/";
    public Pattern datePat = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");

    // 网站编号
    public String sourceNum = "DX013391";
    // 网站名称
    public String sourceName = "广东省保险行业协会";
    // 信息源
    public String infoSource = "企业采购";
    // 设置地区
    public String area = "华南";
    // 设置省份
    public String province = "广东";
    // 设置城市
    public String city;
    // 设置县
    public String district;
    public String createBy = "徐文帅";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);

    public Site getSite() {
        return this.site;
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
            Thread.sleep(500);
            if (url.contains("&pageNumber=")) {
                JSONObject dataJSON = JSONObject.parseObject(page.getRawText()).getJSONObject("message");
                JSONArray rows = dataJSON.getJSONArray("list");
                if (rows.size() > 0) {
                    String key = "询标、交易、机构、需求、废旧、废置、处置、报废、供应商、承销商、服务商、调研、优选、择选、择优、选取、公选、选定、摇选、摇号、摇珠、抽选、定选、定点、招标、采购、询价、询比、竞标、竞价、竞谈、竞拍、竞卖、竞买、竞投、竞租、比选、比价、竞争性、谈判、磋商、投标、邀标、议标、议价、单一来源、标段、明标、明投、出让、转让、拍卖、招租、出租、预审、发包、承包、分包、外包、开标、遴选、答疑、补遗、澄清、延期、挂牌、变更、预公告、监理、改造工程、报价、小额、零星、自采、商谈";
                    String[] keys = key.split("、");
                    for (int i = 0; i < rows.size(); i++) {
                        JSONObject row = rows.getJSONObject(i);
                        String link = baseUrl;
                        String id = row.getString("C_ID");
                        String detailLink = link;
                        String date = "";
                        Matcher dateMat = datePat.matcher(row.getString("T_TIME"));
                        if (dateMat.find()) {
                            date = dateMat.group(1);
                            date += dateMat.group(3).length() == 2 ? "-" + dateMat.group(3) : "-0" + dateMat.group(3);
                            date += dateMat.group(5).length() == 2 ? "-" + dateMat.group(5) : "-0" + dateMat.group(5);
                        }
                        String title = row.getString("C_TITLE").trim();
                        if (!CheckProclamationUtil.isProclamationValuable(title, keys)) {
                            continue;
                        }
                        String content = row.getString("C_CONTENT");
                        BranchNew branch = new BranchNew();
                        branch.setId(id);
                        serviceContext.setCurrentRecord(branch.getId());
                        branch.setLink(link);
                        branch.setDetailLink(detailLink);
                        branch.setDate(date);
                        branch.setTitle(title);
                        branch.setContent(content);
                        detailList.add(branch);
                    }
                    List<BranchNew> branchNewList = checkData(detailList, serviceContext);
                    for (BranchNew branch : branchNewList) {
                        serviceContext.setCurrentRecord(branch.getId());
                        String detailHtml = branch.getContent();
                        Document detailDoc = Jsoup.parse("<div id='content'>" + detailHtml + "</div>");
                        String title = branch.getTitle().replace("...", "");
                        String date = branch.getDate();
                        String content = "";
                        Element contentElement = detailDoc.select("div#content").first();
                        if (contentElement != null) {
                            Elements aList = contentElement.select("a");
                            for (Element a : aList) {
                                String href = a.attr("href");
                                a.attr("rel", "noreferrer");
                                if (href.startsWith("mailto")) {
                                    continue;
                                }
                                if (href.contains(".doc") || href.contains(".pdf") || href.contains(".zip") || href.contains(".xls")) {
                                    a.text("附件下载：" + a.text());
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
                                        src = baseUrl + "/" + src;
                                        img.attr("src", src);
                                    } else if (src.indexOf("./") == 0) {
                                        src = url.substring(0, url.lastIndexOf("/")) + src.substring(src.lastIndexOf("./") + 1);
                                        img.attr("src", src);
                                    } else if (src.startsWith("//")) {
                                        src = baseUrl.substring(0, baseUrl.indexOf(":") + 1) + src;
                                        img.attr("src", src);
                                    } else if (src.indexOf("/") == 0) {
                                        src = baseUrl + src;
                                        img.attr("src", src);
                                    } else {
                                        src = url.substring(0, url.lastIndexOf("/") + 1) + src;
                                        img.attr("src", src);
                                    }
                                }
                            }
                            contentElement.select("script").remove();
                            contentElement.select("style").remove();
                            content = contentElement.outerHtml();
                        }
                        RecordVO recordVO = new RecordVO();
                        recordVO.setId(branch.getId());
                        recordVO.setListTitle(branch.getTitle());
                        recordVO.setTitle(title);
                        recordVO.setDetailLink(branch.getDetailLink());
                        recordVO.setDetailHtml(detailHtml);
                        recordVO.setDdid(SpecialUtil.stringMd5(detailHtml));
                        recordVO.setDate(date);
                        recordVO.setContent(content.replaceAll("\\ufeff|\\u2002|\\u200b|\\u2003", ""));
                        dataStorage(serviceContext, recordVO, branch.getType());
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }

                if (serviceContext.getPageNum() == 1) {
                    serviceContext.setMaxPage(dataJSON.getInteger("totalPage"));
                }
                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    String href = listUrl.replace("&pageNumber=1", "&pageNumber=" + serviceContext.getPageNum());
                    page.addTargetRequest(href);
                }
            } else {
                BranchNew branch = map.get(url);
                if (branch != null) {
                    map.remove(url);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }


}
