package com.bidizhaobiao.data.Crawl.service.impl.Y03981;

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
 * 程序员：袁武  日期：2021-6-9
 * 原网站：http://hospital.whu.edu.cn/xwgg/tzgg.htm
 * 主页:http://hospital.whu.edu.cn
 **/

@Service("Y03981_ZhaobGgService")
public class Y03981_ZhaobGgService extends SpiderService implements PageProcessor {

    public Spider spider = null;

    public String listUrl = "http://hospital.whu.edu.cn/xwgg/tzgg.htm";

    public String baseUrl = "http://hospital.whu.edu.cn";

    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(3).setTimeOut(30000).setSleepTime(20);
    // 网站编号
    public String sourceNum = "Y03981";
    // 网站名称
    public String sourceName = "武汉大学医院";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华中";
    // 设置省份
    public String province = "湖北";
    // 设置城市
    public String city = "武汉市";
    // 设置县
    public String district = "武昌区";
    // 设置CreateBy
    public String createBy = "袁武";

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
        logger.info("{}进入process方法，当前的链接为：{},现在请求的网页个数为：{}", serviceContext.getName(), page.getUrl().toString(), serviceContext.getSpider().getPageCount());
        String html = page.getHtml().toString();
        String url = page.getUrl().toString();
        try {
            if (url.contains("xwgg")) {
                Document document = Jsoup.parse(html);
                Elements lis = document.select("ul[class=my_n_r_m_ul01] li");
                List<BranchNew> detailList = new ArrayList<BranchNew>();
                if (lis.size() > 0) {
                    for (int i = 0; i < lis.size(); i++) {
                        Element li = lis.get(i);
                        Element a = li.select("a").first();
                        String date = li.select("i").first().text().trim()
                                .replaceAll("[\u00a0\u1680\u180e\u2000-\u200a\u2028\u2029\u202f\u205f\u3000\ufeff\\s+]", "").replaceAll("[.|/|年|月]", "-");
                        Matcher m = p.matcher(date);
                        if (m.find()) {
                            date = SpecialUtil.date2Str(SpecialUtil.str2Date(m.group()));
                        }
                        String title = "";
                        if (a.hasAttr("title")) {
                            title = a.attr("title").trim();
                            if (title.equals("")) {
                                title = a.text().trim();
                            }
                        } else {
                            title = a.text().trim();
                        }
                        String key="征集、询标、交易、机构、需求、废旧、废置、处置、报废、供应商、承销商、服务商、调研、择选、择优、选取、优选、公选、选定、摇选、摇号、摇珠、抽选、定选、定点、招标、采购、询价、询比、竞标、竞价、竞谈、竞拍、竞卖、竞买、竞投、竞租、比选、比价、竞争性、谈判、磋商、投标、邀标、议标、议价、单一来源、标段、明标、明投、出让、转让、拍卖、招租、出租、预审、发包、承包、分包、外包、开标、遴选、答疑、补遗、澄清、延期、挂牌、变更、预公告、监理、改造工程、报价、小额、零星、自采、商谈";
                        String result[] = key.split("、");
                        if(!CheckProclamationUtil.isValuableByExceptTitleKeyWords(title,result)){
                            continue;
                        }
                        String href = a.attr("href").trim();
                        href = href.replace("..","");
                        String id = href;
                        String link = "";
                        if (href.contains("http")) {
                            link = href;
                        } else {
                            link = baseUrl + href;
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
                //下一页
                if (document.select("span[class=p_next p_fun] a:contains(下一页)").first() != null) {
                    if (serviceContext.isNeedCrawl()) {
                        String nextPage =  document.select("span[class=p_next p_fun] a:contains(下一页)").first().attr("href");
                        if(!nextPage.contains("tzgg")){
                            nextPage = "http://hospital.whu.edu.cn/xwgg/tzgg/" + nextPage;
                        }else{
                            nextPage = "http://hospital.whu.edu.cn/xwgg/" + nextPage;
                        }
                        page.addTargetRequest(nextPage);
                    }
                }
            } else {
                Document document = Jsoup.parse(html);
                BranchNew branchNew = map.get(url);
                String title = branchNew.getTitle();
                String id = branchNew.getId();
                serviceContext.setCurrentRecord(id);
                String date = branchNew.getDate();
                String detailLink = branchNew.getDetailLink();
                String detailTitle = title;
                String content = "";
                Element contentE = document.select("div[class = news_show]").first();
                if (contentE != null) {
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
                    content =  contentE.outerHtml();
                }
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
