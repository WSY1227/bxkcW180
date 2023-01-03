package com.bidizhaobiao.data.Crawl.service.impl.DX011440;

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
 * @程序员: 潘嘉明 日期：2022-01-17 13:38
 * @原网站: http://www.ahhldl.com/material/materialList/index/53.html
 * @主页：TODO
 **/
@Service("DX011440_ZhaobGgService")
public class DX011440_ZhaobGgService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://www.ahhldl.com/api/content-list?cid=53&size=5&simple=false&is_top=0&is_comm=0&page=1";

    public String baseUrl = "http://www.ahhldl.com";
    // 网站编号
    public String sourceNum = "DX011440";
    // 网站名称
    public String sourceName = "安徽华菱电缆集团有限公司";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华东";
    // 设置省份
    public String province = "安徽";
    // 设置城市
    public String city = "";
    // 设置县
    public String district = "";
    // 设置CreateBy
    public String createBy = "潘嘉明";

    //public boolean isNeedSaveFileAddSSL = true;

    public Pattern p = Pattern.compile("(?<year>\\d{4})(\\.|年|/|-)(\\d{1,2})(\\.|月|/|-)(\\d{1,2})");

    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(3).setTimeOut(30000).setSleepTime(20);

    public Site getSite() {
        return this.site
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36");
    }

    @Override
    public void startCrawl(int threadNum, int crawlType) {
        // 赋值
        serviceContextEvaluation();
        serviceContext.setCrawlType(crawlType);
        // 保存日志
        saveCrawlLog(serviceContext);
        // 启动爬虫
        spider = Spider.create(this).thread(threadNum)
                .setDownloader(new MyDownloader(serviceContext, false, listUrl));
        Request request = new Request(listUrl);
        spider.addRequest(request);
        serviceContext.setSpider(spider);
        spider.run();
        // 爬虫状态监控部分
        saveCrawlResult(serviceContext);
    }

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        String html = page.getRawText();
        try {
            if (url.contains("page")) {
                JSONObject root = new JSONObject(html);
                if (serviceContext.getPageNum() == 1) {
                    //String total = root.getJSONObject("result").get("total").toString();
                    int max = root.getInt("last_page");//Integer.parseInt(total);
                    //max = max % 16 == 0 ? max / 16 : max / 16 + 1;
                    serviceContext.setMaxPage(max);
                }
                JSONArray list = root.getJSONArray("data");
                List<BranchNew> detailList = new ArrayList<BranchNew>();
                if (list.length() > 0) {
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject item = list.getJSONObject(i);
                        String title = item.get("title").toString();

                        String id = item.get("article_id").toString();
                        String content = item.get("content").toString();
                        if ("".equals(content)) continue;
                        String link = "http://www.ahhldl.com/article-" + id;
                        String detailLink = link;
                        String date = item.get("publish_time").toString();
                        Matcher matcher = p.matcher(date);
                        if (matcher.find()) {
                            date = matcher.group().replaceAll("[/|年|月|\\.]", "-");
                            date = SpecialUtil.date2Str(SpecialUtil.str2Date(date));
                        }
                        dealWithNullTitleOrNullId(serviceContext, title, id);
                        BranchNew branch = new BranchNew();
                        branch.setId(id);
                        branch.setTitle(title);
                        branch.setContent(content);
                        serviceContext.setCurrentRecord(id);
                        branch.setDetailLink(detailLink);
                        branch.setLink(link);
                        branch.setDate(date);
                        detailList.add(branch);
                    }
                    // 校验数据,判断是否需要继续触发爬虫

                    List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                    for (BranchNew branchNew : needCrawlList) {
//                        map.put(branch.getLink(), branch);
//                        page.addTargetRequest(branch.getLink());
                        String title = Jsoup.parse(branchNew.getTitle()).text().replace("...", "");
                        String id = branchNew.getId();
                        serviceContext.setCurrentRecord(id);
                        String detailLink = branchNew.getDetailLink();
                        String detailTitle = title;
                        String date = branchNew.getDate();
                        String content = "";
                        Document document = Jsoup.parse(branchNew.getContent());
                        Element contentE = document.select("body").first();
                        if (contentE != null) {
                            contentE.select("iframe").remove();
                            contentE.select("meta").remove();
                            contentE.select("input").remove();
                            contentE.select("style").remove();
                            contentE.select("script").remove();
                            if (contentE.select("a").first() != null) {
                                Elements as = contentE.select("a");
                                for (Element a : as) {
                                    String href = a.attr("href");
                                    href = href.replace(" ", "%20");
                                    if (href.startsWith("file:")) {
                                        a.removeAttr("href");
                                        continue;
                                    }
                                    if ("".equals(href) || href == null
                                            || href.indexOf("#") == 0
                                            || href.startsWith("javascript:") || href.contains("mailto:")) {
                                        a.removeAttr("href");
                                        continue;
                                    }
                                    if (!href.contains("@") && !"".equals(href) && !href.contains("javascript") && !href.contains("http") && !href.contains("#")) {
                                        if (href.startsWith("../")) {
                                            href = baseUrl + "/" + href.substring(href.lastIndexOf("./") + 2, href.length());
                                            a.attr("href", href);
                                            a.attr("rel", "noreferrer");
                                        } else if (href.startsWith("./")) {
                                            href = url.substring(0, url.lastIndexOf("/")) + href.replace("./", "/");
                                            a.attr("href", href);
                                            a.attr("rel", "noreferrer");
                                        } else if (href.startsWith("/")) {
                                            href = baseUrl + href;
                                            a.attr("href", href);
                                            a.attr("rel", "noreferrer");
                                        } else {
                                            href = url.substring(0, url.lastIndexOf("/") + 1) + href;
                                            a.attr("href", href);
                                            a.attr("rel", "noreferrer");
                                        }
                                    }
                                    a.attr("href", href);
                                }
                            }
                            if (contentE.select("img").first() != null) {
                                Elements imgs = contentE.select("img");
                                for (Element img : imgs) {
                                    if (img.hasAttr("word_img") && img.attr("word_img").contains("file:")) {
                                        img.remove();
                                        continue;
                                    }
                                    String src = img.attr("src");
                                    if (src.contains("data:image")) {
                                        try {
                                            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
                                            String dateString = formatter.format(new Date());
                                            String path = imgPath + "/" + dateString + "/" + branchNew.getDate() + "/" + sourceNum;
                                            String uuid = UUID.randomUUID().toString();
                                            String fileName = uuid + ".jpg";
                                            String newLink = "http://www.bidizhaobiao.com/file/" + dateString + "/" + branchNew.getDate()
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
                                    if (src.startsWith("file:")) {
                                        img.remove();
                                        continue;
                                    }
                                    if (!"".equals(src) && !src.contains("#") && !src.contains("javascript:") && !src.contains("http")) {
                                        if (src.startsWith("//")) {
                                            src = "http:" + src;
                                            img.attr("src", src);
                                        } else if (src.startsWith("../")) {
                                            src = baseUrl + "/" + src.substring(src.lastIndexOf("./") + 2, src.length());
                                            img.attr("src", src);
                                        } else if (src.startsWith("/")) {
                                            src = baseUrl + src;
                                            img.attr("src", src);
                                        } else if (src.startsWith("./")) {
                                            src = url.substring(0, url.lastIndexOf("/")) + src.replace("./", "/");
                                            img.attr("src", src);
                                        } else {
                                            src = url.substring(0, url.lastIndexOf("/") + 1) + src;
                                            img.attr("src", src);
                                        }
                                    }
                                }
                            }
                            content = "<div>" + title + "</div>" + contentE.outerHtml();
                        }
                        if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip") || url.contains(".xls")) {
                            content = "<div>附件下载：<a href='" + url + "'>" + detailTitle + "</a></div>";
                            html = Jsoup.parse(content).toString();
                            date = SpecialUtil.date2Str(new Date());
                        }
                        RecordVO recordVO = new RecordVO();
                        recordVO.setId(id);
                        recordVO.setListTitle(title);
                        recordVO.setDate(date);
                        recordVO.setContent(content.replaceAll("\\ufeff|\\u2002|\\u200b|\\u2003", ""));
                        recordVO.setTitle(detailTitle);//详情页标题
                        recordVO.setDdid(SpecialUtil.stringMd5(html));//详情页md5
                        recordVO.setDetailLink(detailLink);//详情页链接
                        recordVO.setDetailHtml(html);
                        logger.info("入库id==={}", id);
                        dataStorage(serviceContext, recordVO, branchNew.getType());
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }

                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    page.addTargetRequest(listUrl.replace("page=1", "page=" + serviceContext.getPageNum()));
                }
            } else {

            }
        } catch (Exception e) {
            e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }

}
