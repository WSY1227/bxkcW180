package com.bidizhaobiao.data.Crawl.service.impl.DS_10128;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
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
 * 程序员：刘伟伟  日期：2021-04-29
 * 原网站：http://scjinkaijia.com/GongGaoList/index/49
 * 主页：http://scjinkaijia.com
 **/

@Service("DS_10128_1_ZhaobGgService")
public class DS_10128_1_ZhaobGgService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://scjinkaijia.com/GongGaoList/index/49";

    public String baseUrl = "http://scjinkaijia.com";

    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(3).setTimeOut(30000).setSleepTime(20);
    // 网站编号
    public String sourceNum = "10128-1";
    // 网站名称
    public String sourceName = "四川省金凯佳工程项目管理有限公司";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "西南";
    // 设置省份
    public String province = "四川";
    // 设置城市
    public String city = "甘孜藏族自治州";
    // 设置县
    public String district;
    // 设置CreateBy
    public String createBy = "刘伟伟";

    public Pattern p = Pattern.compile("(\\d{4})(年|/|-)(\\d{1,2})(月|/|-)(\\d{1,2})");

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
        Request request = new Request(listUrl);
        spider.addRequest(request);
        serviceContext.setSpider(spider);
        spider.run();
        // 爬虫状态监控部分
        saveCrawlResult(serviceContext);
    }

    public void process(Page page) {
        String html = page.getHtml().toString();
        String url = page.getUrl().toString();
        try {
            if (url.equals(listUrl)) {
                Document document = Jsoup.parse(html);
                Elements lis = document.select("table.table1 tbody").select("tr:has(a)");
                List<BranchNew> detailList = new ArrayList<BranchNew>();
                if (lis.size() > 0) {
                    for (int i = 0; i < lis.size(); i++) {
                        Element li = lis.get(i);
                        Element a = li.select("a").first();
                        String title = "";
                        if (a.hasAttr("title")) {
                            title = a.attr("title").trim();
                            if (title.equals("")) {
                                title = a.text().trim();
                            }
                        } else {
                            title = a.text().trim();
                        }
                        String href = a.attr("href").trim();
                        String id = href;
                        String link = "http://scjinkaijia.com/" + href;
                        String date = li.text().replaceAll("[.|/|年|月]", "-");
                        Matcher m = p.matcher(date);
                        if (m.find()) {
                            date = SpecialUtil.date2Str(SpecialUtil.str2Date(m.group()));
                        }
                        dealWithNullTitleOrNullId(serviceContext, title, id);
                        BranchNew branch = new BranchNew();
                        branch.setTitle(title);
                        branch.setId(id);
                        serviceContext.setCurrentRecord(id);
                        branch.setDetailLink(link);
                        branch.setLink(link);
                        branch.setDate(date);
                        detailList.add(branch);
                    }
                    // 校验数据,判断是否需要继续触发爬虫
                    List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                    for (BranchNew branch : needCrawlList) {
                        map.put(branch.getLink(), branch);
                        page.addTargetRequest(branch.getLink());
                    }
                } else {
                    dealWithNullListPage(serviceContext);
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
                Document document = Jsoup.parse(html);
                Element contentE = document.select("div.newscontent").first();
                contentE.select("iframe").remove();
                contentE.select("input").remove();
                contentE.select("button").remove();
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
                    }
                }
                if (contentE.select("img").first() != null) {
                    Elements imgs = contentE.select("img");
                    for (Element img : imgs) {
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
                        if (!src.contains("javascript") && !"".equals(src) && !src.contains("http")) {
                            if (src.contains("../")) {
                                src = baseUrl + "/" + src.substring(src.lastIndexOf("./") + 1, src.length());
                                img.attr("src", src);
                            } else if (src.startsWith("./")) {
                                src = url.substring(0, url.lastIndexOf("/")) + src.replace("./", "/");
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
                content = "<div>" + title + "</div>" + contentE.outerHtml();
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
