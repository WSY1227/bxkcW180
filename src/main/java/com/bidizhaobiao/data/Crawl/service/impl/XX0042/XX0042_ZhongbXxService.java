package com.bidizhaobiao.data.Crawl.service.impl.XX0042;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
import com.bidizhaobiao.data.Crawl.utils.CheckProclamationUtil;
import com.bidizhaobiao.data.Crawl.utils.SpecialUtil;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
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
 * 程序员：白嘉全
 * 日期：2020-12-02
 * 原网站：http://www.bdu.edu.cn/syxw/tzgg.htm
 * 主页：http://www.bdu.edu.cn
 */

@Service
public class XX0042_ZhongbXxService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://www.bdu.edu.cn/syxw/tzgg.htm";

    // 网站名
    public Pattern p = Pattern.compile("(\\d{4})(年|/|-)(\\d{1,2})(月|/|-)(\\d{1,2})");
    public Pattern p_p = Pattern.compile("view\\('(.*?)','(.*?)','(.*?)'\\)");
    public Pattern p_t = Pattern.compile("共有(\\d+)条信息");
    // 网站编号
    public String sourceNum = "XX0042";
    // 网站名称
    public String sourceName = "保定学院";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华北";
    // 设置省份
    public String province = "河北";
    // 设置城市
    public String city = "保定市";
    // 设置县
    public String district = "莲池区";
    // 设置县
    public String createBy = "白嘉全";
    //站源类型
    public String taskType;
    //站源名称
    public String taskName;
    public double priod = 4;
    //是否需要入广联达
    public boolean isNeedInsertGonggxinxi = false;
    public String baiDu = "https://www.baidu.com/?wd=";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20).setCharset("UTF-8");

    public Site getSite() {
        return this.site.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
    }

    public void startCrawl(int threadNum, int crawlType) {
        // 赋值
        serviceContextEvaluation();
        // 保存日志
        serviceContext.setCrawlType(crawlType);
        saveCrawlLog(serviceContext);
        // 启动爬虫
        spider = Spider.create(this).thread(threadNum)
                .setDownloader(new MyDownloader(serviceContext, true, listUrl));
        Request request = new Request(baiDu + listUrl);
        spider.addRequest(request);
        serviceContext.setSpider(spider);
        spider.run();
        // 爬虫状态监控部分
        saveCrawlResult(serviceContext);
    }

    public String getListContent(String path) {
        String listContent = "";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        try {
            httpClient = getHttpClient(true, true);
            HttpGet httpGet = new HttpGet(path);
            httpGet.addHeader("Connection", "close");
            RequestConfig config = RequestConfig.custom().setConnectTimeout(10000).setConnectionRequestTimeout(10000).setSocketTimeout(10000).build();
            httpGet.setConfig(config);
            response = httpClient.execute(httpGet);
            response.addHeader("Connection", "close");
            if (response.getStatusLine().getStatusCode() == 200) {
                listContent = EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return listContent;
    }


    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        try {
            Thread.sleep(1000);
            List<BranchNew> detailList = new ArrayList<BranchNew>();
            url = url.substring(url.indexOf("wd=") + 3);
            String deTail = null;
            for (int i = 0; i < 3; i++) {
                deTail = getListContent(url);
                if (!deTail.equals("")) {
                    break;
                }
            }
            if (!url.contains("info")) {
                Document document = Jsoup.parse(deTail);
                Elements lis = document.select("li[id^=line_u8_]");
                if (lis.size() > 0) {
                    for (Element li : lis) {
                        Element a = li.select("a").first();
                        String title = a.text().trim();
                        if (title.equals("")) {
                            title = "公示公告";
                        }
                        if (!title.contains("...") && !CheckProclamationUtil.isProclamationValuable(title)) {
                            continue;
                        }
                        String href = a.attr("href");
                        if (!href.contains("info")) {
                            continue;
                        }
                        String id = "/" + href.substring(href.indexOf("info"));
                        String link = "http://www.bdu.edu.cn" + id;
                        String detailLink = link;
                        String date = li.select("i").first().text().trim().replace("[", "").replace("]", "");
                        BranchNew branch = new BranchNew();
                        branch.setId(id);
                        branch.setLink(link);
                        branch.setDetailLink(detailLink);
                        branch.setTitle(title);
                        branch.setDate(date);
                        detailList.add(branch);
                    }
                    List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                    for (BranchNew branch : needCrawlList) {
                        map.put(branch.getLink(), branch);
                        page.addTargetRequest(baiDu + branch.getLink());
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
                Element ele = document.select("a:contains(下页)").first();
                if (ele != null && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    String href = ele.attr("href");
                    if (href.contains("/")) {
                        href = href.substring(href.indexOf("/") + 1);
                    }
                    String nextPage = "https://www.bdu.edu.cn/syxw/tzgg/" + href;
                    page.addTargetRequest(baiDu + nextPage);
                }
            } else {
                String baseUrl = "http://www.bdu.edu.cn";
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
                String detailContent = deTail;
                Document document = Jsoup.parse(deTail);
                if (document.select("div.content-title.fl h3").first() == null) {
                    return;
                }
                detailTitle = document.select("div.content-title.fl h3").first().text().trim().replace(" ", "").replace("...", "");
                if (detailTitle.equals("")) {
                    detailTitle = "公示公告";
                }
                if (!CheckProclamationUtil.isProclamationValuable(detailTitle)) {
                    return;
                }
                Element contentE = document.select("div.content").first();
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
        contentE.select("div.content-title.fl i").remove();
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
