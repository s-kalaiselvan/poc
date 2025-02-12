package com.elastic.crawler.sample;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

public class WebCrawlerAndLogger {

	private static final String ELASTICSEARCH_HOST = Messages.getString("crawler.elasticsearch.host");
	private static final String ELASTICSEARCH_AUTHORIZATION = Messages.getString("crawler.elasticsearch.authorization");
	private static final String INDEX_NAME = Messages.getString("crawler.index.name");
	private static final String URL_TO_CRAWLE = Messages.getString("crawler.url.to.crawle");
	private Set<String> visitedUrls = new HashSet<>();
    private int maxDepth = 3;

	private RestClient restClient;
	private Map<String, Object> jsonData = new HashMap<>();

	public WebCrawlerAndLogger() {}

	public static void main(String[] args) {
		System.out.println("Start...");
        WebCrawlerAndLogger crawler = new WebCrawlerAndLogger();
        boolean conectionSuccess = crawler.connectELastic();
        if (conectionSuccess) {
        	crawler.crawlVisit(URL_TO_CRAWLE);
        	crawler.crawl(URL_TO_CRAWLE, 2);
        }
        System.out.println("End !!!...");
        try {
			crawler.restClient.close();
		} catch (IOException e) {
			System.exit(1);
		}
	}

    public void crawl(String url, int depth) {
        if (depth > maxDepth) 
            return;
        if (visitedUrls.contains(url)) 
            return;
        visitedUrls.add(url);

        Document document = crawlVisit(url);
        if (document == null)
        	return;
        indexContent();
        Elements links = document.select("a[href]");
        for (Element link : links) {
        	String nextUrl = link.absUrl("href");
        	if (nextUrl.startsWith("http")) {
        		crawl(nextUrl, depth + 1);
        	}
        }
    }

	private boolean connectELastic() {
		Header authHeader = new BasicHeader(HttpHeaders.AUTHORIZATION, ELASTICSEARCH_AUTHORIZATION);
		restClient = RestClient.builder(HttpHost.create(ELASTICSEARCH_HOST)).setDefaultHeaders(new Header[] { authHeader }).build();
		return true;
	}

	private String indexContent() {
		String res = "Error response";
		try {
			ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper(new ObjectMapper()));
			ElasticsearchClient esClient = new ElasticsearchClient(transport);
			IndexResponse response = esClient.index(i -> i.index(INDEX_NAME).document(jsonData));
			System.out.println("Indexed document ID: " + response.id());
			res = response.id();
		} catch (ElasticsearchException | IOException e) {
			System.err.println("Error in indexing: " + e.getMessage());
//		} finally {
//			transport.close();
		}
		System.out.println("Index id: "+ res);
		return res;
	}

	private Document crawlVisit(String urlToCrawle) {
		jsonData.clear();
		jsonData.put("url", urlToCrawle);
		try {
			System.out.println("Crawling: " + urlToCrawle);
			Document doc = Jsoup.connect(urlToCrawle)
					.header("Content-Type","application/x-www-form-urlencoded")
					.cookie("TALanguage", "ALL")
					.referrer(urlToCrawle)         
					.header("X-Requested-With", "XMLHttpRequest")
					.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36")
					.get();
			String title = doc.title();
			jsonData.put("title", title);
			Elements paragraphs = doc.select("p");
			StringBuilder content = new StringBuilder();
			for (Element p : paragraphs) {
				content.append(p.text()).append("\n");
			}
			jsonData.put("content", content.toString());
			Elements linkElems = doc.select("a[href]");
			List<String> links = new LinkedList();
            for (Element link : linkElems) {
                String nextUrl = link.absUrl("href");
                links.add(nextUrl);
            }
            jsonData.put("links", links.toString());
			return doc;
        } catch (IOException e) {
			System.err.println("Error in crawling: " + e.getMessage());
		}
		return null;
	}
}
/*
   "_source": {
          "last_crawled_at": "2025-02-06T04:35:29Z",
          "image": "https://www.elmex.de/content/dam/cp-sites/oral-care/elmex/de_de/sensitive-teeth/dental-emergency-thumbnail.jpg",
          "url_path_dir3": "schnelle-hilfe-bei-zahnschmerzen",
          "additional_urls": [
            "https://www.elmex.de/articles/about-sensitive-teeth/schnelle-hilfe-bei-zahnschmerzen"
          ],
          "body_content": "Unsere Mission Produkte Produktvorteile Kariesschutz Kinderzahnpflege Schmerzempfindlichkeit Zahnschmelz Alle Produkte Produkttypen Zahnpasta Zahnspülung / Mundspülung Gelée Zahnbürste Interdentalbürste Mundgesundheitstipps Kariesprävention & Behandlung Gesunde Kinderzähne Vermeidung von Zahnschmelz-Abbau Empfindliche Zähne : Ursachen & Behandlung Alle Artikel & Tipps Selbsttest Zahnschmelzverlust-Risikorechner Fluoridrechner Welche elmex Mundpflege-Produkte passen zu mir? Habe ich schmerzempfindliche Zähne? Menü schließen Schnelle Hilfe bei Zahnschmerzen Linderung von Zahnschmerzen bei zahnärztlichen Notfällen Zahnschmerzen können Ihren Tag völlig zum Stillstand kommen lassen und die Nahrungsaufnahme, das Schlafen oder die Konzentration erschweren. Wenn Ihre Zahnschmerzen sehr stark sind, kann es sich um einen zahnärztlichen Notfall handeln. Im Folgenden erfahren Sie mehr über die Ursachen starker Zahnschmerzen und woran Sie erkennen können, dass diese dringend behandelt werden müssen. Was verursacht Zahnschmerzen? Laut GZFA sind die häufigsten Ursachen von Zahnschmerzen: Karies Gingivitis und Parodontitis Gesprungene oder abgebrochene Zähne Lose oder beschädigte Füllungen Infektionen Bruxismus (Zähneknirschen) Abszesse Sie sollten bei jeder Art von Zahnschmerzen zum Zahnarzt gehen. Manche Ursachen sind jedoch dringender als andere. Die GZFA unterteilt Zahnschmerzen in zwei Arten: von den Zähnen kommend (odontogen) und non-odontogen, also nicht von den Zähnen ausgehend. Behandlung von leichten Zahnschmerzen Bei leichten Zahnschmerzen, wie sie bei gefüllten oder überkronten Zähnen öfter auftreten, empfiehlt die KZVBW trotzdem einen Termin bei Ihrem Zahnarzt zu vereinbaren. Diese Art Zahnschmerzen ist nicht ansteigend, was bedeutet, dass sie während Sie auf Ihren Zahnarzttermin warten, wahrscheinlich nicht schlimmer werden. Wenn Sie feststellen, dass die Schmerzen in der Zwischenzeit schwer zu ertragen sind, können folgende Maßnahmen hilfreich sein: Nehmen Sie ein rezeptfreies Schmerzmittel wie Ibuprofen oder Paracetamol ein. Spülen Sie Ihren Mund mit Salzwasser aus. Essen Sie weiche Lebensmittel. Vermeiden Sie übermäßiges Kauen mit den schmerzenden Zähnen. Vermeiden Sie sehr heiße, sehr kalte oder süße Speisen und Getränke. Rauchen Sie nicht. Fragen Sie Ihren Arzt, Zahnarzt oder Apotheker, bevor Sie Medikamente einnehmen, insbesondere wenn Sie bereits Medikamente gegen eine andere Erkrankung einnehmen. Behandlung starker Zahnschmerzen Wenn Ihre Schmerzen nicht auf rezeptfreie Schmerzmittel und andere Hausmittel ansprechen oder Sie Anzeichen einer Infektion feststellen, handelt es sich um einen Notfall. Infektionszeichen können Fieber, Schwellungen oder Reizungen in dem Bereich, in dem Sie die Schmerzen wahrnehmen, sein. In diesem Fall empfiehlt der Zahnärztliche Notdienst, einen sofortigen Termin bei Ihrem Zahnarzt zu vereinbaren oder sich außerhalb der Öffnungszeiten an einen zahnärztlichen Notdienst zu wenden. Bei folgenden Anzeichen sollten Sie sofort einen Zahnarzt aufsuchen: Schwerer, anhaltender, pochender Zahnschmerz, der bis zum Kieferknochen, Hals oder Ohr ausstrahlen kann Empfindlichkeit gegenüber heißen und kalten Temperaturen Empfindlichkeit gegenüber dem Kau- oder Beißdruck Fieber Schwellungen im Gesicht oder auf der Wange Zarte, geschwollene Lymphknoten unter dem Kiefer oder im Nacken Plötzlicher Ansturm von übel riechender, salziger Flüssigkeit in Ihrem Mund und Schmerzlinderung, wenn der Abszess reißt Mehr Artikel Schmerzen nach dem Bleaching oder der Zahnaufhellung? Strahlend schöne Zähne müssen nicht weh tun! Erfahren Sie hier, wie Sie Ihre Schmerzen lindern und schmerzfreier Ihre Zähne aufhellen können! mehr erfahren » Empfindliche Zähne nach der professionellen Zahnreinigung Warum entstehen Zahnschmerzen nach einer Zahnreinigung? Wie lassen sich die Beschwerden lindern? Tipps zur Pflege nach der professionellen Reinigung. mehr erfahren » Zahnschmerzen bei Kälte und Wärme - Was hilft? Es gibt nichts Schöneres als eine heiße Tasse Kaffee zum Wachwerden am Morgen oder ein kaltes Eis an einem sonnigen Tag, es sei denn, Sie haben empfindliche Zähne. Es fällt Ihnen sicher schwer, Ihre Lieblingsspeisen und -getränke zu genießen, wenn diese stechende Schmerzen im Bereich Ihrer Zähne verursachen. Empfindliche Zähne sind leider jedoch oft mehr als nur eine Unannehmlichkeit… mehr erfahren » Schmerzen der Zahnnerven: Ursachen und Behandlung Sie genießen ein kaltes Getränk oder etwas Süßes und plötzlich schießt ein scharfer Schmerz durch Ihre Zähne und unterbricht Ihren Genuss. Genauso schnell wie er gekommen ist, verschwindet er auch wieder, doch nur, bis zum nächsten Auslöser. Dieses Phänomen ist höchstwahrscheinlich auf eine Überempfindlichkeit Ihres Dentins zurückzuführen, welche sich wie Zahnschmerzen anfühlen kann... mehr erfahren » Was hilft gegen starke Zahnschmerzen? Wenn Zahnschmerzen auftreten, können Sie sich nur noch schwer auf etwas anderes konzentrieren. Glücklicherweise gibt es viele Möglichkeiten, Zahnschmerzen zu lindern, während Sie auf einen Termin beim Zahnarzt warten. Die beste Vorgehensweise hängt von der Ursache Ihrer Zahnschmerzen ab… mehr erfahren » Was hilft gegen Zahnschmerzen - Hausmittel und Tipps Fast jeder von uns ist irgendwann in seinem Leben einmal von Zahnschmerzen betroffen. Tatsächlich kommen Zahnschmerzen so häufig vor, dass die Weltgesundheitsorganisation (WHO) die Behandlung von Zahnschmerzen sogar zu einer ihrer Prioritäten für die Globalen Gesundheitsziele 2020 erklärt hat… mehr erfahren » Wenn der Zahn beim süßen Naschen weh tut, was tun? Erfahre hier, wie du Zahnschmerzen bei süßem Essen lindern kannst. Damit das Naschen wieder Spaß macht! mehr erfahren » Wärme- und kälteempfindliche Zähne? Das können Sie dagegen tun! Woher kommt Wärme- oder Kälteempfindlichkeit im Zahn? Und was können Sie dagegen tun? Damit Eis essen wieder Spaß macht! Erfahre hier alles mit elmex® Sensitive mehr erfahren » Zähneputzen bei empfindlichen Zähnen | elmex® Bestimmen Sie die Ursache Ihrer Zahnempfindlichkeit und erfahren Sie mehr über die möglichen zur Verfügung stehenden Behandlungen. mehr erfahren » So finden Sie die richtige Mundspülung für empfindliche Zähne Was gehört in eine Mund- und Zahnspülung für empfindliche Zähne? Wie kann sie bei schmerzempfindlichen Zähnen helfen? Alle Infos dazu finden Sie hier. mehr erfahren » Was verursacht schmerzempfindliche Zähne? | elmex® Schmerzempfindliche Zähne sind ein weit verbreitetes Phänomen. Mehr als die Hälfte aller Erwachsenen kennt dieses Problem. Erfahren Sie mehr auf Elmex.de. mehr erfahren » Empfindliche Zähne nach der Zahnaufhellung | elmex® Eine Zahnaufhellung ist eine gute Option zum Entfernen von Verfärbungen. Nach der Zahnaufhellung erleben viele Menschen jedoch Zahnempfindlichkeit. mehr erfahren » Was tun bei Zahnfleischentzündung? Alle Infos zu Ursachen und Behandlung! Von Ursache, über Symptome zur Behandlung! Erfahren Sie hier welche Möglichkeiten es gibt, entzündetem Zahnfleisch effektiv und langfristig entgegenzuwirken. mehr erfahren » Die ideale Zahnpasta für empfindliche Zähne | elmex® Die ideale Zahnpasta für empfindliche Zähne kann Ihre Schmerzen in zwei Wochen lindern. Auf Folgendes sollten Sie achten, um Ihren Zahnschmelz zu schützen. mehr erfahren » Freiliegende Zahnhälse? Was tun? Erfahren Sie hier alles! Freiliegende Zahnhälse können sehr schmerzhaft sein! Lesen Sie hier, was freiliegende Zahnhälse verursacht und welche Behandlungsmöglichkeiten es gibt! mehr erfahren » Schmerzempfindliche Zähne, was tun? 5 Tipps, die helfen! Leiden Sie unter extrem empfindlichen oder schmerzempfindlichen Zähnen? Was können Sie tun? Erfahren Sie hier alles! mehr erfahren » Zahnfüllungen: Arten, Behandlungen und alles was Sie wissen müssen Zahnfüllungen: Bedeutung, Materialien, Haltbarkeit und Pflege für optimale Mundgesundheit - Alles, was Sie wissen müssen! mehr erfahren » Zahnschmerzen und empfindliche Zähne beim Kind, was tun? Wie Sie Zahnschmerzen bei Ihrem Kind lindern und die empfindlichen Zähne Ihres Kindes dauerhaft stärken können, erfahren Sie hier. mehr erfahren » Ist ein schmerzempfindlicher Zahn etwas Ernstes? Alle Infos hier! Alles zu Ursachen, Symptome und Behandlung von schmerzempfindlichen Zähnen erfahren Sie hier mit elmex® Sensitive! mehr erfahren » Schmerzt der Zahn beim Aufdrücken? Ursachen, Tipps und Behandlung Alles zu Ursachen, Symptome und Behandlung von druckempfindlichen Zähnen erfahren Sie hier mit elmex® Sensitive! mehr erfahren » Empfindliche Zähne in der Schwangerschaft | elmex® Während der Schwangerschaft empfindliche Zähne zu haben, ist vollkommen normal, Sie können die Reizungen jedoch lindern. mehr erfahren » Empfindliches Zahnfleisch? Drei überraschende Ursachen und was Sie tun können Was tun bei empfindlichem Zahnfleisch? Erfahren Sie hier, was Sie tun können, um entzündetes Zahnfleisch zu behandeln für eine gute Mundgesundheit. mehr erfahren » ALLE ANSEHEN Weitere Produkte: <span>elmex® Sensitive Professional</span> Repair & Prevent Zahnpasta elmex® Sensitive Professional Repair & Prevent Zahnpasta repariert sofort sensible Zahnbereiche & beugt Schmerzempfindlichkeit vor. Jetzt ausprobieren! <span>elmex® Sensitive</span> Sanftes Weiß Zahnpasta elmex® Sensitive sanftes Weiß Zahnpasta bietet eine effektive Dreifach-Wirkung und klinisch geprüften Schutz für empfindliche Zähne. Jetz ausprobieren! <span>elmex® Sensitive Professional</span> Zahnspülung elmex® Sensitive Professional Mundspülung für effektive und anhaltende Schmerzlinderung durch die PRO-ARGIN Technologie. Jetzt ausprobieren! <span>elmex® Sensitive Professional</span> Sanftes Weiß Zahnpasta Zahnpasta für sofortige Schmerzlinderung bei schmerzempfindlichen Zähnen, die hilft das natürliche Weiß der Zähne wiederherzustellen. Jetzt testen! <span>elmex® Sensitive Professional</span> Zahnpasta elmex® Sensitive Professional Zahnpasta, medizinische Zahnpasta für sofortige & anhaltende Schmerzlinderung bei schmerzempfindlichen Zähnen. Jetzt testen! Top Webseite Produkte Mundgesundheitstipps elmex für Schmerzempfindlichkeit Kariesschutz Kinderzahnpflege Zahnschmelzschutz Unser Unternehmen Hilfe & Kontakt Corporate Website Unsere Mission Für Fachkreise Rechtliches Impressum Datenschutzrichtlinie Nutzungsbedingungen Corporate Website Cookie-Einstellungstool Newsletter JETZT ANMELDEN Sie befinden sich auf der deutschen Seite > Ändern Besuchen Sie uns auf der ganzen Welt Argentina Austria - Österreich Belgium - Belgique Belgium - België Brazil - Brasil Chile Croatia - Hrvatska Czech Republic - Česká Republika Eesti Finland - Suomi France - France Germany - Deutschland Greater China - Hong Kong SAR Magyarország Italy - Italia Latvia - Latvija Lietuva Netherlands - Nederland Poland - Polska România Serbian (Serbia) Slovensko Slovenija Switzerland (Schweiz) Switzerland (Suisse) © 2024 CP GABA GmbH. Alle Rechte vorbehalten.",
          "domains": [
            "https://www.elmex.de"
          ],
          "title": "Schnelle Hilfe bei Zahnschmerzen | elmex®",
          "url": "https://www.elmex.de/articles/about-sensitive-teeth/schnelle-hilfe-bei-zahnschmerzen",
          "url_scheme": "https",
          "meta_description": "Zahnschmerzen können Ihren Tag völlig zum Stillstand kommen lassen und die Nahrungsaufnahme, das Schlafen oder die Konzentration erschweren. Wenn Ihre Zahnschmerzen sehr stark sind, kann es sich um einen zahnärztlichen Notfall handeln…",
          "headings": [
            "Schnelle Hilfe bei Zahnschmerzen",
            "Linderung von Zahnschmerzen bei zahnärztlichen Notfällen",
            "Was verursacht Zahnschmerzen?",
            "Behandlung von leichten Zahnschmerzen",
            "Behandlung starker Zahnschmerzen",
            "Mehr Artikel",
            "Schmerzen nach dem Bleaching oder der Zahnaufhellung?",
            "Empfindliche Zähne nach der professionellen Zahnreinigung",
            "Zahnschmerzen bei Kälte und Wärme - Was hilft?",
            "Schmerzen der Zahnnerven: Ursachen und Behandlung",
            "Was hilft gegen starke Zahnschmerzen?",
            "Was hilft gegen Zahnschmerzen - Hausmittel und Tipps",
            "Wenn der Zahn beim süßen Naschen weh tut, was tun?",
            "Wärme- und kälteempfindliche Zähne? Das können Sie dagegen tun!",
            "Zähneputzen bei empfindlichen Zähnen | elmex®",
            "So finden Sie die richtige Mundspülung für empfindliche Zähne",
            "Was verursacht schmerzempfindliche Zähne? | elmex®",
            "Empfindliche Zähne nach der Zahnaufhellung | elmex®",
            "Was tun bei Zahnfleischentzündung? Alle Infos zu Ursachen und Behandlung!",
            "Die ideale Zahnpasta für empfindliche Zähne | elmex®",
            "Freiliegende Zahnhälse? Was tun? Erfahren Sie hier alles!",
            "Schmerzempfindliche Zähne, was tun? 5 Tipps, die helfen!",
            "Zahnfüllungen: Arten, Behandlungen und alles was Sie wissen müssen",
            "Zahnschmerzen und empfindliche Zähne beim Kind, was tun?",
            "Ist ein schmerzempfindlicher Zahn etwas Ernstes? Alle Infos hier!"
          ],
          "links": [
            "https://colgate-germany.jebbit.com/gxant7it?L=Full+Page&JC=Website+Header",
            "https://www.colgatepalmolive.de/contact-us",
            "https://www.cpgabaprofessional.de/",
            "https://www.elmex.de/",
            "https://www.elmex.de/?triggerlightbox=true",
            "https://www.elmex.de/articles",
            "https://www.elmex.de/articles/about-sensitive-teeth",
            "https://www.elmex.de/articles/about-sensitive-teeth/schnelle-hilfe-bei-zahnschmerzen#",
            "https://www.elmex.de/articles/caries-protection",
            "https://www.elmex.de/articles/enamel-protection",
            "https://www.elmex.de/articles/kids-teeth",
            "https://www.elmex.de/fluoridrechner",
            "https://www.elmex.de/our-mission",
            "https://www.elmex.de/products-range",
            "https://www.elmex.de/products-range/caries-protection/elmex-gel",
            "https://www.elmex.de/products-range?type=Interdental",
            "https://www.elmex.de/products-range?type=Zahnb%C3%BCrste",
            "https://www.elmex.de/products-range?type=Zahnpasta",
            "https://www.elmex.de/products-range?type=Zahnsp%C3%BClung",
            "https://www.elmex.de/products-range?type=elmex%C2%AE%20Kariesschutz",
            "https://www.elmex.de/products-range?type=elmex%C2%AE%20Opti-schmelz",
            "https://www.elmex.de/products-range?type=elmex%C2%AE%20Sensitive",
            "https://www.elmex.de/products-range?type=elmex%C2%AE%20für%20Kinder",
            "https://www.elmex.de/risikorechner",
            "https://www.elmex.de/search"
          ],
          "id": "67a43c114121c9b57d33e234",
          "url_port": 443,
          "url_host": "www.elmex.de",
          "url_path_dir2": "about-sensitive-teeth",
          "url_path": "/articles/about-sensitive-teeth/schnelle-hilfe-bei-zahnschmerzen",
          "url_path_dir1": "articles"
        }
 */
