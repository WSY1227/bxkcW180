package com.bidizhaobiao.data.Crawl.service.impl.DS_10833;

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
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.utils.HttpConstant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 程序员： 赖晓晖  日期：2020-07-03
 * 原网站： http://shwj.jingmen.gov.cn/col/col1438/index.html?uid=1646&pageNum=1
 * 主页：http://shwj.jingmen.gov.cn
 **/

@Service("DS_10833_ZhongbXxService")
public class DS_10833_ZhongbXxService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://shwj.jingmen.gov.cn/col/col1438/index.html?uid=1646&pageNum=1";

    public String baseUrl = "http://shwj.jingmen.gov.cn";

    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(3).setTimeOut(30000).setSleepTime(20);
    // 网站编号
    public String sourceNum = "10833";
    // 网站名称
    public String sourceName = "荆门市商务局";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华中";
    // 设置省份
    public String province = "湖北";
    // 设置城市
    public String city = "荆门市";
    // 设置县
    public String district;
    // 设置CreateBy
    public String createBy = "赖晓晖";

    public Pattern p = Pattern.compile("(\\d{4})(年|/|-)(\\d{1,2})(月|/|-)(\\d{1,2})");

    public Pattern p_p = Pattern.compile("view\\('(.*?)','(.*?)','(.*?)'\\)");

    public Site getSite() {
        return this.site.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
    }

    public void startCrawl(int ThreadNum, int crawlType) {
        // 赋值
        serviceContextEvaluation();
        serviceContext.setCrawlType(crawlType);
        //设置附件下载
        serviceContext.setSaveFile(true);
        // 保存日志
        saveCrawlLog(serviceContext);
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

    public Request getPost(int pageno) {
        listUrl = "http://shwj.jingmen.gov.cn/module/web/jpage/dataproxy.jsp?startrecord=1&endrecord=90&perpage=15";
        Request request = null;
        request = new Request(listUrl.replace("startrecord=1", "startrecord=" + ((pageno - 1) * 45 + 1)));
        request.setMethod(HttpConstant.Method.POST);
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("col", "1");
            params.put("webid", "6");
            params.put("path", "/");
            params.put("columnid", "1438");
            params.put("sourceContentType", "1");
            params.put("unitid", "1646");
            params.put("webname", "荆门市商务局");
            params.put("permissiontype", "0");
            request.setRequestBody(HttpRequestBody.form(params, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return request;
    }

    public void process(Page page) {
        String url = page.getUrl().toString();
        try {
            // HtmlPage htmlPage = client.getPage(url);
            List<BranchNew> detailList = new ArrayList<BranchNew>();
            Thread.sleep(2000);
            // 判断是否是翻页连接
            if (!url.contains("/art/")) {
                String cont = page.getRawText().substring(page.getRawText().indexOf("<datastore"), page.getRawText().lastIndexOf("tore>") + 5);
                cont = cont.substring(cont.indexOf("<li"), cont.lastIndexOf("li") + 3);
                cont = cont.replace("]]></record><record><![CDATA[", "");
                cont = "<ul>" + cont + "</ul>";
                Document document = Jsoup.parse(cont.replace("&nbsp;", "").replace("&amp;", "&").replace("&ensp;", "").replace("<![CDATA[", "").replace("]]>", "").replace("&lt;", "<").replace("&gt;", ">").replace("&#39;", "'"));
                Elements lis = document.select("ul").first().select("li:has(a)");
                if (lis.size() > 0) {
                    for (int i = 0; i < lis.size(); i++) {
                        Element li = lis.get(i);
                        Element a = li.select("a").first();
                        String title = "";
                        if (a.hasAttr("title")) {
                            title = a.attr("title").trim();
                        } else {
                            title = a.text().trim();
                        }
                        title = title.replace("...", "");
                        if (!CheckProclamationUtil.isProclamationValuable(title)) {
                            continue;
                        }
                        String href = a.attr("href").trim();
                        String link = "";
                        if (href.contains("http")) {
                            link = href;
                            href = href.substring(href.indexOf("?") + 1, href.length());
                        } else {
                            link = baseUrl + href;
                        }
                        String id = href;
                        String detailLink = link;
                        String date = li.text().trim().replaceAll("[.|/|年|月]", "-");
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Matcher m = p.matcher(date);
                        if (m.find()) {
                            date = sdf.format(sdf.parse(m.group()));
                        }
                        dealWithNullTitleOrNullId(serviceContext, title, id);
                        BranchNew branch = new BranchNew();
                        branch.setTitle(title);
                        branch.setId(id);
                        branch.setDetailLink(detailLink);
                        branch.setLink(link);
                        branch.setDate(date);
                        detailList.add(branch);
                    }
                    // 校验数据,判断是否需要继续触发爬虫
                    List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                    for (BranchNew branchNew : needCrawlList) {
                        map.put(branchNew.getLink(), branchNew);
                        page.addTargetRequest(branchNew.getLink());
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
                if (lis.size() == 45 && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    page.addTargetRequest(getPost(serviceContext.getPageNum()));
                }
            } else {
                // 列表页请求
                BranchNew branchNew = map.get(url);
                if (branchNew == null) {
                    return;
                }
                String title = Jsoup.parse(branchNew.getTitle()).text();
                String id = branchNew.getId();
                String date = branchNew.getDate();
                String detailLink = branchNew.getDetailLink();
                String detailTitle = title;
                String content = "";
                String detailContent = page.getRawText();
                Document document = Jsoup.parse(page.getRawText().replace("&nbsp;", "").replace("&amp;", "&").replace("&ensp;", "").replace("<![CDATA[", "").replace("]]>", "").replace("&lt;", "<").replace("&gt;", ">"));
                if (document.select(".con-title").first() == null) {
                    detailTitle = title.trim().replace(" ", "").replace("...", "");
                } else {
                    detailTitle = document.select(".con-title").first().text().trim().replace(" ", "").replace("...", "");
                    title = detailTitle;
                }
                Element contentE = document.select(".main-txt").first();
                contentE.select("*[style~=^.*display\\s*:\\s*none\\s*(;\\s*[0-9A-Za-z]+|;\\s*)?$]").remove();
                contentE.select("script").remove();
                contentE.select("style").remove();
                contentE.select("iframe").remove();
                if (contentE.select("a") != null) {
                    Elements as = contentE.select("a");
                    for (Element a : as) {
                        String href = a.attr("href");
                        if (!href.contains("@") && !"".equals(href) && !href.contains("javascript") && !href.contains("http") && !href.contains("#")) {
                            if (href.contains("./")) {
                                href = baseUrl + "/" + href.substring(href.lastIndexOf("./") + 1, href.length());
                                a.attr("href", href);
                            } else if (href.startsWith("/")) {
                                href = baseUrl + href;
                                a.attr("href", href);
                            } else {
                                href = baseUrl + "/" + href;
                                a.attr("href", href);
                            }
                        } else if (href.contains("#") && a.attr("onclick").length() > 3) {
                            String ur = a.attr("onclick");
                            ur = ur.substring(ur.lastIndexOf("http"), ur.lastIndexOf("'"));
                            a.removeAttr("onclick");
                            a.attr("href", ur);
                        }
                    }
                }
                if (contentE.select("img").first() != null) {
                    Elements imgs = contentE.select("img");
                    for (Element img : imgs) {
                        String src = img.attr("src");
                        if (!src.contains("javascript") && !"".equals(src) && !src.contains("http")) {
                            if (src.contains("./")) {
                                src = baseUrl + "/" + src.substring(src.lastIndexOf("./") + 1, src.length());
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
                if (contentE.select("a[href*=javascript]").first() != null) {
                    Elements as = contentE.select("a[href*=javascript]");
                    for (Element a : as) {
                        a.removeAttr("href");
                    }
                }
                if (contentE.select("a[href*=#]").first() != null) {
                    Elements as = contentE.select("a[href*=#]");
                    for (Element a : as) {
                        a.removeAttr("href");
                    }
                }
                content = "<div>" + title + "</div><br>" + contentE.outerHtml();
                detailContent = detailContent != null ? detailContent : Jsoup.parse(content).html();
                RecordVO recordVO = new RecordVO();
                recordVO.setId(id);
                recordVO.setListTitle(title);
                recordVO.setDate(date);
                recordVO.setContent(content);
                recordVO.setTitle(detailTitle);//详情页标题
                //recordVO.setDdid(SpecialUtil.stringMd5(detailContent));//详情页md5
                recordVO.setDetailLink(detailLink);//详情页链接
                recordVO.setDetailHtml(detailContent);
                dataStorage(serviceContext, recordVO, branchNew.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }


}
