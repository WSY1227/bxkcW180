package com.bidizhaobiao.data.Crawl.service.impl.DX001172;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.utils.HttpConstant;

/**
 * @author 作者: 廉建林
 * @version 创建时间：2018年8月9日 下午2:05:15 类说明
 */
@Service
public class DX001172_ZhaobGgService extends SpiderService implements PageProcessor {
	public Spider spider = null;
	public int successNum = 0;
	public String listUrl = "http://222.222.237.162:802/TenderShow";
	public String hostUrl = "http://222.222.237.162:802";

	// 设置县
	public Pattern p = Pattern.compile("(\\d{4})(年|/|-)(\\d{1,2})(月|/|-)(\\d{1,2})");
	public Pattern p_p = Pattern.compile("共(\\d+)页");
	// 设置地区
	public String area = "华北";
	// 设置省份
	public String province = "河北";
	// 设置城市
	public String city = "邯郸市";
	// 设置县
	public String district = "武安市";
	public String createBy = "杨维阵";;
	public String __VIEWSTATE = "";
	// 网站编号
	protected String sourceNum = "DX001172";
	// 网站名称
	protected String sourceName = "河北龙凤山铸业有限公司";
	// 过时时间分割点
	protected String SplitPointStr = "2016-01-01";
	// 信息源
	protected String infoSource = "政府采购";
	// 抓取网站的相关配置，包括：编码、抓取间隔、重试次数
	Site site = Site.me().setCycleRetryTimes(2).setTimeOut(30000).setSleepTime(20);
	public double priod = 4;

	// public String __EVENTVALIDATION = "";
	public Site getSite() {
		return this.site;
	}

	public void startCrawl(int ThreadNum, int crawlType) {
		// 赋值
		serviceContextEvaluation();
		serviceContext.setCrawlType(crawlType);
		// 保存日志
		saveCrawlLog(serviceContext);
		// 启动爬虫
		spider = Spider.create(this).thread(ThreadNum).setDownloader(new MyDownloader(serviceContext, false, listUrl));
		;
		spider.addRequest(new Request(listUrl));
		serviceContext.setSpider(spider);
		spider.run();
		serviceContext.setSpider(spider);
		// 爬虫状态监控部分
		saveCrawlResult(serviceContext);
	}

	public void process(Page page) {
		String url = page.getUrl().toString();
		try {
			// HtmlPage htmlPage = client.getPage(url);
			List<BranchNew> detailList = new ArrayList<BranchNew>();
			Thread.sleep(500);
			// 判断是否是翻页连接
			if (url.contains("TenderShow")) {
				Document doc = Jsoup.parse(page.getRawText());
				Element tab = doc.select("table.mytable").first();
				if (tab != null) {
					Element span = doc.getElementById("ContentPlaceHolder1_AspNetPager1");
					Matcher m2 = p_p.matcher(span.text());
					if (m2.find()) {
						int maxPage = Integer.parseInt(m2.group(1));
						serviceContext.setMaxPage(maxPage);
					}
					Elements list = tab.select("tr:has(a)");
					for (int i = 0; i < list.size(); i++) {
						Element li = list.get(i);
						String no = li.select("td").first().text().trim();
						Element a = li.select("a").last();
						String id = a.attr("href");
						String link = "http://222.222.237.162:802/" + id;
						id = id.substring(id.indexOf("?") + 1);
						String title = a.text().trim() + "(" + no + ")";
						Matcher m = p.matcher(li.text());
						String date = "";
						if (m.find()) {
							String month = m.group(3).length() == 2 ? m.group(3) : "0" + m.group(3);
							String day = m.group(5).length() == 2 ? m.group(5) : "0" + m.group(5);
							date = m.group(1) + "-" + month + "-" + day;
						}
						BranchNew branch = new BranchNew();
						branch.setId(id);
						branch.setLink(link);
						branch.setTitle(title);
						branch.setDate(date);
						detailList.add(branch);
					}
					if (doc.getElementById("__VIEWSTATE") != null) {
						__VIEWSTATE = doc.getElementById("__VIEWSTATE").attr("value");
					}
					// 校验数据List<BranchNew> detailList,int pageNum,String
					// sourceNum
					// 校验数据,判断是否需要继续触发爬虫
					List<BranchNew> needCrawlList = checkData(detailList, serviceContext);
					for (BranchNew branch : needCrawlList) {
						map.put(branch.getLink(), branch);
						page.addTargetRequest(branch.getLink());
					}

				} else {
					dealWithNullListPage(serviceContext);
				}
				if (serviceContext.getPageNum() <= serviceContext.getMaxPage() && serviceContext.isNeedCrawl()) {
					serviceContext.setPageNum((serviceContext.getPageNum() + 1));
					page.addTargetRequest(getRequest(__VIEWSTATE, serviceContext.getPageNum()));
				}
			} else {
				String Content = "";
				BranchNew bn = map.get(url);
				map.remove(url);
				String Title = bn.getTitle();
				String recordId = bn.getId();
				String Time = bn.getDate();
				// String detailCon = getContext(detailUrl);
				Document doc = Jsoup.parse(page.getRawText());
				Elements aList = doc.select("a");
				for (Element a : aList) {
					String href = a.attr("href");
					if (href.contains("UploadedFile") && a.hasAttr("id")) {
						Element tr = a.parent().parent();
						String fileName = tr.select("td").first().text().trim();
						a.text("文件下载" + fileName);
					}
					if (href.startsWith("mailto")) {
						continue;
					}
					if (href.startsWith("javascript")) {
						a.remove();
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
						if (href.indexOf("//") == 0) {
							href = "http:" + href;
							a.attr("href", href);
						} else if (href.indexOf("/") == 0) {
							href = hostUrl + href;
							a.attr("href", href);
						} else if (href.indexOf("../") == 0) {
							href = href.replace("../", "");
							href = hostUrl + "/" + href;
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
				Element tit = doc.select("div.row:contains(招标编号)").first();
				if (tit != null) {
					Content = tit.outerHtml();
				}
				Element div = doc.select("div.row:contains(招标内容)").first();
				if (div != null) {
					div.select("input[src]").remove();
					Content += div.outerHtml();
				}
				// System.out.println(Content);
				// Title = "";
				RecordVO recordVo = new RecordVO();
				recordVo.setDetailLink(url);
				recordVo.setTitle(Title);
				recordVo.setContent(Content);
				recordVo.setId(recordId);
				recordVo.setDate(Time);
				// 入库操作（包括数据校验和入库）
				dataStorage(serviceContext, recordVo, bn.getType());

			}
		} catch (Exception e) {
			dealWithError(url, serviceContext, e);
			//
		}
	}

	public Request getRequest(String __VIEWSTATE, int page) {
		Request request = new Request(listUrl);
		request.setMethod(HttpConstant.Method.POST);
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("__VIEWSTATE", __VIEWSTATE);
		params.put("__VIEWSTATEGENERATOR", "E39FE4C2");
		params.put("__EVENTTARGET", "ctl00$ContentPlaceHolder1$AspNetPager1");
		params.put("__EVENTARGUMENT", page);
		try {
			request.setRequestBody(HttpRequestBody.form(params, "UTF-8"));
		} catch (Exception e) {
			// TODO Auto-generated catch block

		}
		return request;
	}
}
