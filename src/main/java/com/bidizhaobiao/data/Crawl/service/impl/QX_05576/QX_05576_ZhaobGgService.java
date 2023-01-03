package com.bidizhaobiao.data.Crawl.service.impl.QX_05576;

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
 * @author 作者: 何子杰
 * @version 创建时间：2021-01-29
 */
@Service("QX_05576_ZhaobGgService")
public class QX_05576_ZhaobGgService extends SpiderService implements PageProcessor {
    public  Spider spider = null;
    public  int successNum = 0;
    public  String listUrl = "http://zrzy.jiangsu.gov.cn/wx/lxfj/tzgg/index.htm";

    // 设置县
    public  Pattern p = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");
    // 设置地区
    public String area = "华东";
    // 设置省份
    public String province = "江苏";
    // 设置城市
    public String city = "无锡市";
    // 设置县
    public String district = "梁溪区";
    public String createBy = "何子杰";
    // 网站编号
    protected String sourceNum = "05576";
    // 网站名称
    protected String sourceName = "无锡市国土资源局梁溪分局";
    // 过时时间分割点
    protected String SplitPointStr = "2016-01-01";
    // 信息源
    protected String infoSource = "政府采购";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);
    public  double priod = 2;

    public Site getSite() {
        return this.site;
    }

    public void startCrawl(int ThreadNum, int crawlType) {
        // 赋值
        serviceContextEvaluation();        // 保存日志
        serviceContext.setCrawlType(crawlType);
        saveCrawlLog(serviceContext);
        // 启动爬虫
        spider = Spider.create(this).thread(ThreadNum)
                .setDownloader(new MyDownloader(serviceContext, false, listUrl));
        Request request = new Request(listUrl);
        spider.addRequest(request);
        serviceContext.setSpider(spider);
        spider.run();
        serviceContext.setSpider(spider);
        // 爬虫状态监控部分
        saveCrawlResult(serviceContext);
    }

    public void process(Page page) {
        String url = page.getUrl().toString();
        try {
            // HtmlPage htmlPage = client.getPage(url);
            List<BranchNew> detailList = new ArrayList<BranchNew>();
            Thread.sleep(1000);
            // 判断是否是翻页连接
            if (url.contains("index")) {
                Document document = Jsoup.parse(page.getRawText().replace("&nbsp;", " "));
                if (serviceContext.getPageNum() == 1) {
                    String pageCount = page.getRawText();
                    String pageReg = "createPageHTML\\((\\d{1,5}),";
                    Pattern pagePa = Pattern.compile(pageReg);
                    Matcher pageMa = pagePa.matcher(pageCount);
                    if (pageMa.find()) {
                        pageCount = pageMa.group(1);
                        int maxPage = Integer.valueOf(pageCount);
                        // System.out.println(maxPage);
                        serviceContext.setMaxPage(maxPage);
                    }
                }
                Elements trs = document.select("table[width=93%]").first().select("tr");
                if (trs.size() > 0) {
                    for (Element tr : trs) {
                        Element a = tr.select("a").first();
                        String title = a.attr("title").trim().replace(" ", "");
                        String s = "招标、采购、询价、询比、竞标、竞价、竞谈、竞拍、竞卖、竞买、竞投、竞租、比选、比价、竞争性、谈判、磋商、投标、邀标、议标、议价、单一来源、遴选、标段、明标、明投、出让、转让、拍卖、招租、预审、发包、开标、答疑、补遗、澄清、挂牌";
                        String[] rules = s.split("、");
                        if (!CheckProclamationUtil.isProclamationValuable(title, rules)) {
                            continue;
                        }
                        String href = a.attr("href");
                        String id = href.substring(href.indexOf("/"));
                        String link = "http://zrzy.jiangsu.gov.cn/wx/lxfj/tzgg" + id;
                        String date = tr.select("td").last().text().trim();
                        dealWithNullTitleOrNullId(serviceContext, title, id);
                        BranchNew branch = new BranchNew();
                        branch.setId(id);
                        serviceContext.setCurrentRecord(branch.getId());
                        branch.setTitle(title);
                        branch.setLink(link);
                        branch.setDate(date);
                        detailList.add(branch);
                    }
                    // 校验数据List<BranchNew> detailList,int pageNum,String
                    // sourceNum
                    // 校验数据,判断是否需要继续触发爬虫
                    List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                    for (BranchNew branch : needCrawlList) {
                        map.put(branch.getLink(), branch);
                        page.addTargetRequest(branch.getLink());
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    page.addTargetRequest(listUrl.replace("index", "index_" + (serviceContext.getPageNum() - 1)));
                }
            } else {
                String baseUrl = "http://zrzy.jiangsu.gov.cn";
                BranchNew branchNew = map.get(url);
                String id = branchNew.getId();
                String title = branchNew.getTitle();
                serviceContext.setCurrentRecord(id);
                String date = branchNew.getDate();
                Document document = Jsoup.parse(page.getRawText().replace("&nbsp;", " ").replace("amp;", ""));
                Element contentE = document.select("div.TRS_Editor").first();
                setUrl(contentE, baseUrl, url);
                String content = contentE.outerHtml();
                RecordVO recordVo = new RecordVO();
                        recordVo.setDetailLink(url);
                recordVo.setTitle(title);
                recordVo.setContent(content);
                recordVo.setId(id);
                recordVo.setDate(date);
                // 入库操作（包括数据校验和入库）
                dataStorage(serviceContext, recordVo, branchNew.getType());
                //
            }
        } catch (Exception e) {
            // e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }

    public void setUrl(Element conE, String baseUrl, String url) {
        Element contentE = conE;
        contentE.select("input").remove();
        contentE.select("*[style~=^.*display\\s*:\\s*none\\s*(;\\s*[0-9A-Za-z]+|;\\s*)?$]").remove();
        contentE.select("script").remove();
        contentE.select("style").remove();
        if (contentE.select("a") != null) {
            Elements as = contentE.select("a");
            for (Element a : as) {
                String href = a.attr("href");
                if (!href.contains("@") && !"".equals(href) && !href.contains("javascript") && !href.contains("http")) {
                    href = baseUrl + href;
                    a.attr("href", href);
                }
            }
        }
        if (contentE.select("img").first() != null) {
            Elements imgs = contentE.select("img");
            for (Element img : imgs) {
                String src = img.attr("src");
                if (!src.contains("javascript") && !"".equals(src) && !src.contains("http")) {
                    src = baseUrl + src;
                    img.attr("src", src);
                }
            }
        }
    }
}
