package com.bidizhaobiao.data.Crawl.service.impl.DX011456;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
import com.bidizhaobiao.data.Crawl.utils.CheckProclamationUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 程序员：余林锐 日期：2022-01-17
 * 原网站：http://www.sanxiarenjia.com/news/index?cid=50
 * 主页：http://www.sanxiarenjia.com
 **/
@Service
public class DX011456_ZhaobGgService extends SpiderService implements PageProcessor {
    public Spider spider = null;

    public String listUrl = "http://www.sanxiarenjia.com/news/index?cid=50";
    public String baseUrl = "http://www.sanxiarenjia.com/news/index?cid=50";
    public Pattern datePat = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");

    // 网站编号
    public String sourceNum = "DX011456";
    // 网站名称
    public String sourceName = "三峡人家";
    // 信息源
    public String infoSource = "政府采购";
    // 设置地区
    public String area = "华中";
    // 设置省份
    public String province = "湖北";
    // 设置城市
    public String city = "宜昌";
    // 设置县
    public String district = "夷陵";
    public String createBy = "余林锐";
    // 抓取网站的相关配置，包括：编码、抓取间隔、重试次数等
    Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);

    public Site getSite() {
        return this.site
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
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
            Thread.sleep(2000);
            if(url.contains("baidu")){
                url=url.substring(url.indexOf("wd=")+3);
            }
            String html=getHtmlByGet(url);
            int times=2;
            while ("".equals(html)&&times>0){
                html=getHtmlByGet(url);
                times--;
            }
            if (url.equals(listUrl)) {
                Document doc = Jsoup.parse(page.getRawText());
                Elements listElement = doc.select("div.content_item");
                if (listElement.size() > 0) {
                    for (Element li : listElement) {

                        String id = li.attr("id");
                        String link = "http://www.sanxiarenjia.com/news/get_details?id="+id;
                        String detailLink ="http://www.sanxiarenjia.com/newsdetail/index?id="+id+"&cid=50";
                        String date = "";
                        Matcher dateMat = datePat.matcher(li.text());
                        if (dateMat.find()) {
                            date = dateMat.group(1);
                            date += dateMat.group(3).length() == 2 ? "-" + dateMat.group(3) : "-0" + dateMat.group(3);
                            date += dateMat.group(5).length() == 2 ? "-" + dateMat.group(5) : "-0" + dateMat.group(5);
                        }
                        String title =li.select("div.name").text();
                        String keyWords = "招标、采购、询价、询比、竞标、竞价、竞谈、竞拍、竞卖、竞买、竞投、竞租、比选、比价、竞争性、谈判、磋商、投标、邀标、议标、议价、单一来源、遴选、标段、明标、明投、出让、转让、拍卖、招租、预审、发包、开标、答疑、补遗、澄清、挂牌";
                        String[] keys = keyWords.split("、");
                        if(!CheckProclamationUtil.isValuableByExceptTitleKeyWords(title,keys)){
                             continue;
                        }
                        if (!CheckProclamationUtil.isProclamationValuable(title, null)) {
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
                        page.addTargetRequest("https://www.baidu.com/?wd="+branch.getLink());
                    }

                } else {
                    dealWithNullListPage(serviceContext);
                }
            } else {

                BranchNew branchNew = map.get(url);
                if(branchNew!=null){

                    page.setRawText(html);
                    String title = branchNew.getTitle();
                    String id = branchNew.getId();
                    serviceContext.setCurrentRecord(id);
                    String date = branchNew.getDate();
                    String detailLink = branchNew.getDetailLink();
                    String detailTitle = title;
                    String content = "";
                    String fjCon = "";
                    String content1 = "";
                    JSONObject jsonObject = new JSONObject(page.getRawText());

                    if(jsonObject.has("content")){
                        content1 = jsonObject.getString("content");
                    }

                    Document document = Jsoup.parse(content1);
                    Element contentE = document;
                    if(contentE!=null){

                        contentE.removeAttr("style");
                        contentE.removeAttr("style");
                        contentE.select("iframe").remove();
                        contentE.select("style").remove();
                        contentE.select("input").remove();
                        contentE.select("script").remove();
                        if (contentE.select("a") != null) {
                            Elements as = contentE.select("a");
                            for (Element a : as) {
                                String href = a.attr("href");
                                a.attr("rel", "noreferrer");
                                if (href.startsWith("mailto")) {
                                    continue;
                                }
                                if (href.startsWith("file://")) {
                                    a.remove();
                                    continue;
                                }
                                if (href.startsWith("HTTP")) {
                                    href.replace("HTTPS","https").replace("HTTP","http");
                                    a.attr("href", href);
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

                                href = href.replace(" ","%20")
                                        .replace("[","%5B")
                                        .replace("]","%5D")
                                        .replace("{","%7B")
                                        .replace("}","%7D");
                                a.attr("href", href);
                            }
                        }
                        if (contentE.select("img").first() != null) {
                            Elements imgs = contentE.select("img");
                            for (Element img : imgs) {
                                String src = img.attr("src");
                                if (src.startsWith("file://")) {
                                    img.remove();
                                    continue;
                                }
                                if(src.startsWith("HTTP")){
                                    src.replace("HTTPS","https").replace("HTTP","http");
                                    img.attr("src", src);
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
                                src = src.replace(" ","%20")
                                        .replace("[","%5B")
                                        .replace("]","%5D")
                                        .replace("{","%7B")
                                        .replace("}","%7D");
                                img.attr("src", src);
                            }
                        }
                        content = contentE.outerHtml();
                    } else if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip") || url.contains(".xls")) {
                        content = "<div>附件下载：<a href='" + url + "'>" + branchNew.getTitle() + "</a></div>";
                        content = Jsoup.parse(content).toString();
                    }
                    //content = content + "<br><div>更多咨询报价请点击：<a rel=\"noreferrer\" href=\"" + detailLink + "\">" + detailLink + "</ a></div>";
                    content = content.replaceAll("\\ufeff|\\u2002|\\u200b|\\u2003", "");
                    RecordVO recordVO = new RecordVO();
                    recordVO.setId(id);
                    recordVO.setListTitle(title);
                    recordVO.setDate(date);
                    recordVO.setContent(content.replaceAll("\\u2002", " "));
                    recordVO.setTitle(detailTitle);//详情页标题
                    recordVO.setDetailLink(detailLink);//详情页链接
                    logger.info("入库id==={}", id);
                    dataStorage(serviceContext, recordVO, branchNew.getType());
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            dealWithError(url, serviceContext, e);
        }
    }

public String getHtmlByGet(String url) {
		String result = "";
		// 1 创建  请求工具对象
		CloseableHttpClient httpClient = null;

		CloseableHttpResponse response = null;
		// 2 设置请求方式
		HttpGet get = new HttpGet(url);
		//设置请求头
		get.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36");
		get.addHeader("X-Requested-With","XMLHttpRequest");
		// 3 执行请求
		try {
			httpClient=getHttpClient(true,false);
			RequestConfig config = RequestConfig.custom().setConnectTimeout(15 * 1000).setSocketTimeout(15 * 1000).build();
			get.setConfig(config);
			response = httpClient.execute(get);
			// 4 判断是否请求成功
			if (response.getStatusLine().getStatusCode() == 200) {
				HttpEntity entity = response.getEntity();// 实体
                result = EntityUtils.toString(entity,"utf-8");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (response != null) {
					response.close();
				}
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}


}
