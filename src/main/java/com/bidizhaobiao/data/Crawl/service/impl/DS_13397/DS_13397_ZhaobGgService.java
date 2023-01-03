package com.bidizhaobiao.data.Crawl.service.impl.DS_13397;

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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 程序员：许广衡
 * 日期：2020-06-11
 * 原网站：http://www.ycxdfh.cn/news/15/
 * 主页：http://www.ycxdfh.cn
 **/
@Service
public class DS_13397_ZhaobGgService extends SpiderService implements PageProcessor {
    public Spider spider = null;

    public String listUrl = "http://www.ycxdfh.cn/comp/news/list.do?compId=news_list-1544847682593&cid=15&pageSize=10&currentPage=1";
    public String baseUrl = "http://www.ycxdfh.cn";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    public Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);

    // 网站编号
    public String sourceNum = "13397";
    // 网站名称
    public String sourceName = "江苏希地丰华项目管理集团有限公司宜昌分公司";
    // 信息源
    private String infoSource = "政府采购";

    // 设置地区
    public String area = "华中";
    // 设置省份
    public String province = "湖北";
    // 设置城市
    public String city = "宜昌市";
    // 设置县
    public String district;
    // 设置县
    public String createBy = "许广衡";

    //private Pattern pattern_page = Pattern.compile("1/(\\d+)");
    //private Pattern pattern_page = Pattern.compile("countPage =(\\d+)");
    private Pattern pattern = Pattern.compile("totalPage:\"(.*?)\",");
    private Pattern p = Pattern.compile("20\\d{2}-\\d{1,2}-\\d{1,2}");
    //是否需要入广联达
    private boolean isNeedInsertGonggxinxi = false;
    //站源类型
    private String taskType;
    //站源名称
    private String taskName;


    public Site getSite() {
        return this.site.setCharset("UTF-8");
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
        Document doc = page.getHtml().getDocument();
        String url = page.getUrl().toString();
        try {
            Thread.sleep(1000);
            if (url.contains("currentPage")) {
                if (serviceContext.getPageNum() == 1) {
                    Matcher m = pattern.matcher(page.getRawText());
                    if (m.find()) {
                        String max = m.group(1);
                        int maxPage = Integer.parseInt(max);
                        serviceContext.setMaxPage(maxPage);

                    }
                }
                Element conTag = doc.select("div[class=e_box e_box-000 p_news]").first();
                Elements eachTags = conTag.select("div[class=e_box e_ListBox-001 p_articles]:has(a)");
                List<BranchNew> detailList = new ArrayList<>();
                if (eachTags.size() > 0) {
                    for (Element eachTag : eachTags) {
                        if (eachTag.select("a").first() == null) continue;

                        String title = eachTag.select("h3").first().text().trim();

                        String date = eachTag.text().trim();
                        date = date.replaceAll("[./年月]", "-");
                        Matcher m = p.matcher(date);
                        if (m.find()) {
                            date = sdf.get().format(sdf.get().parse(m.group()));
                        }
//                      if (!CheckProclamationUtil.isProclamationValuable(title, keys)) {
//                            continue;
//                      }
                        String id = eachTag.select("a[class=e_link e_link-000 p_LinkC]").first().attr("href").trim();
                        String link = "http://www.ycxdfh.cn" + id;
                        BranchNew bn = new BranchNew();
                        bn.setTitle(title);
                        bn.setId(id);
                        bn.setDate(date);
                        bn.setLink(link);
                        detailList.add(bn);
                    }
                    // 校验数据List<BranchNew> detailList,int pageNum,String
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
                    int index = serviceContext.getPageNum();
                    page.addTargetRequest(listUrl.replace("currentPage=1", "currentPage=" + index));
                }
            } else {
                if (page.getStatusCode() == 404) return;
                BranchNew bn = map.get(url);
                if (bn == null) {
                    return;
                }
                String Title = bn.getTitle();
                String recordId = bn.getId();
                String Time = bn.getDate();
                map.remove(url);//清除冗余
                String path = "http://www.ycxdfh.cn";
                String path1 = bn.getLink();
                path1 = path1.substring(0, path1.lastIndexOf("/"));
                doc.select("input").remove();
                doc.select("meta").remove();
                doc.select("script").remove();
                doc.select("link").remove();
                doc.select("style").remove();
                doc.outputSettings().prettyPrint(true);//允许格式化文档格式
                String content = "";
                String title = "";
                Element conTag = doc.select("div[class=e_box e_box-001 p_news]").first();
                if (conTag != null) {
                    Element titleTag = conTag.select("h1").first();
                    conTag.select("div[class=e_box e_box-000  p_previewBox]").remove();
                    conTag.select("div[class=e_box e_box-000  p_nextBox]").remove();
                    conTag.select("*[style~=^.*display\\s*:\\s*none\\s*(;\\s*[0-9A-Za-z]+|;\\s*)?$]").remove();//删除隐藏格式
                    conTag.select("iframe").remove();
                    Elements as = conTag.select("a");
                    for (Element ae : as) {
                        String href = ae.attr("href");
                        if (!"#".equals(href) && !href.contains("http") && href.length() > 0 && !href.contains("HTTP")) {
                            if (href.indexOf("../../..") == 0) {
                                href = path + href.replace("../../..", "");
                            } else if (href.indexOf("../") == 0) {
                                href = path + href.replace("../", "/");
                            } else if (href.indexOf("./") == 0) {
                                href = path1 + href.substring(1);
                            } else if (href.indexOf("/") == 0) {
                                href = path + href;
                            } else {
                                href = path1 + href;
                            }
                        }
                        ae.attr("rel", "noreferrer");
                        ae.attr("href", href);
                        if (href.contains("mailto:") || href.contains("#")) {
                            ae.remove();
                        }
                    }
                    Elements imgs = conTag.select("img");
                    for (Element imge : imgs) {
                        String src = imge.attr("src");
                        if (!src.contains("http") && !src.contains("HTTP") && !src.startsWith("data")) {
                            if (src.indexOf("../") == 0) {
                                src = path + src.replace("../", "/");
                            } else if (src.indexOf("./") == 0) {
                                src = path1 + src.substring(1);
                            } else if (src.indexOf("/") == 0) {
                                src = path + src;
                            } else {
                                src = path1 + src;
                            }
                        }
                        imge.attr("rel", "noreferrer");
                        imge.attr("src", src);
                    }
                    content = conTag.outerHtml();
                    if (titleTag != null) {
                        title = titleTag.text().trim();
                        title = title.replaceAll("[\u00a0\u1680\u180e\u2000-\u200a\u2028\u2029\u202f\u205f\u3000\ufeff\\s+]+", "");
                        title = title.replace("【", "");
                        title = title.replace("】", "");
                        title = title.replace("…", "").replace("...", "");
                    }
                }else{
                    return;
                }
                RecordVO recordVo = new RecordVO();
                        recordVo.setDetailLink(url);
                recordVo.setTitle(title);
                recordVo.setListTitle(Title);
                recordVo.setContent(content);
                recordVo.setId(recordId);
                recordVo.setDate(Time);
                //System.out.println(title + content);
                // 入库操作（包括数据校验和入库）
                dataStorage(serviceContext, recordVo, bn.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();//输出报错
            dealWithError(url, serviceContext, e);
        }
    }


}
