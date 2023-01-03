package com.bidizhaobiao.data.Crawl.service.impl.XX0319;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 程序员：许广衡
 * 日期：2020-10-29
 * 原网站：http://ztb.bnuz.edu.cn/zbgg1.htm
 * 主页：http://www.bnuz.edu.cn/
 **/
@Service
public class XX0319_ZhongbXxService extends SpiderService implements PageProcessor {
    public Spider spider = null;

    public String listUrl = "https://www.baidu.com/?wd=http://ztb.bnuz.edu.cn/zbgg1.htm";
    public String baseUrl = "http://ztb.bnuz.edu.cn";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    public Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);

    // 网站编号
    public String sourceNum = "XX0319";
    // 网站名称
    public String sourceName = "北京师范大学珠海分校";
    // 设置地区
    public String area = "华南";
    // 设置省份
    public String province = "广东";
    // 设置城市
    public String city = "珠海市";
    // 设置县
    public String district;
    // 设置县
    public String createBy = "许广衡";
    // 信息源
    private String infoSource = "政府采购";
    //private Pattern pattern_page = Pattern.compile("1/(\\d+)");
    //private Pattern pattern_page = Pattern.compile("countPage =(\\d+)");
    private Pattern pattern = Pattern.compile("url=(.*?)\"");
    private Pattern p = Pattern.compile("20\\d{2}-\\d{1,2}-\\d{1,2}");
    //是否需要入广联达
    private boolean isNeedInsertGonggxinxi = false;
    //站源类型
    private String taskType;
    //站源名称
    private String taskName;


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

    public String getContent(String path) {
        String respStr = "";
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse httpResponse = null;
        try {
            httpClient = getHttpClient(true, false);
            HttpGet httpGet = new HttpGet(path);
            httpGet.addHeader("User-Agent",
                    "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36");
            httpGet.addHeader("Content-type", "text/html; charset=utf-8");
            httpGet.addHeader("Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(20 * 1000)
                    .setSocketTimeout(30 * 1000).build();
            httpGet.setConfig(requestConfig);
            httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                respStr = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return respStr;
    }

    public void process(Page page) {
        String url = page.getUrl().toString();
        url = url.substring(url.indexOf("wd=") + 3);
        String result = getContent(url);
        int times = 2;
        while (result.length() < 10 && times > 0) {
            result = getContent(url);
            times--;
        }
        Document doc = Jsoup.parse(result);
        try {
            Thread.sleep(1000);
            if (url.contains("zbgg1")) {
                if (result.contains("1%")) {
                    if (serviceContext.getPageNum() == 1) {
                        String max = doc.select("td[width=1%]").text().trim().replaceAll("[\u00a0\u1680\u180e\u2000-\u200a\u2028\u2029\u202f\u205f\u3000\ufeff\\s+]+", "");
                        max = max.substring(max.indexOf("/") + 1);
                        int maxPage = Integer.parseInt(max);
                        serviceContext.setMaxPage(maxPage);
                    }
                    Element conTag = doc.select("div[id=BoxContent]").first();
                    Elements eachTags = conTag.select("li:has(a)");
                    List<BranchNew> detailList = new ArrayList<>();
                    if (eachTags.size() > 0) {
                        for (Element eachTag : eachTags) {
                            if (eachTag.select("a").first() == null) continue;
                            String title = eachTag.select("a").first().attr("title").trim();
                            if (title.length() < 2) {
                                title = eachTag.select("a").first().text().trim();
                            }
                            String date = eachTag.text().trim();
                            date = date.replaceAll("[./年月]", "-");
                            Matcher m = p.matcher(date);
                            if (m.find()) {
                                date = sdf.get().format(sdf.get().parse(m.group()));
                            }
                            String id = eachTag.select("a").first().attr("href").trim();
                            if (id.contains("info")) {
                                String link = "http://ztb.bnuz.edu.cn/" + id.substring(id.indexOf("info"));
                                BranchNew bn = new BranchNew();
                                bn.setTitle(title);
                                bn.setId(id);						serviceContext.setCurrentRecord(id);
                                bn.setDate(date);
                                bn.setLink(link);
                                detailList.add(bn);
                            }
                        }
                        // 校验数据List<BranchNew> detailList,int pageNum,String
                        List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
                        for (BranchNew branch : needCrawlList) {
                            map.put(branch.getLink(), branch);
                            page.addTargetRequest("https://www.baidu.com/?wd=" + branch.getLink());
                        }
                    } else {
                        dealWithNullListPage(serviceContext);
                    }
                } else {
                    dealWithNullListPage(serviceContext);
                }
                if (serviceContext.getPageNum() < serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    int index = serviceContext.getPageNum();
                    page.addTargetRequest(listUrl.replace("zbgg1", "zbgg1/" + (serviceContext.getMaxPage() - index + 1)));
                }
            } else {
                if (page.getStatusCode() == 404) return;
                 BranchNew bn = map.get(url);
				serviceContext.setCurrentRecord(bn.getId());
                if (bn == null) {
                    return;
                }
                String Title = bn.getTitle();
                String recordId = bn.getId();
                String Time = bn.getDate();
                map.remove(url);//清除冗余
                String path = "http://ztb.bnuz.edu.cn";
                String path1 = bn.getLink();
                path1 = path1.substring(0, path1.lastIndexOf("/"));
                doc.select("input").remove();
                doc.select("meta").remove();
                doc.select("script").remove();
                doc.select("link").remove();
                doc.select("style").remove();
                doc.outputSettings().prettyPrint(true);//允许格式化文档格式
                String content = "";
                Element conTag = doc.select("form[name=_newscontent_fromname]").first();
                if (conTag != null) {
                    conTag.select("div.laiyuan").remove();
                    conTag.select("div[class=footer2]").remove();
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

                } else if (url.contains(".doc") || url.contains(".rar") || url.contains(".pdf") || url.contains(".zip") || url.contains(".xls")) {
                    content = "<div>附件下载：<a href='" + url + "'>" + Title + "</a></div>";
                } else {
                    return;
                }
                RecordVO recordVo = new RecordVO();
                recordVo.setDetailLink(url);
                recordVo.setTitle(Title);
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
