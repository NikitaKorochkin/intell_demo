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
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class XMLParserFromZipArchive {
    private static final String URL = "https://www.nalog.gov.ru/opendata/";

    public static void main(String[] args) {

        long before1 = System.currentTimeMillis();
        String mainPage = connect(URL);
        long after1 = System.currentTimeMillis();
        System.out.println("Method 1 finished at " + (after1 - before1));
        long before2 = System.currentTimeMillis();
        String zipLink = getZipArchiveLink(mainPage);
        long after2 = System.currentTimeMillis();
        System.out.println("Method 2 finished at " + (after2 - before2));
        long before3 = System.currentTimeMillis();
        List<String> xmls = getAllXMLs(zipLink);
        long after3 = System.currentTimeMillis();
        System.out.println("Method 3 finished at " + (after3 - before3));
        long before4 = System.currentTimeMillis();
        List<Company> companies = new ArrayList<>();
        for (String xml: xmls)  {
            companies.add(getCompany(xml));
        }
        long after4 = System.currentTimeMillis();
        System.out.println("Method 4 finished at " + (after4 - before4));

        companies.stream()
                .sorted(Company::compareTo)
                .limit(5)
                .forEach(System.out::println);
    }


    private static String connect(String url) {
        Document doc = null;
        String tmpUrl = null;
        try {
            doc = Jsoup.connect(URL).get();
            System.out.println("Connected");
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
            System.out.println("Connected");
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

    private static List<String> getAllXMLs(String link) {
        List<String> xmlFiles = new ArrayList<>();
        try {
            URL zip = new URL(link);
            HttpURLConnection connection = (HttpURLConnection) zip.openConnection();
            System.out.println("++++");
            connection.setRequestMethod("GET");
            InputStream inputStream = connection.getInputStream();
            System.out.println("++++");
            ZipInputStream zipIn = new ZipInputStream(inputStream);
            ZipEntry entry = zipIn.getNextEntry();
            BufferedReader xmlFileReader = new BufferedReader(new InputStreamReader(zipIn), 32786 * 2);


            while (entry != null) {
                xmlFiles.add(xmlFileReader.readLine());

                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }

            zipIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return xmlFiles;
    }

    private static Company getCompany(String xml)    {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;
        Company company = null;
        String name = null;
        String inn = null;
        String count = null;
        try {
            reader = factory.createXMLEventReader(new StringReader(xml));
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
                    }
                }
            }
                    reader.close();
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }

        return company;
    }
}



