package com.bidizhaobiao.data.Crawl.service.impl.DX011472;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
import com.bidizhaobiao.data.Crawl.utils.CheckProclamationUtil;
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
 * 程序员：张景腾 日期：2022-01-17
 * 原网站：http://www.hnxcdb.com/protectRule.html?GUID=3BBBC143-C2EE-4B1E-9EAA-C261A09D0A0B
 * 主页：http://www.hnxcdb.com
 **/
@Service
public class DX011472_ZhaobGgService extends SpiderService implements PageProcessor {
    public Spider spider = null;

    public String listUrl = "http://www.hnxcdb.com/HT/API/HOME/WZLISTA?GUID=3BBBC143-C2EE-4B1E-9EAA-C261A09D0A0B&INDEX=1&NUM=6";
    public String baseUrl = "http://www.hnxcdb.com";
    public Pattern datePat = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");

    // 网站编号
    public String sourceNum = "DX011472";
    // 网站名称
    public String sourceName = "湖南省湘诚融资担保有限责任公司";
    // 信息源
    public String infoSource = "企业采购";
    // 设置地区
    public String area = "华中";
    // 设置省份
    public String province = "湖南";
    // 设置城市
    public String city;
    // 设置县
    public String district;
    public String createBy = "张景腾";
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
            if (url.contains("INDEX")) {
                JSONObject jsonObject = new JSONObject(page.getRawText());
                if (serviceContext.getPageNum() == 1) {
                    int count = jsonObject.getInt("COUNT");
                    serviceContext.setMaxPage(count % 6 == 0 ? count / 6 : count / 6 + 1);
                }
                JSONArray datas = jsonObject.getJSONArray("DB");
                if (datas.length() > 0) {
                    String key = "征集、招标、采购、询价、询比、竞标、竞价、竞谈、竞拍、竞卖、竞买、竞投、竞租、比选、比价、竞争性、谈判、磋商、投标、邀标、议标、议价、单一来源、遴选、标段、明标、明投、出让、转让、拍卖、招租、预审、发包、开标、答疑、补遗、澄清、挂牌";
                    String[] keys = key.split("、");
                    for (int i = 0; i < datas.length(); i++) {
                        JSONObject data = datas.getJSONObject(i);
                        String title = data.getString("NAME");
                        if (!CheckProclamationUtil.isProclamationValuable(title, keys)) {
                            continue;
                        }
                        String createtime = data.getString("CREATETIME");
                        String date = "";
                        Matcher dateMat = datePat.matcher(createtime);
                        if (dateMat.find()) {
                            date = dateMat.group(1);
                            date += dateMat.group(3).length() == 2 ? "-" + dateMat.group(3) : "-0" + dateMat.group(3);
                            date += dateMat.group(5).length() == 2 ? "-" + dateMat.group(5) : "-0" + dateMat.group(5);
                        }
                        String id = data.getString("GUID");
                        if (id.contains("{")) {
                            id = id.replace("{", "%7B").replace("}", "%7D");
                        }
                        String link = "http://www.hnxcdb.com/deTails.html?GUID=" + id;
                        String detailLink = link;
                        String content = data.getString("TEXT");
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
                        map.put(branch.getLink(), branch);
                        page.addTargetRequest(new Request(branch.getLink()));
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    String href = listUrl.replace("INDEX=1", "INDEX=" + serviceContext.getPageNum());
                    page.addTargetRequest(href);
                }
            } else {
                BranchNew branch = map.get(url);
                if (branch != null) {
                    map.remove(url);
                    serviceContext.setCurrentRecord(branch.getId());
                    String html = branch.getContent();
                    Document contentElement = Jsoup.parse(html);
                    String title = branch.getTitle().replace("...", "");
                    String date = branch.getDate();
                    String content = "";
                    if (contentElement != null) {
                        Elements aList = contentElement.select("a");
                        for (Element a : aList) {
                            String href = a.attr("href");
                            a.attr("rel", "noreferrer");
                            if (href.startsWith("mailto")) {
                                continue;
                            }
                            if (href.contains("20200930101347694769.doc")) {
                                a.remove();
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
                    } else if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip") || url.contains(".xls")) {
                        content = "<div>附件下载：<a href='" + url + "'>" + branch.getTitle() + "</a></div>";
                        html = Jsoup.parse(content).toString();
                    }
                    RecordVO recordVO = new RecordVO();
                    recordVO.setId(branch.getId());
                    recordVO.setListTitle(branch.getTitle());
                    recordVO.setTitle(title);
                    recordVO.setDetailLink(branch.getDetailLink());
                    recordVO.setDetailHtml(html);
                    recordVO.setDdid(SpecialUtil.stringMd5(html));
                    recordVO.setDate(date);
                    recordVO.setContent(content.replaceAll("\\ufeff|\\u2002|\\u200b|\\u2003", ""));
                    dataStorage(serviceContext, recordVO, branch.getType());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }


}
