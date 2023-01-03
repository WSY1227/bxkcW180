package com.bidizhaobiao.data.Crawl.service.impl.DS_10217;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.bidizhaobiao.data.Crawl.entity.oracle.BranchNew;
import com.bidizhaobiao.data.Crawl.entity.oracle.RecordVO;
import com.bidizhaobiao.data.Crawl.service.MyDownloader;
import com.bidizhaobiao.data.Crawl.service.SpiderService;
import com.bidizhaobiao.data.Crawl.utils.SpecialUtil;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

/**
 * 程序员：杨维阵 日期: 2020-06-05
 * 原网站：http://www.mmzczb.com/article/list.asp?classid=40&order=0&page=1
 * 主页：http://www.mmzczb.com/
 **/

@Service
public class DS_10217_GonggBgService extends SpiderService implements PageProcessor {

	public Spider spider = null;

	public String listUrl = "http://www.mmzczb.com/article/list.asp?classid=43&order=0&page=1";
	public String baseUrl = "http://www.mmzczb.com";
	public Pattern p = Pattern.compile("(\\d{4})(年|/|-|\\.)(\\d{1,2})(月|/|-|\\.)(\\d{1,2})");
	// 网站编号
	public String sourceNum = "10217";
	// 网站名称
	public String sourceName = "茂名市众诚招标采购有限公司";
	// 信息源
	public String infoSource = "政府采购";
	// 设置地区
	public String area = "华南";
	// 设置省份
	public String province = "广东";
	// 设置城市
	public String city = "茂名市";
	// 设置县
	public String district;
	public String createBy = "杨维阵";
	// 站源类型
	public String taskType = "";
	// 站源名称
	public String taskName = "";
	// 抓取网站的相关配置，包括：编码、抓取间隔、重试次数
	Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20).setCharset("GBK");
	// 是否需要入广联达
	public boolean isNeedInsertGonggxinxi = false;
	// 信息源

	public Site getSite() {
		return this.site.setUserAgent(
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.86 Safari/537.36");
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
			Thread.sleep(1000);
			if (url.contains("page=")) {
				Document doc = page.getHtml().getDocument();
				Element tab = doc.select("ul[id=mainlistUL]").last();
				if (tab != null) {
					Elements list = tab.select("li:has(a)");
					if (list.size() > 0) {
						for (int i = 0; i < list.size(); i++) {
							Element li = list.get(i);
							Element a = li.select("a[href]").first();
							String id = a.attr("href").trim();
							String link = url.substring(0, url.lastIndexOf("/") + 1) + id;
							id = id.substring(id.lastIndexOf("?") + 1);
							Matcher m = p.matcher(li.text());
							String date = "";
							if (m.find()) {
								String month = m.group(3).length() == 2 ? m.group(3) : "0" + m.group(3);
								String day = m.group(5).length() == 2 ? m.group(5) : "0" + m.group(5);
								date = m.group(1) + "-" + month + "-" + day;
								int year = Integer.parseInt(m.group(1));
								if (year < 2016) {
									continue;
								}
							}
							String title = a.attr("title").trim();
							if (title == null || title.length() < 2) {
								title = a.text().replaceAll("\\s*", "").trim();
							}
							BranchNew branch = new BranchNew();
							branch.setId(id);
							branch.setLink(link);
							branch.setTitle(title);
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
					dealWithNullListPage(serviceContext);
				}
				Element ele = doc.select("a:contains(下一页)").first();
				if (ele != null && ele.attr("href").startsWith("list") && serviceContext.isNeedCrawl()) {
					serviceContext.setPageNum(serviceContext.getPageNum() + 1);
					String href = ele.attr("href");
					String nextPage = url.substring(0, url.lastIndexOf("/") + 1) + href;
					page.addTargetRequest(nextPage);
				}
			} else {
				String detailHtml = page.getHtml().toString();
				String Content = "";
				BranchNew bn = map.get(url);
				if (bn != null) {
					String Title = bn.getTitle();
					String date = bn.getDate();
					Document doc = Jsoup.parse(page.getRawText());
					Elements aList = doc.select("a");
					for (Element a : aList) {
						String href = a.attr("href");
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
							if (href.indexOf("/") == 0) {
								href = baseUrl + href;
								a.attr("href", href);
							} else if (href.indexOf("../") == 0) {
								href = href.replace("../", "");
								href = baseUrl + "/" + href;
								a.attr("href", href);
							} else if (href.indexOf("./") == 0) {
								href = url.substring(0, url.lastIndexOf("/") + 1) + href.substring(2);
								a.attr("href", href);
							} else {
								href = url.substring(0, url.lastIndexOf("/") + 1) + href;
								a.attr("href", href);
							}
						}
					}
					Elements imgList = doc.select("IMG");
					for (Element img : imgList) {
						String href = img.attr("src");
						if (href.contains("dkxx.png")) {
							img.remove();
							continue;
						}
						if (href.length() > 10 && href.indexOf("http") != 0) {
							if (href.indexOf("../") == 0) {
								href = href.replace("../", "");
								href = baseUrl + "/" + href;
								img.attr("src", href);
							} else if (href.indexOf("./") == 0) {
								href = url.substring(0, url.lastIndexOf("/") + 1) + href.substring(2);
								img.attr("src", href);
							} else if (href.indexOf("/") == 0) {
								href = baseUrl + href;
								img.attr("src", href);
							} else {
								href = url.substring(0, url.lastIndexOf("/") + 1) + href;
								img.attr("src", href);
							}
						}
					}
					Element div = doc.select("div[id=mainNewsContent]").first();
					if (div != null) {
						Element tit = doc.select("div[id=mainBody] h1").first();
						if (tit != null) {
							Content = tit.outerHtml();
							Title = tit.text().trim();
						}
						div.select("script").remove();
						div.select("style").remove();
						Content += div.html().replace("链接错误", "链接出现错误");

					} else if (url.contains(".doc") || url.contains(".pdf") || url.contains(".zip")
							|| url.contains(".xls")) {
						Content = "<div>附件下载：<a href='" + url + "'>" + Title + "</a></div>";
						detailHtml = Jsoup.parse(Content).toString();
					} else if (page.getRawText().contains("数据库连接出错")) {
						return;
					}
					RecordVO recordVO = new RecordVO();
					recordVO.setId(bn.getId());
					recordVO.setListTitle(bn.getTitle());
					recordVO.setTitle(Title);
					recordVO.setDetailLink(url);
					recordVO.setDetailHtml(detailHtml);
					recordVO.setDdid(SpecialUtil.stringMd5(detailHtml));
					recordVO.setDate(date);
					recordVO.setContent(Content);
					dataStorage(serviceContext, recordVO, bn.getType());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			dealWithError(url, serviceContext, e);
		}
	}
}
