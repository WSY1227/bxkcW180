package com.bidizhaobiao.data.Crawl.service.impl.DS_14124;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
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
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.utils.HttpConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 程序员：刘伟伟
 * 日期：2021-01-04
 * 原网站：首页/招标信息/货物/变更公告
 * 主页：http://hz.fzbidding.com
 **/

@Service("DS_14124_2_GonggBgService")
public class DS_14124_2_GonggBgService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://hzapi.fzbidding.com/hz/portal/portalBidding/list";

    public String baseUrl = "http://hzapi.fzbidding.com";

    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(3).setTimeOut(60000).setSleepTime(20);
    // 网站编号
    public String sourceNum = "14124-2";
    // 网站名称
    public String sourceName = "赢标·电子招标采购交易平台菏泽专区";
    // 信息源
    public String infoSource = "政府采购";

    // 设置地区
    public String area = "华东";
    // 设置省份
    public String province = "山东";
    // 设置城市
    public String city = "菏泽市";
    // 设置县
    public String district = "";
    // 设置县
    public String createBy = "刘伟伟";
    

    public Pattern p = Pattern.compile("20(\\d{2})(-|年|/|\\.)(\\d{1,2})(-|月|/|\\.)(\\d{1,2})(日|/|)");

    public Site getSite() {
        return this.site.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
    }

    public void startCrawl(int ThreadNum, int crawlType) {
        // 赋值
        serviceContextEvaluation();
        serviceContext.setCrawlType(crawlType);
        // 保存日志
        saveCrawlLog(serviceContext);
        // 启动爬虫
        spider = Spider.create(this).thread(ThreadNum)
                .setDownloader(new MyDownloader(serviceContext, false, listUrl));
        Request request = getListRequest(1);
        spider.addRequest(request);
        serviceContext.setSpider(spider);
        spider.run();
        // 爬虫状态监控部分
        saveCrawlResult(serviceContext);
    }

    public Request getListRequest(int pageIndex) {
        Request request = new Request(listUrl);
        try {
            request.setMethod(HttpConstant.Method.POST);
            request.addHeader("Connection", "close");
            request.addHeader("Content-Type", "application/json;charset=UTF-8");
            String json = "{\"portalBiddingType\":\"1\",\"tenderProjectType\":\"B\",\"page\":" + pageIndex + ",\"limit\":10,\"_t\":1609813775}";
            request.setRequestBody(HttpRequestBody.json(json, "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return request;
    }

    public Request getContentRequest(String path, String id) {
        Request request = new Request(path);
        try {
            request.setMethod(HttpConstant.Method.POST);
            request.addHeader("Connection", "close");
            request.addHeader("Content-Type", "application/json;charset=UTF-8");
            String json = "{\"id\":\"" + id + "\",\"_t\":1609812569}";
            request.setRequestBody(HttpRequestBody.json(json, "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return request;
    }

    public void process(Page page) {
        String html = page.getRawText();
        String url = page.getUrl().toString();
        try {
            if (url.contains("list")) {
                if (html.contains("records")) {
                    JSONObject object = new JSONObject(html);
                    if (serviceContext.getPageNum() == 1) {
                        if (object.has("pages")) {
                            String pages = object.get("pages").toString();
                            int maxPage = Integer.parseInt(pages);
                            serviceContext.setMaxPage(maxPage);
                        }
                    }
                    JSONArray records = object.getJSONArray("records");
                    List<BranchNew> detailList = new ArrayList<BranchNew>();
                    if (records.length() > 0) {
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject info = records.getJSONObject(i);
                            String title = info.get("noticeName").toString();
                            title = title.replace("...", "");
                            String id = info.get("id").toString();
                            String link = "http://hzapi.fzbidding.com/hz/portal/portalBidding/detail?id=" + id;
                            String date = info.get("noticeSendTime").toString().replaceAll("[.|/|年|月]", "-");
                            Matcher m = p.matcher(date);
                            if (m.find()) {
                                date = SpecialUtil.date2Str(SpecialUtil.str2Date(m.group()));
                            }
                            String detailLink = "http://hz.fzbidding.com/detail" + id;
                            dealWithNullTitleOrNullId(serviceContext, title, id);
                            BranchNew branch = new BranchNew();
                            branch.setTitle(title);
                            branch.setId(id);
                            serviceContext.setCurrentRecord(branch.getId());
                            branch.setDetailLink(detailLink);
                            branch.setLink(link);
                            branch.setDate(date);
                            detailList.add(branch);
                        }
                        // 校验数据,判断是否需要继续触发爬虫
                        List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                        for (BranchNew branch : needCrawlList) {
                            map.put(branch.getLink(), branch);
                            page.addTargetRequest(getContentRequest(branch.getLink(), branch.getId()));
                        }
                    } else {
                        dealWithNullListPage(serviceContext);
                    }
                }
                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    page.addTargetRequest(getListRequest(serviceContext.getPageNum()));
                }
            } else {
                BranchNew branchNew = map.get(url);
                String title = Jsoup.parse(branchNew.getTitle()).text();
                String id = branchNew.getId();
                serviceContext.setCurrentRecord(id);
                String date = branchNew.getDate();
                String detailLink = branchNew.getDetailLink();
                String detailTitle = title;
                String content = "";
                JSONObject object = new JSONObject(html);
                JSONObject detail = object.getJSONObject("detail");
                content = detail.get("noticeContext").toString();
                if (detail.has("portalEncloseure")) {
                    JSONArray portalEncloseure = detail.getJSONArray("portalEncloseure");
                    for (int i = 0; i < portalEncloseure.length(); i++) {
                        JSONObject info = portalEncloseure.getJSONObject(i);
                        String enclosureUrl = info.get("enclosureUrl").toString();
                        String enclosureName = info.get("enclosureName").toString();
                        if (!"".equals(enclosureUrl) && enclosureUrl != null) {
                            String fj = "<a href=" + enclosureUrl + ">" + enclosureName + "</a>";
                            content += fj;
                        }
                    }
                }
                Document document = Jsoup.parse(content);
                Element contentE = document.select("body").first();
                //contentE.removeAttr("style");
                contentE.select("iframe").remove();
                contentE.select("style").remove();
                contentE.select("input").remove();
                contentE.select("script").remove();
                if (contentE.select("a") != null) {
                    Elements as = contentE.select("a");
                    for (Element a : as) {
                        String href = a.attr("href");
                        if ("".equals(href) || href == null
                                || href.indexOf("#") == 0
                                || href.contains("javascript:")) {
                            a.removeAttr("href");
                            continue;
                        }
                        if (!href.contains("@") && !"".equals(href) && !href.contains("javascript") && !href.contains("http") && !href.contains("#")) {
                            if (href.contains("../")) {
                                href = baseUrl + "/" + href.substring(href.lastIndexOf("./") + 1, href.length());
                                a.attr("href", href);
                                a.attr("rel", "noreferrer");
                            } else if (href.startsWith("./")) {
                                href = baseUrl + href.replace("./", "/");
                                a.attr("href", href);
                                a.attr("rel", "noreferrer");
                            } else if (href.startsWith("/")) {
                                href = baseUrl + href;
                                a.attr("href", href);
                                a.attr("rel", "noreferrer");
                            } else {
                                href = baseUrl + "/" + href;
                                a.attr("href", href);
                                a.attr("rel", "noreferrer");
                            }
                        }
                        //a.attr("rel", "noreferrer");
                    }
                }
                if (contentE.select("img").first() != null) {
                    Elements imgs = contentE.select("img");
                    for (Element img : imgs) {
                        String src = img.attr("src");
                        if (!src.contains("javascript") && !"".equals(src) && !src.contains("http")) {
                            if (src.contains("../")) {
                                src = baseUrl + "/" + src.substring(src.lastIndexOf("./") + 1, src.length());
                                img.attr("src", src);
                            } else if (src.startsWith("./")) {
                                src = baseUrl + src.replace("./", "/");
                                img.attr("src", src);
                            } else if (src.startsWith("/")) {
                                src = baseUrl + src;
                                img.attr("src", src);
                            } else {
                                src = baseUrl + "/" + src;
                                img.attr("src", src);
                            }
                        }
                    }
                }
                content = "<div>" + title + "</div>" + contentE.html();
                html = html != null ? html : Jsoup.parse(content).html();
                RecordVO recordVO = new RecordVO();
                recordVO.setId(id);
                recordVO.setListTitle(title);
                recordVO.setDate(date);
                recordVO.setContent(content.replaceAll("\\u2002", " "));
                recordVO.setTitle(detailTitle);//详情页标题
                recordVO.setDdid(SpecialUtil.stringMd5(html));//详情页md5
                recordVO.setDetailLink(detailLink);//详情页链接
                recordVO.setDetailHtml(html);
                logger.info("入库id==={}", id);
                dataStorage(serviceContext, recordVO, branchNew.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }


}
