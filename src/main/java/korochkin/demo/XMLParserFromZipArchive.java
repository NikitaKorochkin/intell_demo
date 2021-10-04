package korochkin.demo;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class XMLParserFromZipArchive {
    private static final String URL = "https://www.nalog.gov.ru/opendata/";
    private static final int TOP_COMPANIES_COUNT = 5;

    public static void main(String[] args) {
        String mainPage = connect(URL);
        String zipLink = getZipArchiveLink(mainPage);
        SortedSet<Company> companySet = getAllCompaniesFromZipArchive(zipLink);
        companySet.forEach(System.out::println);
    }


    private static String connect(String url) {
        Document doc;
        String tmpUrl = null;
        try {
            doc = Jsoup.connect(URL).get();
            System.out.println("Connected to URL");
            Element table = doc.select("table").first();
            Elements tds = table.select("td");
            Element line = tds.get(86 * 4 - 3);
            Element a = line.select("a").first();
            String href = a.attr("href");
            System.out.println(href);
            tmpUrl = URL + href.replace("/opendata/", "");
        } catch (IOException e) {
            System.out.println("Connection failed");
            e.printStackTrace();
        }
        return tmpUrl;
    }

    private static String getZipArchiveLink(String url) {
        String zipUrl = null;
        try {
            Document zipDocument = Jsoup.connect(url).get();
            System.out.println("Connected to zip URL");
            Element tableWithLink = zipDocument.selectFirst("table");
            Elements tdsZip = tableWithLink.select("td");
            Element tdZip = tdsZip.get(8 * 3 - 1);
            Element aZip = tdZip.selectFirst("a");
            zipUrl = aZip.attr("href");
        } catch (IOException e) {
            System.out.println("Connection failed");
            e.printStackTrace();
        }
        return zipUrl;
    }

    private static SortedSet<Company> getAllCompaniesFromZipArchive(String link) {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader;
        SortedSet<Company> companies = new TreeSet<>();
        Company company;
        String name = null;
        String inn = null;
        String count = null;
        try {
            URL zip = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) zip.openConnection();
            connection.setRequestMethod("GET");
            InputStream inputStream = connection.getInputStream();
            ZipInputStream zipIn = new ZipInputStream(inputStream);
            ZipEntry entry = zipIn.getNextEntry();
            BufferedReader xmlFileReader = new BufferedReader(new InputStreamReader(zipIn));

            while (entry != null) {
                reader = factory.createXMLEventReader(new StringReader(xmlFileReader.readLine()));
                while (reader.hasNext()) {
                    XMLEvent event = reader.nextEvent();
                    if (event.isStartElement()) {
                        StartElement startElement = event.asStartElement();
                        if (startElement.getName().getLocalPart().equals("СведНП")) {
                            Attribute nameAttr = startElement.getAttributeByName(new QName("НаимОрг"));
                            Attribute innAttr = startElement.getAttributeByName(new QName("ИННЮЛ"));
                            name = nameAttr.getValue();
                            inn = innAttr.getValue();
                        }
                        if (startElement.getName().getLocalPart().equals("СведССЧР")) {
                            Attribute countAttr = startElement.getAttributeByName(new QName("КолРаб"));
                            count = countAttr.getValue();
                        }
                    }
                    if (event.isEndElement()) {
                        EndElement endElement = event.asEndElement();
                        if (endElement.getName().getLocalPart().equals("Документ")) {
                            company = new Company(name, inn, count);
                            companies.add(company);
                            if (companies.size() > TOP_COMPANIES_COUNT) {
                                companies.remove(companies.last());
                            }
                        }
                    }
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
            if (e.getMessage().contains("ParseError")) {
                System.out.println("Check xml document structure");
            }
        }
        return companies;
    }
}



