package com.bidizhaobiao.data.Crawl.service.impl.XX0503;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 程序员：刘伟伟
 * 日期：2020-04-26
 * 原网站：https://www.yzpc.edu.cn/2511/list.htm
 * 主页：
 */

@Service("XX0503_ZhaobGgService")
public class XX0503_ZhaobGgService extends SpiderService implements PageProcessor {

    public  Spider spider = null;

    public  String listUrl = "https://www.yzpc.edu.cn/2511/list1.htm";
    public  String baseUrl = "https://www.yzpc.edu.cn";

    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(3).setTimeOut(60000).setSleepTime(20).setCharset("UTF-8");
    // 网站编号
    public String sourceNum = "XX0503";
    // 网站名称
    public String sourceName = "扬州市职业大学";
    // 信息源
    public  String infoSource = "政府采购";

    // 设置地区
    public String area = "华东";
    // 设置省份
    public String province = "江苏";
    // 设置城市
    public String city="扬州市";
    // 设置县
    public String district="邗江区";
    // 设置县
    public String createBy = "刘伟伟";
    //是否需要入广联达
    public  boolean isNeedInsertGonggxinxi = false;
    //站源类型
    public  String taskType;
    //站源名称
    public  String taskName;

    public double priod = 4;

    // 网站名
    public  Pattern p = Pattern.compile("(\\d{4})(年|/|-)(\\d{1,2})(月|/|-)(\\d{1,2})");
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public Site getSite() {
         return this.site.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");
    }

    public void startCrawl(int ThreadNum, int crawlType) {
         // 赋值
         serviceContextEvaluation();
         // 保存日志
         saveCrawlLog(serviceContext);
         serviceContext.setCrawlType(crawlType);
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
            if (url.contains("list")) {
                Document document = Jsoup.parse(html);
                if (serviceContext.getPageNum() == 1) {
                    Element elementPage = document.select("a.last").last();
                    String pages = elementPage.attr("href").replaceAll("[\u00a0\u1680\u180e\u2000-\u200a\u2028\u2029\u202f\u205f\u3000\ufeff\\s+]", "");
                    pages = pages.substring(pages.indexOf("st") + 2, pages.indexOf("."));
                    int maxPage = Integer.parseInt(pages);
                    serviceContext.setMaxPage(maxPage);
                }
                Elements lis = document.select(".column-news-item");
                List<BranchNew> detailList = new ArrayList<BranchNew>();
                if (lis.size() > 0) {
                    for (int i = 0; i < lis.size(); i++) {
                        Element li = lis.get(i);
                        Element a = li.select("a").first();
                        String title = "";
                        if (a.hasAttr("title")) {
                            title = a.attr("title").trim();
                            if (title.equals("")) {
                                title = a.select("span.column-news-title").text().trim();
                            }
                        } else {
                             title = a.select("span.column-news-title").text().trim();
                        }
                        title = title.replace("...", "");
                        if (!CheckProclamationUtil.isProclamationValuable(title, null)) {
                            continue;
                        }
                        String href = a.attr("href").trim();
                        String id = href;
                        String link = "https://www.yzpc.edu.cn"+ id;
                        String date = li.text().replaceAll("[.|/|年|月]", "-");
                        Matcher m = p.matcher(date);
                        if (m.find()) {
                            date = sdf.format(sdf.parse(m.group()));
                        }
                        dealWithNullTitleOrNullId(serviceContext, title, id);
                        BranchNew branch = new BranchNew();
                        branch.setTitle(title);
                        branch.setId(id);
                        branch.setDetailLink(link);
                        branch.setLink(link);
                        branch.setDate(date);
                        detailList.add(branch);
                    }
                    // 校验数据,判断是否需要继续触发爬虫
                    List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                    for (BranchNew branch : needCrawlList) {
                        if (branch.getLink().contains("_upload")){
                            String content = "<div>" + branch.getTitle() + "</div>" + "<a href="+branch.getLink()+">文件下载</a>";
                            html = html != null ? html : Jsoup.parse(content).html();
                            RecordVO recordVO = new RecordVO();
                            recordVO.setId(branch.getId());
                            recordVO.setListTitle(branch.getTitle());
                            recordVO.setDate(branch.getDate());
                            recordVO.setContent(content);
                            recordVO.setTitle(branch.getTitle());//详情页标题
                            recordVO.setDdid(SpecialUtil.stringMd5(html));//详情页md5
                            recordVO.setDetailLink(branch.getLink());//详情页链接
                            recordVO.setDetailHtml(html);
                            dataStorage(serviceContext, recordVO, branch.getType());
                        }else {
                            map.put(branch.getLink(), branch);
                            page.addTargetRequest(branch.getLink());
                        }
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    page.addTargetRequest(listUrl.replace("list1", "list" + serviceContext.getPageNum()));
                }
            } else {
                BranchNew branchNew = map.get(url);
                String title = branchNew.getTitle();
                String id = branchNew.getId();
                String date = branchNew.getDate();
                String detailLink = branchNew.getDetailLink();
                String detailTitle = title;
                String content = "";
                Document document = Jsoup.parse(html);
                Element contentE = document.select("div.wp_articlecontent").last();
                if (contentE.select("a") != null) {
                            Elements as = contentE.select("a");
                            for (Element a : as) {
                                String href = a.attr("href");
                                if (!href.contains("_upload")){
                                    a.remove();
                                }
                                if (!href.contains("@") && !"".equals(href) && !href.contains("javascript") && !href.contains("http") && !href.contains("#")) {
                                    if (href.contains("../")) {
                                        href = baseUrl + "/" + href.substring(href.lastIndexOf("./") + 1, href.length());
                                        a.attr("href", href);
                                    } else if (href.startsWith("./")) {
                                        href = url.substring(0, url.lastIndexOf("/")) + href.replace("./", "/");
                                        a.attr("href", href);
                                        a.removeAttr("rel");
                                    } else if (href.startsWith("/")) {
                                        href = baseUrl + href;
                                        a.attr("href", href);
                                        a.removeAttr("rel");
                                    } else {
                                        href = baseUrl + "/" + href;
                                        a.attr("href", href);
                                        a.removeAttr("rel");
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
                                if (src.contains("file:///C:/Users")){
                                    img.remove();
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
                html = html != null ? html : Jsoup.parse(content).html();
                RecordVO recordVO = new RecordVO();
                recordVO.setId(id);
                recordVO.setListTitle(title);
                recordVO.setDate(date);
                recordVO.setContent(content);
                recordVO.setTitle(detailTitle);//详情页标题
                recordVO.setDdid(SpecialUtil.stringMd5(html));//详情页md5
                recordVO.setDetailLink(detailLink);//详情页链接
                recordVO.setDetailHtml(html);
                dataStorage(serviceContext, recordVO, branchNew.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }


}
