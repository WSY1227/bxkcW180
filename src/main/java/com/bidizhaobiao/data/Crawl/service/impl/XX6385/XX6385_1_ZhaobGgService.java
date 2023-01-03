package com.bidizhaobiao.data.Crawl.service.impl.XX6385;

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

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 程序员：郭建婷 日期：2021-11-03
 * 原网站：http://www.fsswdx.gov.cn/xxgk/cgxx/
 * 主页：http://www.fsswdx.gov.cn
 **/

@Service
public class XX6385_1_ZhaobGgService extends SpiderService implements PageProcessor {
    public Spider spider = null;
    public String listUrl = "http://www.fsswdx.gov.cn/xxgk/cgxx/index.html";
    public String homeUrl = "http://www.fsswdx.gov.cn";
    public Pattern datePat = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");
    // 网站编号
    public String sourceNum = "XX6385-1";
    // 网站名称
    public String sourceName = "中共佛山市委党校";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华南";
    // 设置省份
    public String province = "广东";
    // 设置城市
    public String city = "佛山市";
    // 设置县
    public String district;
    // 设置CreateBy
    public String createBy = "郭建婷";
    public String nextHref = "";
    //附件
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20).setCharset("UTF-8");

    public Site getSite() {
        return this.site.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
    }

    @Override
    public void startCrawl(int threadNum, int crawlType) {
        // 赋值
        serviceContextEvaluation();
        // 保存日志
        serviceContext.setCrawlType(crawlType);
        saveCrawlLog(serviceContext);
        // 启动爬虫
        spider = Spider.create(this).thread(threadNum)
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
            if (url.contains("index")) {
                Document doc = Jsoup.parse(page.getRawText());
                Elements listElement = doc.select("div.list>ul>li");
                if (listElement.size() > 0) {
                    for (Element element : listElement) {
                        Element a = element.select("a").first();
                        String link = a.attr("href").trim();
                        String id = link.substring(link.lastIndexOf("/") + 1);
                        if (link.startsWith("/")) {
                            link = homeUrl + link;
                        } else if (link.contains("http") && !link.contains(homeUrl.replaceAll("https|http", ""))) {
                            continue;
                        } else if (link.contains("./")) {
                            link = homeUrl + link.substring(link.lastIndexOf("./") + 1);
                        } else if (!link.contains("http") && !link.startsWith("/")) {
                            link = homeUrl + "/" + link;
                        }
                        String detailLink = link;
                        String date = "";
                        Matcher dateMat = datePat.matcher(element.select("span").text());
                        if (dateMat.find()) {
                            date = dateMat.group(1);
                            date += dateMat.group(3).length() == 2 ? "-" + dateMat.group(3) : "-0" + dateMat.group(3);
                            date += dateMat.group(5).length() == 2 ? "-" + dateMat.group(5) : "-0" + dateMat.group(5);
                        }
                        String title = a.attr("title").trim();
                        if (title.length() < 2) title = a.text().trim();
                        if (!title.contains("...") && !CheckProclamationUtil.isProclamationValuable(title, null)) {
                            continue;
                        }
                        BranchNew branch = new BranchNew();
                        branch.setId(id);
                        serviceContext.setCurrentRecord(branch.getId());
                        branch.setLink(link);
                        branch.setDetailLink(detailLink);
                        branch.setDate(date);
                        branch.setTitle(title);
                        detailList.add(branch);
                    }
                    List<BranchNew> branchNewList = checkData(detailList, serviceContext);
                    for (BranchNew branch : branchNewList) {
                        map.put(branch.getLink(), branch);
                        page.addTargetRequest(new Request(branch.getLink()));
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
                if (serviceContext.getPageNum() == 1) {
                    Matcher pageMat = Pattern.compile("id='CP' size='(\\d+)'").matcher(page.getRawText());
                    if (pageMat.find()) {
                        int maxPage = Integer.parseInt(pageMat.group(1));
                        serviceContext.setMaxPage(maxPage);
                    }
                }
                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    page.addTargetRequest(listUrl.replace("index", "index_" + serviceContext.getPageNum()));
                }
            } else {
                BranchNew branch = map.get(url);
                if (branch != null) {
                    map.remove(url);
                    serviceContext.setCurrentRecord(branch.getId());
                    String detailHtml = page.getRawText();
                    Document doc = Jsoup.parse(detailHtml);
                    String title = branch.getTitle().trim().replace("...", "");
                    String date = branch.getDate();
                    String content = "";
                    Element contentElement = doc.select("div.text").first();
                    if (contentElement != null) {
                        saveFile(contentElement, url, date);
                        contentElement.select("h2").first().remove();
                        contentElement.select("script").remove();
                        contentElement.select("style").remove();
                        contentElement.select("*[style~=^.*display\\s*:\\s*none\\s*(;\\s*[0-9A-Za-z]+|;\\s*)?$]").remove();
                        content = "<div>" + title + "</div>" + contentElement.outerHtml();
                    } else if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip") || url.contains(".xls") || url.contains(".xlsx")) {
                        content = "<div>附件下载：<a href='" + url + "'>" + branch.getTitle() + "</a></div>";
                        detailHtml = Jsoup.parse(content).toString();
                    }
                    content = content.replaceAll("\\ufeff|\\u2002|\\u200b|\\u2003", "");
                    RecordVO recordVO = new RecordVO();
                    recordVO.setId(branch.getId());
                    recordVO.setListTitle(branch.getTitle());
                    recordVO.setTitle(title);
                    recordVO.setDetailLink(branch.getDetailLink());
                    recordVO.setDetailHtml(detailHtml);
                    recordVO.setDdid(SpecialUtil.stringMd5(detailHtml));
                    recordVO.setDate(date);
                    recordVO.setContent(content);
                    dataStorage(serviceContext, recordVO, branch.getType());
                }
            }
        } catch (Exception e) {
            dealWithError(url, serviceContext, e);
        }
    }

    public void saveFile(Element contentElement, String url, String date) {
        Elements iframes = contentElement.select("iframe");
        for (Element iframe : iframes) {
            String src = iframe.attr("src").trim();
            String a = "<a href=\"" + src + "\">" + "详见附件</a>";
            iframe.after(a);
            iframe.remove();
        }
        Elements aList = contentElement.select("a");
        for (Element a : aList) {
            String href = a.attr("href");
            a.attr("rel", "noreferrer");
            if (href.startsWith("mailto")) {
                continue;
            }
            if (a.hasAttr("data-download")) {
                href = a.attr("data-download");
                a.attr("href", href);
                a.removeAttr("data-download");
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
                if (href.indexOf("//www") == 0) {
                    href = homeUrl.substring(0, homeUrl.indexOf(":") + 1) + href;
                    a.attr("href", href);
                } else if (href.indexOf("../") == 0) {
                    href = href.replace("../", "");
                    href = homeUrl + "/" + href;
                    a.attr("href", href);
                } else if (href.startsWith("/")) {
                    href = homeUrl + href;
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
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
                    String dateString = formatter.format(new Date());
                    String path = imgPath + "/" + dateString + "/" + date + "/" + sourceNum;
                    String uuid = UUID.randomUUID().toString();
                    String fileName = uuid + ".jpg";
                    String newLink = "http://www.bidizhaobiao.com/file/" + dateString + "/" + date
                            + "/" + sourceNum + "/" + fileName;
                    // 文件保存位置
                    File saveDir = new File(path);
                    if (!saveDir.exists()) {
                        saveDir.mkdirs();
                    }
                    byte[] imagedata = DatatypeConverter
                            .parseBase64Binary(src.substring(src.indexOf(",") + 1));
                    BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imagedata));
                    ImageIO.write(bufferedImage, "png", new File(path + "/" + fileName));
                    img.attr("src", newLink);
                } catch (Exception e) {
                    img.remove();
                }
                continue;
            }
            if (src.length() > 10 && src.indexOf("http") != 0) {
                if (src.indexOf("../") == 0) {
                    src = src.replace("../", "");
                    src = homeUrl + "/" + src;
                    img.attr("src", src);
                } else if (src.indexOf("./") == 0) {
                    src = url.substring(0, url.lastIndexOf("/")) + src.substring(src.lastIndexOf("./") + 1);
                    img.attr("src", src);
                } else if (src.startsWith("//")) {
                    src = homeUrl.substring(0, homeUrl.indexOf(":") + 1) + src;
                    img.attr("src", src);
                } else if (src.indexOf("/") == 0) {
                    src = homeUrl + src;
                    img.attr("src", src);
                } else {
                    src = url.substring(0, url.lastIndexOf("/") + 1) + src;
                    img.attr("src", src);
                }
            }
        }
    }


}
