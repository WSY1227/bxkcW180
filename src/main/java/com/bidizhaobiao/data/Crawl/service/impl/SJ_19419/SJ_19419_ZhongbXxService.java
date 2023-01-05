package com.bidizhaobiao.data.Crawl.service.impl.SJ_19419;

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
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.utils.HttpConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 程序员：徐文帅 日期：2023-01-05
 * 原网站：http://www.henan-highway.cn/website/wd_lanmu.aspx?CRT_NTYPE=2
 * 主页：http://www.henan-highway.cn
 **/
@Service
public class SJ_19419_ZhongbXxService extends SpiderService implements PageProcessor {
    public Spider spider = null;
    //列表界面
    public String listUrl = "http://www.henan-highway.cn/website/wd_lanmu.aspx?CRT_NTYPE=2";
    //域名
    public String baseUrl = "http://www.henan-highway.cn";
    public Pattern datePat = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");

    // 网站编号
    public String sourceNum = "19419";
    // 网站名称
    public String sourceName = "河南高速公路联网管理中心";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华中";
    // 设置省份
    public String province = "河南";
    // 设置城市
    public String city;
    // 设置县
    public String district;
    public String createBy = "徐文帅";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);

    public Site getSite() {
        return this.site;
    }

    public Request getListRequest(int pageNumber) {
        Request request = new Request(listUrl);
        request.setMethod(HttpConstant.Method.POST);
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        //将键值对数组添加到map中
        Map<String, Object> params = new HashMap<>();
        params.put("__VIEWSTATE", "1NsTc/O1Rmj9SPPT6c0agGA9fpdVyAk07h+ftQNdqBok9Mv3t8UUb5mPMxTdbxS/krN8rr+meErNX633HA3lTUSUeV4HjjqNze5P2gWFMn8IQ34YdxmsHuuzArgrya/zLFTl20ViBKRhZ/UPkHKs8+vvUS5SbpuqhlcF/JgG9DZ4sHJly3X27yAGunlrlBs6QVYGcoG/cYOXhwGJy2Xu2rQJulcMWMCdh8XJDCwzzN3UxDXpGF1WSv2BCM/gXPnmFkqjHhwfXj9WM8AcWD1GSZxDC6WRIbZhWRJ+klmjzGEkpawgNaRZ1PY8/EkrDy1o5HSHL6DX0M8ntwF18bgsQTkMvia4KAWKyXAdO4jKG0vXJFIh57DuGgN4cPgnKVJ3WSqZeRbUcEcLNWC2K7td99TOcbwaVP1ewEIt3awiaCJHbzNd9ZhASZXy5QYFolGwcfDqGpPo+h2QWM+aW+AMdKraRGif/NmrRh5ejG+nfzM+JAcGS2tHvG5BkR2rt7HTgiP7yJAm/fRIgLi3/v23mll8YPOB38Dy9xXwLqcCZH+v1Mi/VJ1QGQo55IE9l64ShoZaoFJX5wj+sd2+hH1iCq9NXNRjKimHhBpbkhCQU8uPuQPo9O5JqGLyQw7lKupBzlY48urxJQ9P26KE6kkeZaplsrzKBQDA8dxMzFlq2pFakTD7Nga4Xv6YEdbAN+RJNeLr+S1rl/0mF/TCAvD0w7WpfQyOqTq6GpC/Ayiv9TtaJa5pIPQvZr06fBhu+FfYMrAVmnI5JO+QXYLh4wbcibOEz0nymdzdHx0TY973lURNzKzf5gojjOTZMSkpLfX0tSV23n+jVGbdkXT03VVg79BPCNZj4QRUwiBEoKdRIuNunsk1snUZZn7ATDH4eDN/jipoyKZAT73ondYJXSDDf8jvWnAiLkrYrp0E/x8GPKAvud8RKXIOp7QcMcNDJIV2CLbWnx2chSB6tDE9I2zBOJ2Lf3xV/5gOrkexS24fGs1gTzbrUxbTNQHXYfGfe1j76sk+5M+p6Rb9SQkPfQowGrFQO+8dkojNK15iIswGfE2CMMi9oBWHV+BZ7GVJXYlD0mXgaBGNmYL7K4ri9EFgsWsvDuk47rXNWQk/t9OgfDt/hcw9QH2g+6XWnHxaAe+TlvIsPw/HdZqw3sYqyDi1upboj9IKJAeraXLnlHOUOMHQ+janWExGBMkNLi5zXcrKSOZAzoYZz7PC1N/07Cjapn4o4IskWrOQIr/c5eIAH6IUXE4jLwxdNFqinlENPwkWfrQX3GJNb5UpR2nydMiyTZEn0mSAVi/eZdXBYp85hrzf9Lso7ZilZTKO/+JU4uA4ovyDs8w46ofGCDIrqr/HaCI940nMRm9k0sVvgXEZgovlm+r/ESbbEzFeerEaON8D+XITQFUjRnIqQTc/RVeliIr6jBVJ/tb7P/BMKG0IEGc=");
        params.put("__VIEWSTATEGENERATOR", "E195C244");
        params.put("__EVENTTARGET", "AspNetPager1");
        params.put("__EVENTARGUMENT", pageNumber);
        params.put("AspNetPager1_input", pageNumber - 1);
        //设置request参数
        request.setRequestBody(HttpRequestBody.form(params, "utf-8"));
        return request;
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

    @Override
    public void process(Page page) {
        String url = page.getUrl().toString();
        try {
            List<BranchNew> detailList = new ArrayList<BranchNew>();
            Thread.sleep(500);
            if (url.contains("?CRT_NTYPE=")) {
                Document doc = Jsoup.parse(page.getRawText());
                Elements listElement = doc.select("#DataList1 table:has(tbody>tr>.UNFirstInfo)");
                if (listElement.size() > 0) {
                    for (Element element : listElement) {
                        Element a = element.select("a").first();
                        String link = a.attr("href").trim();
                        String id = link.substring(link.lastIndexOf("?") + 1);
                        link = url.substring(0, url.lastIndexOf("/") + 1) + link;
                        String detailLink = link;
                        String date = "";
                        Matcher dateMat = datePat.matcher(element.text());
                        if (dateMat.find()) {
                            date = dateMat.group(1);
                            date += dateMat.group(3).length() == 2 ? "-" + dateMat.group(3) : "-0" + dateMat.group(3);
                            date += dateMat.group(5).length() == 2 ? "-" + dateMat.group(5) : "-0" + dateMat.group(5);
                        }
                        String title = a.attr("title").trim();
                        if (title.length() < 2) title = a.text().trim();
                        if (!CheckProclamationUtil.isProclamationValuable(title)) {
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
                Element nextPage = doc.select("a:contains(下一页)").first();
                if (nextPage != null && nextPage.attr("href").contains("__doPostBack") && serviceContext.isNeedCrawl()) {
                    serviceContext.setPageNum(serviceContext.getPageNum() + 1);
                    page.addTargetRequest(getListRequest(serviceContext.getPageNum()));
                }
            } else {
                BranchNew branch = map.get(url);
                if (branch != null) {
                    map.remove(url);
                    serviceContext.setCurrentRecord(branch.getId());
                    String detailHtml = page.getRawText();
                    Document doc = Jsoup.parse(detailHtml);
                    String title = branch.getTitle().replace("...", "");
                    String date = branch.getDate();
                    String content = "";
                    Element contentElement = doc.select(".article-box").first();
                    if (contentElement != null) {
                        Elements aList = contentElement.select("a");
                        for (Element a : aList) {
                            String href = a.attr("href");
                            a.attr("rel", "noreferrer");
                            if (href.startsWith("mailto")) {
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
                        Element titleElement = contentElement.select("#tit").first();
                        if (titleElement != null) {
                            title = titleElement.text().trim();
                        }
                        titleElement.remove();
                        contentElement.select(".content-title").remove();
                        contentElement.select("script").remove();
                        contentElement.select("style").remove();
                        content = contentElement.outerHtml();
                    } else if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip") || url.contains(".xls")) {
                        content = "<div>附件下载：<a href='" + url + "'>" + branch.getTitle() + "</a></div>";
                        detailHtml = Jsoup.parse(content).toString();
                    }
                    RecordVO recordVO = new RecordVO();
                    recordVO.setId(branch.getId());
                    recordVO.setListTitle(branch.getTitle());
                    recordVO.setTitle(title);
                    recordVO.setDetailLink(branch.getDetailLink());
                    recordVO.setDetailHtml(detailHtml);
                    recordVO.setDdid(SpecialUtil.stringMd5(detailHtml));
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
