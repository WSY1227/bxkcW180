package com.bidizhaobiao.data.Crawl.service.impl.XX7114;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
import com.bidizhaobiao.data.Crawl.utils.CheckProclamationUtil;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 程序员：陈广艺
 * 日期：2021-11-01
 * 原网站：首页/公告公示/仅抓取当页标题
 * 主页：http://www.lthxx.sjedu.cn
 **/
@Service
public class XX7114_ZhongbXxService extends SpiderService implements PageProcessor {
    public Spider spider = null;

    public String listUrl = "http://www.lthxx.sjedu.cn/directive/8bd2e55074cd9c45134b794161b9a469.data?random=1021031773145552024";
    public String baseUrl = "http://www.lthxx.sjedu.cn";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);

    // 网站编号
    public String sourceNum = "XX7114";
    // 网站名称
    public String sourceName = "上海市松江区李塔汇学校";
    // 信息源
    public String infoSource = "政府采购";

    // 设置地区
    public String area = "华东";
    // 设置省份
    public String province = "上海";
    // 设置城市
    public String city;
    // 设置县
    public String district = "松江区";
    // 设置县
    public String createBy = "陈广艺";

    //public  Pattern pattern_page = Pattern.compile("1/(\\d+)");
    //public  Pattern pattern_page = Pattern.compile("countPage =(\\d+)");
    public Pattern pattern = Pattern.compile("createPageHTML\\((\\d+),");
    public Pattern p = Pattern.compile("20\\d{2}-\\d{1,2}-\\d{1,2}");
    //是否需要入广联达
    public boolean isNeedInsertGonggxinxi = false;
    //站源类型
    public String taskType;
    //站源名称
    public String taskName;


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

        String url = page.getUrl().toString();
        try {
            Thread.sleep(1000);
            if (url.equals(listUrl)) {
                JSONArray list = new JSONArray(page.getRawText());
                List<BranchNew> detailList = new ArrayList<>();
                if (list.length() > 0) {
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject oo = list.getJSONObject(i);
                        String title = oo.get("DOCTITLE").toString();
                        String date = oo.get("DOCRELTIME").toString();
                        long time = Long.parseLong(date);
                        Date d = new Date(time);
                        date = sdf.get().format(d);
                        date = date.replaceAll("[./年月]", "-");
                        Matcher m = p.matcher(date);
                        if (m.find()) {
                            date = sdf.get().format(sdf.get().parse(m.group()));
                        }
                        if (!CheckProclamationUtil.isProclamationValuable(title)) {
                        	continue;
                        }
                        String id = oo.get("LINK").toString();
                        id = id.substring(0, id.lastIndexOf("."));
                        String link = "http://www.lthxx.sjedu.cn/data/" + id + ".data";
                        BranchNew bn = new BranchNew();
                        bn.setTitle(title);
                        bn.setId(id);						serviceContext.setCurrentRecord(id);
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
                    page.addTargetRequest(listUrl.replace("default_1", "default_" + index));
                }
            } else {
                JSONObject obj = new JSONObject(page.getRawText());
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
                String content = "";
                String title = "";
                obj = obj.getJSONObject("doc");
                content = obj.get("DOCHTMLCON").toString();
                Document doc = Jsoup.parse(content);
                String path = "http://www.lthxx.sjedu.cn";
                String path1 = bn.getLink();
                path1 = path1.substring(0, path1.lastIndexOf("/"));
                doc.select("input").remove();
                doc.select("meta").remove();
                doc.select("script").remove();
                doc.select("link").remove();
                doc.select("style").remove();
                doc.outputSettings().prettyPrint(true);//允许格式化文档格式
                Element conTag = doc;
                if (conTag != null) {
                    conTag.select("*[style~=^.*display\\s*:\\s*none\\s*(;\\s*[0-9A-Za-z]+|;\\s*)?$]").remove();//删除隐藏格式
                    conTag.select("iframe").remove();
                    Elements as = conTag.select("a");
                    for (Element ae : as) {
                        String href = ae.attr("href");
                        if (!"#".equals(href) && !href.contains("http") && href.length() > 0 && !href.contains("HTTP")) {
                            if (href.indexOf("../") == 0) {
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
                    }
                    Elements imgs = conTag.select("img");
                    for (Element imge : imgs) {
                        String src = imge.attr("src");
                        if (!src.contains("http") && !src.contains("HTTP") && !src.startsWith("data")) {
                            if (src.indexOf("../") == 0) {
                                src = path1 + src.replace("../", "/");
                            } else if (src.indexOf("./") == 0) {
                                src = path1 + src.substring(2);
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
